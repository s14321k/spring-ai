package com.smartshop.customer.springbootai.config.rag;

import com.smartshop.customer.springbootai.advisors.TokenUsageAuditAdvisor;
import com.smartshop.customer.springbootai.rag.PIIMaskingDocumentPostProcessor;
import org.springframework.ai.chat.cache.semantic.SemanticCacheAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.preretrieval.query.transformation.TranslationQueryTransformer;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VectorSearchRagClientConfig {

    /* ── Shared infrastructure ── */

    @Bean
    ChatMemory chatMemory(JdbcChatMemoryRepository jdbcChatMemoryRepository) {
        return MessageWindowChatMemory.builder()
                .maxMessages(10)
                .chatMemoryRepository(jdbcChatMemoryRepository)
                .build();
    }

    /* ── Advisor 1: General RAG (loose retrieval, 3 docs, 0.5 threshold) ── */

    @Bean
    public RetrievalAugmentationAdvisor generalRetrievalAdvisor(OpenAiChatModel model, VectorStore vectorStore) {
        return RetrievalAugmentationAdvisor.builder()
                .queryTransformers(
                        TranslationQueryTransformer.builder()
                                .chatClientBuilder(ChatClient.create(model).mutate())
                                .targetLanguage("en")
                                .build())
                .documentRetriever(
                        VectorStoreDocumentRetriever.builder()
                                .vectorStore(vectorStore)
                                .topK(3)
                                .similarityThreshold(0.5)
                                .build())
                .documentPostProcessors(PIIMaskingDocumentPostProcessor.builder())
                .build();
    }

    @Bean
    public ChatClient vectorGeneralRAGChatClient(
            OpenAiChatModel model,
            ChatMemory chatMemory,
            RetrievalAugmentationAdvisor generalRetrievalAdvisor,
            SemanticCacheAdvisor redisSemanticCacheAdvisor) {

        Advisor loggerAdvisor = new SimpleLoggerAdvisor();
        Advisor tokenUsageAdvisor = new TokenUsageAuditAdvisor();
        Advisor memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();

        return ChatClient.builder(model)
                .defaultSystem("You are a general knowledge assistant. Answer using the retrieved context.")
                .defaultAdvisors(loggerAdvisor, memoryAdvisor, tokenUsageAdvisor,
                        generalRetrievalAdvisor, redisSemanticCacheAdvisor)
                .build();
    }

    /* ── Advisor 2: HR RAG (stricter retrieval, 5 docs, 0.7 threshold) ── */

    @Bean
    public RetrievalAugmentationAdvisor hrRetrievalAdvisor(OpenAiChatModel model, VectorStore vectorStore) {
        return RetrievalAugmentationAdvisor.builder()
                .queryTransformers(
                        TranslationQueryTransformer.builder()
                                .chatClientBuilder(ChatClient.create(model).mutate())
                                .targetLanguage("en")
                                .build())
                .documentRetriever(
                        VectorStoreDocumentRetriever.builder()
                                .vectorStore(vectorStore)
                                .topK(5)
                                .similarityThreshold(0.7)   // stricter for policy docs
                                .build())
                .documentPostProcessors(PIIMaskingDocumentPostProcessor.builder())
                .build();
    }

    @Bean
    public ChatClient vectorHrRAGChatClient(
            OpenAiChatModel model,
            ChatMemory chatMemory,
            RetrievalAugmentationAdvisor hrRetrievalAdvisor,
            SemanticCacheAdvisor redisSemanticCacheAdvisor) {

        Advisor loggerAdvisor = new SimpleLoggerAdvisor();
        Advisor tokenUsageAdvisor = new TokenUsageAuditAdvisor();
        Advisor memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();

        return ChatClient.builder(model)
                .defaultSystem("You are an HR policy assistant. Answer strictly from company HR documents.")
                .defaultAdvisors(loggerAdvisor, memoryAdvisor, tokenUsageAdvisor,
                        hrRetrievalAdvisor, redisSemanticCacheAdvisor)
                .build();
    }

    /* ── Plain client for MANUAL RAG (NO retrieval advisor → no double-fetch) ── */

    @Bean
    public ChatClient vectorManualChatClient(
            OpenAiChatModel model,
            ChatMemory chatMemory,
            SemanticCacheAdvisor redisSemanticCacheAdvisor) {

        Advisor loggerAdvisor = new SimpleLoggerAdvisor();
        Advisor tokenUsageAdvisor = new TokenUsageAuditAdvisor();
        Advisor memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();

        return ChatClient.builder(model)
                .defaultAdvisors(loggerAdvisor, memoryAdvisor, tokenUsageAdvisor, redisSemanticCacheAdvisor)
                .build();   // intentionally NO RetrievalAugmentationAdvisor
    }
}
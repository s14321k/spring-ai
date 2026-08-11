package com.smartshop.customer.springbootai.config.rag;

import com.smartshop.customer.springbootai.advisors.TokenUsageAuditAdvisor;
import com.smartshop.customer.springbootai.rag.websearch.SerperWebDocumentRetriever;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.util.List;

@Configuration
public class WebSearchRagClientConfig {

    /**
     * LLM backend: <b>Gemma (local Docker)</b> — {@link OpenAiChatModel} is wired to
     * {@code spring.ai.openai.chat.base-url} ({@code ${IP_ADDRESS}:12434/engines/v1}), not OpenAI cloud.
     *
     * <p>RAG context comes from {@link SerperWebDocumentRetriever} (Serper web-search HTTP API).
     * That retriever is not an LLM; only the final answer generation hits Gemma.</p>
     */
    @Bean
    public ChatClient webSearchRAGChatClient(
            OpenAiChatModel model,
            ChatMemory chatMemory,
            RestClient.Builder restClientBuilder) {

        Advisor loggerAdvisor = new SimpleLoggerAdvisor();
        Advisor tokenUsageAdvisor = new TokenUsageAuditAdvisor();
        Advisor memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();

        var webSearchRAGAdvisor = RetrievalAugmentationAdvisor.builder()
                .documentRetriever(
                        SerperWebDocumentRetriever.builder()
                                .restClientBuilder(restClientBuilder)
                                .maxResults(5)
                                .build())
                .build();

        return ChatClient.builder(model)
                .defaultAdvisors(List.of(
                        loggerAdvisor,
                        memoryAdvisor,
                        tokenUsageAdvisor,
                        webSearchRAGAdvisor))
                .build();
    }
}
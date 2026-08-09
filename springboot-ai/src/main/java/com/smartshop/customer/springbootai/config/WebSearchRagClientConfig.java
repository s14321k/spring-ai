package com.smartshop.customer.springbootai.config;

import com.smartshop.customer.springbootai.advisors.TokenUsageAuditAdvisor;
import com.smartshop.customer.springbootai.rag.GoogleWebSearchDocumentRetriever;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Configures a {@link ChatClient} dedicated to <b>web-search RAG</b>.
 *
 * <p>Unlike {@code ChatClientConfig#chatMemoryClient}, which retrieves context from a
 * local {@code VectorStore}, this client uses {@link GoogleWebSearchDocumentRetriever} (Google Custom Search)
 * so every prompt is augmented with live web results before the model call.</p>
 *
 * <p><b>Why a separate config/bean?</b> Vector RAG and web RAG need different
 * {@code DocumentRetriever}s. Keeping them as two named clients
 * ({@code chatMemoryClient} vs {@code webSearchRAGChatClient}) lets controllers pick
 * the source of truth without mixing retrievers on one bean.</p>
 *
 * <p><b>Default advisors:</b> logging, conversation memory, token audit, and
 * {@link RetrievalAugmentationAdvisor} wired to Google Custom Search ({@code maxResults=5}).</p>
 *
 * @see GoogleWebSearchDocumentRetriever
 * @see SystemUserChatClientConfig
 */
@Configuration
public class WebSearchRagClientConfig {

    /**
     * Creates and configures a ChatClient bean that combines chat memory, logging,
     * token usage tracking, and web-search-based Retrieval-Augmented Generation (RAG).
     *
     * This client uses Google Custom Search (via GoogleWebSearchDocumentRetriever) instead of a local
     * vector store to fetch relevant documents for augmenting the AI's responses.
     * The advisors are applied in order: logger → memory → token usage → web search RAG.
     *
     * @param model                the GoogleGenAiChatModel to use for generating responses
     * @param chatMemory           the memory store for maintaining conversation history
     * @param restClientBuilder    the HTTP client builder used by the web search retriever
     * @return a fully configured ChatClient bean
     */
    @Bean                                                          // Registers this method's return value as a Spring bean
    public ChatClient webSearchRAGChatClient(GoogleGenAiChatModel model,   // The AI model that will generate responses
                                             ChatMemory chatMemory,        // Stores past conversation turns for context
                                             RestClient.Builder restClientBuilder) {  // HTTP client for making web search calls

        Advisor loggerAdvisor = new SimpleLoggerAdvisor();          // Logs requests and responses for debugging
        Advisor tokenUsageAdvisor = new TokenUsageAuditAdvisor();   // Tracks how many tokens are consumed per call
        Advisor memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();  // Remembers conversation history

        // Web-search equivalent of ChatClientConfig.retrievalAugmentationAdvisor:
        // same advisor type, different DocumentRetriever (Tavily instead of VectorStore).
        var webSearchRAGAdvisor = RetrievalAugmentationAdvisor.builder()  // Builds the RAG advisor that fetches external docs
                .documentRetriever(GoogleWebSearchDocumentRetriever.builder()       // Uses web search (Tavily) instead of vector DB
                        .restClientBuilder(restClientBuilder).maxResults(5).build())  // Sets HTTP client and limits to 5 search results
                .build();                                               // Finalizes the web search RAG advisor

        return ChatClient.builder(model)                              // Starts building the ChatClient with the given AI model
                .defaultAdvisors(List.of(loggerAdvisor, memoryAdvisor, tokenUsageAdvisor,  // Adds all advisors in execution order
                        webSearchRAGAdvisor))
                .build();                                             // Creates the final ChatClient bean
    }
}

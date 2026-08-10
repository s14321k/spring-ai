package com.smartshop.customer.springbootai.config;

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

    /**
     * Creates a <b>stateful</b> {@link ChatClient} configured with conversation
     * memory, retrieval-augmented generation (RAG), logging, and token usage auditing.
     *
     * <p>The client is configured with the following advisors:</p>
     * <ul>
     *   <li>{@link SimpleLoggerAdvisor} — logs every request and response.</li>
     *   <li>{@link MessageChatMemoryAdvisor} — persists and replays conversation
     *       history so the model retains context across multiple turns.</li>
     *   <li>{@link TokenUsageAuditAdvisor} — records token consumption for each request.</li>
     *   <li>{@link RetrievalAugmentationAdvisor} — automatically retrieves relevant
     *       documents from the configured {@link VectorStore} and injects them into
     *       the prompt before it is sent to the model.</li>
     * </ul>
     *
     * <p>The {@link ChatMemory} stores conversation history using the configured
     * {@code ChatMemoryRepository}. Conversations are isolated using the
     * {@code CONVERSATION_ID} advisor parameter supplied at runtime.</p>
     *
     * @param model the {@link OpenAiChatModel} used to generate responses
     * @param chatMemory the conversation memory implementation
     * @param retrievalAugmentationAdvisor the advisor that performs automatic
     *        retrieval of relevant documents for Retrieval-Augmented Generation (RAG)
     * @return a configured {@link ChatClient} supporting conversational memory,
     *         automatic document retrieval, request logging, and token auditing
     */
    @Bean
    public ChatClient chatMemoryClient(OpenAiChatModel model, ChatMemory chatMemory,
                                       RetrievalAugmentationAdvisor retrievalAugmentationAdvisor,
                                       SemanticCacheAdvisor  semanticCacheAdvisor) {
        Advisor loggerAdvisor = new SimpleLoggerAdvisor();
        Advisor tokenUsageAdvisor = new TokenUsageAuditAdvisor();
        Advisor memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();

        return ChatClient.builder(model)
                .defaultAdvisors(loggerAdvisor, memoryAdvisor, tokenUsageAdvisor, retrievalAugmentationAdvisor, semanticCacheAdvisor)
                .build();
    }

    /**
     * Creates a {@link ChatMemory} bean that persists conversation history in a
     * relational database via {@link JdbcChatMemoryRepository}.
     *
     * <p>{@link MessageWindowChatMemory} is used here with a {@code maxMessages(10)}
     * sliding window. This means only the <b>10 most recent</b> messages are kept in
     * context. Older messages are silently dropped, which:</p>
     * <ul>
     *   <li>Keeps token usage predictable and bounded</li>
     *   <li>Prevents the prompt from growing indefinitely on long conversations</li>
     *   <li>Reduces cost and latency for each subsequent LLM call</li>
     * </ul>
     *
     * <p>Because the repository is JDBC-backed (not in-memory), conversations are
     * durable across restarts and shareable between horizontally scaled app instances.</p>
     *
     * @param jdbcChatMemoryRepository the JDBC repository that handles CRUD of
     *                                 chat messages in the underlying SQL database
     * @return a sliding-window chat memory backed by persistent JDBC storage
     */
    @Bean
    ChatMemory chatMemory(JdbcChatMemoryRepository jdbcChatMemoryRepository) {
        return MessageWindowChatMemory.builder()
                .maxMessages(10)
                .chatMemoryRepository(jdbcChatMemoryRepository)
                .build();
    }

    /**
     * Creates the {@link RetrievalAugmentationAdvisor} used for Retrieval-Augmented
     * Generation (RAG).
     *
     * <p>The advisor automatically performs a similarity search against the configured
     * {@link VectorStore} for every user prompt before it is sent to the AI model.
     * The retrieved documents are then injected into the prompt as additional context,
     * eliminating the need for manual VectorStore searches in application code.
     *
     * <p>Additional features:
     * <ul>
     *     <li><b>Query Translation</b> - Automatically translates non-English user
     *     queries to English before vector search.</li>
     *     <li><b>PII Masking</b> - Redacts personally identifiable information from
     *     retrieved documents before they are sent to the AI model.</li>
     * </ul>
     *
     * <p>Retriever configuration:
     * <ul>
     *     <li><b>topK = 3</b> - Retrieves up to three most relevant documents.</li>
     *     <li><b>similarityThreshold = 0.5</b> - Ignores documents with similarity
     *     scores below 50%.</li>
     * </ul>
     *
     * @param vectorStore the vector database used to retrieve semantically similar documents
     * @param model       the OpenAI chat model used to power the translation transformer
     * @return a configured {@link RetrievalAugmentationAdvisor} for automatic RAG
     */
    @Bean                                                          // Exposes this advisor as a Spring bean
    RetrievalAugmentationAdvisor retrievalAugmentationAdvisor(OpenAiChatModel model, VectorStore vectorStore) {  // Injects vector DB and OpenAI model
        return RetrievalAugmentationAdvisor.builder()               // Starts building the RAG advisor
                // Pre-processing / Pre-Retrieval
                .queryTransformers(TranslationQueryTransformer.builder()  // Adds a query translation step
//                        .chatClientBuilder(ChatClient.builder(model))  // Both bottom and this are same
                        .chatClientBuilder(ChatClient.create(model).mutate())     // Creates a mutable chat client from the model
                        .targetLanguage("en")                       // Translates all queries to English before searching
                        .build())                                   // Finishes the transformer setup
                .documentRetriever(                                 // Configures where to fetch documents from
                        VectorStoreDocumentRetriever.builder()          // Uses vector store as the document source
                                .vectorStore(vectorStore)               // Sets the injected vector database
                                .topK(3)                                // Fetches top 3 most similar documents
                                .similarityThreshold(0.5)               // Ignores matches below 50% similarity
                                .build()                                // Finishes the retriever setup
                )
                // Post-processing / Post-Retrieval
                .documentPostProcessors(PIIMaskingDocumentPostProcessor.builder()) // Masks PII (e.g., emails, phone numbers) in retrieved docs
                .build();                                           // Creates the final RAG advisor
    }
}

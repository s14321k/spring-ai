package com.smartshop.customer.springbootai.controller;

import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.*;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

@RestController
@RequestMapping("/rag-api")
public class RagController {
    private final ChatClient chatMemoryClient;

    /**
     * Retained only as a reference for the former manual RAG path (see commented code in
     * {@link #getString}). Document retrieval is now automatic via
     * {@code RetrievalAugmentationAdvisor} registered on {@code chatMemoryClient} in
     * {@code ChatClientConfig} — controllers no longer call {@link VectorStore} directly.
     */
    @SuppressWarnings("unused")
    private final VectorStore vectorStore;

    private final ChatClient webSearchRAGChatClient;

    public RagController(ChatClient chatMemoryClient, VectorStore vectorStore, ChatClient webSearchRAGChatClient) {
        this.chatMemoryClient = chatMemoryClient;
        this.vectorStore = vectorStore;
        this.webSearchRAGChatClient = webSearchRAGChatClient;
    }

    @Value("classpath:/promtTemplates/systemPromptRandomDataTemplate.st")
    private Resource systemPromptRandomDataTemplate;

    /**
     * Handles GET requests to "/random-chat" to provide AI-generated responses
     * based on vector-store similarity search.
     * <p>
     * Flow:
     * 1. Receives username (from header) and user message (from query param).
     * 2. Builds a similarity search request to find top 3 documents matching the message
     *    with at least 0.5 similarity score.
     * 3. Joins the retrieved documents' text into a single context string.
     * 4. Sends a prompt to the AI chat client, injecting the similar documents as context
     *    into the system prompt and tagging the conversation with the username.
     * 5. Returns the AI-generated response content.
     *
     * @param username the user's identifier passed in the request header
     * @param message  the user's input message passed as a query parameter
     * @return the AI-generated chat response
     */
    @GetMapping("/random-chat")                                        // Maps HTTP GET requests to "/random-chat" endpoint
    public String randomChat(@RequestHeader("username") String username,  // Extracts "username" value from request headers
                             @RequestParam("message") String message) {   // Extracts "message" value from query parameters

        return getString(username, message, systemPromptRandomDataTemplate);
    }

    @Value("classpath:/promtTemplates/hrSystemPromptTemplatePdf.st")
    private Resource hrSystemPromptTemplatePdf;

    @GetMapping("/document-chat")                                       // Maps HTTP GET requests to "/random-chat" endpoint
    public String documentChat(@RequestHeader("username") String username,  // Extracts "username" value from request headers
                             @RequestParam("message") String message) {   // Extracts "message" value from query parameters

        return getString(username, message, hrSystemPromptTemplatePdf);
    }

    /**
     * Sends a user message to the AI model and returns the generated response.
     *
     * <p>Note:
     * The manual VectorStore similarity search previously performed in this method
     * has been commented out because retrieval is now handled automatically by
     * {@link RetrievalAugmentationAdvisor}. The advisor executes the configured
     * similarity search (topK = 3, similarityThreshold = 0.5) and injects the
     * retrieved documents into the prompt before it is sent to the model.
     *
     * <p>This method is now responsible only for:
     * <ul>
     *     <li>Passing the conversation ID for chat memory.</li>
     *     <li>Sending the user's message.</li>
     *     <li>Returning the model's response.</li>
     * </ul>
     */
    @Nullable
    private String getString(String username,
                             String message,
                             Resource systemPromptTemplate) {
        // -------------------------------------------------------------------------
        // COMMENTED OUT — replaced by ChatClientConfig.retrievalAugmentationAdvisor
        // -------------------------------------------------------------------------
        // Before: this controller manually built a VectorStore SearchRequest, ran
        // similaritySearch, joined document text, and injected it into a system
        // prompt template via .system(...).param("documents", similarContext).
        //
        // After: chatMemoryClient is built in ChatClientConfig with a default
        // RetrievalAugmentationAdvisor that uses VectorStoreDocumentRetriever
        // (topK=3, similarityThreshold=0.5). On every .call(), the advisor:
        //   1) searches the VectorStore with the user message
        //   2) injects matching documents into the prompt automatically
        // So the manual search + context join + .system(documents=...) below is
        // redundant and would double-retrieve / double-inject context if enabled.
        // Keep this block as a learning reference for the pre-advisor RAG style.
        // -------------------------------------------------------------------------
//        SearchRequest searchRequest = SearchRequest.builder()       // Starts building a search request object
//                .query(message)                                     // Sets the search query to the user's message
//                .topK(3)                                            // Limits results to top 3 most similar documents
//                .similarityThreshold(0.5)                           // Filters out matches below 50% similarity
//                .build();                                           // Finalizes and creates the search request
//
//        List<Document> similarDocs = vectorStore.similaritySearch(searchRequest);  // Queries vector database for similar documents
//
//        String similarContext = similarDocs.stream()                // Starts processing the list of found documents
//                .map(Document::getText)                             // Extracts text content from each document
//                .collect(Collectors.joining(System.lineSeparator())); // Joins all texts with new lines into one string

        return chatMemoryClient.prompt()                            // Begins building an AI prompt request
                // COMMENTED OUT — same reason as above: document context is no longer
                // passed through a system prompt template. RetrievalAugmentationAdvisor
                // on chatMemoryClient (ChatClientConfig) supplies RAG context.
                // systemPromptTemplate is still injected into this method for the
                // random-chat / document-chat endpoints as a historical parameter;
                // re-enable .system(...) only if you need custom template wording
                // *in addition to* (or instead of) the advisor-based RAG path.
                /*.system(                                            // Configures the system-level instructions
                        promptSystemSpec -> promptSystemSpec.text(systemPromptTemplate)  // Uses predefined system prompt template
                                .param("documents", similarContext))*/ // Injects the found documents into the template placeholder
                .advisors(a -> a.param(CONVERSATION_ID, username))  // Tags conversation with username for memory tracking
                .user(message)                                      // Sets the user input message
                .call()                                             // Sends the prompt to the AI model
                .content();                                         // Returns only the text content of the AI response
    }

    @GetMapping("/web-search")
    public String webSearchChat(@RequestHeader("username") String username,
                                 @RequestParam("message") String message
                             ) {
        String answer = webSearchRAGChatClient.prompt()
                .advisors(a -> a.param(CONVERSATION_ID, username))
                .user(message)
                .call()
                .content();
        return answer;
    }
}

package com.smartshop.customer.springbootai.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

@RestController
@RequestMapping("/rag-api")
public class RagController {
    private final ChatClient chatMemoryClient;
    private final VectorStore vectorStore;

    @Value("classpath:/promtTemplates/systemPromptRandomDataTemplate.st.st")
    private Resource systemPromptRandomDataTemplate;

    public RagController(ChatClient chatMemoryClient, VectorStore vectorStore) {
        this.chatMemoryClient = chatMemoryClient;
        this.vectorStore = vectorStore;
    }

    @GetMapping("/random-chat")
    public String randomChat(@RequestHeader("username" ) String username,
                             @RequestParam("message") String message) {
        SearchRequest searchRequest = SearchRequest.builder().query(message).topK(3).similarityThreshold(0.5).build();
        List<Document> similarDocs = vectorStore.similaritySearch(searchRequest);
        String similarContext = similarDocs.stream().map(Document::getText).collect(Collectors.joining(System.lineSeparator()));
        return chatMemoryClient.prompt()
                .system(
                promptSystemSpec -> promptSystemSpec.text(systemPromptRandomDataTemplate)
                                .param("documents", similarContext))    // documents is what we have mentioned in the systemPromptRandomDataTemplate file
                .advisors(a -> a.param(CONVERSATION_ID, username))
                .user(username)
                .call()
                .content();
    }
}

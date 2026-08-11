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
@RequestMapping("/rag-api/manual")
public class ManualRagController {

    private final ChatClient vectorManualChatClient;   // NO RAG advisor
    private final VectorStore vectorStore;

    @Value("classpath:/promtTemplates/systemPromptRandomDataTemplate.st")
    private Resource systemPromptRandomDataTemplate;

    @Value("classpath:/promtTemplates/hrSystemPromptTemplatePdf.st")
    private Resource hrSystemPromptTemplatePdf;

    public ManualRagController(ChatClient vectorManualChatClient, VectorStore vectorStore) {
        this.vectorManualChatClient = vectorManualChatClient;
        this.vectorStore = vectorStore;
    }

    @GetMapping("/random-chat")
    public String randomChatDirect(@RequestHeader("username") String username,
                                   @RequestParam("message") String message) {
        return getStringManualRag(username, message, systemPromptRandomDataTemplate);
    }

    @GetMapping("/document-chat")
    public String documentChatDirect(@RequestHeader("username") String username,
                                     @RequestParam("message") String message) {
        return getStringManualRag(username, message, hrSystemPromptTemplatePdf);
    }

    private String getStringManualRag(String username,
                                      String message,
                                      Resource systemPromptTemplate) {
        SearchRequest searchRequest = SearchRequest.builder()
                .query(message)
                .topK(3)
                .similarityThreshold(0.5)
                .build();

        List<Document> similarDocs = vectorStore.similaritySearch(searchRequest);

        String similarContext = similarDocs.stream()
                .map(Document::getText)
                .collect(Collectors.joining(System.lineSeparator()));

        return vectorManualChatClient.prompt()
                .system(spec -> spec
                        .text(systemPromptTemplate)
                        .param("documents", similarContext))
                .advisors(a -> a.param(CONVERSATION_ID, username))
                .user(message)
                .call()
                .content();
    }
}
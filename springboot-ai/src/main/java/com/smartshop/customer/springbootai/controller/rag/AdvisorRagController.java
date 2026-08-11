package com.smartshop.customer.springbootai.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

@RestController
@RequestMapping("/rag-api/advisor")
public class AdvisorRagController {

    private final ChatClient vectorGeneralRAGChatClient;
    private final ChatClient vectorHrRAGChatClient;

    public AdvisorRagController(ChatClient vectorGeneralRAGChatClient,
                                ChatClient vectorHrRAGChatClient) {
        this.vectorGeneralRAGChatClient = vectorGeneralRAGChatClient;
        this.vectorHrRAGChatClient = vectorHrRAGChatClient;
    }

    /**
     * General advisor-based RAG.
     * Uses generalRetrievalAdvisor (topK=3, threshold=0.5) with a casual persona.
     */
    @GetMapping("/random-chat")
    public String randomChat(@RequestHeader("username") String username,
                             @RequestParam("message") String message) {
        return vectorGeneralRAGChatClient.prompt()
                .advisors(a -> a.param(CONVERSATION_ID, username))
                .user(message)
                .call()
                .content();
    }

    /**
     * HR advisor-based RAG.
     * Uses hrRetrievalAdvisor (topK=5, threshold=0.7) with a formal HR persona.
     */
    @GetMapping("/document-chat")
    public String documentChat(@RequestHeader("username") String username,
                               @RequestParam("message") String message) {
        return vectorHrRAGChatClient.prompt()
                .advisors(a -> a.param(CONVERSATION_ID, username))
                .user(message)
                .call()
                .content();
    }
}
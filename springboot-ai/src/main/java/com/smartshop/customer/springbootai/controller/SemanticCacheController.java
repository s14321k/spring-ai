package com.smartshop.customer.springbootai.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/semantic-cache-api")
public class SemanticCacheController {

    private final ChatClient openSemanticChatClient;

    public SemanticCacheController(ChatClient openSemanticChatClient) {
        this.openSemanticChatClient = openSemanticChatClient;
    }

    @GetMapping("/cache-chat")
    public String cacheChat(@RequestParam("message") String message) {
        return openSemanticChatClient
                .prompt()
                .user(message)
                .call()
                .content();
    }
}

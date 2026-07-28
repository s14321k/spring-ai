package com.smartshop.customer.springbootai.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/multi-model-chat")
public class MultiModelChatController {

    private final ChatClient openAiChatClient;
    private final ChatClient ollamaChatClient;

    MultiModelChatController(@Qualifier("openAiChatClient") ChatClient openAiChatClient, @Qualifier("ollamaChatClient") ChatClient ollamaChatClient) {
        this.openAiChatClient = openAiChatClient;
        this.ollamaChatClient = ollamaChatClient;
    }

    @GetMapping("/open-ai-chat")
    public String openAiChat(@RequestParam(value = "message", required = true) String message) {
        return openAiChatClient.prompt(message).call().content();
    }

    @GetMapping("/ollama-ai-chat")
    public String ollamaAiChat(@RequestParam(value = "message", required = true) String message) {
        return ollamaChatClient.prompt(message).call().content();
    }
}

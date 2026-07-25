package com.smartshop.customer.springbootai.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ChatController {

//    This is for gemini config

//    private final GoogleGenAiChatModel chatClient;
//
//    public ChatController(GoogleGenAiChatModel genAiChatModel) {
//        this.chatClient = genAiChatModel;
//    }
//
//    @GetMapping("/chat")
//    public ResponseEntity<String> sendMessage(@RequestParam("message") String message) {
//        return ResponseEntity.ok(chatClient.call(message));
//    }

    private final ChatClient chatClient;

    public ChatController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @GetMapping("/chat")
    public ResponseEntity<String> sendMessage(@RequestParam("message") String message) {
        return ResponseEntity.ok(chatClient.prompt(message).call().content());
    }
}

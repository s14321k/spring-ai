package com.smartshop.customer.springbootai.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
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

    private final ChatClient openAiChatClient;

    public ChatController(@Qualifier("openAiChatClient") ChatClient ChatClient) {
        this.openAiChatClient = ChatClient;
    }

    @GetMapping("/chat")
    public ResponseEntity<String> sendMessage(@RequestParam("message") String message) {
        return ResponseEntity.ok(openAiChatClient.prompt(message).call().content());
    }

    @GetMapping("/system-chat")
    public ResponseEntity<String> sendSystemMessage(@RequestParam("message") String message) {
        return ResponseEntity.ok(openAiChatClient.prompt()
                .system("""
                        You are an internal HR assistant.\s
                        You will answer questions about HR policies, benefits, and procedures.\s
                        If user asks for help with anything outside of these topics, kindly inform them that you can only assist with queries related to HR policies.\s
                        If you don't know the answer, respond with "I'm not sure about that.\s
                        Please contact HR for assistance.
                        """)
                .user(message)
                .call()
                .content());
    }
}

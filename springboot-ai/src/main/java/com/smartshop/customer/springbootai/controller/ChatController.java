package com.smartshop.customer.springbootai.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
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

    private final ChatClient defaultSystemUserChatClient;

    public ChatController(@Qualifier("openAiChatClient") ChatClient ChatClient, @Qualifier("defaultSystemUserChatClient") ChatClient defaultSystemUserChatClient) {
        this.openAiChatClient = ChatClient;
        this.defaultSystemUserChatClient = defaultSystemUserChatClient;
    }

//    This is moved to ChatClientConfig.java class

//    public ChatController(ChatClient.Builder clientBuilder) {
//        this.openAiChatClient = clientBuilder
//                .defaultSystem("""
//                        You are an internal HR assistant.\s
//                        You will answer questions about HR policies, benefits, and procedures.\s
//                        If user asks for help with anything outside of these topics, kindly inform them that you can only assist with queries related to HR policies.\s
//                        If you don't know the answer, respond with "I'm not sure about that.\s
//                        Please contact HR for assistance.
//                        """)
//                .defaultUser("How can you help me?")
//                .build();
//    }


    @GetMapping("/chat")
    public ResponseEntity<String> sendMessage(@RequestParam("message") String message) {
        return ResponseEntity.ok(openAiChatClient.prompt(message).call().content());
    }

    @GetMapping("/system-chat")
    public ResponseEntity<String> sendSystemMessage(@RequestParam("message") String message) {
        return ResponseEntity.ok(defaultSystemUserChatClient
                .prompt()
        // ------------------Moved to constructor level using default-------------------
        // ------------------We can leave this as it is. But it will override the constructor level prompt--------------------
//                .system("""
//                        You are an internal HR assistant.\s
//                        You will answer questions about HR policies, benefits, and procedures.\s
//                        If user asks for help with anything outside of these topics, kindly inform them that you can only assist with queries related to HR policies.\s
//                        If you don't know the answer, respond with "I'm not sure about that.\s
//                        Please contact HR for assistance.
//                        """)
        // ------------------This can be commneted if you want default user message which is in constructor default user method
                .user(message)
                .call()
                .content());
    }

//    Using this in the promtTemplate.st file
//    To overide this curly braces {}, we can use TemplateRender which is in spring AI page.
//    String promtTemplate =
//            """
//                A customer named {customerName} sent the following message:
//                "{customerMessage}"
//
//                Write a polite and helpful email response addressing the issue.
//                Maintain a professional tone and provide reassurance.
//
//                Respond as if you're writing the email body only. Don't include subject, signature
//                """;

    @Value("classpath:/promtTemplates/userPromptTemplate.st")
    private Resource userPromptTemplate;

    @GetMapping("/email")
    public ResponseEntity<String> sendEmail(@RequestParam("customerName") String customerName,
            @RequestParam("customerMessage") String customerMessage) {
        return ResponseEntity.ok(openAiChatClient
                .prompt()
                .system("""
                        You are a professional customer service assistant which helps drafting email
                        responses to improve the productivity of the customer support team
                        """)
                .user(promtTemplateSpec ->
                        promtTemplateSpec.text(userPromptTemplate)
                                .param("customerName", customerName)
                                .param("customerMessage", customerMessage))
                .call()
                .content());
    }

    @Value("classpath:/promtTemplates/systemPromptTemplate.st")
    private Resource systemPromptTemplate;

    @GetMapping("/promt-stuffing")
    public ResponseEntity<String> promptStuffing(@RequestParam("message") String message) {
        return ResponseEntity.ok(defaultSystemUserChatClient
                .prompt()
                .system(systemPromptTemplate)
                .user(message)
                .call()
                .content());
    }
}

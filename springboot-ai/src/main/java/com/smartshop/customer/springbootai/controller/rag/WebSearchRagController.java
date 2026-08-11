package com.smartshop.customer.springbootai.controller.rag;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

@RestController
@RequestMapping("/rag-api/web")
public class WebSearchRagController {

    private final ChatClient webSearchRAGChatClient;

    public WebSearchRagController(ChatClient webSearchRAGChatClient) {
        this.webSearchRAGChatClient = webSearchRAGChatClient;
    }

    @GetMapping("/search")
    public String webSearchChat(@RequestHeader("username") String username,
                                @RequestParam("message") String message) {
        return webSearchRAGChatClient.prompt()
                .advisors(a -> a.param(CONVERSATION_ID, username))
                .user(message)
                .call()
                .content();
    }
}
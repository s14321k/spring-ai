package com.smartshop.customer.springbootai.controller;

import com.smartshop.customer.springbootai.config.cache.CacheTrackerConfig;
import com.smartshop.customer.springbootai.dto.ChatResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/semantic-cache-api")
public class SemanticCacheController {

    private final ChatClient openRedisSemanticChatClient;
    private final ChatClient openVectorSemanticChatClient;
    private final ChatClient openRedisSemanticChatClientRes;
    private final ChatClient openVectorSemanticChatClientRes;

    public SemanticCacheController(ChatClient openRedisSemanticChatClient, ChatClient openVectorSemanticChatClient, ChatClient openRedisSemanticChatClientRes, ChatClient openVectorSemanticChatClientRes) {
        this.openRedisSemanticChatClient = openRedisSemanticChatClient;
        this.openVectorSemanticChatClient = openVectorSemanticChatClient;
        this.openRedisSemanticChatClientRes = openRedisSemanticChatClientRes;
        this.openVectorSemanticChatClientRes = openVectorSemanticChatClientRes;
    }

    @GetMapping("/cache-redis-chat")
    public String cacheRedisChat(@RequestParam("message") String message) {
        return openRedisSemanticChatClient
                .prompt()
                .user(message)
                .call()
                .content();
    }

    @GetMapping("/cache-vector-chat")
    public String cacheVectorChat(@RequestParam("message") String message) {
        return openVectorSemanticChatClient
                .prompt()
                .user(message)
                .call()
                .content();
    }

    @GetMapping("/cache-redis-chat-response")
    public ChatResponse cacheRedisChatRes(@RequestParam("message") String message) {
        return getChatResponse(message, openRedisSemanticChatClientRes);
    }

    @GetMapping("/cache-vector-chat-response")
    public ChatResponse cacheVectorChatRes(@RequestParam("message") String message) {
        return getChatResponse(message, openVectorSemanticChatClientRes);
    }

    @NonNull
    private ChatResponse getChatResponse(@RequestParam("message") String message, ChatClient openSemanticChatClientRes) {
        long start = System.currentTimeMillis();
        try {
            String content = openSemanticChatClientRes
                    .prompt()
                    .user(message)
                    .call()
                    .content();

            String source = CacheTrackerConfig.getResolvedSource(); // "qdrant-cache" or "llm"
            return new ChatResponse(content, source, System.currentTimeMillis() - start);
        } finally {
            CacheTrackerConfig.clear();
        }
    }
}

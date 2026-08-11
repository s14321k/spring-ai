package com.smartshop.customer.springbootai.controller.cache;

import com.smartshop.customer.springbootai.config.cache.CacheTrackerConfig;
import com.smartshop.customer.springbootai.dto.ChatResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/semantic-cache-api/redis")
public class RedisSemanticCacheController {

    private final ChatClient openRedisSemanticChatClient;
    private final ChatClient openRedisSemanticChatClientRes;

    public RedisSemanticCacheController(ChatClient openRedisSemanticChatClient,
                                        ChatClient openRedisSemanticChatClientRes) {
        this.openRedisSemanticChatClient = openRedisSemanticChatClient;
        this.openRedisSemanticChatClientRes = openRedisSemanticChatClientRes;
    }

    // Plain text response — hits Redis semantic cache before calling LLM.
    @GetMapping("/chat")
    public String chat(@RequestParam("message") String message) {
        return openRedisSemanticChatClient
                .prompt()
                .user(message)
                .call()
                .content();
    }

    // Wrapped response with cache hit source ("redis-cache" or "llm") + latency.
    @GetMapping("/chat-response")
    public ChatResponse chatResponse(@RequestParam("message") String message) {
        long start = System.currentTimeMillis();
        try {
            String content = openRedisSemanticChatClientRes.prompt().user(message).call().content();
            String source = CacheTrackerConfig.getResolvedSource();
            return new ChatResponse(content, source, System.currentTimeMillis() - start);
        } finally {
            CacheTrackerConfig.clear();
        }
    }
}
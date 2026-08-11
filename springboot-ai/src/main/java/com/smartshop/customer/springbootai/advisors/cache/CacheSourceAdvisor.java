package com.smartshop.customer.springbootai.advisors.cache;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.core.Ordered;
import reactor.core.publisher.Flux;

import com.smartshop.customer.springbootai.config.cache.CacheTrackerConfig;

@Slf4j
public class CacheSourceAdvisor implements CallAdvisor, StreamAdvisor {

    private final String cacheType; // "redis" or "qdrant"

    public CacheSourceAdvisor(String cacheType) {
        this.cacheType = cacheType;
    }

    @Override
    public @NonNull ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        String userMessage = request.prompt().getUserMessage().getText();
        log.info("[{}] >>> ENTER advisor. User message: {}", cacheType, userMessage);

        CacheTrackerConfig.setCurrentCacheType(cacheType);
        try {
            ChatClientResponse response = chain.nextCall(request);

            boolean llmCalled = CacheTrackerConfig.isLlmCalled();
            String source = llmCalled ? "llm" : cacheType + "-cache";
            CacheTrackerConfig.setResolvedSource(source);

            log.info("[{}] <<< EXIT advisor. LLM called = {}, source = {}", cacheType, llmCalled, source);
            return response;
        } finally {
            // Don't clear yet
        }
    }

    @Override
    public @NonNull Flux<ChatClientResponse> adviseStream(@NonNull ChatClientRequest request, StreamAdvisorChain chain) {
        CacheTrackerConfig.setCurrentCacheType(cacheType);
        return chain.nextStream(request)
                .doOnNext(resp -> {
                    String source = CacheTrackerConfig.isLlmCalled() ? "llm" : cacheType + "-cache";
                    CacheTrackerConfig.setResolvedSource(source);
                });
    }

    @Override
    public @NonNull String getName() {
        return "CacheSourceAdvisor-" + cacheType;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
package com.smartshop.customer.springbootai.advisors.cache;

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

import java.util.logging.Logger;

public class LLMCallMarkerAdvisor implements CallAdvisor, StreamAdvisor {

    Logger log = Logger.getLogger(LLMCallMarkerAdvisor.class.getName());

    @Override
    public @NonNull ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        log.info("[MARKER] >>> LLM CALL MARKED - This means cache MISSED");
        CacheTrackerConfig.markLlmCalled();
        return chain.nextCall(request);
    }

    @Override
    public @NonNull Flux<ChatClientResponse> adviseStream(@NonNull ChatClientRequest request, StreamAdvisorChain chain) {
        CacheTrackerConfig.markLlmCalled();
        return chain.nextStream(request);
    }

    @Override
    public @NonNull String getName() {
        return "LLMCallMarkerAdvisor";
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE; // Runs innermost, closest to LLM
    }
}
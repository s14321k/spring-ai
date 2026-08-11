package com.smartshop.customer.springbootai.config.cache;

import com.smartshop.customer.springbootai.advisors.TokenUsageAuditAdvisor;
import com.smartshop.customer.springbootai.advisors.cache.CacheSourceAdvisor;
import com.smartshop.customer.springbootai.advisors.cache.LLMCallMarkerAdvisor;
import org.springframework.ai.chat.cache.semantic.SemanticCacheAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenSemanticClientConfig {

    @Bean
    public ChatClient openRedisSemanticChatClient(OpenAiChatModel openAiChatModel, SemanticCacheAdvisor redisSemanticCacheAdvisor) {
        return ChatClient.builder(openAiChatModel)
                .defaultAdvisors(List.of(new SimpleLoggerAdvisor(),
                        new TokenUsageAuditAdvisor(), redisSemanticCacheAdvisor))
                .build();
    }

    @Bean
    public ChatClient openVectorSemanticChatClient(OpenAiChatModel openAiChatModel, SemanticCacheAdvisor vectorSemanticCacheAdvisor) {
        return ChatClient.builder(openAiChatModel)
                .defaultAdvisors(List.of(new SimpleLoggerAdvisor(),
                        new TokenUsageAuditAdvisor(), vectorSemanticCacheAdvisor))
                .build();
    }


//    For cache check
    @Bean
    public CacheSourceAdvisor redisCacheSourceAdvisor() {
        return new CacheSourceAdvisor("redis");
    }

    @Bean
    public CacheSourceAdvisor qdrantCacheSourceAdvisor() {
        return new CacheSourceAdvisor("qdrant");
    }

    @Bean
    public LLMCallMarkerAdvisor llmCallMarkerAdvisor() {
        return new LLMCallMarkerAdvisor();
    }

    @Bean
    public ChatClient openRedisSemanticChatClientRes(
            OpenAiChatModel openAiChatModel,
            SemanticCacheAdvisor redisSemanticCacheAdvisor,
            CacheSourceAdvisor redisCacheSourceAdvisor,
            LLMCallMarkerAdvisor llmCallMarkerAdvisor) {

        return ChatClient.builder(openAiChatModel)
                .defaultAdvisors(List.of(
                        redisCacheSourceAdvisor,      // outer: sets tracking
                        new SimpleLoggerAdvisor(),
                        new TokenUsageAuditAdvisor(),
                        redisSemanticCacheAdvisor,    // cache check
                        llmCallMarkerAdvisor          // inner: only runs on miss
                ))
                .build();
    }

    @Bean
    public ChatClient openVectorSemanticChatClientRes(
            OpenAiChatModel openAiChatModel,
            SemanticCacheAdvisor vectorSemanticCacheAdvisor,
            CacheSourceAdvisor qdrantCacheSourceAdvisor,
            LLMCallMarkerAdvisor llmCallMarkerAdvisor) {

        return ChatClient.builder(openAiChatModel)
                .defaultAdvisors(List.of(
                        qdrantCacheSourceAdvisor,     // outer: sets tracking
                        new SimpleLoggerAdvisor(),
                        new TokenUsageAuditAdvisor(),
                        vectorSemanticCacheAdvisor,   // cache check
                        llmCallMarkerAdvisor          // inner: only runs on miss
                ))
                .build();
    }
}

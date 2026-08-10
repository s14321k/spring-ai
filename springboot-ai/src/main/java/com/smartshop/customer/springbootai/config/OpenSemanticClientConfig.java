package com.smartshop.customer.springbootai.config;

import com.smartshop.customer.springbootai.advisors.TokenUsageAuditAdvisor;
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
    public ChatClient openSemanticChatClient(OpenAiChatModel openAiChatModel, SemanticCacheAdvisor semanticCacheAdvisor) {
        return ChatClient.builder(openAiChatModel)
                .defaultAdvisors(List.of(new SimpleLoggerAdvisor(),
                        new TokenUsageAuditAdvisor(), semanticCacheAdvisor))
                .build();
    }
}

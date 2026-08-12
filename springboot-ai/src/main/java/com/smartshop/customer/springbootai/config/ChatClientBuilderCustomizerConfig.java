package com.smartshop.customer.springbootai.config;

import com.smartshop.customer.springbootai.advisors.TokenUsageAuditAdvisor;
import org.springframework.ai.chat.client.ChatClientBuilderCustomizer;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientBuilderCustomizerConfig {

    @Bean
    public ChatClientBuilderCustomizer loggerAdviserChatClientBuilderCustomizer() {
        return builder -> builder.defaultAdvisors(new SimpleLoggerAdvisor());
    }

    @Bean
    @ConditionalOnProperty(name = "audit.token-usage.enabled", havingValue = "true")
    public ChatClientBuilderCustomizer auditAdviserChatClientBuilderCustomizer() {
        return builder -> builder.defaultAdvisors(new TokenUsageAuditAdvisor());
    }
}
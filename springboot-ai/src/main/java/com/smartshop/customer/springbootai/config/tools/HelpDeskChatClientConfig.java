package com.smartshop.customer.springbootai.config.tools;

import com.smartshop.customer.springbootai.advisors.TokenUsageAuditAdvisor;
import com.smartshop.customer.springbootai.tools.TimeTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientBuilderCustomizer;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.util.List;

@Configuration
public class HelpDeskChatClientConfig {

    @Value("classpath:/promtTemplates/helpDeskSystemPromptTemplate.st")
    Resource systemPromptTemplate;

    @Bean
    public ChatClient helpDeskChatClient(GoogleGenAiChatModel model,
                                         ChatMemory chatMemory, TimeTools timeTools) {
        Advisor loggerAdvisor = new SimpleLoggerAdvisor();
        Advisor tokenUsageAdvisor = new TokenUsageAuditAdvisor();
        Advisor memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();

        return ChatClient.builder(model)
                .defaultTools(timeTools)
                .defaultAdvisors(List.of(
                        loggerAdvisor,
                        memoryAdvisor,
                        tokenUsageAdvisor))
                .build();
    }

    @Bean
    public ChatClient helpDeskChatClientBuilderCustomizer(GoogleGenAiChatModel model,
                                         ChatMemory chatMemory, TimeTools timeTools,
                                         List<ChatClientBuilderCustomizer> customizers) {
//        Advisor loggerAdvisor = new SimpleLoggerAdvisor();
//        Advisor tokenUsageAdvisor = new TokenUsageAuditAdvisor();     // This is auto injected due to the configuration
//                                                                      // in the ChatClientBuilderCustomizerConfig.

        // Create builder tied to the specific model
        ChatClient.Builder chatClientBuilder = ChatClient.builder(model);

        // Manually apply every registered customizer (logger, audit, etc.)
        customizers.forEach(customizer -> customizer.customize(chatClientBuilder));

        Advisor memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();

        return chatClientBuilder
                .defaultTools(timeTools)
                .defaultAdvisors(List.of(
//                        loggerAdvisor,
//                        tokenUsageAdvisor,    // This is auto injected due to the configuration in the ChatClientBuilderCustomizerConfig.
                        memoryAdvisor))
                .build();
    }
}
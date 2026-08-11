package com.smartshop.customer.springbootai.config.tools;

import com.smartshop.customer.springbootai.advisors.TokenUsageAuditAdvisor;
import com.smartshop.customer.springbootai.tools.TimeTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class TimeChatClientConfig {

    @Bean
    public ChatClient timeChatClient(GoogleGenAiChatModel model,
                                          ChatMemory chatMemory) {

        Advisor loggerAdvisor = new SimpleLoggerAdvisor();
        Advisor tokenUsageAdvisor = new TokenUsageAuditAdvisor();
        Advisor memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();

        return ChatClient.builder(model)
                .defaultAdvisors(List.of(
                        loggerAdvisor,
                        memoryAdvisor,
                        tokenUsageAdvisor))
                .build();
    }

    /**
     * LLM backend: <b>Gemma (local Docker)</b> — uses {@link OpenAiChatModel} at
     * {@code spring.ai.openai.chat.base-url} ({@code ${IP_ADDRESS}:12434/engines/v1}).
     * Time lookups are handled by {@code TimeTools}, not the LLM.
     */
    @Bean
    public ChatClient timeToolsChatClient(GoogleGenAiChatModel model,
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
}

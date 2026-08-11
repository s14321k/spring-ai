package com.smartshop.customer.springbootai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GemmaClientConfig {

    /**
     * LLM backend: <b>Gemma (local Docker)</b> — uses {@link OpenAiChatModel} with the
     * OpenAI-compatible endpoint at {@code spring.ai.openai.chat.base-url}
     * ({@code ${IP_ADDRESS}:12434/engines/v1}, model {@code ai/gemma3}).
     *
     * <p>Minimal {@link ChatClient} with no preset system instructions or advisors.</p>
     *
     * @param model the Docker-hosted Gemma model exposed via the OpenAI API
     * @return a vanilla Gemma chat client
     */
    @Bean
    public ChatClient gemmaOpenAiChatClient(OpenAiChatModel model) {
        return ChatClient.create(model);
    }
}

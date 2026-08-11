package com.smartshop.customer.springbootai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GenAiClientConfig {

    /**
     * LLM backend: <b>Gemini (Google cloud API)</b> — uses {@link GoogleGenAiChatModel}
     * authenticated via {@code spring.ai.google.genai.api-key} ({@code ${OPEN_API_KEY}}).
     *
     * <p>Minimal {@link ChatClient} with no preset system instructions or advisors.</p>
     *
     * @param model the Google Gemini model
     * @return a vanilla Gemini chat client
     */
    @Bean
    public ChatClient genAiChatClient(GoogleGenAiChatModel model) {
        return ChatClient.create(model);
    }
}

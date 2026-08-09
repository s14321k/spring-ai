package com.smartshop.customer.springbootai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GenAiClientConfig {

    /**
     * Creates a minimal {@link ChatClient} backed by the <b>Google Gemini</b> model.
     *
     * <p>Uses {@link ChatClient}#create(Model) for direct instantiation with no extra
     * advisors, system prompts, or default options. Ideal for general-purpose,
     * unconstrained chat where you want the model's raw behavior.</p>
     *
     * @param model the {@link GoogleGenAiChatModel} to route requests through
     * @return a vanilla Gemini chat client
     */
    @Bean
    public ChatClient genAiChatClient(GoogleGenAiChatModel model) {
        return ChatClient.create(model);
    }
}

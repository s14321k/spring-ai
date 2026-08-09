package com.smartshop.customer.springbootai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GemmaClientConfig {

    /**
     * Creates a minimal {@link ChatClient} backed by the <b>OpenAI</b> model.
     *
     * <p>Identical in spirit to {@code genAiChatClient} but wired to
     * {@link OpenAiChatModel}. Provides a clean, unopinionated client when you need
     * OpenAI capabilities without preset system instructions or logging.</p>
     *
     * @param model the {@link OpenAiChatModel} to route requests through
     * @return a vanilla OpenAI chat client
     */
    @Bean
    public ChatClient gemmaOpenAiChatClient(OpenAiChatModel model) {
        return ChatClient.create(model);
    }
}

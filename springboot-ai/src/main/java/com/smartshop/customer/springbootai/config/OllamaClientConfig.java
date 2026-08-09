package com.smartshop.customer.springbootai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OllamaClientConfig {

    /**
     * Creates an <b>Ollama</b>-based {@link ChatClient} with a logging advisor.
     *
     * <p>Unlike the vanilla clients above, this one uses the {@code Builder} pattern and
     * attaches a {@link SimpleLoggerAdvisor} as a <i>default</i> advisor, meaning every
     * request/response is automatically logged. Useful for local or self-hosted models
     * where you want visibility into traffic without repeating advisor setup per call.</p>
     *
     * @param model the {@link OllamaChatModel} to route requests through
     * @return an Ollama chat client with request/response logging
     */
    @Bean
    public ChatClient ollamaChatClient(OllamaChatModel model) {
        ChatClient.Builder chatClientBuilder = ChatClient.builder(model)
                .defaultAdvisors(new SimpleLoggerAdvisor());
        return chatClientBuilder.build();
    }
}

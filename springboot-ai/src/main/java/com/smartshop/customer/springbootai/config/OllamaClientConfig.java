package com.smartshop.customer.springbootai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OllamaClientConfig {

    /**
     * LLM backend: <b>Ollama (local/remote server)</b> — uses {@link OllamaChatModel}
     * at {@code spring.ai.ollama.base-url} ({@code ${IP_ADDRESS}:11434}, model
     * {@code llama3.2:1b}).
     *
     * <p>Includes a {@link SimpleLoggerAdvisor} for automatic request/response logging.</p>
     *
     * @param model the Ollama-hosted chat model
     * @return an Ollama chat client with request/response logging
     */
    @Bean
    public ChatClient ollamaChatClient(OllamaChatModel model) {
        ChatClient.Builder chatClientBuilder = ChatClient.builder(model)
                .defaultAdvisors(new SimpleLoggerAdvisor());
        return chatClientBuilder.build();
    }
}

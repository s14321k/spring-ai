package com.smartshop.customer.springbootai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {

    @Bean
    public ChatClient openAiChatClient(OpenAiChatModel model) {
        return ChatClient.create(model);
    }

    @Bean
    public ChatClient ollamaChatClient(OllamaChatModel model) {
        ChatClient.Builder chatClientBuilder = ChatClient.builder(model);
        return chatClientBuilder.build();
    }
}

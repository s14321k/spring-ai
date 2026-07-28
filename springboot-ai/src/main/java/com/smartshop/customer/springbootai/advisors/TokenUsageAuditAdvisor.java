package com.smartshop.customer.springbootai.advisors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;

public class TokenUsageAuditAdvisor implements CallAdvisor {

    private static final Logger logger = LoggerFactory.getLogger(TokenUsageAuditAdvisor.class);

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {

        // Got this from the class SimpleLoggerAdvisor.java. For future reference
        ChatClientResponse chatClientResponse = callAdvisorChain.nextCall(chatClientRequest);
        ChatResponse chatResponse =chatClientResponse.chatResponse();

        // Condition 'chatResponse.getMetadata() != null' is always 'true'
        // We are using this if we have own model and we don't have a default advisor use this
        if(chatResponse.getMetadata() != null) {
            Usage usage = chatResponse.getMetadata().getUsage();

            if(usage != null) {
                logger.info("Token usage details : {}", usage.toString());
            }
        }
        return chatClientResponse;
    }

    @Override
    public String getName() {
        return "TokenUsageAuditAdvisor";
    }

    @Override
    public int getOrder() {
        return 1;
    }
}

package com.smartshop.customer.springbootai.controller.tools;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

@RestController
@RequestMapping("/tools-api")
public class TimeController {

    private final ChatClient timeToolsChatClient;

    public TimeController(ChatClient timeToolsChatClient) {
        this.timeToolsChatClient = timeToolsChatClient;
    }

    @GetMapping("/local-time")
    public String localTime(@RequestHeader("username") String username, @RequestParam("message") String message) {
        return timeToolsChatClient
                .prompt()
                .advisors(a -> a.param(CONVERSATION_ID, username))
                .user(message)
                .call()
                .content();
    }
}

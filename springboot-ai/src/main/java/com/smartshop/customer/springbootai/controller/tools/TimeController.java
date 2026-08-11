package com.smartshop.customer.springbootai.controller.tools;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

@RestController
@RequestMapping("/tools-api")
public class TimeController {

    private final ChatClient timeToolsChatClient;
    private final ChatClient timeChatClient;

    public TimeController(ChatClient timeToolsChatClient, ChatClient timeChatClient) {
        this.timeToolsChatClient = timeToolsChatClient;
        this.timeChatClient = timeChatClient;
    }

    @GetMapping("/local-time-fail")
    public String localTimeFail(@RequestHeader("username") String username, @RequestParam("message") String message) {
        return timeChatClient
                .prompt()
                .advisors(a -> a.param(CONVERSATION_ID, username))
                .user(message)
                .call()
                .content();
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

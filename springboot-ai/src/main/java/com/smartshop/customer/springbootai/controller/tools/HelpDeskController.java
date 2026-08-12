package com.smartshop.customer.springbootai.controller.tools;

import com.smartshop.customer.springbootai.tools.HelpDeskTools;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/tools-api")
@RequiredArgsConstructor
public class HelpDeskController {

    private final ChatClient helpDeskChatClient;

    private final HelpDeskTools helpDeskTools;

    @GetMapping("/help-desk")
    public String helpDesk(
            @RequestHeader("username") String username,         // Gets the username from the request header
            @RequestParam("message") String message) {          // Gets the user's message from the request parameter

        return helpDeskChatClient.prompt()
                .advisors(a -> a.param(
                        ChatMemory.CONVERSATION_ID, username))  // Uses username as the conversation ID for chat memory
                .user(message)                                  // Sends the user's message to the LLM
                .tools(helpDeskTools)                           // Makes HelpDeskTools methods available as LLM tools
                .toolContext(Map.of(
                        "username", username))              // Passes username to ToolContext for tool methods
                .call()                                         // Executes the LLM request and any required tools
                .content();                                     // Returns the final LLM response as a String
    }
}

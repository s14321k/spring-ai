package com.smartshop.customer.springbootai.controller;

import com.smartshop.customer.springbootai.advisors.TokenUsageAuditAdvisor;
import com.smartshop.customer.springbootai.model.CountryCities;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.converter.ListOutputConverter;
import org.springframework.ai.converter.MapOutputConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ChatController {

    private final ChatClient genAiChatClient;              // Gemini model
    private final ChatClient openAiChatClient;             // OpenAI model (basic)
    private final ChatClient defaultSystemUserChatClient;  // OpenAI with default system + user prompts
    private final ChatClient vectorGeneralRAGChatClient;   // OpenAI with RAG + memory advisors

    public ChatController(
            @Qualifier("genAiChatClient") ChatClient genAiChatClient,
            @Qualifier("gemmaOpenAiChatClient") ChatClient openAiChatClient,
            @Qualifier("defaultSystemUserChatClient") ChatClient defaultSystemUserChatClient,
            @Qualifier("vectorGeneralRAGChatClient") ChatClient vectorGeneralRAGChatClient) {
        this.genAiChatClient = genAiChatClient;
        this.openAiChatClient = openAiChatClient;
        this.defaultSystemUserChatClient = defaultSystemUserChatClient;
        this.vectorGeneralRAGChatClient = vectorGeneralRAGChatClient;
    }

    /* ── Basic chat endpoints ── */

    // Simple Gemini call — no system prompt, no memory, just raw Q&A.
    @GetMapping("/gemini-chat")
    public ResponseEntity<String> sendGeminiMessage(@RequestParam("message") String message) {
        return ResponseEntity.ok(genAiChatClient.prompt(message).call().content());
    }

    // OpenAI call with default system prompt baked into the ChatClient bean.
    @GetMapping("/chat")
    public ResponseEntity<String> sendMessage(@RequestParam("message") String message) {
        return ResponseEntity.ok(openAiChatClient.prompt(message).call().content());
    }

    // OpenAI call that overrides the default user prompt per request.
    // Also attaches a TokenUsageAuditAdvisor just for this call.
    @GetMapping("/system-chat")
    public ResponseEntity<String> sendSystemMessage(@RequestParam("message") String message) {
        return ResponseEntity.ok(defaultSystemUserChatClient
                .prompt()
                // .system("...")  // uncomment to override the bean's defaultSystem()
                .advisors(new TokenUsageAuditAdvisor())
                .user(message)     // overrides the bean's defaultUser() for this call only
                .call()
                .content());
    }

    /* ── Template-based endpoints ── */

    // Loads a .st (StringTemplate) file from classpath and fills placeholders at runtime.
    // Example template: "Hello {customerName}, regarding: {customerMessage}"
    @Value("classpath:/promtTemplates/userPromptTemplate.st")
    private Resource userPromptTemplate;

    // Drafts a customer service email using an external template file.
    // Keeping prompts in files lets you tweak wording without recompiling code.
    @GetMapping("/email")
    public ResponseEntity<String> sendEmail(
            @RequestParam("customerName") String customerName,
            @RequestParam("customerMessage") String customerMessage) {

        return ResponseEntity.ok(defaultSystemUserChatClient
                .prompt()
                .system("You are a professional customer service email assistant.")
                .user(promptSpec -> promptSpec
                        .text(userPromptTemplate)
                        .param("customerName", customerName)
                        .param("customerMessage", customerMessage))
                .call()
                .content());
    }

    // Loads the system prompt from an external file instead of a Java string.
    // Good for long instructions that change often.
    @Value("classpath:/promtTemplates/systemPromptTemplate.st")
    private Resource systemPromptTemplate;

    @GetMapping("/prompt-stuffing")
    public ResponseEntity<String> promptStuffing(@RequestParam("message") String message) {
        return ResponseEntity.ok(defaultSystemUserChatClient
                .prompt()
                .advisors(List.of(new TokenUsageAuditAdvisor(), new SimpleLoggerAdvisor()))
                .system(systemPromptTemplate)   // injects the entire file content as system message
                .user(message)
                .call()
                .content());
    }

    // Same as above but streams tokens back as they are generated (SSE-style).
    // Use this for a "typing" effect in the UI.
    @GetMapping("/stream")
    public Flux<String> streamResponse(@RequestParam("message") String message) {
        return defaultSystemUserChatClient
                .prompt()
                .system(systemPromptTemplate)
                .user(message)
                .stream()   // <-- .stream() instead of .call()
                .content();
    }

    /* ── Structured output (JSON → Java objects) ── */

    // 1. Single POJO — model returns JSON that maps directly to CountryCities.class
    @GetMapping("/structured-entity-format")
    public CountryCities structuredEntityFormat(@RequestParam("message") String message) {
        return openAiChatClient
                .prompt()
                .advisors(new SimpleLoggerAdvisor())
                .user(message)
                .call()
                .entity(CountryCities.class);
    }

    // 2. List of strings — model returns a list (e.g., "top 5 cities")
    @GetMapping("/chat-list")
    public List<String> structuredListFormat(@RequestParam("message") String message) {
        return openAiChatClient
                .prompt()
                .advisors(new SimpleLoggerAdvisor())
                .user(message)
                .call()
                .entity(new ListOutputConverter());
    }

    // 3. List of POJOs — needed because Java erases generics at runtime.
    // ParameterizedTypeReference preserves the type info for deserialization.
    @GetMapping("/chat-entity-list")
    public List<CountryCities> structuredEntityListFormat(@RequestParam("message") String message) {
        return openAiChatClient
                .prompt()
                .advisors(new SimpleLoggerAdvisor())
                .user(message)
                .call()
                .entity(new ParameterizedTypeReference<List<CountryCities>>() {});
    }

    // 4. Dynamic map — flexible, no POJO needed, but you lose type safety.
    @GetMapping("/chat-map")
    public Map<String, Object> structuredMapFormat(@RequestParam("message") String message) {
        return openAiChatClient
                .prompt()
                .advisors(new SimpleLoggerAdvisor())
                .user(message)
                .call()
                .entity(new MapOutputConverter());
    }

    /* ── Chat memory demos ── */

    // Shared memory: every request uses the same conversation ID ("default").
    // All users share one conversation context. Good for testing, bad for production.
    @GetMapping("/chat-memory")
    public String chatMemory(@RequestParam("message") String message) {
        return vectorGeneralRAGChatClient
                .prompt()
                .user(message)
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, "default"))
                .call()
                .content();
    }

    // Isolated memory: each user gets their own conversation history.
    // The username header becomes the conversation ID, so histories never mix.
    @GetMapping("/chat-user-memory")
    public String chatMemoryWithUserName(
            @RequestParam("message") String message,
            @RequestHeader("userName") String userName) {

        return vectorGeneralRAGChatClient
                .prompt()
                .user(message)
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, userName))
                .call()
                .content();
    }
}
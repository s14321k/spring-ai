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

//    This is for gemini config

//    private final GoogleGenAiChatModel chatClient;
//
//    public ChatController(GoogleGenAiChatModel genAiChatModel) {
//        this.chatClient = genAiChatModel;
//    }
//
//    @GetMapping("/chat")
//    public ResponseEntity<String> sendMessage(@RequestParam("message") String message) {
//        return ResponseEntity.ok(chatClient.call(message));
//    }

    private final ChatClient genAiChatClient;
    private final ChatClient openAiChatClient;
    private final ChatClient defaultSystemUserChatClient;
    private final ChatClient chatMemoryClient;

    public ChatController(@Qualifier("genAiChatClient") ChatClient genAiChatClient, @Qualifier("openAiChatClient") ChatClient openAiChatClient,
                          @Qualifier("defaultSystemUserChatClient") ChatClient defaultSystemUserChatClient, @Qualifier("chatMemoryClient") ChatClient chatMemoryClient) {
        this.genAiChatClient = genAiChatClient;
        this.openAiChatClient = openAiChatClient;
        this.defaultSystemUserChatClient = defaultSystemUserChatClient;
        this.chatMemoryClient = chatMemoryClient;
    }

    /**
     * Sends a message to the <b>Gemini</b> model using a minimal prompt API.
     *
     * <p>This endpoint uses a raw {@code genAiChatClient} with <b>no default system or user
     * prompts</b> configured. It is the simplest possible interaction — just pass the user
     * message and return the model's response. Useful for generic, unconstrained Q&A or
     * when you want the model to behave with its base training only.</p>
     *
     * @param message the raw user input
     * @return the model's response wrapped in {@code ResponseEntity}
     */
    @GetMapping("/gemini-chat")
    public ResponseEntity<String> sendGeminiMessage(@RequestParam("message") String message) {
        return ResponseEntity.ok(genAiChatClient.prompt(message).call().content());
    }

    /*
     * The following constructor-level setup has been moved to ChatClientConfig.java.
     *
     * Configuring defaultSystem() and defaultUser() on the ChatClient builder means
     * EVERY prompt sent through this client automatically prepends the system
     * instruction and the default user message — no need to repeat them per request.
     *
     *   this.openAiChatClient = clientBuilder
     *       .defaultSystem("You are an internal HR assistant...")
     *       .defaultUser("How can you help me?")
     *       .build();
     */

    /**
     * Sends a message to the <b>OpenAI</b> model using defaults from
     * {@code ChatClientConfig}.
     *
     * <p>The underlying {@code openAiChatClient} was built with
     * {@code .defaultSystem(...)} and {@code .defaultUser(...)}, so this single-line
     * call inherits both automatically. Use this when you want consistent HR-assistant
     * behavior without repeating the system prompt every time.</p>
     *
     * @param message the user's question (appended after the default user context)
     * @return the HR assistant's response wrapped in {@code ResponseEntity}
     */
    @GetMapping("/chat")
    public ResponseEntity<String> sendMessage(@RequestParam("message") String message) {
        return ResponseEntity.ok(openAiChatClient.prompt(message).call().content());
    }

    /**
     * Sends a message to the <b>OpenAI</b> model with an explicit advisor and optional
     * runtime override behavior.
     *
     * <p><b>Default inheritance:</b> Because {@code defaultSystemUserChatClient} was
     * configured with {@code .defaultSystem(...)} and {@code .defaultUser(...)} in
     * {@code ChatClientConfig}, those values are automatically applied <i>unless</i> you
     * explicitly call {@code .system(...)} or {@code .user(...)} here — which would
     * <b>override</b> the defaults for this request only.</p>
     *
     * <p>The commented-out {@code .system(...)} block shows what an override would look
     * like. The current implementation relies on the constructor-level default, keeping
     * the code DRY.</p>
     *
     * <p><b>Advisors:</b> A {@link TokenUsageAuditAdvisor} is attached per-request to
     * log or audit token consumption, demonstrating how you can layer cross-cutting
     * behavior on top of the base client.</p>
     *
     * @param message the user message; passed to {@code .user()} to override the
     *                default user text for this specific call
     * @return the model's response wrapped in {@code ResponseEntity}
     */
    @GetMapping("/system-chat")
    public ResponseEntity<String> sendSystemMessage(@RequestParam("message") String message) {
        return ResponseEntity.ok(defaultSystemUserChatClient
                .prompt()
                // .system("...")  // <-- Would OVERRIDE the defaultSystem from ChatClientConfig
                .advisors(new TokenUsageAuditAdvisor())
                .user(message)     // <-- Overrides the defaultUser("How can you help me?") for this call
                .call()
                .content());
    }

//    Using this in the promtTemplate.st file
//    To overide this curly braces {}, we can use TemplateRender which is in spring AI page.
//    String promtTemplate =
//            """
//                A customer named {customerName} sent the following message:
//                "{customerMessage}"
//
//                Write a polite and helpful email response addressing the issue.
//                Maintain a professional tone and provide reassurance.
//
//                Respond as if you're writing the email body only. Don't include subject, signature
//                """;

    // Note on template files:
    // The .st (StringTemplate) files use curly braces {placeholder} for variable substitution.
    // If this conflicts with Spring's property placeholder syntax or other templating,
    // configure a custom TemplateRender to override the delimiters.

    @Value("classpath:/promtTemplates/userPromptTemplate.st")
    private Resource userPromptTemplate;

    /**
     * Generates a customer service email using an <b>external user prompt template</b>.
     *
     * <p>Instead of hardcoding the user message in Java, this loads a {@code .st} (StringTemplate)
     * file from the classpath. Placeholders like {@code {customerName}} and
     * {@code {customerMessage}} inside the template are bound at runtime via
     * {@link org.springframework.ai.chat.prompt.PromptTemplateSpec#param(String, Object)}.</p>
     *
     * <p><b>Why this matters:</b> Keeping prompt text in external files lets non-developers
     * (e.g., prompt engineers) tweak wording without recompiling code. It also separates
     * formatting concerns from business logic.</p>
     *
     * <p><i>Template tip:</i> Curly braces in {@code .st} files may clash with Spring's
     * {@code ${...}} property placeholders. If you encounter parsing issues, supply a custom
     * {@code TemplateRender} to change the delimiter syntax.</p>
     *
     * @param customerName    the name to inject into the template
     * @param customerMessage the raw customer inquiry to inject into the template
     * @return a drafted email body wrapped in {@code ResponseEntity}
     */
    @GetMapping("/email")
    public ResponseEntity<String> sendEmail(@RequestParam("customerName") String customerName,
                                            @RequestParam("customerMessage") String customerMessage) {
        return ResponseEntity.ok(defaultSystemUserChatClient
                .prompt()
                .advisors(new TokenUsageAuditAdvisor())
                .system("""
                        You are a professional customer service assistant which helps drafting email
                        responses to improve the productivity of the customer support team
                        """)
                .user(promtTemplateSpec ->
                        promtTemplateSpec.text(userPromptTemplate)
                                .param("customerName", customerName)
                                .param("customerMessage", customerMessage))
                .call()
                .content());
    }

    @Value("classpath:/promtTemplates/systemPromptTemplate.st")
    private Resource systemPromptTemplate;

    /**
     * Demonstrates <b>system prompt stuffing</b> by loading the system instruction from an
     * external template resource.
     *
     * <p>Rather than embedding the system prompt as a Java string, this approach injects the
     * entire content of {@code systemPromptTemplate.st} as the system message. This is ideal
     * for long, evolving instructions (e.g., role definitions, safety guidelines, output rules)
     * that you want to version-control or hot-reload independently of the codebase.</p>
     *
     * <p>The commented-out {@code .options(...)} lines show how to override the LLM model or
     * temperature <i>per request</i>, taking precedence over the defaults configured in the
     * {@code ChatClient} bean.</p>
     *
     * @param message the end-user's input message
     * @return the AI response wrapped in {@code ResponseEntity}
     */
    @GetMapping("/promt-stuffing")
    public ResponseEntity<String> promptStuffing(@RequestParam("message") String message) {
        return ResponseEntity.ok(defaultSystemUserChatClient
                .prompt()
//                .options(OpenAiChatOptions.builder().model(ChatModel.GPT_5_4_NANO_2026_03_17.asString())
//                        .temperature(0.7)) // Use this if we use open ai model
//                .options(OpenAiChatOptions.builder().model("gemma3")) // We have did this same in chat client config file
                .advisors(List.of(new TokenUsageAuditAdvisor(), new SimpleLoggerAdvisor()))
                .system(systemPromptTemplate)
                .user(message)
                .call()
                .content());
    }

    /**
     * Streams the LLM response as a reactive {@link Flux} of partial string chunks.
     *
     * <p>Identical prompt composition to the non-streaming endpoints (system template + user
     * message), but uses {@code .stream()} instead of {@code .call()}. This pushes tokens to
     * the client as they are generated, reducing perceived latency and enabling
     * Server-Sent Events (SSE) style delivery.</p>
     *
     * <p><b>Use when:</b> You want a real-time typing effect in the UI or need to process
     * large responses incrementally without waiting for the full generation to finish.</p>
     *
     * @param message the end-user's input message
     * @return a {@code Flux<String>} emitting content chunks as the model produces them
     */
    @GetMapping("/stream")
    public Flux<String> streamResponse(@RequestParam("message") String message) {
        return defaultSystemUserChatClient
                .prompt()
                .system(systemPromptTemplate)
                .user(message)
                .stream()
                .content();
    }

    /**
     * Demonstrates Spring AI's structured output conversion strategies for mapping LLM responses
     * to typed Java objects. The framework automatically instructs the model to return JSON
     * and handles deserialization via various {@link org.springframework.ai.converter.OutputConverter}s.
     *
     * <p><b>1. Single POJO Entity ({@code /structured-entity-format})</b></p>
     * <ul>
     *   <li>{@code .entity(CountryCities.class)} — direct mapping to a concrete bean.</li>
     *   <li>Internally uses {@link BeanOutputConverter}; ideal when the schema is fixed and known.</li>
     *   <li>Equivalent to manually passing {@code new BeanOutputConverter<>(CountryCities.class)}.</li>
     * </ul>
     *
     * <p><b>2. List of Primitives ({@code /chat-list})</b></p>
     * <ul>
     *   <li>{@code .entity(new ListOutputConverter())} — returns a {@code List<String>}.</li>
     *   <li>Useful for simple enumerated outputs (e.g., "give me a list of city names").</li>
     *   <li>No custom POJO required; the converter handles comma-separated or JSON-array responses.</li>
     * </ul>
     *
     * <p><b>3. List of Typed Objects ({@code /chat-entity-list})</b></p>
     * <ul>
     *   <li>{@code .entity(new ParameterizedTypeReference<List<CountryCities>>() {})} — preserves generic type info at runtime.</li>
     *   <li>Required for parameterized types because Java type erasure makes {@code List<CountryCities>.class} impossible.</li>
     *   <li>Use this when you need a collection of structured objects rather than plain strings.</li>
     * </ul>
     *
     * <p><b>4. Dynamic Map ({@code /chat-map})</b></p>
     * <ul>
     *   <li>{@code .entity(new MapOutputConverter())} — returns {@code Map<String, Object>}.</li>
     *   <li>Best for flexible or schemaless responses where defining a POJO upfront is impractical.</li>
     *   <li>Trade-off: you lose compile-time type safety and must cast values manually.</li>
     * </ul>
     *
     * <p><b>Key Takeaway</b></p>
     * Choose the converter that matches your certainty about the response shape:
     * <pre>
     *   Known schema    → BeanOutputConverter / entity(Class)
     *   List of strings → ListOutputConverter
     *   List of POJOs   → ParameterizedTypeReference
     *   Unknown schema  → MapOutputConverter
     * </pre>
     *
     * @param message the natural-language prompt that describes the desired structured output
     * @return the AI response bound to the target type for each respective endpoint
     */
    @GetMapping("/structured-entity-format")
    public CountryCities structuredEntityFormat(@RequestParam("message") String message) {

        return openAiChatClient
                .prompt()
                .advisors(new SimpleLoggerAdvisor())
                .user(message)
                .call()
//                .entity(new BeanOutputConverter<>(CountryCities.class)) // We can use both way.
                .entity(CountryCities.class);
    }

    @GetMapping("/chat-list")
    public List<String> structuredListFormat(@RequestParam("message") String message) {

        return openAiChatClient
                .prompt()
                .advisors(new SimpleLoggerAdvisor())
                .user(message)
                .call()
                .entity(new ListOutputConverter());
    }

    @GetMapping("/chat-entity-list")
    public List<CountryCities> structuredEntityListFormat(@RequestParam("message") String message) {

        return openAiChatClient
                .prompt()
                .advisors(new SimpleLoggerAdvisor())
                .user(message)
                .call()
                .entity(new ParameterizedTypeReference<List<CountryCities>>() {
                });
    }

    @GetMapping("/chat-map")
    public Map<String,Object> structuredMapFormat(@RequestParam("message") String message) {

        return openAiChatClient
                .prompt()
                .advisors(new SimpleLoggerAdvisor())
                .user(message)
                .call()
                .entity(new MapOutputConverter());
    }

    /**
     * Demonstrates two strategies for managing chat memory with Spring AI's {@link MessageChatMemoryAdvisor}.
     *
     * <p><b>1. Shared Memory ({@code /chat-memory})</b></p>
     * <ul>
     *   <li>Uses a hardcoded {@code "default"} conversation ID.</li>
     *   <li>All users share the same conversation context.</li>
     *   <li>Useful for: single-user applications, global state, or testing/POC scenarios.</li>
     *   <li><i>Debug tip:</i> Set a breakpoint in {@code MessageChatMemoryAdvisor} to observe how the
     *       same {@code CONVERSATION_ID} retrieves the shared message history.</li>
     * </ul>
     *
     * <p><b>2. Per-User Isolated Memory ({@code /chat-user-memory})</b></p>
     * <ul>
     *   <li>Uses the {@code userName} header as the dynamic conversation ID.</li>
     *   <li>Each user gets an isolated memory context; conversations never leak between users.</li>
     *   <li>Useful for: multi-user applications where personalization and privacy matter.</li>
     *   <li><i>Debug tip:</i> Set a breakpoint in {@code InMemoryChatMemoryRepository.findByConversationId()}
     *       to verify that different {@code userName} values map to separate memory stores.</li>
     * </ul>
     *
     * <p><b>Key Takeaway</b></p>
     * The {@link ChatMemory#CONVERSATION_ID} parameter is the isolation boundary. Choose a fixed ID
     * for shared/global context, or a user-scoped ID (e.g., username, session ID, JWT subject) to
     * keep each user's conversation history private and distinct.
     *
     * @param message the user's input message
     * @param userName the unique identifier for per-user memory isolation (header-based)
     * @return the AI-generated response
     */
    @GetMapping("/chat-memory")
    public String chatMemory(@RequestParam("message") String message) {
        return chatMemoryClient
                .prompt()
                .user(message)
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, "default"))
                .call()
                .content();
    }

    @GetMapping("/chat-user-memory")
    public String chatMemoryWithUserName(@RequestParam("message") String message,
                                         @RequestHeader("userName") String userName) {
        return chatMemoryClient
                .prompt()
                .user(message)
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, userName))
                .call()
                .content();
    }
}

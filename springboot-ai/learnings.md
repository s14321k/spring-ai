# spring.ai.chat.client.enabled — Default Value & Correction

*Reference note for Spring AI multi-model ChatClient configuration*

## Default Value

The default value is **true**, confirmed by the Spring AI source annotation:

```java
@ConditionalOnProperty(prefix="spring.ai.chat.client", name="enabled",
                       havingValue="true", matchIfMissing=true)
```

`matchIfMissing=true` means: if the property is not set at all, Spring AI still treats it as **true** — the `ChatClient.Builder` autoconfiguration is on by default.

## Correction to Earlier Explanation

An earlier response in this conversation stated that Spring AI **auto-disables** the builder when it detects multiple `ChatModel` beans. That is **not accurate**.

Per the official Spring AI documentation: by default, Spring AI autoconfigures a single `ChatClient.Builder` bean. However, when working with multiple chat models, the `ChatClient.Builder` autoconfiguration must be manually disabled by setting `spring.ai.chat.client.enabled=false`, which then allows multiple `ChatClient` instances to be created manually.

## What This Means in Practice

- **Default (`true`)**: Spring AI tries to autoconfigure **one** `ChatClient.Builder`, wired to whichever single `ChatModel` bean it finds.
- **With multiple `ChatModel` beans** (e.g., OpenAI + Ollama) and this left at `true`: the autoconfiguration can hit an **ambiguous-bean** situation for `ChatClient.Builder`, since it doesn't know which model to use.
- **Setting it to `false`** is the documented way to avoid that conflict, so custom `ChatClient` beans can be built manually per model — which matches the approach already used in `ChatClientConfig.java`.

## Current Property File Setting

The current `application.properties` has:

```properties
spring.ai.chat.client.enabled=true
```

This is actually the **opposite** of what the documentation recommends for a multi-model setup.

## Recommendation

Since all three `ChatClient` beans (`openAiChatClient`, `ollamaChatClient`, `defaultSystemUserChatClient`) are already built manually, and no autoconfigured `ChatClient.Builder` is injected anywhere, the safer and more correct setting per Spring AI's documented pattern is:

```properties
spring.ai.chat.client.enabled=false
```

## Why No Error Occurs Currently

Since the application isn't currently failing at startup, this likely means nothing in the code path depends on the autoconfigured `ChatClient.Builder` bean, so the bean-ambiguity conflict never surfaces. Still, aligning the property with the documented pattern (`false`) is recommended to avoid confusion or bean-resolution errors if that dependency is introduced later.

---

*Source: Spring AI official documentation and ChatClientAutoConfiguration API reference (spring-ai v1.1.x).*

# Roles in Spring AI

In Spring AI, every message sent to or received from an LLM is tagged with a **role**, represented by the `MessageType` enum:

```java
public enum MessageType {
    USER("user"),
    ASSISTANT("assistant"),
    SYSTEM("system"),
    TOOL("tool");
}
```

These roles map directly to how most LLM providers (OpenAI, Ollama, etc.) categorize conversational turns — Spring AI recognizes message categories for distinct conversational roles such as system, user, function, or assistant, and simply standardizes them across all providers so your code doesn't change if you switch models.

## 1. System Role — `SystemMessage`

Sets the AI's behavior, persona, and constraints before the conversation starts.

- Defines *who* the AI is and *how* it should respond
- Usually provided once, at the start of the interaction
- Example: `"You are a senior Java architect. Answer only Java and software architecture related questions."`

```java
chatClient.prompt()
    .system("You are an internal HR assistant. Only answer HR-related questions.")
    .user(message)
    .call()
    .content();
```

This is exactly what `defaultSystem(...)` sets in your `ChatClientConfig.java`.

## 2. User Role — `UserMessage`

Represents input from the end-user or developer — questions, prompts, or commands the model should respond to.

- The primary driver of the conversation
- In most apps, this is whatever comes in from the frontend, REST API, or another system

```java
chatClient.prompt().user("How many paid leave days do I get?").call().content();
```

## 3. Assistant Role — `AssistantMessage`

The AI's own response to the user's input.

- Represents the model's previous replies, tracked to maintain conversational flow
- Also carries **tool/function call requests** when the model decides it needs external data (e.g., "call `getLeaveBalance()`")
- Including past `AssistantMessage`s in a new request gives the model memory of what it already said

```java
chatClient.prompt()
    .messages(
        new UserMessage("What is a Record?"),
        new AssistantMessage("A Record is an immutable data carrier.")
    )
    .user("How is it different from a POJO?")
    .call()
    .content();
```

## 4. Tool/Function Role — `ToolResponseMessage`

Returns the **result** of a tool/function call that the assistant requested.

- Comes after an `AssistantMessage` that included a tool-call request
- Lets the model use real data (API responses, DB lookups, calculations) instead of guessing
- Example flow: User asks a question → Assistant requests a tool call → Tool executes → Tool result sent back as a `TOOL` message → Assistant produces final answer using that data

## Summary Table

| Role | Class | Purpose | Who sends it |
|---|---|---|---|
| **System** | `SystemMessage` | Sets behavior, persona, constraints | Developer (usually once, upfront) |
| **User** | `UserMessage` | The actual question/input | End-user or frontend |
| **Assistant** | `AssistantMessage` | The model's reply (may include tool-call requests) | The AI model |
| **Tool** | `ToolResponseMessage` | Result of an executed tool/function call | Application, on the model's behalf |

## How This Applies to Your Project

In `ChatClientConfig.java`, `defaultSystem(...)` on `defaultSystemUserChatClient` creates a **`SYSTEM`**-role message that's prepended to every request. When `ChatController.sendSystemMessage()` calls `.user(message)`, that becomes the **`USER`**-role message. The model then returns an **`ASSISTANT`**-role message as `.content()`. If you later add function calling (e.g., a tool to look up leave balances), you'd see `TOOL` messages appear in that exchange too.

# Prompt Stuffing

Based on the selected region in the image, here is a clear summary of the concepts presented:
What is Prompt Stuffing?

• Definition: Giving the Large Language Model (LLM) an open-book exam by providing reference text alongside your question.
• Purpose: It allows the LLM to answer accurately using information it was not originally trained on.
• Alternative Names: Also known as in-context learning or retrieval-augmented prompting.

Example: Internal Company Policy Lookup

• The "Stuffed" Prompt: You feed the model a specific rule: "According to the company's HR policy, employees are eligible for 18 days of paid leave annually. Unused leave can be carried over to the next year."
• The Question: "How many paid leaves do employees get each year?"
• The LLM Answer: "Employees are eligible for 18 days of paid leave annually."

Limitations & Next Steps

• The Problem: You cannot manually stuff massive amounts of data because you will exceed the model's token limit (maximum text capacity).
• The Solution: The course introduces Retrieval-Augmented Generation (RAG), a programmatic technique to dynamically find and inject only the most relevant context into the prompt.

# Advisors in Spring AI

## The Easy Way to Think About It

An **Advisor** is like a **middleware/interceptor** that sits between your code and the actual AI model call. It can look at the request *before* it goes to the model, and look at the response *after* it comes back — and modify either one, or even short-circuit the whole thing.

Think of it like Spring's own **filter chain** or **AOP interceptors**, but for AI prompts instead of HTTP requests.

## The Flow

```
Your code: chatClient.prompt().user("What's my leave balance?").call()
        │
        ▼
┌─────────────────────────────────────────────┐
│              ADVISOR CHAIN                    │
│                                               │
│  Advisor 1 (e.g. Logger)      ──before──▶    │
│  Advisor 2 (e.g. Memory)      ──before──▶    │
│  Advisor 3 (e.g. RAG/QA)      ──before──▶    │
│                                               │
└─────────────────────────────────────────────┘
        │
        ▼
   ChatModel (actual call to OpenAI/Ollama/etc.)
        │
        ▼
┌─────────────────────────────────────────────┐
│         Response flows BACK through          │
│         the same chain, in reverse order      │
│                                               │
│  Advisor 3  ──after──▶                       │
│  Advisor 2  ──after──▶                       │
│  Advisor 1  ──after──▶                       │
└─────────────────────────────────────────────┘
        │
        ▼
   Final response returned to your code
```

The Spring AI framework creates a `ChatClientRequest` from the user's prompt along with an empty advisor context object. Each advisor in the chain processes the request, potentially modifying it — and can even choose to **block** the request entirely without calling the model, in which case that advisor is responsible for filling in the response itself.

## Where Advisors Fit in Your Project

Right now, your `ChatController` and `MultiModelChatController` call the model **directly** with no advisors:

```java
openAiChatClient.prompt(message).call().content();
```

Advisors would slot in right here:

```java
chatClient.prompt()
    .advisors(
        new SimpleLoggerAdvisor(),               // logs request/response
        MessageChatMemoryAdvisor.builder(memory).build(),  // remembers conversation
        QuestionAnswerAdvisor.builder(vectorStore).build() // grounds answer in your HR docs (RAG)
    )
    .user(message)
    .call()
    .content();
```

This is exactly how you'd turn your current HR bot from "one-off stateless Q&A" into "remembers the conversation + answers from real policy documents."

## The Common Built-in Advisors

| Advisor | What it does |
|---|---|
| **`SimpleLoggerAdvisor`** | Logs the request and response data of the ChatClient — useful for debugging and monitoring your AI interactions. |
| **`MessageChatMemoryAdvisor`** | Adds prior conversation messages into the prompt so the model "remembers" earlier turns (since the chat API itself is stateless). |
| **`PromptChatMemoryAdvisor`** | Alternative memory strategy — folds history into the system prompt text instead of as separate messages. |
| **`VectorStoreChatMemoryAdvisor`** | Extracts memory from a vector store, useful for retrieving relevant history from large conversation datasets. |
| **`QuestionAnswerAdvisor`** | Implements the RAG pattern — searches a vector store for relevant docs and stuffs them into the prompt before calling the model. |
| **`RetrievalAugmentationAdvisor`** | A more configurable/composable RAG advisor (query rewriting, multiple retrievers, etc.). |
| **`SafeGuardAdvisor`** | A basic, sensitive-word-based filter that helps block the model from producing harmful content — it can block the request entirely instead of calling the model. |

## Order Matters

Advisors execute in ascending order of their `order` value *before* the model call, and in descending order *after* the model call. In practice: **memory advisors should usually go before RAG advisors**, so that:

```
MessageChatMemoryAdvisor  →  adds past conversation to prompt
QuestionAnswerAdvisor     →  now searches using the question AND the conversation history, giving more relevant results
```

If you reverse the order, the RAG search happens without knowing what was discussed earlier, potentially missing context-dependent follow-up questions ("what about next year?" needs to know what "this year" referred to).

## Best Practices

1. **Register advisors at build time**, not per-request, using `defaultAdvisors()` on the builder — the docs explicitly recommend this pattern:

```java
ChatClient.builder(chatModel)
    .defaultAdvisors(
        new SimpleLoggerAdvisor(),
        MessageChatMemoryAdvisor.builder(chatMemory).build(),
        QuestionAnswerAdvisor.builder(vectorStore).build()
    )
    .build();
```

Only override or add extras per-call when you genuinely need runtime-specific behavior (e.g., passing a dynamic `conversationId`).

2. **Order deliberately**: memory → retrieval/RAG → guardrails is a sensible default flow, since each step benefits from the context the previous one added.

3. **Use `SimpleLoggerAdvisor` in dev, not blindly in prod** — it's great for debugging prompt/response content, but be mindful of logging sensitive user data in production.

4. **Let guardrail advisors (like `SafeGuardAdvisor`) run early** so they can block bad input before you spend tokens on memory lookups or vector search.

5. **Advisors participate in Observability** — since Spring AI advisors integrate with the Observability stack, you get metrics/traces for each advisor's execution for free; use that instead of hand-rolled logging where possible.

6. **Don't overload one advisor with multiple responsibilities** — compose small, focused advisors (one for memory, one for RAG, one for logging) rather than one giant custom advisor doing everything; it keeps the chain composable and each piece independently testable.

## Applied to Your `defaultSystemUserChatClient`

Your current bean:

```java
@Bean
public ChatClient defaultSystemUserChatClient(OpenAiChatModel model) {
    return ChatClient.builder(model)
            .defaultSystem("...")
            .build();
}
```

A more capable HR-bot version using advisors:

```java
@Bean
public ChatClient defaultSystemUserChatClient(OpenAiChatModel model, ChatMemory chatMemory, VectorStore vectorStore) {
    return ChatClient.builder(model)
            .defaultSystem("You are an internal HR assistant...")
            .defaultAdvisors(
                new SimpleLoggerAdvisor(),
                MessageChatMemoryAdvisor.builder(chatMemory).build(),
                QuestionAnswerAdvisor.builder(vectorStore).build()
            )
            .build();
}
```

This would give you: logged interactions, conversation memory across turns, and answers grounded in actual HR policy documents — instead of relying purely on the system prompt and the small model's own judgment (which, as we saw with your Gemma3 issue, isn't fully reliable on its own).

# RAG - Retrival Augmented Generation
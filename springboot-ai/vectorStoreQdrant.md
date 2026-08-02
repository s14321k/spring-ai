Nice — full stack working end to end now. Here's why each piece fit together:

## 1. Embedding client now points at your Docker engine, not real OpenAI

```properties
spring.ai.openai.embedding.base-url=${IP_ADDRESS}:12434/engines/v1
spring.ai.openai.embedding.model=ai/mxbai-embed-large
```

Spring AI's OpenAI starter builds **two independent auto-configured clients** off the same `spring-ai-starter-model-openai` dependency: `OpenAiChatModel` and `OpenAiEmbeddingModel`. Each has its own `base-url`/`model`/`api-key` sub-namespace (`chat.*` and `embedding.*`), and each falls back to the top-level `spring.ai.openai.*` property only if its own isn't set. Since you gave `embedding.base-url` explicitly, it stopped inheriting the default `api.openai.com` and started hitting your local Docker Model Runner instead — same fix pattern as `chat.base-url` earlier. `api-key=dummy` is still shared from the top-level property, and Docker Model Runner doesn't validate it, so no key mismatch this time.

One thing worth double-checking: I'd originally suggested `spring.ai.openai.embedding.options.model` (nested under `options`), but you used `spring.ai.openai.embedding.model` directly and it worked — Spring AI's `OpenAiEmbeddingProperties` likely exposes `model` as a top-level convenience alias that maps into the same `OpenAiEmbeddingOptions.model` internally. Either form is fine as long as it resolved correctly, which your successful run confirms.

## 2. The starter dependency actually builds the `QdrantClient` bean

```gradle
implementation 'org.springframework.ai:spring-ai-starter-vector-store-qdrant'
```

This was the earlier missing piece — the bare `-autoconfigure-` artifact only has the `@Configuration` class, not the `io.qdrant:client` dependency it needs. The starter bundles both, so `QdrantVectorStoreAutoConfiguration`'s `@ConditionalOnBean(QdrantClient.class)` condition is now satisfied, and it can construct the `VectorStore` bean.

## 3. Single embedding provider removed the bean ambiguity

```properties
spring.ai.model.embedding=openai
```

This disables `OllamaEmbeddingAutoConfiguration` entirely (it's a comma-separated *enable-list*, not a priority list), leaving exactly one `EmbeddingModel` candidate for `QdrantVectorStoreAutoConfiguration` to autowire — no more "2 beans found" error.

## 4. Qdrant connects and builds the collection

```properties
spring.ai.vectorstore.qdrant.host=localhost
spring.ai.vectorstore.qdrant.port=6334
spring.ai.vectorstore.qdrant.collection-name=sarath-spring-ai
spring.ai.vectorstore.qdrant.initialize-schema=true
```

With the `QdrantClient` bean now present (step 2), these properties wire it to your local Qdrant gRPC port. `initialize-schema=true` tells Spring AI to auto-create the `sarath-spring-ai` collection with vector dimensions matching your embedding model's output size — `mxbai-embed-large` produces 1024-dim vectors, and the collection schema is created to match automatically.

## End-to-end flow now

`RandomDataLoader.@PostConstruct` → `vectorStore.add(documents)` → `OpenAiEmbeddingModel` calls `${IP_ADDRESS}:12434/engines/v1/embeddings` with `ai/mxbai-embed-large` → vectors returned → `QdrantVectorStore` writes them into the `sarath-spring-ai` collection on `localhost:6334` — all local, no cloud calls, no real API key needed anywhere in the chain.
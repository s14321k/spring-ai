package com.smartshop.customer.springbootai.config.cache;

import org.springframework.ai.chat.cache.semantic.SemanticCache;
import org.springframework.ai.chat.cache.semantic.SemanticCacheAdvisor;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.redis.cache.semantic.DefaultSemanticCache;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.RedisClient;

/**
 * Configuration class for setting up semantic caching with Redis.
 * <p>
 * This class defines the beans required to enable semantic caching,
 * which stores and retrieves cached responses based on semantic similarity
 * rather than exact key matching. It requires a running Redis instance
 * and an embedding model to compute vector representations of cache keys.
 * </p>
 */
@Configuration
public class RedisSemanticCacheConfig {

    /**
     * Creates and configures a {@link RedisClient} bean for connecting
     * to the Redis server.
     * <p>
     * Connection details are read from application properties:
     * <ul>
     *   <li>{@code spring.data.redis.host} — defaults to {@code localhost}</li>
     *   <li>{@code spring.data.redis.port} — defaults to {@code 6379}</li>
     * </ul>
     *
     * @param host the Redis server hostname
     * @param port the Redis server port
     * @return a configured {@link RedisClient} instance
     */
    /** No LLM — Redis client for semantic cache storage ({@code spring.data.redis.host/port}). */
    @Bean
    RedisClient redisClient(
            @Value("${spring.data.redis.host:localhost}") String host,
            @Value("${spring.data.redis.port:6379}") int port) {
        return RedisClient.builder().hostAndPort(host, port).build();
    }

    /**
     * Creates and configures a {@link SemanticCache} bean backed by Redis.
     * <p>
     * The semantic cache uses vector embeddings to find cached entries
     * whose keys are semantically similar to the query, enabling fuzzy
     * cache hits beyond exact string matching.
     * </p>
     *
     * @param redisClient     the Redis client for persistence and vector search
     * @param embeddingModel  the model used to generate embeddings for cache keys
     * @return a configured {@link SemanticCache} instance
     */
    /**
     * Embedding backend: <b>Docker Gemma engine</b> ({@code ai/mxbai-embed-large} at
     * {@code spring.ai.openai.embedding.base-url}, {@code ${IP_ADDRESS}:12434/engines/v1}).
     * No chat LLM — similarity search only.
     */
    @Bean
    public SemanticCache redisSemanticCache(RedisClient redisClient, EmbeddingModel embeddingModel) {
        return DefaultSemanticCache.builder()
                .jedisClient(redisClient)  // Redis client for storing embeddings and cache entries
                .embeddingModel(embeddingModel)  // Embedding model to convert text queries into vector representations
                .similarityThreshold(0.7)   // Minimum cosine similarity (0.0–1.0) required for a cache hit;
                                            // 0.8 ensures only highly semantically similar queries match
                .indexName("sarath-spring-ai-semantic-cache")   // Name of the Redis Search index used for vector similarity queries
                .prefix("cache: ")  // Key prefix applied to all cache entries for namespace isolation
                .build();
    }

    /** No LLM — advisor that intercepts requests to check the Redis semantic cache before calling the LLM. */
    @Bean
    public SemanticCacheAdvisor redisSemanticCacheAdvisor(SemanticCache redisSemanticCache) {
        return SemanticCacheAdvisor.builder().cache(redisSemanticCache).build();
    }
}

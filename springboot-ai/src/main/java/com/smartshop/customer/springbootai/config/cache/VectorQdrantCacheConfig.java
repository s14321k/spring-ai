package com.smartshop.customer.springbootai.config.cache;

import io.qdrant.client.QdrantClient;
import org.springframework.ai.chat.cache.semantic.SemanticCache;
import org.springframework.ai.chat.cache.semantic.SemanticCacheAdvisor;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.qdrant.QdrantVectorStore;
import org.springframework.ai.vectorstore.redis.cache.semantic.DefaultSemanticCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VectorQdrantCacheConfig {

    @Bean
    public VectorStore vectorStoreCache(QdrantClient qdrantClient, EmbeddingModel embeddingModel) {
        return QdrantVectorStore.builder(qdrantClient, embeddingModel)
                .collectionName("spring-ai-vector-cache")
                .initializeSchema(true)
                .build();
    }

    @Bean
    SemanticCache vectorSemanticCache(VectorStore vectorStoreCache, EmbeddingModel embeddingModel) {
        return DefaultSemanticCache.builder()
                .vectorStore(vectorStoreCache)
                .embeddingModel(embeddingModel)
                .similarityThreshold(0.7)
                .build();
    }

    @Bean
    public SemanticCacheAdvisor vectorSemanticCacheAdvisor(SemanticCache vectorSemanticCache) {
        return SemanticCacheAdvisor.builder().cache(vectorSemanticCache).build();
    }
}

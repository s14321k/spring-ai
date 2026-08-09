package com.smartshop.customer.springbootai.rag;

import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SerperWebDocumentRetriever implements DocumentRetriever {

    private static final String SERPER_BASE_URL = "https://google.serper.dev/search";
    private final RestClient restClient;
    private final int resultLimit;

    // Constructor updated to take Builder fields
    public SerperWebDocumentRetriever(RestClient.Builder clientBuilder, int resultLimit) {
        Assert.notNull(clientBuilder, "clientBuilder cannot be null");

        String apiKey = System.getenv("SERPER_API_KEY");
        Assert.hasText(apiKey, "Environment variable SERPER_API_KEY must be set");

        this.restClient = clientBuilder
                .baseUrl(SERPER_BASE_URL)
                .defaultHeader("X-API-KEY", apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .build();

        if (resultLimit <= 0) {
            throw new IllegalArgumentException("resultLimit must be greater than 0");
        }
        this.resultLimit = resultLimit;
    }

    @Override
    public List<Document> retrieve(Query query) {
        Assert.notNull(query, "query cannot be null");

        Map<String, Object> requestBody = Map.of(
                "q", query.text(),
                "num", resultLimit
        );

        SerperResponse response = restClient.post()
                .body(requestBody)
                .retrieve()
                .body(SerperResponse.class);

        if (response == null || response.organic() == null) {
            return List.of();
        }

        List<Document> docs = new ArrayList<>();
        for (SerperHit hit : response.organic()) {
            docs.add(Document.builder()
                    .text(hit.snippet())
                    .metadata("title", hit.title())
                    .metadata("url", hit.link())
                    .build());
        }
        return docs;
    }

    private record SerperResponse(List<SerperHit> organic) {}
    private record SerperHit(String title, String link, String snippet) {}

    // -------------------------------------------------------------
    // BUILDER PATTERN IMPLEMENTATION
    // -------------------------------------------------------------
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private RestClient.Builder clientBuilder;
        private int maxResults = 5; // default fallback

        private Builder() {}

        public Builder restClientBuilder(RestClient.Builder clientBuilder) {
            this.clientBuilder = clientBuilder;
            return this;
        }

        public Builder maxResults(int maxResults) {
            if (maxResults <= 0) {
                throw new IllegalArgumentException("maxResults must be greater than 0");
            }
            this.maxResults = maxResults;
            return this;
        }

        public SerperWebDocumentRetriever build() {
            return new SerperWebDocumentRetriever(clientBuilder, maxResults);
        }
    }
}
package com.smartshop.customer.springbootai.rag;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

public class GoogleWebSearchDocumentRetriever implements DocumentRetriever {

    private static final Logger logger = LoggerFactory.getLogger(GoogleWebSearchDocumentRetriever.class);

    private static final String API_KEY_ENV = "GOOGLE_SEARCH_API_KEY";
    private static final String SEARCH_ENGINE_ID_ENV = "GOOGLE_SEARCH_ENGINE_ID";
    private static final String GOOGLE_BASE_URL = "https://www.googleapis.com/customsearch/v1";

    private final String apiKey;
    private final String searchEngineId;
    private final int resultLimit;
    private final RestClient restClient;

    public GoogleWebSearchDocumentRetriever(RestClient.Builder clientBuilder, int resultLimit) {
        Assert.notNull(clientBuilder, "clientBuilder cannot be null");

        this.apiKey = System.getenv(API_KEY_ENV);
        Assert.hasText(apiKey, "Environment variable " + API_KEY_ENV + " must be set");

        this.searchEngineId = System.getenv(SEARCH_ENGINE_ID_ENV);
        Assert.hasText(searchEngineId, "Environment variable " + SEARCH_ENGINE_ID_ENV + " must be set");

        this.restClient = clientBuilder
                .baseUrl(GOOGLE_BASE_URL)
                .build();

        if (resultLimit <= 0 || resultLimit > 10) {
            // Google API caps max results at 10 per request
            throw new IllegalArgumentException("resultLimit must be between 1 and 10");
        }
        this.resultLimit = resultLimit;
    }

    @Override
    public @NonNull List<Document> retrieve(@NonNull Query query) {
        Assert.notNull(query, "query cannot be null");
        String q = query.text();
        Assert.hasText(q, "query.text() cannot be empty");

        logger.info("Executing Google Search for query: {}", q);

        // Google uses HTTP GET with query parameters: ?key=...&cx=...&q=...&num=...
        GoogleResponsePayload response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("key", apiKey)
                        .queryParam("cx", searchEngineId)
                        .queryParam("q", q)
                        .queryParam("num", resultLimit)
                        .build())
                .retrieve()
                .body(GoogleResponsePayload.class);

        if (response == null || CollectionUtils.isEmpty(response.items())) {
            return List.of();
        }

        List<Document> docs = new ArrayList<>(response.items().size());
        for (GoogleResponsePayload.Item item : response.items()) {
            Document doc = Document.builder()
                    .text(item.snippet())
                    .metadata("title", item.title())
                    .metadata("url", item.link())
                    .build();
            docs.add(doc);
        }

        return docs;
    }

    // Google Custom Search API JSON response records
    private record GoogleResponsePayload(List<Item> items) {
        private record Item(
                String title,
                String link,
                String snippet
        ) {}
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private RestClient.Builder clientBuilder;
        private int resultLimit = 5;

        private Builder() {}

        public Builder restClientBuilder(RestClient.Builder clientBuilder) {
            this.clientBuilder = clientBuilder;
            return this;
        }

        public Builder maxResults(int maxResults) {
            this.resultLimit = maxResults;
            return this;
        }

        public GoogleWebSearchDocumentRetriever build() {
            return new GoogleWebSearchDocumentRetriever(clientBuilder, resultLimit);
        }
    }
}

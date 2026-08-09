package com.smartshop.customer.springbootai.rag;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom Spring AI {@link DocumentRetriever} that grounds RAG in <b>live web search</b>
 * via the <a href="https://tavily.com">Tavily</a> Search API.
 *
 * <p><b>Why this is used:</b> {@code VectorStoreDocumentRetriever} only finds documents
 * already embedded in the app’s vector DB. For current events or public knowledge not
 * loaded offline, this retriever calls Tavily and maps each hit into a Spring AI
 * {@link Document} so {@link org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor}
 * can inject them into the prompt automatically.</p>
 *
 * <p><b>Where it is wired:</b> constructed inside
 * {@code WebSearchRagClientConfig} and registered as the document retriever on the
 * {@code webSearchRAGChatClient} bean — not as a standalone {@code @Bean}.</p>
 *
 * <p><b>Usage:</b> set env {@code TAVILY_SEARCH_API_KEY}, then call any prompt on
 * {@code webSearchRAGChatClient}; the advisor invokes {@link #retrieve(Query)} per turn.</p>
 */
public class WebSearchDocRetriever implements DocumentRetriever {

    private static final Logger logger = LoggerFactory.getLogger(WebSearchDocRetriever.class);

    private static final String TAVILY_API_KEY = "OPEN_API_KEY";
    private static final String TAVILY_BASE_URL = "https://api.tavily.com/search";
    private static final int DEFAULT_RESULT_LIMIT = 5;
    private final int resultLimit;
    private final RestClient restClient;

    public WebSearchDocRetriever(RestClient.Builder clientBuilder, int resultLimit) {
        Assert.notNull(clientBuilder, "clientBuilder cannot be null");
        String apiKey = System.getenv(TAVILY_API_KEY);
        System.out.println("TAVILY_API_KEY: " + apiKey);
        Assert.hasText(apiKey, "Environment variable " + TAVILY_API_KEY + " must be set");
        this.restClient = clientBuilder
                .baseUrl(TAVILY_BASE_URL)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();
        if (resultLimit <= 0) {
            throw new IllegalArgumentException("resultLimit must be greater than 0");
        }
        this.resultLimit = resultLimit;
    }

    /**
     * Retrieves relevant documents from an underlying data source based on the given
     * query.
     *
     * @param query The query to use for retrieving documents
     * @return The list of relevant documents
     */
    @Override
    public @NonNull List<Document> retrieve(Query query) {
        logger.info("Processing query: {}", query.text());
        Assert.notNull(query, "query cannot be null");

        String q = query.text();
        Assert.hasText(q, "query.text() cannot be empty");

        TavilyResponsePayload response = restClient.post()
                .body(new TavilyRequestPayload(q, "advanced", resultLimit))
                .retrieve()
                .body(TavilyResponsePayload.class);

        if (response == null || CollectionUtils.isEmpty(response.results())) {
            return List.of();
        }

        List<Document> docs = new ArrayList<>(response.results().size());
        for (TavilyResponsePayload.Hit hit : response.results()) {
            // Map each Tavily hit into a Spring AI Document with metadata and score.
            Document doc = Document.builder()
                    .text(hit.content())
                    .metadata("title", hit.title())
                    .metadata("url", hit.url())
                    .score(hit.score())
                    .build();
            docs.add(doc);
        }
        return docs;
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    record TavilyRequestPayload(String query, String searchDepth, int maxResults) {}

    record TavilyResponsePayload(List<Hit> results) {
        record Hit(String title, String url, String content, Double score) {}
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private RestClient.Builder clientBuilder;
        private int resultLimit = DEFAULT_RESULT_LIMIT;

        private Builder() {}

        public Builder restClientBuilder(RestClient.Builder clientBuilder) {
            this.clientBuilder = clientBuilder;
            return this;
        }

        public Builder maxResults(int maxResults) {
            if (maxResults <= 0) {
                throw new IllegalArgumentException("maxResults must be greater than 0");
            }
            this.resultLimit = maxResults;
            return this;
        }

        public WebSearchDocRetriever build() {
            return new WebSearchDocRetriever(clientBuilder, resultLimit);
        }
    }
}

package com.study.secubot.core;

import com.study.secubot.llm.LLMClient;
import com.study.secubot.rag.VectorStoreRetriever;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;

public class ReviewEngine {

    private final LLMClient llmClient;
    private final VectorStoreRetriever retriever;
    private final ObjectMapper mapper = new ObjectMapper();

    public ReviewEngine(LLMClient llmClient, VectorStoreRetriever retriever) {
        this.llmClient = llmClient;
        this.retriever = retriever;
    }

    public ReviewResult process(String diff) throws IOException {
        // 1. Retrieve Context (RAG)
        List<String> contextDocs = retriever.retrieve(diff);
        String context = String.join("\n\n", contextDocs);

        // 2. Call LLM
        String llmResponse = llmClient.review(diff, context);

        // 3. Parse LLM Response
        // Expected JSON: { "risk_level": "...", "summary": "..." }
        try {
            JsonNode node = mapper.readTree(llmResponse);
            String riskLevel = node.path("risk_level").asText("UNKNOWN");
            String summary = node.path("summary").asText("No summary provided.");
            return new ReviewResult(riskLevel, summary, context);
        } catch (Exception e) {
            // Fallback if LLM response is not JSON
            return new ReviewResult("UNKNOWN", llmResponse, context);
        }
    }

    public static class ReviewResult {
        public String riskLevel;
        public String summary;
        public String usedContext;

        public ReviewResult(String riskLevel, String summary, String usedContext) {
            this.riskLevel = riskLevel;
            this.summary = summary;
            this.usedContext = usedContext;
        }
    }
}

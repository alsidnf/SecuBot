package com.study.secubot.core;

import java.io.IOException;
import java.util.List;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.secubot.rag.VectorStoreRetriever;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;

@Service
public class ReviewEngine {

    private final ChatModel chatModel;
    private final VectorStoreRetriever retriever;
    private final ObjectMapper mapper = new ObjectMapper();
    private final MessageBuilder messageBuilder;

    public ReviewEngine(ChatModel chatModel, VectorStoreRetriever retriever, MessageBuilder messageBuilder) {
        this.chatModel = chatModel;
        this.retriever = retriever;
        this.messageBuilder = messageBuilder;
    }

    public ReviewResult process(String diff) throws IOException {
        // 1. Retrieve Context (RAG)
        List<String> contextDocs = retriever.retrieve(diff);
        String context = String.join("\n\n", contextDocs);

        // 2. Call LLM
        ChatResponse response = chatModel.chat(
                messageBuilder.buildSecurityReviewRequest(diff, context));
        String llmResponse = response.aiMessage().text();

        // 3. Parse LLM Response
        // Expected JSON: { "risk_level": "...", "summary": "..." }
        try {
            // Clean up potentially markdown-wrapped JSON
            String jsonContent = cleanMarkdownJson(llmResponse);
            JsonNode node = mapper.readTree(jsonContent);
            String riskLevel = node.path("risk_level").asText("UNKNOWN");
            String summary = node.path("summary").asText("No summary provided.");
            return new ReviewResult(riskLevel, summary, context);
        } catch (Exception e) {
            // Fallback if LLM response is not JSON
            return new ReviewResult("UNKNOWN", llmResponse, context);
        }
    }

    private String cleanMarkdownJson(String text) {
        if (text.startsWith("```json")) {
            text = text.substring(7);
        } else if (text.startsWith("```")) {
            text = text.substring(3);
        }
        if (text.endsWith("```")) {
            text = text.substring(0, text.length() - 3);
        }
        return text.trim();
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

package com.study.secubot.llm;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Component
public class InternalLLMClient implements LLMClient {

    private final String endpoint;
    private final String apiKey;
    private final OkHttpClient client;
    private final ObjectMapper mapper;

    public InternalLLMClient(@Value("${secubot.llm.endpoint}") String endpoint,
            @Value("${secubot.llm.api-key}") String apiKey) {
        this.endpoint = endpoint;
        this.apiKey = apiKey;
        this.client = new OkHttpClient();
        this.mapper = new ObjectMapper();
    }

    @Override
    public String review(String diff, String context) throws IOException {
        // System.out.println("Endpoint: " + endpoint); // Sensitive info?

        if ("mock".equalsIgnoreCase(endpoint)) {
            return mockReview(diff);
        }

        // Use the endpoint as provided (Authentication will be via Header)
        String finalUrl = endpoint;

        // Build Request Payload
        String prompt = buildPrompt(diff, context);
        // Gemini structure: { "contents": [{ "parts": [{ "text": "..." }] }] }
        Map<String, Object> part = new HashMap<>();
        part.put("text", prompt);

        Map<String, Object> content = new HashMap<>();
        content.put("parts", java.util.Collections.singletonList(part));

        Map<String, Object> payload = new HashMap<>();
        payload.put("contents", java.util.Collections.singletonList(content));

        String jsonPayload = mapper.writeValueAsString(payload);

        RequestBody body = RequestBody.create(jsonPayload, MediaType.parse("application/json"));

        Request.Builder requestBuilder = new Request.Builder()
                .url(finalUrl)
                .post(body);

        // Add API Key Header if present
        if (apiKey != null && !apiKey.isEmpty()) {
            requestBuilder.header("x-goog-api-key", apiKey);
        }

        Request request = requestBuilder.build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "No body";
                throw new IOException("LLM Request failed: " + response.code() + " - " + errorBody);
            }

            String responseBody = response.body().string();
            return parseGeminiResponse(responseBody);
        }
    }

    private String parseGeminiResponse(String responseBody) throws IOException {
        // Parse Gemini Response to extract the text
        // { "candidates": [ { "content": { "parts": [ { "text": "..." } ] } } ] }
        try {
            com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(responseBody);
            com.fasterxml.jackson.databind.JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && candidates.size() > 0) {
                com.fasterxml.jackson.databind.JsonNode content = candidates.get(0).path("content");
                com.fasterxml.jackson.databind.JsonNode parts = content.path("parts");
                if (parts.isArray() && parts.size() > 0) {
                    String text = parts.get(0).path("text").asText();
                    // The text itself should be the JSON we asked for (Risk Level, Summary)
                    // We need to clean it up potentially (e.g. remove markdown code blocks ```json
                    // ... ```)
                    return cleanMarkdownJson(text);
                }
            }
            return "{\"risk_level\": \"UNKNOWN\", \"summary\": \"Failed to parse Gemini response: " + responseBody
                    + "\"}";
        } catch (Exception e) {
            throw new IOException("Failed to parse Gemini JSON", e);
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

    private String mockReview(String diff) {
        // Simple mock logic logic
        if (diff.contains("Statement") || diff.contains("exec")) {
            return "{\"risk_level\": \"HIGH\", \"summary\": \"Potential SQL Injection detected. Please use PreparedStatement.\"}";
        }
        return "{\"risk_level\": \"LOW\", \"summary\": \"Looks good to me.\"}";
    }

    private String buildPrompt(String diff, String context) {
        // Explicitly ask for JSON format
        return "You are a senior security engineer. Review the following code diff for security vulnerabilities.\n" +
                "Use the provided Context/Security Guidelines if relevant.\n\n" +
                "Context:\n" + context + "\n\n" +
                "Diff:\n" + diff + "\n\n" +
                "IMPORTANT: Respond ONLY with a valid JSON object in the following format:\n" +
                "{ \"risk_level\": \"HIGH\" | \"MEDIUM\" | \"LOW\", \"summary\": \"...\" }\n" +
                "Do not include any explanation outside the JSON.";
    }
}

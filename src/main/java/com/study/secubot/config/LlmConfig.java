package com.study.secubot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

@Configuration
public class LlmConfig {

    @Value("${secubot.llm.provider:gemini}")
    private String provider;

    @Value("${secubot.llm.api-key}")
    private String apiKey;

    @Value("${secubot.llm.model:gemini-2.5-flash}")
    private String modelName;

    @Bean
    public ChatModel chatLanguageModel() {
        if ("openai".equalsIgnoreCase(provider)) {
            return OpenAiChatModel.builder()
                    .apiKey(apiKey)
                    .modelName(modelName)
                    .build();
        } else {
            // Default to Google AI Gemini
            return GoogleAiGeminiChatModel.builder()
                    .apiKey(apiKey)
                    .modelName(modelName)
                    .temperature(0.0) // Security reviews should be deterministic
                    .build();
        }
    }
}

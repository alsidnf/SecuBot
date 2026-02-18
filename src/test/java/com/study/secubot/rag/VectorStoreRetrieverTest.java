package com.study.secubot.rag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

class VectorStoreRetrieverTest {

    private EmbeddingStore<TextSegment> embeddingStore;
    private EmbeddingModel embeddingModel;
    private VectorStoreRetriever retriever;

    @BeforeEach
    void setUp() {
        embeddingStore = new InMemoryEmbeddingStore<>();
        embeddingModel = new AllMiniLmL6V2EmbeddingModel();
        retriever = new VectorStoreRetriever(embeddingStore, embeddingModel);

        // Manually set maxResults since we are not using Spring context here
        // or we could use constructor injection if we modified the class to support it
        // easily without field injection
        // Using ReflectionTestUtils for simplicity as field is private
        ReflectionTestUtils.setField(retriever, "maxResults", 2);
    }

    @Test
    void testRetrieve() {
        // Given
        TextSegment segment1 = TextSegment.from("Security Guideline: Avoid SQL Injection");
        TextSegment segment2 = TextSegment.from("Performance Tip: Use StringBuilder");
        TextSegment segment3 = TextSegment.from("Security Guideline: Use HTTPS");

        embeddingStore.add(embeddingModel.embed(segment1).content(), segment1);
        embeddingStore.add(embeddingModel.embed(segment2).content(), segment2);
        embeddingStore.add(embeddingModel.embed(segment3).content(), segment3);

        // When
        List<String> results = retriever.retrieve("SQL Injection");

        // Then
        assertEquals(2, results.size());
        assertTrue(results.get(0).contains("SQL Injection"));
    }
}

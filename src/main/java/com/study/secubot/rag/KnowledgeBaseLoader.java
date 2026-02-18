package com.study.secubot.rag;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;

@Component
public class KnowledgeBaseLoader {

    private final Path knowledgeBasePath;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;

    public KnowledgeBaseLoader(@Value("${secubot.knowledge-base.path:knowledge-base}") String path,
            EmbeddingStore<TextSegment> embeddingStore,
            EmbeddingModel embeddingModel) {
        this.knowledgeBasePath = Paths.get(path);
        this.embeddingStore = embeddingStore;
        this.embeddingModel = embeddingModel;
    }

    public void load() throws IOException {
        if (!Files.exists(knowledgeBasePath) || !Files.isDirectory(knowledgeBasePath)) {
            System.err.println("Knowledge base directory not found: " + knowledgeBasePath);
            return;
        }

        try (Stream<Path> paths = Files.walk(knowledgeBasePath)) {
            List<Path> markdownFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".md"))
                    .collect(Collectors.toList());

            for (Path file : markdownFiles) {
                String content = Files.readString(file);
                String fileName = file.getFileName().toString();

                Document document = Document.from(content, Metadata.from("filename", fileName));
                // Increased chunk size for better context, with overlap to maintain continuity
                DocumentSplitter splitter = DocumentSplitters.recursive(500, 50);
                List<TextSegment> segments = splitter.split(document);
                List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
                embeddingStore.addAll(embeddings, segments);

                System.out.println("Loaded and embedded security doc: " + fileName);
            }
        }
    }
}

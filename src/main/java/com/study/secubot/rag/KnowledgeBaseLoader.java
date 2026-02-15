package com.study.secubot.rag;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;

public class KnowledgeBaseLoader {

    private final Path knowledgeBasePath;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;

    public KnowledgeBaseLoader(String path, EmbeddingStore<TextSegment> embeddingStore, EmbeddingModel embeddingModel) {
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
                DocumentSplitter splitter = DocumentSplitters.recursive(300, 0);
                List<TextSegment> segments = splitter.split(document);
                List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
                embeddingStore.addAll(embeddings, segments);

                System.out.println("Loaded and embedded security doc: " + fileName);
            }
        }
    }
}

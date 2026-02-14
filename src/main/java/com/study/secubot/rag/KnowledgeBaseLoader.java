package com.study.secubot.rag;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class KnowledgeBaseLoader {

    private final Path knowledgeBasePath;
    private final Map<String, String> knowledgeBase = new HashMap<>();

    public KnowledgeBaseLoader(String path) {
        this.knowledgeBasePath = Paths.get(path);
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
                knowledgeBase.put(fileName, content);
                System.out.println("Loaded security doc: " + fileName);
            }
        }
    }

    public Map<String, String> getKnowledgeBase() {
        return knowledgeBase;
    }
}

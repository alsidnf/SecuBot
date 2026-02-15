package com.study.secubot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.secubot.core.ReviewEngine;
import com.study.secubot.github.GitHubService;
import com.study.secubot.github.GitHubServiceImpl;
import com.study.secubot.llm.InternalLLMClient;
import com.study.secubot.llm.LLMClient;
import com.study.secubot.rag.KnowledgeBaseLoader;
import com.study.secubot.rag.VectorStoreRetriever;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;

@Command(name = "secubot", mixinStandardHelpOptions = true, version = "secubot 1.0", description = "Automated Security Review Bot for Pull Requests")
public class CheckRunner implements Callable<Integer> {

    @Option(names = { "--llm-endpoint" }, description = "LLM API Endpoint", required = true)
    private String llmEndpoint;

    @Option(names = { "--llm-api-key" }, description = "LLM API Key")
    private String llmApiKey;

    @Option(names = { "--github-token" }, description = "GitHub Token")
    private String githubToken;

    @Option(names = { "--pr-url" }, description = "Pull Request URL")
    private String prUrl;

    @Option(names = { "--context-lines" }, defaultValue = "20", description = "Context lines for diff")
    private int contextLines;

    @Option(names = {
            "--knowledge-base-path" }, defaultValue = "knowledge-base", description = "Path to RAG knowledge base")
    private String knowledgeBasePath;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new CheckRunner()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        // 1. Initialize Components
        GitHubService gitHubService = new GitHubServiceImpl(githubToken);
        InternalLLMClient llmClient = new InternalLLMClient(llmEndpoint, llmApiKey);

        // Initialize Vector Search components
        EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();
        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

        // Load Knowledge Base
        KnowledgeBaseLoader kbLoader = new KnowledgeBaseLoader(knowledgeBasePath, embeddingStore, embeddingModel);
        try {
            kbLoader.load();
        } catch (IOException e) {
            System.err.println("Failed to load knowledge base: " + e.getMessage());
            return 1; // Return non-zero to indicate failure
        }

        // Initialize Retriever
        VectorStoreRetriever retriever = new VectorStoreRetriever(embeddingStore, embeddingModel);
        ReviewEngine engine = new ReviewEngine(llmClient, retriever);

        // 2. Resolve PR URL (CLI arg or Event File)
        String targetPrUrl = prUrl;
        if (targetPrUrl == null || targetPrUrl.isEmpty()) {
            targetPrUrl = extractPrUrlFromEvent();
        }

        if (targetPrUrl == null) {
            System.err.println("Error: Could not determine Pull Request URL.");
            return 1;
        }

        System.out.println("Processing PR: " + targetPrUrl);

        // 3. Fetch Diff
        String diff = gitHubService.getPullRequestDiff(targetPrUrl);
        System.out.println("Fetched diff size: " + diff.length());

        // 4. Run Review
        ReviewEngine.ReviewResult result = engine.process(diff);

        // 5. Post Comment
        String commentBody = buildCommentBody(result);
        gitHubService.postComment(targetPrUrl, commentBody);

        // 6. Block if High Risk
        if ("HIGH".equalsIgnoreCase(result.riskLevel) || "CRITICAL".equalsIgnoreCase(result.riskLevel)) {
            System.err.println("Blocking PR due to HIGH/CRITICAL risk.");
            return 1; // Non-zero exit code to fail the action
        }

        return 0;
    }

    private String extractPrUrlFromEvent() {
        String eventPath = System.getenv("GITHUB_EVENT_PATH");
        if (eventPath == null)
            return null;

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(new File(eventPath));
            // For pull_request event
            return root.path("pull_request").path("url").asText(null);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String buildCommentBody(ReviewEngine.ReviewResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("### \uD83D\uDEA8 SecuBot Security Review\n\n");
        sb.append("**Risk Level**: `").append(result.riskLevel).append("`\n\n");
        sb.append("**Summary**:\n").append(result.summary).append("\n\n");

        if (result.usedContext != null && !result.usedContext.isEmpty()) {
            sb.append("<details><summary> Referenced Security Guidelines </summary>\n\n");
            sb.append(result.usedContext);
            sb.append("\n</details>");
        }
        return sb.toString();
    }
}

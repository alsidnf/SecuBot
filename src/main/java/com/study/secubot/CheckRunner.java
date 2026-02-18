package com.study.secubot;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.secubot.core.ReviewEngine;
import com.study.secubot.github.GitHubService;
import com.study.secubot.rag.KnowledgeBaseLoader;

import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Component
@Command(name = "secubot", mixinStandardHelpOptions = true, version = "secubot 1.0", description = "Automated Security Review Bot for Pull Requests")
@Slf4j
public class CheckRunner implements CommandLineRunner, Callable<Integer>, ExitCodeGenerator {

    private final GitHubService gitHubService;
    private final ReviewEngine engine;
    private final KnowledgeBaseLoader kbLoader;

    private int exitCode;

    @Value("${pr-url:}")
    private String prUrl;

    @Value("${context-lines:20}")
    private int contextLines;

    public CheckRunner(GitHubService gitHubService, ReviewEngine engine, KnowledgeBaseLoader kbLoader) {
        this.gitHubService = gitHubService;
        this.engine = engine;
        this.kbLoader = kbLoader;
    }

    @Override
    public void run(String... args) throws Exception {
        this.exitCode = new CommandLine(this).execute(args);
    }

    @Override
    public int getExitCode() {
        return this.exitCode;
    }

    @Override
    public Integer call() {
        // 1. Load Knowledge Base
        try {
            kbLoader.load();
        } catch (IOException e) {
            log.error("Failed to load knowledge base: " + e.getMessage());
            return 1;
        }

        // 2. Resolve PR URL (CLI arg or Event File)
        String targetPrUrl = prUrl;
        if (targetPrUrl == null || targetPrUrl.isEmpty()) {
            targetPrUrl = extractPrUrlFromEvent();
        }

        if (targetPrUrl == null) {
            log.error("Error: Could not determine Pull Request URL.");
            return 1;
        }

        log.info("Processing PR: " + targetPrUrl);

        try {
            // 3. Fetch Diff
            String diff = gitHubService.getPullRequestDiff(targetPrUrl);
            log.info("Fetched diff size: " + diff.length());

            // 4. Run Review
            log.info("Running security review...");
            ReviewEngine.ReviewResult result = engine.process(diff);

            // 5. Post Comment
            log.info("Posting comment...");
            String commentBody = buildCommentBody(result);
            gitHubService.postComment(targetPrUrl, commentBody);

            // 6. Block if High Risk
            if ("HIGH".equalsIgnoreCase(result.riskLevel) || "CRITICAL".equalsIgnoreCase(result.riskLevel)) {
                log.error("Blocking PR due to HIGH/CRITICAL risk.");
                return 1;
            }

            return 0;

        } catch (Exception e) {
            log.error("Error processing PR: " + e.getMessage());
            return 1;
        }
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

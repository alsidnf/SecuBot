package com.study.secubot.github;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GitHubServiceImpl implements GitHubService {

    private final String token;
    private final OkHttpClient client;
    private final ObjectMapper mapper;

    public GitHubServiceImpl(@Value("${secubot.github.token}") String token) {
        this.token = token;
        this.client = new OkHttpClient();
        this.mapper = new ObjectMapper();
    }

    @Override
    public String getPullRequestDiff(String prUrl) throws IOException {
        // prUrl e.g. https://api.github.com/repos/owner/repo/pulls/1
        Request request = new Request.Builder()
                .url(prUrl)
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/vnd.github.v3.diff")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful())
                throw new IOException("Unexpected code " + response);
            return response.body().string();
        }
    }

    @Override
    public void postComment(String prUrl, String body) throws IOException {
        // prUrl is pull request url, but comments are posted to issues endpoint
        // Or if review comment, pull requests endpoint.
        // Simple comment: POST /repos/{owner}/{repo}/issues/{issue_number}/comments

        // We need to transform PR URL to Issue URL or use the "comments_url" from PR
        // details
        // For simplicity, let's assume we are passed the comments URL directly or we
        // construct it.
        // If prUrl is passed as "https://api.github.com/repos/owner/repo/pulls/1",
        // convert to "https://api.github.com/repos/owner/repo/issues/1/comments"

        String commentsUrl = prUrl.replace("/pulls/", "/issues/") + "/comments";

        String json = mapper.writeValueAsString(new CommentRequest(body));
        RequestBody requestBody = RequestBody.create(json, MediaType.parse("application/json"));

        Request request = new Request.Builder()
                .url(commentsUrl)
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/vnd.github.v3+json")
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                // log but maybe not throw?
                System.err.println("Failed to post comment: " + response.body().string());
            }
        }
    }

    private static class CommentRequest {
        public String body;

        public CommentRequest(String body) {
            this.body = body;
        }
    }
}

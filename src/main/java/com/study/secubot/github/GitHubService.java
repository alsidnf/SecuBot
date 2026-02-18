package com.study.secubot.github;

import java.io.IOException;

public interface GitHubService {
    String getPullRequestDiff(String prUrl) throws IOException;

    void postComment(String prUrl, String body) throws IOException;
}

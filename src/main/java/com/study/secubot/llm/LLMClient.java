package com.study.secubot.llm;

import java.io.IOException;

public interface LLMClient {
    String review(String diff, String context) throws IOException;
}

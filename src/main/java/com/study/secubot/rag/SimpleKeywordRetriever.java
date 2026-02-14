package com.study.secubot.rag;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SimpleKeywordRetriever {

    private final Map<String, String> knowledgeBase;

    public SimpleKeywordRetriever(Map<String, String> knowledgeBase) {
        this.knowledgeBase = knowledgeBase;
    }

    public List<String> retrieve(String codeDiff) {
        List<String> relevantDocs = new ArrayList<>();

        // Naive implementation for PoC:
        // Check for specific keywords associated with known vulnerabilities.
        // In a real system, this would be a vector search or a more sophisticated
        // keyword mapping.

        if (containsSqlInjectionKeywords(codeDiff)) {
            String doc = knowledgeBase.get("SQL_Injection_Prevention.md");
            if (doc != null) {
                relevantDocs.add(doc);
            }
        }

        // Add more checks here for other vulnerabilities

        return relevantDocs;
    }

    private boolean containsSqlInjectionKeywords(String codeDiff) {
        String lowerDiff = codeDiff.toLowerCase();
        // Check for common SQL injection patterns in the diff
        return (lowerDiff.contains("select") || lowerDiff.contains("insert") || lowerDiff.contains("update"))
                && (lowerDiff.contains("+") || lowerDiff.contains("concat"));
    }
}

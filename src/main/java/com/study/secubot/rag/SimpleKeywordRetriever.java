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
            addDocIfPresent(relevantDocs, "SQL_Injection_Prevention.md");
        }
        if (containsXssKeywords(codeDiff)) {
            addDocIfPresent(relevantDocs, "XSS_Prevention.md");
        }
        if (containsSensitveDataKeywords(codeDiff)) {
            addDocIfPresent(relevantDocs, "Sensitive_Data_Exposure.md");
        }
        if (containsGdprKeywords(codeDiff)) {
            addDocIfPresent(relevantDocs, "GDPR_Personal_Data_Encryption.md");
        }

        return relevantDocs;
    }

    private void addDocIfPresent(List<String> docs, String filename) {
        String doc = knowledgeBase.get(filename);
        if (doc != null) {
            docs.add(doc);
        }
    }

    private boolean containsSqlInjectionKeywords(String codeDiff) {
        String lowerDiff = codeDiff.toLowerCase();
        return (lowerDiff.contains("select") || lowerDiff.contains("insert") || lowerDiff.contains("update"))
                && (lowerDiff.contains("+") || lowerDiff.contains("concat"));
    }

    private boolean containsXssKeywords(String codeDiff) {
        String lowerDiff = codeDiff.toLowerCase();
        return lowerDiff.contains("request.getparameter") || lowerDiff.contains("response.getwriter")
                || lowerDiff.contains("innerhtml") || lowerDiff.contains("redirect");
    }

    private boolean containsSensitveDataKeywords(String codeDiff) {
        String lowerDiff = codeDiff.toLowerCase();
        return lowerDiff.contains("apikey") || lowerDiff.contains("password") || lowerDiff.contains("secret")
                || lowerDiff.contains("token") || lowerDiff.contains("printstacktrace");
    }

    private boolean containsGdprKeywords(String codeDiff) {
        String lowerDiff = codeDiff.toLowerCase();
        return lowerDiff.contains("email") || lowerDiff.contains("phone") || lowerDiff.contains("ssn")
                || lowerDiff.contains("user") || lowerDiff.contains("address");
    }
}

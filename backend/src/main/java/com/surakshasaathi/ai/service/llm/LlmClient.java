package com.surakshasaathi.ai.service.llm;

import java.util.Map;

public interface LlmClient {
    String chatCompletions(Map<String, Object> requestBody);
}

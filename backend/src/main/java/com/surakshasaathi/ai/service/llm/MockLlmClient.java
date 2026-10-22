package com.surakshasaathi.ai.service.llm;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;

// Used only in specific contexts if needed; default prod/test behavior is real client per config
@Component
@Profile("mock-llm")
public class MockLlmClient implements LlmClient {
    @Override
    public String chatCompletions(Map<String, Object> requestBody) {
        return null;
    }
}

package com.surakshasaathi.ai.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.surakshasaathi.ai.dto.analysis.LlmVerdict;
import com.surakshasaathi.ai.dto.analysis.RiskLevel;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LlmPromptServiceParseTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void parsesCleanJson() {
        String json = """
                {"riskLevel":"HIGH","simpleExplanation":"Beware of this message.",
                 "safeActions":["Stop and think"],"prohibitedActions":["Never share OTP"],
                 "verificationSteps":"Check the official app."}
                """;
        LlmVerdict v = LlmPromptServiceImpl.parseJson(json, mapper);
        assertThat(v).isNotNull();
        assertThat(v.riskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(v.safeActions()).containsExactly("Stop and think");
    }

    @Test
    void parsesMarkdownFencesAndSurroundingProse() {
        String content = "Sure! Here is the analysis:\n"
                + "```json\n"
                + "{\"riskLevel\": \"medium\", \"simpleExplanation\": \"Thoda sa shak hai\", "
                + "\"safeActions\": [\"Ask a trusted person\"], \"prohibitedActions\": [], "
                + "\"verificationSteps\": \"Official site par verify karein\"}\n"
                + "```";
        LlmVerdict v = LlmPromptServiceImpl.parseJson(content, mapper);
        assertThat(v).isNotNull();
        assertThat(v.riskLevel()).isEqualTo(RiskLevel.MEDIUM); // case-insensitive enum
    }

    @Test
    void emptyContentReturnsNull() {
        assertThat(LlmPromptServiceImpl.parseJson("", mapper)).isNull();
        assertThat(LlmPromptServiceImpl.parseJson("   ", mapper)).isNull();
        assertThat(LlmPromptServiceImpl.parseJson(null, mapper)).isNull();
    }

    @Test
    void invalidJsonReturnsNull() {
        assertThat(LlmPromptServiceImpl.parseJson("I am sorry, I cannot do that.", mapper)).isNull();
        assertThat(LlmPromptServiceImpl.parseJson("{ not json", mapper)).isNull();
    }

    @Test
    void missingRequiredFieldsReturnsNull() {
        String json = """
                {"safeActions":["x"],"prohibitedActions":[],"verificationSteps":"y"}
                """;
        assertThat(LlmPromptServiceImpl.parseJson(json, mapper)).isNull();
    }
}
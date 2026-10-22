package com.surakshasaathi.ai.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-context web-layer contract test for the authentication the React client
 * now relies on. Boots the real Spring context (security filters + JPA + H2) so
 * that a 401-vs-200 regression is caught end-to-end.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void publicEmergencyGuidelineNeedsNoToken() throws Exception {
        mvc.perform(get("/api/v1/emergency/guideline").param("language", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void protectedAnalysisIsUnauthorizedWithoutToken() throws Exception {
        mvc.perform(post("/api/v1/analyze/text")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Your KYC has expired\",\"language\":\"en\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void wrongPasswordReturnsUnauthorized() throws Exception {
        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nobody@example.com\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registerReturnsTokenThatUnlocksAnalysis() throws Exception {
        String registerBody = objectMapper.writeValueAsString(Map.of(
                "name", "Integration User",
                "email", "integration.user@example.com",
                "password", "secret1"));

        String response = mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        JsonNode data = objectMapper.readTree(response).path("data");
        String token = data.path("accessToken").asText();
        assertThat(token).isNotBlank();

        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("integration.user@example.com"));

        mvc.perform(post("/api/v1/analyze/text")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Your KYC has expired. Share the OTP now.\",\"language\":\"en\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.riskLevel").value("HIGH"));
    }
}

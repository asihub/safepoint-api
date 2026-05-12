package com.safepoint.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.safepoint.api.config.SecurityConfig;

import com.safepoint.api.model.AnalysisResponse;
import com.safepoint.api.service.AnalysisService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AnalysisController.class)
@Import(SecurityConfig.class)
class AnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AnalysisService analysisService;

    private AnalysisResponse stubResponse(String riskLevel, boolean show988) {
        return AnalysisResponse.builder()
                .riskLevel(riskLevel)
                .confidence(0.85)
                .phq9Score(5)
                .gad7Score(3)
                .show988(show988)
                .explanation("Test explanation")
                .build();
    }

    @Test @DisplayName("Valid request, questionnaire only → 200 with riskLevel and show988")
    void valid_request_questionnaire_only() throws Exception {
        when(analysisService.analyze(any())).thenReturn(stubResponse("LOW", false));

        mockMvc.perform(post("/api/v1/analysis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "questionnaireScores", Map.of("phq9", 5, "gad7", 3)
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riskLevel").value("LOW"))
                .andExpect(jsonPath("$.show988").value(false));
    }

    @Test @DisplayName("Valid request with freeText → 200")
    void valid_request_with_free_text() throws Exception {
        when(analysisService.analyze(any())).thenReturn(stubResponse("MEDIUM", false));

        mockMvc.perform(post("/api/v1/analysis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "questionnaireScores", Map.of("phq9", 5, "gad7", 3),
                                "freeText", "I have been struggling lately with anxiety and sleep issues."
                        ))))
                .andExpect(status().isOk());
    }

    @Test @DisplayName("freeText exceeds 5000 chars → 400")
    void free_text_too_long_returns_400() throws Exception {
        mockMvc.perform(post("/api/v1/analysis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "questionnaireScores", Map.of("phq9", 5, "gad7", 3),
                                "freeText", "a".repeat(5001)
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test @DisplayName("Empty body → 400")
    void empty_body_returns_400() throws Exception {
        mockMvc.perform(post("/api/v1/analysis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest());
    }

    @Test @DisplayName("Invalid content type → 415")
    void invalid_content_type_returns_415() throws Exception {
        mockMvc.perform(post("/api/v1/analysis")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("plain text body"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test @DisplayName("Response contains all expected fields")
    void response_contains_all_expected_fields() throws Exception {
        when(analysisService.analyze(any())).thenReturn(stubResponse("HIGH", true));

        mockMvc.perform(post("/api/v1/analysis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "questionnaireScores", Map.of("phq9", 5, "gad7", 3)
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riskLevel").exists())
                .andExpect(jsonPath("$.confidence").exists())
                .andExpect(jsonPath("$.phq9Score").exists())
                .andExpect(jsonPath("$.gad7Score").exists())
                .andExpect(jsonPath("$.show988").exists())
                .andExpect(jsonPath("$.explanation").exists());
    }
}

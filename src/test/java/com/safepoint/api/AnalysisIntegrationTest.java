package com.safepoint.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.safepoint.api.model.AnalysisResponse;
import com.safepoint.api.model.AnalysisResponse.MlAnalysisResult;
import com.safepoint.api.service.MlService;
import com.safepoint.api.service.TranslationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AnalysisIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private MlService mlService;

    @MockitoBean
    private TranslationService translationService;

    // 15 words, 8 unique — passes the quality gate
    private static final String PASSING_TEXT =
            "one two three four five six seven eight one two three four five six seven";

    // 10 words — fails the quality gate (< 15 words)
    private static final String SHORT_TEXT = "I feel sad today and very tired all the day";

    private MlAnalysisResult mlResult(String riskLevel, double confidence) {
        return MlAnalysisResult.builder()
                .riskLevel(riskLevel)
                .confidence(confidence)
                .signals(List.of())
                .scores(AnalysisResponse.MlScores.builder().low(0.1).medium(0.1).high(0.8).build())
                .build();
    }

    private String body(Map<String, Object> map) throws Exception {
        return objectMapper.writeValueAsString(map);
    }

    @Test @DisplayName("Questionnaire only — LOW: phq9=2, gad7=1 → 200, riskLevel=LOW, show988=false")
    void questionnaire_only_low() throws Exception {
        mockMvc.perform(post("/api/v1/analysis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("questionnaireScores", Map.of("phq9", 2, "gad7", 1)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riskLevel").value("LOW"))
                .andExpect(jsonPath("$.show988").value(false));
    }

    @Test @DisplayName("Questionnaire only — MEDIUM: phq9=10 → 200, riskLevel=MEDIUM")
    void questionnaire_only_medium() throws Exception {
        mockMvc.perform(post("/api/v1/analysis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("questionnaireScores", Map.of("phq9", 10, "gad7", 0)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riskLevel").value("MEDIUM"));
    }

    @Test @DisplayName("Questionnaire only — HIGH: phq9=20 → 200, riskLevel=HIGH, show988=true")
    void questionnaire_only_high() throws Exception {
        mockMvc.perform(post("/api/v1/analysis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("questionnaireScores", Map.of("phq9", 20, "gad7", 0)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riskLevel").value("HIGH"))
                .andExpect(jsonPath("$.show988").value(true));
    }

    @Test @DisplayName("ML raises final risk: phq9=5, freeText passes gate, ML=HIGH/0.90 → riskLevel=HIGH")
    void ml_raises_final_risk() throws Exception {
        when(mlService.analyze(anyString())).thenReturn(mlResult("HIGH", 0.90));

        mockMvc.perform(post("/api/v1/analysis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of(
                                "questionnaireScores", Map.of("phq9", 5, "gad7", 0),
                                "freeText", PASSING_TEXT
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riskLevel").value("HIGH"));
    }

    @Test @DisplayName("ML below confidence — ignored: phq9=5, ML=HIGH/0.50 → riskLevel=LOW")
    void ml_below_confidence_ignored() throws Exception {
        when(mlService.analyze(anyString())).thenReturn(mlResult("HIGH", 0.50));

        mockMvc.perform(post("/api/v1/analysis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of(
                                "questionnaireScores", Map.of("phq9", 5, "gad7", 0),
                                "freeText", PASSING_TEXT
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riskLevel").value("LOW"));
    }

    @Test @DisplayName("ML service down: phq9=10, ML throws → 200, riskLevel=MEDIUM (questionnaire only)")
    void ml_service_down_falls_back_to_questionnaire() throws Exception {
        when(mlService.analyze(anyString())).thenThrow(new RuntimeException("ML service down"));

        mockMvc.perform(post("/api/v1/analysis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of(
                                "questionnaireScores", Map.of("phq9", 10, "gad7", 0),
                                "freeText", PASSING_TEXT
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riskLevel").value("MEDIUM"));
    }

    @Test @DisplayName("Text fails quality gate: phq9=5, freeText=10 words → ML never called")
    void text_fails_quality_gate_ml_never_called() throws Exception {
        mockMvc.perform(post("/api/v1/analysis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of(
                                "questionnaireScores", Map.of("phq9", 5, "gad7", 0),
                                "freeText", SHORT_TEXT
                        ))))
                .andExpect(status().isOk());

        verify(mlService, never()).analyze(anyString());
    }

    @Test @DisplayName("lang=es triggers translation: TranslationService called")
    void lang_es_triggers_translation() throws Exception {
        when(translationService.translateToEnglish(anyString(), eq("es"))).thenReturn(PASSING_TEXT);
        when(mlService.analyze(anyString())).thenReturn(null);

        mockMvc.perform(post("/api/v1/analysis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of(
                                "questionnaireScores", Map.of("phq9", 5, "gad7", 0),
                                "freeText", PASSING_TEXT,
                                "lang", "es"
                        ))))
                .andExpect(status().isOk());

        verify(translationService).translateToEnglish(anyString(), eq("es"));
    }
}

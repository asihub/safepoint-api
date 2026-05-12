package com.safepoint.api.service;

import com.safepoint.api.model.AnalysisRequest;
import com.safepoint.api.model.AnalysisResponse;
import com.safepoint.api.model.AnalysisResponse.MlAnalysisResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalysisServiceTest {

    @Mock
    private MlService mlService;

    @Mock
    private TranslationService translationService;

    @InjectMocks
    private AnalysisService analysisService;

    // 15 words, 8 unique — passes the quality gate
    private static final String PASSING_TEXT =
            "one two three four five six seven eight one two three four five six seven";

    private AnalysisRequest requestWithScores(int phq9, int gad7) {
        AnalysisRequest req = new AnalysisRequest();
        req.setQuestionnaireScores(Map.of("phq9", phq9, "gad7", gad7));
        return req;
    }

    private MlAnalysisResult mlResult(String riskLevel, double confidence) {
        return MlAnalysisResult.builder()
                .riskLevel(riskLevel)
                .confidence(confidence)
                .signals(List.of())
                .scores(AnalysisResponse.MlScores.builder().low(0.1).medium(0.2).high(0.7).build())
                .build();
    }

    // ── PHQ-9 / GAD-7 scoring thresholds ─────────────────────────────────────

    @Test @DisplayName("Both zero → LOW")
    void phq9_gad7_both_zero() {
        assertThat(analysisService.analyze(requestWithScores(0, 0)).getRiskLevel()).isEqualTo("LOW");
    }

    @Test @DisplayName("PHQ-9 just below moderate (9) → LOW")
    void phq9_below_moderate() {
        assertThat(analysisService.analyze(requestWithScores(9, 0)).getRiskLevel()).isEqualTo("LOW");
    }

    @Test @DisplayName("PHQ-9 at moderate boundary (10) → MEDIUM")
    void phq9_at_moderate() {
        assertThat(analysisService.analyze(requestWithScores(10, 0)).getRiskLevel()).isEqualTo("MEDIUM");
    }

    @Test @DisplayName("GAD-7 at moderate boundary (10) → MEDIUM")
    void gad7_at_moderate() {
        assertThat(analysisService.analyze(requestWithScores(0, 10)).getRiskLevel()).isEqualTo("MEDIUM");
    }

    @Test @DisplayName("PHQ-9 at severe boundary (20) → HIGH")
    void phq9_at_severe() {
        assertThat(analysisService.analyze(requestWithScores(20, 0)).getRiskLevel()).isEqualTo("HIGH");
    }

    @Test @DisplayName("GAD-7 at severe boundary (15) → HIGH")
    void gad7_at_severe() {
        assertThat(analysisService.analyze(requestWithScores(0, 15)).getRiskLevel()).isEqualTo("HIGH");
    }

    @Test @DisplayName("Both severe → HIGH")
    void both_severe() {
        assertThat(analysisService.analyze(requestWithScores(20, 15)).getRiskLevel()).isEqualTo("HIGH");
    }

    @Test @DisplayName("Higher of the two wins — PHQ-9=20 GAD-7=5 → HIGH")
    void higher_of_two_wins() {
        assertThat(analysisService.analyze(requestWithScores(20, 5)).getRiskLevel()).isEqualTo("HIGH");
    }

    // ── Text quality gate ─────────────────────────────────────────────────────

    @Test @DisplayName("Null free text → ML not called")
    void null_free_text_skips_ml() {
        AnalysisRequest req = requestWithScores(5, 3);
        req.setFreeText(null);
        analysisService.analyze(req);
        verify(mlService, never()).analyze(anyString());
    }

    @Test @DisplayName("Blank free text → ML not called")
    void blank_free_text_skips_ml() {
        AnalysisRequest req = requestWithScores(5, 3);
        req.setFreeText("   ");
        analysisService.analyze(req);
        verify(mlService, never()).analyze(anyString());
    }

    @Test @DisplayName("14 words, 8 unique → ML not called")
    void fourteen_words_eight_unique_skips_ml() {
        // 14 words, 8 unique (one,two,three,four,five,six,seven,eight)
        String text = "one two three four five six seven eight one two three four five six";
        AnalysisRequest req = requestWithScores(5, 3);
        req.setFreeText(text);
        analysisService.analyze(req);
        verify(mlService, never()).analyze(anyString());
    }

    @Test @DisplayName("15 words, 7 unique → ML not called")
    void fifteen_words_seven_unique_skips_ml() {
        // 15 words, 7 unique (one,two,three,four,five,six,seven)
        String text = "one two three four five six seven one two three four five six seven one";
        AnalysisRequest req = requestWithScores(5, 3);
        req.setFreeText(text);
        analysisService.analyze(req);
        verify(mlService, never()).analyze(anyString());
    }

    @Test @DisplayName("15 words, 8 unique → ML called")
    void fifteen_words_eight_unique_calls_ml() {
        AnalysisRequest req = requestWithScores(5, 3);
        req.setFreeText(PASSING_TEXT);
        when(mlService.analyze(anyString())).thenReturn(null);
        analysisService.analyze(req);
        verify(mlService).analyze(anyString());
    }

    @Test @DisplayName("20 words, 10 unique → ML called")
    void twenty_words_ten_unique_calls_ml() {
        // 20 words, 10 unique (one through ten)
        String text = "one two three four five six seven eight nine ten " +
                      "one two three four five six seven eight nine ten";
        AnalysisRequest req = requestWithScores(5, 3);
        req.setFreeText(text);
        when(mlService.analyze(anyString())).thenReturn(null);
        analysisService.analyze(req);
        verify(mlService).analyze(anyString());
    }

    // ── ML confidence threshold ───────────────────────────────────────────────

    @Test @DisplayName("ML confidence 0.59 → ML risk ignored, questionnaire only")
    void ml_confidence_below_threshold_ignored() {
        AnalysisRequest req = requestWithScores(5, 3); // questionnaire = LOW
        req.setFreeText(PASSING_TEXT);
        when(mlService.analyze(anyString())).thenReturn(mlResult("HIGH", 0.59));
        assertThat(analysisService.analyze(req).getRiskLevel()).isEqualTo("LOW");
    }

    @Test @DisplayName("ML confidence exactly 0.60 → ML risk used")
    void ml_confidence_at_threshold_used() {
        AnalysisRequest req = requestWithScores(5, 3); // questionnaire = LOW
        req.setFreeText(PASSING_TEXT);
        when(mlService.analyze(anyString())).thenReturn(mlResult("HIGH", 0.60));
        assertThat(analysisService.analyze(req).getRiskLevel()).isEqualTo("HIGH");
    }

    @Test @DisplayName("ML confidence 0.90 → ML risk used")
    void ml_confidence_high_used() {
        AnalysisRequest req = requestWithScores(5, 3); // questionnaire = LOW
        req.setFreeText(PASSING_TEXT);
        when(mlService.analyze(anyString())).thenReturn(mlResult("MEDIUM", 0.90));
        assertThat(analysisService.analyze(req).getRiskLevel()).isEqualTo("MEDIUM");
    }

    // ── Risk combination — max(questionnaireRisk, mlRisk) ────────────────────

    private AnalysisResponse combineRisk(String qRisk, String mlRisk, double conf) {
        int phq9 = switch (qRisk) { case "HIGH" -> 20; case "MEDIUM" -> 10; default -> 5; };
        AnalysisRequest req = requestWithScores(phq9, 0);
        req.setFreeText(PASSING_TEXT);
        when(mlService.analyze(anyString())).thenReturn(mlResult(mlRisk, conf));
        return analysisService.analyze(req);
    }

    @Test @DisplayName("LOW + LOW → LOW")       void combine_low_low()       { assertThat(combineRisk("LOW",    "LOW",    0.9).getRiskLevel()).isEqualTo("LOW"); }
    @Test @DisplayName("LOW + MEDIUM → MEDIUM") void combine_low_medium()    { assertThat(combineRisk("LOW",    "MEDIUM", 0.9).getRiskLevel()).isEqualTo("MEDIUM"); }
    @Test @DisplayName("LOW + HIGH → HIGH")     void combine_low_high()      { assertThat(combineRisk("LOW",    "HIGH",   0.9).getRiskLevel()).isEqualTo("HIGH"); }
    @Test @DisplayName("MEDIUM + LOW → MEDIUM") void combine_medium_low()    { assertThat(combineRisk("MEDIUM", "LOW",    0.9).getRiskLevel()).isEqualTo("MEDIUM"); }
    @Test @DisplayName("MEDIUM + MEDIUM → MEDIUM") void combine_medium_medium() { assertThat(combineRisk("MEDIUM", "MEDIUM", 0.9).getRiskLevel()).isEqualTo("MEDIUM"); }
    @Test @DisplayName("MEDIUM + HIGH → HIGH")  void combine_medium_high()   { assertThat(combineRisk("MEDIUM", "HIGH",   0.9).getRiskLevel()).isEqualTo("HIGH"); }
    @Test @DisplayName("HIGH + LOW → HIGH")     void combine_high_low()      { assertThat(combineRisk("HIGH",   "LOW",    0.9).getRiskLevel()).isEqualTo("HIGH"); }
    @Test @DisplayName("HIGH + MEDIUM → HIGH")  void combine_high_medium()   { assertThat(combineRisk("HIGH",   "MEDIUM", 0.9).getRiskLevel()).isEqualTo("HIGH"); }
    @Test @DisplayName("HIGH + HIGH → HIGH")    void combine_high_high()     { assertThat(combineRisk("HIGH",   "HIGH",   0.9).getRiskLevel()).isEqualTo("HIGH"); }

    // ── ML unavailable ────────────────────────────────────────────────────────

    @Test @DisplayName("MlService returns null → finalRisk = questionnaireRisk")
    void ml_returns_null_falls_back_to_questionnaire() {
        AnalysisRequest req = requestWithScores(10, 0); // MEDIUM
        req.setFreeText(PASSING_TEXT);
        when(mlService.analyze(anyString())).thenReturn(null);
        assertThat(analysisService.analyze(req).getRiskLevel()).isEqualTo("MEDIUM");
    }

    // ── show988 ───────────────────────────────────────────────────────────────

    @Test @DisplayName("finalRisk LOW → show988 false")
    void show988_low()   { assertThat(analysisService.analyze(requestWithScores(5,  0)).isShow988()).isFalse(); }

    @Test @DisplayName("finalRisk MEDIUM → show988 false")
    void show988_medium() { assertThat(analysisService.analyze(requestWithScores(10, 0)).isShow988()).isFalse(); }

    @Test @DisplayName("finalRisk HIGH → show988 true")
    void show988_high()  { assertThat(analysisService.analyze(requestWithScores(20, 0)).isShow988()).isTrue(); }

    // ── Translation ───────────────────────────────────────────────────────────

    @Test @DisplayName("lang=en, text passes gate → TranslationService NOT called")
    void lang_en_no_translation() {
        AnalysisRequest req = requestWithScores(5, 3);
        req.setFreeText(PASSING_TEXT);
        req.setLang("en");
        when(mlService.analyze(anyString())).thenReturn(null);
        analysisService.analyze(req);
        verify(translationService, never()).translateToEnglish(anyString(), anyString());
    }

    @Test @DisplayName("lang=es, text passes gate → TranslationService called before ML")
    void lang_es_translation_called() {
        AnalysisRequest req = requestWithScores(5, 3);
        req.setFreeText(PASSING_TEXT);
        req.setLang("es");
        when(translationService.translateToEnglish(PASSING_TEXT, "es")).thenReturn(PASSING_TEXT);
        when(mlService.analyze(anyString())).thenReturn(null);
        analysisService.analyze(req);
        verify(translationService).translateToEnglish(PASSING_TEXT, "es");
    }

    @Test @DisplayName("lang=null, text passes gate → TranslationService NOT called")
    void lang_null_no_translation() {
        AnalysisRequest req = requestWithScores(5, 3);
        req.setFreeText(PASSING_TEXT);
        req.setLang(null);
        when(mlService.analyze(anyString())).thenReturn(null);
        analysisService.analyze(req);
        verify(translationService, never()).translateToEnglish(anyString(), anyString());
    }
}

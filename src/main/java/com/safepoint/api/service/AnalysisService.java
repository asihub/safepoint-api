package com.safepoint.api.service;

import com.safepoint.api.model.AnalysisRequest;
import com.safepoint.api.model.dto.AnalysisResponse;
import com.safepoint.api.model.dto.AnalysisResponse.MlAnalysisResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class AnalysisService {

    private final MlService mlService;
    private final TranslationService translationService;

    private static final int PHQ9_MODERATE = 10;
    private static final int PHQ9_SEVERE   = 20;
    private static final int GAD7_MODERATE = 10;
    private static final int GAD7_SEVERE   = 15;
    private static final double ML_HIGH_OVERRIDE_THRESHOLD = 0.35;

    /**
     * Orchestrates the full risk assessment pipeline:
     * 1. Score questionnaire results (PHQ-9, GAD-7)
     * 2. Translate free text to English if UI language is not English
     * 3. Call ML service for free-text analysis
     * 4. Combine signals into final risk level
     * 5. Determine whether 988 should be shown
     */
    public AnalysisResponse analyze(AnalysisRequest request) {
        // Step 1: Score questionnaires
        String questionnaireRisk = scoreQuestionnaires(request.getQuestionnaireScores());

        // Step 2: Translate if needed + ML analysis
        MlAnalysisResult mlResult = null;
        if (request.getFreeText() != null && !request.getFreeText().isBlank()) {
            String textForMl = request.getFreeText();

            // If user selected a non-English UI language — translate before ML
            String lang = request.getLang();
            if (lang != null && !lang.equalsIgnoreCase("en")) {
                textForMl = translationService.translateToEnglish(textForMl, lang);
                log.info("Translated free text from {} to en for ML analysis", lang);
            }

            mlResult = mlService.analyze(textForMl);
        }

        // Step 3: Combine into final risk level
        String finalRisk = combineRiskLevels(questionnaireRisk, mlResult, request.isProxyMode());

        // Step 4: 988 display
        boolean show988 = "HIGH".equals(finalRisk);

        // Step 5: Explanation
        String explanation = buildExplanation(finalRisk, mlResult);

        return AnalysisResponse.builder()
                .riskLevel(finalRisk)
                .confidence(computeOverallConfidence(questionnaireRisk, mlResult))
                .phq9Score(getScore(request.getQuestionnaireScores(), "phq9"))
                .gad7Score(getScore(request.getQuestionnaireScores(), "gad7"))
                .aiAnalysis(mlResult)
                .show988(show988)
                .explanation(explanation)
                .build();
    }

    private String scoreQuestionnaires(Map<String, Integer> scores) {
        if (scores == null || scores.isEmpty()) return "LOW";
        int phq9 = scores.getOrDefault("phq9", 0);
        int gad7 = scores.getOrDefault("gad7", 0);
        if (phq9 >= PHQ9_SEVERE || gad7 >= GAD7_SEVERE)     return "HIGH";
        if (phq9 >= PHQ9_MODERATE || gad7 >= GAD7_MODERATE) return "MEDIUM";
        return "LOW";
    }

    private String combineRiskLevels(String questionnaireRisk,
                                     MlAnalysisResult mlResult,
                                     boolean proxyMode) {
        if (mlResult == null) return questionnaireRisk;
        String mlRisk = mlResult.getRiskLevel();

        boolean planSignal = mlResult.getSignals() != null &&
                             mlResult.getSignals().contains("plan_or_action");
        if ("MEDIUM".equals(mlRisk) &&
            mlResult.getScores().getHigh() >= ML_HIGH_OVERRIDE_THRESHOLD &&
            planSignal && !proxyMode) {
            mlRisk = "HIGH";
            log.info("Risk upgraded to HIGH: plan_or_action signal with high score");
        }

        return higherRisk(questionnaireRisk, mlRisk);
    }

    private String higherRisk(String a, String b) {
        return riskToInt(a) >= riskToInt(b) ? a : b;
    }

    private int riskToInt(String risk) {
        return switch (risk) {
            case "HIGH"   -> 2;
            case "MEDIUM" -> 1;
            default       -> 0;
        };
    }

    private double computeOverallConfidence(String questionnaireRisk, MlAnalysisResult mlResult) {
        if (mlResult != null) return mlResult.getConfidence();
        return switch (questionnaireRisk) {
            case "HIGH"   -> 0.85;
            case "MEDIUM" -> 0.75;
            default       -> 0.90;
        };
    }

    private String buildExplanation(String finalRisk, MlAnalysisResult mlResult) {
        StringBuilder sb = new StringBuilder();
        sb.append("Your risk level has been assessed as ").append(finalRisk).append(". ");
        if (mlResult != null && mlResult.getSignals() != null && !mlResult.getSignals().isEmpty()) {
            sb.append("Your description contained signals related to: ");
            sb.append(String.join(", ", mlResult.getSignals()).replace("_", " ")).append(". ");
        }
        sb.append(switch (finalRisk) {
            case "HIGH"   -> "We strongly recommend reaching out to a crisis service immediately.";
            case "MEDIUM" -> "We recommend speaking with a mental health professional soon.";
            default       -> "Continue monitoring how you feel and reach out if things change.";
        });
        return sb.toString();
    }

    private Integer getScore(Map<String, Integer> scores, String key) {
        return scores != null ? scores.get(key) : null;
    }
}

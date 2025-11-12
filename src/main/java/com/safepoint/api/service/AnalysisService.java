package com.safepoint.api.service;

import com.safepoint.api.model.dto.AnalysisRequest;
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

    // PHQ-9 severity thresholds (standard clinical cutoffs)
    private static final int PHQ9_MODERATE    = 10;
    private static final int PHQ9_SEVERE      = 20;

    // GAD-7 severity thresholds
    private static final int GAD7_MODERATE    = 10;
    private static final int GAD7_SEVERE      = 15;

    // ML confidence threshold for High risk override
    private static final double ML_HIGH_OVERRIDE_THRESHOLD = 0.35;

    /**
     * Orchestrates the full risk assessment pipeline:
     * 1. Score questionnaire results (PHQ-9, GAD-7)
     * 2. Call ML service for free-text analysis (if provided)
     * 3. Combine signals into final risk level
     * 4. Determine whether 988 should be shown
     */
    public AnalysisResponse analyze(AnalysisRequest request) {
        // Step 1: Score questionnaires
        String questionnaireRisk = scoreQuestionnaires(request.getQuestionnaireScores());

        // Step 2: ML analysis (optional)
        MlAnalysisResult mlResult = null;
        if (request.getFreeText() != null && !request.getFreeText().isBlank()) {
            mlResult = mlService.analyze(request.getFreeText());
        }

        // Step 3: Combine into final risk level
        String finalRisk = combineRiskLevels(questionnaireRisk, mlResult, request.isProxyMode());

        // Step 4: Determine 988 display
        boolean show988 = "HIGH".equals(finalRisk);

        // Step 5: Build explanation
        String explanation = buildExplanation(finalRisk, questionnaireRisk, mlResult);

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

    /**
     * Converts questionnaire scores into a risk level.
     * Uses standard clinical cutoffs for PHQ-9 and GAD-7.
     */
    private String scoreQuestionnaires(Map<String, Integer> scores) {
        if (scores == null || scores.isEmpty()) {
            return "LOW";
        }

        int phq9 = scores.getOrDefault("phq9", 0);
        int gad7 = scores.getOrDefault("gad7", 0);

        // High if either questionnaire indicates severe symptoms
        if (phq9 >= PHQ9_SEVERE || gad7 >= GAD7_SEVERE) {
            return "HIGH";
        }
        // Medium if either indicates moderate symptoms
        if (phq9 >= PHQ9_MODERATE || gad7 >= GAD7_MODERATE) {
            return "MEDIUM";
        }
        return "LOW";
    }

    /**
     * Combines questionnaire risk and ML risk into a final assessment.
     * Rules:
     * - Takes the higher of the two signals
     * - If ML model has high confidence (>0.85) on HIGH, override questionnaire result
     * - If ML is unavailable, rely on questionnaire only
     * - In proxy mode, ML result is still included but weighted slightly lower
     */
    private String combineRiskLevels(String questionnaireRisk,
                                     MlAnalysisResult mlResult,
                                     boolean proxyMode) {
        if (mlResult == null) {
            return questionnaireRisk;
        }

        String mlRisk = mlResult.getRiskLevel();

        // Business logic: if ML scores HIGH > threshold even if classified MEDIUM,
        // and plan_or_action signal is present, treat as HIGH
        boolean planSignal = mlResult.getSignals() != null &&
                             mlResult.getSignals().contains("plan_or_action");
        if ("MEDIUM".equals(mlRisk) &&
            mlResult.getScores().getHigh() >= ML_HIGH_OVERRIDE_THRESHOLD &&
            planSignal &&
            !proxyMode) {
            mlRisk = "HIGH";
            log.info("Risk upgraded to HIGH: plan_or_action signal with high={:.3f}",
                     mlResult.getScores().getHigh());
        }

        // Return the more severe of the two
        return higherRisk(questionnaireRisk, mlRisk);
    }

    /**
     * Returns the higher of two risk levels.
     * HIGH > MEDIUM > LOW
     */
    private String higherRisk(String a, String b) {
        int levelA = riskToInt(a);
        int levelB = riskToInt(b);
        return levelA >= levelB ? a : b;
    }

    private int riskToInt(String risk) {
        return switch (risk) {
            case "HIGH"   -> 2;
            case "MEDIUM" -> 1;
            default       -> 0;
        };
    }

    /**
     * Computes an overall confidence score.
     * Uses ML confidence if available, otherwise maps questionnaire result to a fixed score.
     */
    private double computeOverallConfidence(String questionnaireRisk, MlAnalysisResult mlResult) {
        if (mlResult != null) {
            return mlResult.getConfidence();
        }
        // Questionnaire-only confidence is fixed per level
        return switch (questionnaireRisk) {
            case "HIGH"   -> 0.85;
            case "MEDIUM" -> 0.75;
            default       -> 0.90;
        };
    }

    /**
     * Builds a human-readable explanation of the risk assessment for the UI.
     */
    private String buildExplanation(String finalRisk,
                                    String questionnaireRisk,
                                    MlAnalysisResult mlResult) {
        StringBuilder sb = new StringBuilder();

        sb.append("Your risk level has been assessed as ").append(finalRisk).append(". ");

        if (mlResult != null && !mlResult.getSignals().isEmpty()) {
            sb.append("Your description contained signals related to: ");
            sb.append(String.join(", ", mlResult.getSignals()).replace("_", " "));
            sb.append(". ");
        }

        if ("HIGH".equals(finalRisk)) {
            sb.append("We strongly recommend reaching out to a crisis service immediately.");
        } else if ("MEDIUM".equals(finalRisk)) {
            sb.append("We recommend speaking with a mental health professional soon.");
        } else {
            sb.append("Continue monitoring how you feel and reach out if things change.");
        }

        return sb.toString();
    }

    private Integer getScore(Map<String, Integer> scores, String key) {
        return scores != null ? scores.get(key) : null;
    }
}

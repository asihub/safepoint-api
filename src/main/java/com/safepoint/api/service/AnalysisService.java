package com.safepoint.api.service;

import com.safepoint.api.model.AnalysisRequest;
import com.safepoint.api.model.AnalysisResponse;
import com.safepoint.api.model.AnalysisResponse.MlAnalysisResult;
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

  // PHQ-9 severity thresholds (validated clinical cutoffs)
  private static final int PHQ9_MODERATE = 10;
  private static final int PHQ9_SEVERE   = 20;

  // GAD-7 severity thresholds (validated clinical cutoffs)
  private static final int GAD7_MODERATE = 10;
  private static final int GAD7_SEVERE   = 15;

  /**
   * Orchestrates the full risk assessment pipeline:
   * 1. Score questionnaires (PHQ-9, GAD-7) → validated clinical risk
   * 2. Run ML on free text if provided → AI risk signal
   * 3. Combine signals → final risk level
   */
  public AnalysisResponse analyze(AnalysisRequest request) {

    // Step 1 — Questionnaire scoring (PHQ-9 + GAD-7)
    String questionnaireRisk = scoreQuestionnaires(request.getQuestionnaireScores());

    // Step 2 — ML analysis on free text only (skipped if no text provided)
    MlAnalysisResult mlResult = null;
    String freeText = request.getFreeText();
    if (freeText != null && !freeText.isBlank()) {
      String[] words = freeText.trim().split("\\s+");
      long uniqueWords = java.util.Arrays.stream(words)
          .map(String::toLowerCase)
          .distinct()
          .count();
      if (words.length < 15 || uniqueWords < 8) {
        log.info("Skipping ML: {} words, {} unique (min 15 words, 8 unique required)", words.length, uniqueWords);
        freeText = null;
      }
    }
    if (freeText != null && !freeText.isBlank()) {
      String mlInput = freeText.trim();
      String lang = request.getLang();
      if (lang != null && !lang.equalsIgnoreCase("en")) {
        mlInput = translationService.translateToEnglish(mlInput, lang);
        log.info("Translated ML input from {} to en", lang);
      }
      try {
        mlResult = mlService.analyze(mlInput);
      } catch (Exception e) {
        log.warn("ML service call failed, falling back to questionnaire only: {}", e.getMessage());
      }
    }

    // Step 3 — Combine questionnaire + ML signals
    String finalRisk = combineRiskLevels(questionnaireRisk, mlResult);

    return AnalysisResponse.builder()
        .riskLevel(finalRisk)
        .confidence(computeOverallConfidence(questionnaireRisk, mlResult))
        .phq9Score(getScore(request.getQuestionnaireScores(), "phq9"))
        .gad7Score(getScore(request.getQuestionnaireScores(), "gad7"))
        .aiAnalysis(mlResult)
        .show988("HIGH".equals(finalRisk))
        .explanation(buildExplanation(finalRisk, mlResult))
        .build();
  }

  // ── Scoring ───────────────────────────────────────────────────────────────

  /**
   * Scores PHQ-9 and GAD-7 against validated clinical cutoffs.
   * PHQ-9: 0-4 minimal, 5-9 mild, 10-19 moderate, 20-27 severe
   * GAD-7: 0-4 minimal, 5-9 mild, 10-14 moderate, 15-21 severe
   */
  private String scoreQuestionnaires(Map<String, Integer> scores) {
    if (scores == null || scores.isEmpty()) return "LOW";
    int phq9 = scores.getOrDefault("phq9", 0);
    int gad7 = scores.getOrDefault("gad7", 0);
    if (phq9 >= PHQ9_SEVERE || gad7 >= GAD7_SEVERE)     return "HIGH";
    if (phq9 >= PHQ9_MODERATE || gad7 >= GAD7_MODERATE) return "MEDIUM";
    return "LOW";
  }

  /**
   * Combines questionnaire risk with ML signal.
   * Final risk = max(questionnaireRisk, mlRisk).
   * Final risk = max(questionnaireRisk, mlRisk).
   */
  // Minimum ML confidence to include ML signal in final risk
  private static final double ML_MIN_CONFIDENCE = 0.6;

  private String combineRiskLevels(String questionnaireRisk,
                                   MlAnalysisResult mlResult) {
    if (mlResult == null) return questionnaireRisk;

    // Ignore ML if confidence is too low — unreliable signal
    if (mlResult.getConfidence() < ML_MIN_CONFIDENCE) {
      log.info("ML confidence {} below threshold {} — ignoring ML signal",
          mlResult.getConfidence(), ML_MIN_CONFIDENCE);
      return questionnaireRisk;
    }

    // ML is confident enough — take the higher of the two signals
    return higherRisk(questionnaireRisk, mlResult.getRiskLevel());
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  private String higherRisk(String a, String b) {
    return riskToInt(a) >= riskToInt(b) ? a : b;
  }

  private int riskToInt(String risk) {
    return switch (risk != null ? risk : "LOW") {
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
      sb.append("AI detected signals: ")
          .append(String.join(", ", mlResult.getSignals()).replace("_", " "))
          .append(". ");
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

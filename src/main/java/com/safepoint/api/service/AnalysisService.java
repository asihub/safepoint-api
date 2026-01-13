package com.safepoint.api.service;

import com.safepoint.api.model.AnalysisRequest;
import com.safepoint.api.model.AnalysisResponse;
import com.safepoint.api.model.AnalysisResponse.MlAnalysisResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AnalysisService {

  private final MlService mlService;
  private final TranslationService translationService;

  // PHQ-9 severity thresholds
  private static final int PHQ9_MODERATE = 10;
  private static final int PHQ9_SEVERE = 20;

  // GAD-7 severity thresholds
  private static final int GAD7_MODERATE = 10;
  private static final int GAD7_SEVERE = 15;

  // ML confidence threshold for High risk override
  private static final double ML_HIGH_OVERRIDE_THRESHOLD = 0.35;

  // Concern tags that force minimum HIGH risk
  private static final Set<String> HIGH_RISK_CONCERNS = Set.of("Suicidal thoughts", "Thoughts of self-harm");

  // Concern tags that force minimum MEDIUM risk
  private static final Set<String> MEDIUM_RISK_CONCERNS = Set.of("Feeling hopeless", "Feeling worthless", "Feeling like a burden");

  /**
   * Orchestrates the full risk assessment pipeline:
   * 1. Score questionnaires (PHQ-9, GAD-7)
   * 2. Score concerns (hard overrides for high-risk tags)
   * 3. Translate free text if non-English
   * 4. Build ML input (concerns + free text)
   * 5. Call ML service
   * 6. Combine all signals → final risk
   * 7. Build response
   */
  public AnalysisResponse analyze(AnalysisRequest request) {

    // Step 1 — Questionnaire scoring
    String questionnaireRisk = scoreQuestionnaires(request.getQuestionnaireScores());

    // Step 2 — Concern-based override
    String concernOverride = scoreConcerns(request.getConcerns());

    // Step 3 & 4 — Build ML input and optionally translate
    MlAnalysisResult mlResult = null;
    String mlInput = buildMlInput(request.getFreeText(), request.getConcerns());
    if (!mlInput.isBlank()) {
      // Translate to English if UI language is non-English
      String lang = request.getLang();
      if (lang != null && !lang.equalsIgnoreCase("en")) {
        mlInput = translationService.translateToEnglish(mlInput, lang);
        log.info("Translated ML input from {} to en", lang);
      }
      // Step 5 — ML classification
      mlResult = mlService.analyze(mlInput);
    }

    // Step 6 — Combine all signals
    String finalRisk = combineRiskLevels(questionnaireRisk, concernOverride, mlResult, request.isProxyMode());

    // Step 7 — Build response
    boolean show988 = "HIGH".equals(finalRisk);
    String explanation = buildExplanation(finalRisk, mlResult, request.getConcerns());

    return AnalysisResponse.builder().riskLevel(finalRisk).confidence(computeOverallConfidence(questionnaireRisk, mlResult)).phq9Score(getScore(request.getQuestionnaireScores(), "phq9")).gad7Score(getScore(request.getQuestionnaireScores(), "gad7")).aiAnalysis(mlResult).show988(show988).explanation(explanation).build();
  }

  // ── Scoring ───────────────────────────────────────────────────────────────

  private String scoreQuestionnaires(Map<String, Integer> scores) {
    if (scores == null || scores.isEmpty()) return "LOW";
    int phq9 = scores.getOrDefault("phq9", 0);
    int gad7 = scores.getOrDefault("gad7", 0);
    if (phq9 >= PHQ9_SEVERE || gad7 >= GAD7_SEVERE) return "HIGH";
    if (phq9 >= PHQ9_MODERATE || gad7 >= GAD7_MODERATE) return "MEDIUM";
    return "LOW";
  }

  /**
   * Returns minimum risk forced by concern tags.
   * HIGH_RISK_CONCERNS → HIGH, MEDIUM_RISK_CONCERNS → MEDIUM, else LOW.
   */
  private String scoreConcerns(List<String> concerns) {
    if (concerns == null || concerns.isEmpty()) return "LOW";
    for (String c : concerns) {
      if (HIGH_RISK_CONCERNS.contains(c)) return "HIGH";
    }
    for (String c : concerns) {
      if (MEDIUM_RISK_CONCERNS.contains(c)) return "MEDIUM";
    }
    return "LOW";
  }

  /**
   * Combines questionnaire risk, concern override, and ML risk.
   * Final risk = max of all three signals.
   */
  private String combineRiskLevels(String questionnaireRisk, String concernOverride, MlAnalysisResult mlResult, boolean proxyMode) {

    String base = higherRisk(questionnaireRisk, concernOverride);

    if (mlResult == null) return base;

    String mlRisk = mlResult.getRiskLevel();

    // Upgrade MEDIUM → HIGH if plan_or_action signal present with high confidence
    boolean planSignal = mlResult.getSignals() != null && mlResult.getSignals().contains("plan_or_action");
    if ("MEDIUM".equals(mlRisk) && mlResult.getScores().getHigh() >= ML_HIGH_OVERRIDE_THRESHOLD && planSignal && !proxyMode) {
      mlRisk = "HIGH";
      log.info("Risk upgraded to HIGH: plan_or_action signal with high confidence score");
    }

    return higherRisk(base, mlRisk);
  }

  // ── ML input ──────────────────────────────────────────────────────────────

  /**
   * Builds ML input by prepending concern tags to free text.
   * Example: "Concerns: feeling hopeless, sleep problems. I have been struggling..."
   */
  private String buildMlInput(String freeText, List<String> concerns) {
    boolean hasConcerns = concerns != null && !concerns.isEmpty();
    boolean hasText = freeText != null && !freeText.isBlank();

    if (!hasConcerns && !hasText) return "";

    StringBuilder sb = new StringBuilder();
    if (hasConcerns) {
      String tags = concerns.stream().map(String::toLowerCase).collect(Collectors.joining(", "));
      sb.append("Concerns: ").append(tags).append(".");
    }
    if (hasText) {
      if (sb.length() > 0) sb.append(" ");
      sb.append(freeText.trim());
    }
    return sb.toString();
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  private String higherRisk(String a, String b) {
    return riskToInt(a) >= riskToInt(b) ? a : b;
  }

  private int riskToInt(String risk) {
    return switch (risk != null ? risk : "LOW") {
      case "HIGH" -> 2;
      case "MEDIUM" -> 1;
      default -> 0;
    };
  }

  private double computeOverallConfidence(String questionnaireRisk, MlAnalysisResult mlResult) {
    if (mlResult != null) return mlResult.getConfidence();
    return switch (questionnaireRisk) {
      case "HIGH" -> 0.85;
      case "MEDIUM" -> 0.75;
      default -> 0.90;
    };
  }

  private String buildExplanation(String finalRisk, MlAnalysisResult mlResult, List<String> concerns) {
    StringBuilder sb = new StringBuilder();
    sb.append("Your risk level has been assessed as ").append(finalRisk).append(". ");

    if (concerns != null && !concerns.isEmpty()) {
      sb.append("Reported concerns: ").append(String.join(", ", concerns)).append(". ");
    }

    if (mlResult != null && mlResult.getSignals() != null && !mlResult.getSignals().isEmpty()) {
      sb.append("AI detected signals: ").append(String.join(", ", mlResult.getSignals()).replace("_", " ")).append(". ");
    }

    sb.append(switch (finalRisk) {
      case "HIGH" -> "We strongly recommend reaching out to a crisis service immediately.";
      case "MEDIUM" -> "We recommend speaking with a mental health professional soon.";
      default -> "Continue monitoring how you feel and reach out if things change.";
    });

    return sb.toString();
  }

  private Integer getScore(Map<String, Integer> scores, String key) {
    return scores != null ? scores.get(key) : null;
  }
}

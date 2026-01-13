package com.safepoint.api.service;

import com.safepoint.api.model.AnalysisResponse.MlAnalysisResult;
import com.safepoint.api.model.AnalysisResponse.MlScores;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class MlService {

  private final RestTemplate restTemplate;

  @Value("${ml.service.url}")
  private String mlServiceUrl;

  /**
   * Explicit constructor with @Qualifier to resolve ambiguous RestTemplate beans.
   */
  public MlService(@Qualifier("mlRestTemplate") RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  /**
   * Sends free-text to the internal Python ML service for risk classification.
   * Returns null if ML service is unavailable — caller decides how to handle gracefully.
   * User text is NEVER logged here to preserve privacy.
   */
  public MlAnalysisResult analyze(String text) {
    if (text == null || text.isBlank()) {
      return null;
    }

    String url = mlServiceUrl + "/analyze";

    try {
      var requestBody = Map.of("text", text);

      @SuppressWarnings("unchecked")
      Map<String, Object> response = restTemplate.postForObject(
          url, requestBody, Map.class
      );

      if (response == null) {
        log.warn("ML service returned null response");
        return null;
      }

      @SuppressWarnings("unchecked")
      Map<String, Double> scores = (Map<String, Double>) response.get("scores");

      @SuppressWarnings("unchecked")
      List<String> signals = (List<String>) response.getOrDefault("signals", List.of());

      return MlAnalysisResult.builder()
          .riskLevel((String) response.get("risk_level"))
          .confidence(((Number) response.get("confidence")).doubleValue())
          .signals(signals)
          .scores(MlScores.builder()
              .low(scores.getOrDefault("low", 0.0))
              .medium(scores.getOrDefault("medium", 0.0))
              .high(scores.getOrDefault("high", 0.0))
              .build())
          .build();

    } catch (ResourceAccessException e) {
      log.warn("ML service unavailable: {}", e.getMessage());
      return null;
    } catch (Exception e) {
      log.error("Unexpected error calling ML service: {}", e.getMessage());
      return null;
    }
  }
}
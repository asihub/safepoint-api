package com.safepoint.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

@Data
@Schema(description = "Request payload for mental health crisis analysis")
public class AnalysisRequest {

  @Schema(description = "Questionnaire scores from PHQ-9, GAD-7 etc.",
      example = "{\"phq9\": 14, \"gad7\": 10}")
  private Map<String, Integer> questionnaireScores;

  @Schema(description = "Optional free-text description from the user")
  @Size(max = 5000, message = "Text must not exceed 5000 characters")
  private String freeText;

  @Schema(description = "Whether this is a proxy assessment (concern for someone else)")
  private boolean proxyMode = false;

  @Schema(description = "Insurance type for SAMHSA resource filtering")
  private String insuranceType = "UNKNOWN";

  @Schema(description = "UI language selected by user — used to determine if translation is needed",
      allowableValues = {"en", "es"}, example = "es")
  private String lang = "en";

  @Schema(description = "User latitude for resource map")
  private Double latitude;

  @Schema(description = "User longitude for resource map")
  private Double longitude;
}

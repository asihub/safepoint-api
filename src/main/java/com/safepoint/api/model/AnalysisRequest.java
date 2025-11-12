package com.safepoint.api.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Schema(description = "Request payload for mental health crisis analysis")
public class AnalysisRequest {

    @Schema(description = "Questionnaire scores from PHQ-9, GAD-7 etc.",
            example = "{\"phq9\": 14, \"gad7\": 10}")
    private Map<String, Integer> questionnaireScores;

    @Schema(description = "Optional free-text description from the user",
            example = "I've been feeling hopeless lately and can't see the point anymore.")
    @Size(max = 5000, message = "Text must not exceed 5000 characters")
    private String freeText;

    @Schema(description = "Whether this is a proxy assessment (concern for someone else)")
    private boolean proxyMode = false;

    @Schema(description = "Insurance type for SAMHSA resource filtering",
            allowableValues = {"MEDICAID", "MEDICARE", "PRIVATE", "NONE", "UNKNOWN"})
    private String insuranceType = "UNKNOWN";

    @Schema(description = "User latitude for resource map")
    private Double latitude;

    @Schema(description = "User longitude for resource map")
    private Double longitude;
}

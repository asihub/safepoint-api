package com.safepoint.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@Schema(description = "Combined risk assessment result")
public class AnalysisResponse {

    @Schema(description = "Final risk level", allowableValues = {"LOW", "MEDIUM", "HIGH"})
    private String riskLevel;

    @Schema(description = "Overall confidence score (0-1)")
    private double confidence;

    @Schema(description = "PHQ-9 score if provided")
    private Integer phq9Score;

    @Schema(description = "GAD-7 score if provided")
    private Integer gad7Score;

    @Schema(description = "AI text analysis result (only if freeText was provided)")
    private MlAnalysisResult aiAnalysis;

    @Schema(description = "Whether 988 crisis line should be prominently shown")
    private boolean show988;

    @Schema(description = "Brief explanation of the risk factors detected")
    private String explanation;

    @Data
    @Builder
    @Schema(description = "Result from ML text classification")
    public static class MlAnalysisResult {
        private String riskLevel;
        private double confidence;
        private List<String> signals;
        private MlScores scores;
    }

    @Data
    @Builder
    @Schema(description = "Per-class probability scores from ML model")
    public static class MlScores {
        private double low;
        private double medium;
        private double high;
    }
}

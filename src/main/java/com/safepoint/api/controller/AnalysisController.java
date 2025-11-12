package com.safepoint.api.controller;

import com.safepoint.api.model.dto.AnalysisRequest;
import com.safepoint.api.model.dto.AnalysisResponse;
import com.safepoint.api.service.AnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/analysis")
@RequiredArgsConstructor
@Tag(name = "Analysis", description = "Mental health crisis risk assessment")
public class AnalysisController {

    private final AnalysisService analysisService;

    /**
     * Main endpoint: accepts questionnaire scores and optional free text,
     * returns a combined risk assessment.
     * No user identity is required or stored.
     */
    @PostMapping
    @Operation(
        summary = "Analyze mental health risk",
        description = "Combines questionnaire scores and optional AI text analysis " +
                      "into a single risk assessment (LOW / MEDIUM / HIGH)."
    )
    public ResponseEntity<AnalysisResponse> analyze(
            @Valid @RequestBody AnalysisRequest request) {
        AnalysisResponse response = analysisService.analyze(request);
        return ResponseEntity.ok(response);
    }
}

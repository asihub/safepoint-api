package com.safepoint.api.service;

import com.safepoint.api.model.AnalysisResponse.MlAnalysisResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MlServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private MlService mlService;

    @BeforeEach
    void setUp() {
        mlService = new MlService(restTemplate);
        ReflectionTestUtils.setField(mlService, "mlServiceUrl", "http://localhost:8001");
    }

    @Test @DisplayName("Valid response → MlAnalysisResult with correct fields")
    void valid_response_mapped_correctly() {
        Map<String, Object> response = Map.of(
                "risk_level", "HIGH",
                "confidence", 0.87,
                "scores", Map.of("low", 0.05, "medium", 0.08, "high", 0.87),
                "signals", List.of("hopelessness", "sleep_disturbance")
        );
        when(restTemplate.postForObject(anyString(), any(), eq(Map.class))).thenReturn(response);

        MlAnalysisResult result = mlService.analyze("I feel hopeless and cannot sleep");

        assertThat(result).isNotNull();
        assertThat(result.getRiskLevel()).isEqualTo("HIGH");
        assertThat(result.getConfidence()).isEqualTo(0.87);
        assertThat(result.getSignals()).containsExactly("hopelessness", "sleep_disturbance");
        assertThat(result.getScores().getHigh()).isEqualTo(0.87);
        assertThat(result.getScores().getLow()).isEqualTo(0.05);
    }

    @Test @DisplayName("ML service returns null body → returns null")
    void null_body_returns_null() {
        when(restTemplate.postForObject(anyString(), any(), eq(Map.class))).thenReturn(null);
        assertThat(mlService.analyze("some valid text")).isNull();
    }

    @Test @DisplayName("ML service throws ResourceAccessException → returns null")
    void resource_access_exception_returns_null() {
        when(restTemplate.postForObject(anyString(), any(), eq(Map.class)))
                .thenThrow(new ResourceAccessException("Connection refused"));
        assertThat(mlService.analyze("some valid text")).isNull();
    }

    @Test @DisplayName("ML service throws unexpected exception → returns null")
    void unexpected_exception_returns_null() {
        when(restTemplate.postForObject(anyString(), any(), eq(Map.class)))
                .thenThrow(new RuntimeException("Unexpected error"));
        assertThat(mlService.analyze("some valid text")).isNull();
    }

    @Test @DisplayName("Text is null → returns null without calling RestTemplate")
    void null_text_returns_null() {
        assertThat(mlService.analyze(null)).isNull();
        verifyNoInteractions(restTemplate);
    }

    @Test @DisplayName("Text is blank → returns null without calling RestTemplate")
    void blank_text_returns_null() {
        assertThat(mlService.analyze("   ")).isNull();
        verifyNoInteractions(restTemplate);
    }
}

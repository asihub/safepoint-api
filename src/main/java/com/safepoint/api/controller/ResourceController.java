package com.safepoint.api.controller;

import com.safepoint.api.service.SamhsaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/resources")
@RequiredArgsConstructor
@Tag(name = "Resources", description = "Treatment facility lookup via SAMHSA")
public class ResourceController {

    private final SamhsaService samhsaService;

    /**
     * Returns nearby treatment facilities filtered by location and insurance type.
     * Results are sourced from the SAMHSA national treatment locator.
     */
    @GetMapping("/facilities")
    @Operation(
        summary = "Find nearby treatment facilities",
        description = "Returns mental health treatment facilities near the given coordinates, " +
                      "filtered by insurance type. Insurance can be: " +
                      "MEDICAID, MEDICARE, PRIVATE, NONE, UNKNOWN."
    )
    public ResponseEntity<List<Map<String, Object>>> getFacilities(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(defaultValue = "UNKNOWN") String insurance,
            @RequestParam(defaultValue = "10") int limit) {

        List<Map<String, Object>> facilities =
            samhsaService.getFacilities(latitude, longitude, insurance, limit);

        return ResponseEntity.ok(facilities);
    }
}

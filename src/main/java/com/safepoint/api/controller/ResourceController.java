package com.safepoint.api.controller;

import com.safepoint.api.service.SamhsaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/resources")
@RequiredArgsConstructor
public class ResourceController {

  private final SamhsaService samhsaService;

  @GetMapping("/facilities")
  @Operation(summary = "Find nearby mental health treatment facilities",
      description = "Queries SAMHSA FindTreatment.gov API for facilities within the given radius")
  public ResponseEntity<List<Map<String, Object>>> getFacilities(
      @Parameter(description = "User latitude")
      @RequestParam double latitude,

      @Parameter(description = "User longitude")
      @RequestParam double longitude,

      @Parameter(description = "Insurance type filter: MEDICAID, MEDICARE, PRIVATE, NONE, UNKNOWN")
      @RequestParam(defaultValue = "UNKNOWN") String insurance,

      @Parameter(description = "Maximum number of results")
      @RequestParam(defaultValue = "200") int limit,

      @Parameter(description = "Search radius in meters (e.g. 8047=5mi, 16093=10mi, 40234=25mi, 80467=50mi)")
      @RequestParam(defaultValue = "16093") double radiusMeters
  ) {
    List<Map<String, Object>> facilities =
        samhsaService.findFacilities(latitude, longitude, insurance, limit, radiusMeters);
    return ResponseEntity.ok(facilities);
  }
}

package com.safepoint.api.controller;

import com.safepoint.api.dto.SafetyPlanDto;
import com.safepoint.api.entity.SafetyPlan;
import com.safepoint.api.service.SafetyPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/safety-plan")
@RequiredArgsConstructor
@Tag(name = "Safety Plan", description = "Personal crisis response plan management")
public class SafetyPlanController {

  private final SafetyPlanService safetyPlanService;

  /**
   * Creates or updates the user's safety plan.
   * Identified by anonymous userCode + PIN — no personal data stored.
   */
  @PostMapping
  @Operation(
      summary = "Save safety plan",
      description = "Creates or updates the six-step personal safety plan. " +
          "User is identified by code + PIN only — no personal data stored."
  )
  public ResponseEntity<SafetyPlan> save(@Valid @RequestBody SafetyPlanDto dto) {
    SafetyPlan saved = safetyPlanService.save(dto);
    // Do not return the userHash — only the plan content
    saved.setUserHash(null);
    return ResponseEntity.ok(saved);
  }

  /**
   * Retrieves the user's safety plan by code + PIN.
   */
  @GetMapping
  @Operation(
      summary = "Get safety plan",
      description = "Retrieves the safety plan for the given username and PIN."
  )
  public ResponseEntity<SafetyPlan> get(
      @RequestParam String userCode,
      @RequestParam String pin) {

    return safetyPlanService.get(userCode, pin)
        .map(plan -> {
          plan.setUserHash(null); // never expose hash to client
          return ResponseEntity.ok(plan);
        })
        .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Deletes the user's safety plan.
   */
  @DeleteMapping
  @Operation(
      summary = "Delete safety plan",
      description = "Permanently deletes the safety plan for the given username and PIN."
  )
  public ResponseEntity<Void> delete(
      @RequestParam String userCode,
      @RequestParam String pin) {

    boolean deleted = safetyPlanService.delete(userCode, pin);
    return deleted
        ? ResponseEntity.noContent().build()
        : ResponseEntity.notFound().build();
  }
}

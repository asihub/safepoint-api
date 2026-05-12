package com.safepoint.api.controller;

import com.safepoint.api.entity.WellbeingResource;
import com.safepoint.api.service.WellbeingResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/wellbeing")
@RequiredArgsConstructor
@Tag(name = "Wellbeing Resources", description = "Self-help articles and guides for mental wellness")
public class WellbeingResourceController {

  private final WellbeingResourceService service;

  @GetMapping
  @Operation(summary = "Get all wellbeing resources", description = "Returns all resources ordered by category and title.")
  public ResponseEntity<List<WellbeingResource>> getAll() {
    return ResponseEntity.ok(service.getAll());
  }

  @PostMapping("/{id}/refresh-excerpt")
  @Operation(summary = "Refresh excerpt for a resource", description = "Manually triggers AI excerpt generation for a single resource.")
  public ResponseEntity<Void> refreshExcerpt(@PathVariable Long id) {
    service.refreshExcerpt(id);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/refresh-missing-excerpts")
  @Operation(summary = "Refresh all missing excerpts", description = "Triggers AI excerpt generation for all resources that have no excerpt yet. Returns count of successfully updated resources.")
  public ResponseEntity<Map<String, Integer>> refreshMissingExcerpts() {
    int updated = service.refreshMissingExcerpts();
    return ResponseEntity.ok(Map.of("updated", updated));
  }
}

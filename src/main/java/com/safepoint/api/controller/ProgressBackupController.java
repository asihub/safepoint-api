package com.safepoint.api.controller;

import com.safepoint.api.dto.ProgressBackupDto;
import com.safepoint.api.entity.ProgressBackup;
import com.safepoint.api.service.ProgressBackupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/progress-backup")
@RequiredArgsConstructor
@Tag(name = "Progress Backup", description = "Encrypted assessment history backup")
public class ProgressBackupController {

  private final ProgressBackupService service;

  @PostMapping
  @Operation(summary = "Save progress backup",
      description = "Uploads an AES-256-GCM encrypted assessment history blob. Server stores ciphertext only.")
  public ResponseEntity<Map<String, String>> save(@Valid @RequestBody ProgressBackupDto dto) {
    service.save(dto);
    return ResponseEntity.ok(Map.of("status", "saved"));
  }

  @GetMapping
  @Operation(summary = "Get progress backup",
      description = "Returns the encrypted history blob for decryption client-side.")
  public ResponseEntity<Map<String, String>> get(
      @RequestParam String userCode,
      @RequestParam String pin) {

    return service.get(userCode, pin)
        .map(b -> ResponseEntity.ok(Map.of("encryptedData", b.getEncryptedData())))
        .orElse(ResponseEntity.notFound().build());
  }

  @GetMapping("/exists")
  @Operation(summary = "Check if backup exists")
  public ResponseEntity<Map<String, Boolean>> exists(
      @RequestParam String userCode,
      @RequestParam String pin) {

    return ResponseEntity.ok(Map.of("exists", service.exists(userCode, pin)));
  }

  @DeleteMapping
  @Operation(summary = "Delete progress backup")
  public ResponseEntity<Void> delete(
      @RequestParam String userCode,
      @RequestParam String pin) {

    boolean deleted = service.delete(userCode, pin);
    return deleted
        ? ResponseEntity.noContent().build()
        : ResponseEntity.notFound().build();
  }
}

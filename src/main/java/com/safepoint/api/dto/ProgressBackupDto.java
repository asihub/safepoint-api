package com.safepoint.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO for uploading or retrieving an encrypted progress history backup.
 */
@Data
@Schema(description = "Encrypted progress history backup")
public class ProgressBackupDto {

  @NotBlank(message = "User code is required")
  @Schema(description = "Anonymous username", example = "pure-path-79")
  private String userCode;

  @Schema(description = "User PIN (optional if authenticated via session)", example = "1234")
  private String pin;

  @NotBlank(message = "Encrypted data is required")
  @Schema(description = "AES-256-GCM encrypted JSON blob of the assessment history array")
  private String encryptedData;
}

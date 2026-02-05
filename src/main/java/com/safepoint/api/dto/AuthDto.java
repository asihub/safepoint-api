package com.safepoint.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * DTOs for anonymous identity registration and verification.
 */
public class AuthDto {

  @Data
  @Schema(description = "Request to register a new anonymous user")
  public static class RegisterRequest {
    private String username;

    @NotBlank
    @Pattern(regexp = "\\d{4,6}", message = "PIN must be 4-6 digits")
    @Schema(description = "User PIN — 4 to 6 digits", example = "1234")
    private String pin;
  }

  @Data
  @Schema(description = "Response after successful registration")
  public static class RegisterResponse {

    @Schema(description = "Generated human-readable user code", example = "blue-river-42")
    private String userCode;

    @Schema(description = "Save this code — you will need it to access your data from another device")
    private String message;
  }

  @Data
  @Schema(description = "Request to verify credentials")
  public static class VerifyRequest {

    @NotBlank
    @Schema(description = "Your user code", example = "blue-river-42")
    private String userCode;

    @NotBlank
    @Pattern(regexp = "\\d{4,6}", message = "PIN must be 4-6 digits")
    @Schema(description = "Your PIN", example = "1234")
    private String pin;
  }

  @Data
  @Schema(description = "Result of credential verification")
  public static class VerifyResponse {

    @Schema(description = "Whether the credentials are valid")
    private boolean valid;
  }
}

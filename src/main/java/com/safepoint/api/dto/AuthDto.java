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

    @Schema(description = "Requested username (optional). If blank, a username is generated automatically.",
        example = "pure-path-79")
    private String username;

    @NotBlank
    @Pattern(regexp = "\\d{4}", message = "PIN must be exactly 4 digits")
    @Schema(description = "User PIN — exactly 4 digits", example = "1234")
    private String pin;
  }

  @Data
  @Schema(description = "Response after successful registration")
  public static class RegisterResponse {

    @Schema(description = "The user's username", example = "pure-path-79")
    private String username;

    @Schema(description = "Save your username and PIN — you will need them to access your data from another device")
    private String message;
  }

  @Data
  @Schema(description = "Request to verify credentials")
  public static class VerifyRequest {

    @NotBlank
    @Schema(description = "Your username", example = "pure-path-79")
    private String username;

    @NotBlank
    @Pattern(regexp = "\\d{4}", message = "PIN must be exactly 4 digits")
    @Schema(description = "Your PIN", example = "1234")
    private String pin;
  }

  @Data
  @Schema(description = "Result of credential verification")
  public static class VerifyResponse {

    @Schema(description = "Whether the credentials are valid")
    private boolean valid;
  }

  @Data
  @Schema(description = "Request to delete an anonymous user account")
  public static class DeleteRequest {

    @NotBlank
    @Schema(description = "Your username", example = "pure-path-79")
    private String username;

    @NotBlank
    @Schema(description = "Your PIN for confirmation", example = "1234")
    private String pin;
  }
}

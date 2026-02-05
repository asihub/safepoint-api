package com.safepoint.api.controller;

import com.safepoint.api.model.dto.AuthDto;
import com.safepoint.api.service.AnonymousUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Anonymous identity — registration, verification and deletion")
public class AuthController {

  private final AnonymousUserService userService;

  /**
   * Registers a new anonymous user with a username and PIN.
   * No email, name, or personal data required.
   */
  @PostMapping("/register")
  @Operation(
      summary = "Register anonymous user",
      description = "Creates a new anonymous identity. Returns a username " +
          "that the user should save to access their data from another device."
  )
  public ResponseEntity<AuthDto.RegisterResponse> register(
      @Valid @RequestBody AuthDto.RegisterRequest request) {
    AuthDto.RegisterResponse response = userService.register(request.getPin(), request.getUsername());
    return ResponseEntity.ok(response);
  }

  /**
   * Verifies a username + PIN combination.
   * Used before allowing access to saved data (safety plan, history).
   */
  @PostMapping("/verify")
  @Operation(
      summary = "Verify credentials",
      description = "Checks whether the given username and PIN are valid."
  )
  public ResponseEntity<AuthDto.VerifyResponse> verify(
      @Valid @RequestBody AuthDto.VerifyRequest request) {
    boolean valid = userService.verify(request.getUsername(), request.getPin());
    AuthDto.VerifyResponse response = new AuthDto.VerifyResponse();
    response.setValid(valid);
    return ResponseEntity.ok(response);
  }

  /**
   * Deletes an anonymous user account after PIN verification.
   */
  @DeleteMapping("/user")
  @Operation(
      summary = "Delete account",
      description = "Permanently deletes the anonymous user account after verifying the PIN."
  )
  public ResponseEntity<Void> delete(
      @Valid @RequestBody AuthDto.DeleteRequest request) {
    userService.delete(request.getUsername(), request.getPin());
    return ResponseEntity.noContent().build();
  }
}

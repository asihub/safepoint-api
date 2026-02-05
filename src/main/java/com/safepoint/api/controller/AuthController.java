package com.safepoint.api.controller;

import com.safepoint.api.dto.AuthDto;
import com.safepoint.api.service.AnonymousUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Anonymous identity — registration and verification")
public class AuthController {

  private final AnonymousUserService userService;

  /**
   * Registers a new anonymous user.
   * Generates a human-readable code (e.g. "blue-river-42") and stores bcrypt-hashed PIN.
   * No email, name, or personal data required.
   */
  @PostMapping("/register")
  @Operation(
      summary = "Register anonymous user",
      description = "Creates a new anonymous identity. Returns a human-readable code " +
          "that the user should save to access their data from another device."
  )
  public ResponseEntity<AuthDto.RegisterResponse> register(
      @Valid @RequestBody AuthDto.RegisterRequest request) {
    AuthDto.RegisterResponse response = userService.register(request.getPin(), request.getUsername());
    return ResponseEntity.ok(response);
  }

  /**
   * Verifies a user code + PIN combination.
   * Used before allowing access to saved data (safety plan, history).
   */
  @PostMapping("/verify")
  @Operation(
      summary = "Verify credentials",
      description = "Checks whether the given user code and PIN are valid."
  )
  public ResponseEntity<AuthDto.VerifyResponse> verify(
      @Valid @RequestBody AuthDto.VerifyRequest request) {
    boolean valid = userService.verify(request.getUserCode(), request.getPin());
    AuthDto.VerifyResponse response = new AuthDto.VerifyResponse();
    response.setValid(valid);
    return ResponseEntity.ok(response);
  }
}

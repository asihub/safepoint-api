package com.safepoint.api.service;

import com.safepoint.api.model.dto.AuthDto;
import com.safepoint.api.model.entity.AnonymousUser;
import com.safepoint.api.repository.AnonymousUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AnonymousUserService {

  private final AnonymousUserRepository repository;
  private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
  private final SecureRandom random = new SecureRandom();

  // Word lists for human-readable username generation
  private static final List<String> ADJECTIVES = List.of(
      "blue", "green", "silver", "golden", "quiet", "swift", "bright",
      "calm", "clear", "cool", "deep", "fair", "free", "fresh", "gentle",
      "glad", "grand", "kind", "light", "mild", "neat", "pure", "safe",
      "soft", "warm", "wise", "bold", "brave", "still", "true"
  );

  private static final List<String> NOUNS = List.of(
      "river", "mountain", "forest", "valley", "ocean", "meadow", "cloud",
      "stone", "wind", "shore", "field", "grove", "lake", "path", "peak",
      "plain", "ridge", "brook", "creek", "dune", "fern", "glen", "hill",
      "isle", "knoll", "marsh", "moor", "pond", "reef", "spring"
  );

  /**
   * Registers a new anonymous user with a human-readable username and hashed PIN.
   * If the caller provides a username, validates and uses it.
   * Otherwise, generates a unique username in adjective-noun-number format.
   */
  @Transactional
  public AuthDto.RegisterResponse register(String pin, String requestedUsername) {
    String username;
    if (requestedUsername != null && !requestedUsername.isBlank()) {
      String cleaned = requestedUsername.trim().toLowerCase();
      if (!cleaned.matches("^[a-z0-9-]{1,20}$")) {
        throw new IllegalArgumentException("Invalid username format");
      }
      if (repository.findByUsername(cleaned).isPresent()) {
        throw new IllegalArgumentException("Username already taken");
      }
      username = cleaned;
    } else {
      username = generateUniqueUsername();
    }

    AnonymousUser user = new AnonymousUser();
    user.setUsername(username);
    user.setPinHash(passwordEncoder.encode(pin));
    repository.save(user);

    log.info("New anonymous user registered: {}", username);

    AuthDto.RegisterResponse response = new AuthDto.RegisterResponse();
    response.setUsername(username);
    response.setMessage(
        "Save your username: " + username + ". " +
            "You will need it together with your PIN to access your data from another device."
    );
    return response;
  }

  /**
   * Verifies a username + PIN combination.
   * Returns true only if the username exists and the PIN matches the stored hash.
   */
  @Transactional(readOnly = true)
  public boolean verify(String username, String pin) {
    return repository.findByUsername(username)
        .map(user -> passwordEncoder.matches(pin, user.getPinHash()))
        .orElse(false);
  }

  /**
   * Deletes an anonymous user after verifying their PIN.
   */
  @Transactional
  public void delete(String username, String pin) {
    AnonymousUser user = repository.findByUsername(username)
        .orElseThrow(() -> new IllegalArgumentException("User not found"));
    if (!passwordEncoder.matches(pin, user.getPinHash())) {
      throw new IllegalArgumentException("Invalid PIN");
    }
    repository.delete(user);
    log.info("Anonymous user deleted: {}", username);
  }

  /**
   * Generates a unique username in the format: adjective-noun-number.
   * Example: "blue-river-42". Retries up to 10 times to avoid collisions.
   */
  private String generateUniqueUsername() {
    for (int attempt = 0; attempt < 10; attempt++) {
      String adj = ADJECTIVES.get(random.nextInt(ADJECTIVES.size()));
      String noun = NOUNS.get(random.nextInt(NOUNS.size()));
      int number = random.nextInt(90) + 10; // 10–99
      String username = adj + "-" + noun + "-" + number;
      if (!repository.existsByUsername(username)) {
        return username;
      }
    }
    // Fallback — 4-digit suffix guarantees uniqueness
    return ADJECTIVES.get(random.nextInt(ADJECTIVES.size()))
        + "-" + NOUNS.get(random.nextInt(NOUNS.size()))
        + "-" + (random.nextInt(9000) + 1000);
  }
}

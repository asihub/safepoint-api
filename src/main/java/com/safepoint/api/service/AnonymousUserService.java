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

    // Word lists for human-readable code generation
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
     * Registers a new anonymous user with a generated human-readable code and hashed PIN.
     * Retries code generation if a collision occurs (extremely rare).
     */
    @Transactional
    public AuthDto.RegisterResponse register(String pin) {
        String userCode = generateUniqueCode();
        String pinHash  = passwordEncoder.encode(pin);

        AnonymousUser user = new AnonymousUser();
        user.setUserCode(userCode);
        user.setPinHash(pinHash);
        repository.save(user);

        log.info("New anonymous user registered: {}", userCode);

        AuthDto.RegisterResponse response = new AuthDto.RegisterResponse();
        response.setUserCode(userCode);
        response.setMessage(
            "Save your code: " + userCode + ". " +
            "You will need it together with your PIN to access your data from another device."
        );
        return response;
    }

    /**
     * Verifies a user code + PIN combination.
     * Returns true only if the code exists and the PIN matches the stored bcrypt hash.
     */
    @Transactional(readOnly = true)
    public boolean verify(String userCode, String pin) {
        return repository.findByUserCode(userCode)
                .map(user -> passwordEncoder.matches(pin, user.getPinHash()))
                .orElse(false);
    }

    /**
     * Generates a unique human-readable code in the format: adjective-noun-number.
     * Example: "blue-river-42"
     * Retries up to 10 times to avoid collisions.
     */
    private String generateUniqueCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            String adj    = ADJECTIVES.get(random.nextInt(ADJECTIVES.size()));
            String noun   = NOUNS.get(random.nextInt(NOUNS.size()));
            int    number = random.nextInt(90) + 10; // 10–99
            String code   = adj + "-" + noun + "-" + number;

            if (!repository.existsByUserCode(code)) {
                return code;
            }
        }
        // Fallback — append extra random digits to guarantee uniqueness
        return ADJECTIVES.get(random.nextInt(ADJECTIVES.size()))
             + "-" + NOUNS.get(random.nextInt(NOUNS.size()))
             + "-" + (random.nextInt(9000) + 1000);
    }
}

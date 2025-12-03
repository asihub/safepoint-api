package com.safepoint.api.service;

import com.safepoint.api.model.dto.SafetyPlanDto;
import com.safepoint.api.model.entity.SafetyPlan;
import com.safepoint.api.repository.SafetyPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class SafetyPlanService {

    private final SafetyPlanRepository repository;

    /**
     * Saves or updates a safety plan for the given user.
     * The userCode + PIN are hashed — never stored in plain text.
     */
    @Transactional
    public SafetyPlan save(SafetyPlanDto dto) {
        String hash = computeHash(dto.getUserCode(), dto.getPin());

        SafetyPlan plan = repository.findByUserHash(hash)
                .orElse(new SafetyPlan());

        plan.setUserHash(hash);
        plan.setWarningSigns(dto.getWarningSigns());
        plan.setCopingStrategies(dto.getCopingStrategies());
        plan.setSocialDistractions(dto.getSocialDistractions());
        plan.setTrustedContacts(dto.getTrustedContacts());
        plan.setProfessionalResources(dto.getProfessionalResources());
        plan.setEnvironmentSafety(dto.getEnvironmentSafety());

        SafetyPlan saved = repository.save(plan);
        log.info("Safety plan saved for hash prefix: {}", hash.substring(0, 8));
        return saved;
    }

    /**
     * Retrieves a safety plan by user code and PIN.
     * Returns empty if not found or credentials are incorrect.
     */
    @Transactional(readOnly = true)
    public Optional<SafetyPlan> get(String userCode, String pin) {
        String hash = computeHash(userCode, pin);
        return repository.findByUserHash(hash);
    }

    /**
     * Deletes a safety plan. Used when user requests data deletion.
     */
    @Transactional
    public boolean delete(String userCode, String pin) {
        String hash = computeHash(userCode, pin);
        if (repository.existsByUserHash(hash)) {
            repository.deleteByUserHash(hash);
            log.info("Safety plan deleted for hash prefix: {}", hash.substring(0, 8));
            return true;
        }
        return false;
    }

    /**
     * Computes a SHA-256 hash of userCode + ":" + pin.
     * This is the only identifier stored — no PII, no reversible data.
     */
    private String computeHash(String userCode, String pin) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String input = userCode.trim().toLowerCase() + ":" + pin.trim();
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}

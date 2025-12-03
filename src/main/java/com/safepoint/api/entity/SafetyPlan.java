package com.safepoint.api.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Stores a user's personal safety plan.
 * Linked to an anonymous user via hashed ID — no PII stored.
 */
@Entity
@Table(name = "safety_plans")
@Data
@NoArgsConstructor
public class SafetyPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Anonymous user identifier — SHA-256 hash of (userCode + PIN).
     * Not reversible to any real identity.
     */
    @Column(name = "user_hash", nullable = false, unique = true)
    private String userHash;

    // Step 1 — Warning signs
    @Column(name = "warning_signs", columnDefinition = "TEXT")
    private String warningSigns;

    // Step 2 — Internal coping strategies
    @Column(name = "coping_strategies", columnDefinition = "TEXT")
    private String copingStrategies;

    // Step 3 — Social distractions (people and places)
    @Column(name = "social_distractions", columnDefinition = "TEXT")
    private String socialDistractions;

    // Step 4 — Trusted people to contact
    @Column(name = "trusted_contacts", columnDefinition = "TEXT")
    private String trustedContacts;

    // Step 5 — Professional resources and crisis lines
    @Column(name = "professional_resources", columnDefinition = "TEXT")
    private String professionalResources;

    // Step 6 — Environment safety steps
    @Column(name = "environment_safety", columnDefinition = "TEXT")
    private String environmentSafety;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

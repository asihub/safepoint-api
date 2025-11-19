package com.safepoint.api.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Represents an anonymous user identified by a human-readable code and a hashed PIN.
 * No personally identifiable information is stored.
 */
@Entity
@Table(name = "anonymous_users")
@Data
@NoArgsConstructor
public class AnonymousUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Human-readable unique code (e.g. "blue-river-42").
     * Shown to the user so they can log in from another device.
     */
    @Column(name = "user_code", nullable = false, unique = true)
    private String userCode;

    /**
     * bcrypt hash of the user's PIN.
     * The raw PIN is never stored.
     */
    @Column(name = "pin_hash", nullable = false)
    private String pinHash;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}

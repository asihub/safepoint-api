package com.safepoint.api.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Stores an encrypted progress history backup for an anonymous user.
 * The entire history is stored as a single AES-256-GCM encrypted blob —
 * the server never sees assessment data in plain text.
 */
@Entity
@Table(name = "progress_backups")
@Data
@NoArgsConstructor
public class ProgressBackup {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /**
   * Anonymous user identifier — SHA-256 hash of (userCode + PIN).
   * Identical scheme to safety_plans for consistency.
   */
  @Column(name = "user_hash", nullable = false, unique = true)
  private String userHash;

  /**
   * AES-256-GCM encrypted JSON blob of the assessment history array.
   * Encrypted client-side before upload — server stores ciphertext only.
   */
  @Column(name = "encrypted_data", columnDefinition = "TEXT", nullable = false)
  private String encryptedData;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;
}

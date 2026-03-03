package com.safepoint.api.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * A wellbeing resource (article, guide) shown to users with LOW risk.
 * Excerpt is generated automatically via HuggingFace BART summarization.
 */
@Entity
@Table(name = "wellbeing_resources")
@Data
@NoArgsConstructor
public class WellbeingResource {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String title;

  @Column(nullable = false, unique = true)
  private String url;

  @Column(nullable = false)
  private String category;

  @Column(columnDefinition = "TEXT")
  private String description;

  /** AI-generated excerpt from the article content. */
  @Column(columnDefinition = "TEXT")
  private String excerpt;

  /** When the excerpt was last generated. */
  @Column(name = "excerpt_updated_at")
  private LocalDateTime excerptUpdatedAt;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;
}

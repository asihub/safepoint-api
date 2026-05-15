package com.safepoint.api.service;

import com.safepoint.api.entity.WellbeingResource;
import com.safepoint.api.repository.WellbeingResourceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class WellbeingResourceService {

  private final WellbeingResourceRepository repository;
  private final RestTemplate mlRestTemplate;
  private final RestTemplate urlCheckRestTemplate;

  @Value("${ml.service.url:http://localhost:8001}")
  private String mlServiceUrl;

  public WellbeingResourceService(
      WellbeingResourceRepository repository,
      @Qualifier("mlRestTemplate") RestTemplate mlRestTemplate,
      @Qualifier("urlCheckRestTemplate") RestTemplate urlCheckRestTemplate) {
    this.repository = repository;
    this.mlRestTemplate = mlRestTemplate;
    this.urlCheckRestTemplate = urlCheckRestTemplate;
  }

  /** Excerpt refresh interval — 7 days */
  private static final int EXCERPT_TTL_DAYS = 7;

  // ── Public API ────────────────────────────────────────────────────────────

  public List<WellbeingResource> getAll(String lang) {
    String effectiveLang = ("es".equals(lang)) ? "es" : "en";
    return repository.findByStatusAndLanguageOrderByCategoryAscTitleAsc("AVAILABLE", effectiveLang);
  }

  // ── URL availability check ────────────────────────────────────────────────

  /**
   * HEAD-checks every wellbeing resource URL and persists AVAILABLE / UNAVAILABLE status.
   * Only saves when status actually changes. Called by the weekly scheduled job and
   * by the Actuator endpoint for manual triggers.
   */
  @Transactional
  public void checkAllUrlAvailability() {
    List<WellbeingResource> all = repository.findAll();
    log.info("Checking URL availability for {} resources...", all.size());
    int unavailableCount = 0;

    for (WellbeingResource resource : all) {
      boolean reachable = isUrlAvailable(resource.getUrl());
      String newStatus = reachable ? "AVAILABLE" : "UNAVAILABLE";

      if (!newStatus.equals(resource.getStatus())) {
        resource.setStatus(newStatus);
        repository.save(resource);
        if (!reachable) {
          log.warn("Wellbeing resource [id={}, url={}] is unreachable — marked UNAVAILABLE",
              resource.getId(), resource.getUrl());
        } else {
          log.info("Wellbeing resource [id={}, url={}] is reachable again — marked AVAILABLE",
              resource.getId(), resource.getUrl());
        }
      }

      if (!reachable) unavailableCount++;
    }

    log.info("URL availability check complete: {}/{} unavailable.", unavailableCount, all.size());
  }

  // ── Scheduled excerpt generation ──────────────────────────────────────────

  /**
   * Runs weekly (Sunday at 2am) to regenerate stale or missing excerpts,
   * then performs a URL availability pass for all resources.
   */
  @Scheduled(cron = "0 0 2 * * SUN")
  @Transactional
  public void refreshExcerpts() {
    LocalDateTime cutoff = LocalDateTime.now().minusDays(EXCERPT_TTL_DAYS);
    List<WellbeingResource> stale = repository.findStaleOrMissingExcerpts(cutoff);

    if (stale.isEmpty()) {
      log.info("All wellbeing resource excerpts are up to date.");
    } else {
      log.info("Refreshing excerpts for {} resources...", stale.size());
      int success = 0;

      for (WellbeingResource resource : stale) {
        try {
          String excerpt = fetchExcerpt(resource.getUrl());
          if (excerpt != null) {
            resource.setExcerpt(excerpt);
            resource.setExcerptUpdatedAt(LocalDateTime.now());
            repository.save(resource);
            success++;
            log.info("Excerpt updated: {}", resource.getTitle());
          }
          Thread.sleep(2000);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          return;
        } catch (Exception e) {
          log.error("Failed to update excerpt for {}: {}", resource.getTitle(), e.getMessage());
        }
      }

      log.info("Excerpt refresh complete: {}/{} updated.", success, stale.size());
    }

    checkAllUrlAvailability();
  }

  /**
   * Manually trigger excerpt refresh for a single resource (admin use).
   */
  @Transactional
  public void refreshExcerpt(Long resourceId) {
    WellbeingResource resource = repository.findById(resourceId)
        .orElseThrow(() -> new IllegalArgumentException("Resource not found: " + resourceId));

    String excerpt = fetchExcerpt(resource.getUrl());
    if (excerpt != null) {
      resource.setExcerpt(excerpt);
      resource.setExcerptUpdatedAt(LocalDateTime.now());
      repository.save(resource);
    }
  }

  /**
   * Manually trigger excerpt refresh for all resources that have no excerpt.
   */
  @Transactional
  public int refreshMissingExcerpts() {
    List<WellbeingResource> missing = repository.findAllByOrderByCategoryAscTitleAsc()
        .stream()
        .filter(r -> r.getExcerpt() == null)
        .toList();

    int total = missing.size();
    log.info("Refreshing missing excerpts for {} resources...", total);
    int success = 0;

    for (int i = 0; i < missing.size(); i++) {
      WellbeingResource resource = missing.get(i);
      log.info("[{}/{}] Processing: {}", i + 1, total, resource.getTitle());
      try {
        String excerpt = fetchExcerpt(resource.getUrl());
        if (excerpt != null) {
          resource.setExcerpt(excerpt);
          resource.setExcerptUpdatedAt(LocalDateTime.now());
          repository.save(resource);
          success++;
          log.info("[{}/{}] ✓ Updated: {}", i + 1, total, resource.getTitle());
        } else {
          log.warn("[{}/{}] ✗ No excerpt: {}", i + 1, total, resource.getTitle());
        }
        Thread.sleep(2000);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      } catch (Exception e) {
        log.error("[{}/{}] ✗ Failed: {} — {}", i + 1, total, resource.getTitle(), e.getMessage());
      }
    }

    log.info("Missing excerpt refresh complete: {}/{} updated.", success, missing.size());
    return success;
  }

  // ── Internal ──────────────────────────────────────────────────────────────

  /**
   * Returns true if the URL responds with a non-error HTTP status (< 400).
   * Returns false on 4xx, 5xx, connection failure, or timeout.
   */
  boolean isUrlAvailable(String url) {
    try {
      urlCheckRestTemplate.headForHeaders(url);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Calls Python ML service to fetch and summarize a resource URL.
   * Returns null if the ML service is unavailable or summarization fails.
   */
  @SuppressWarnings("unchecked")
  private String fetchExcerpt(String url) {
    try {
      String endpoint = mlServiceUrl + "/summarize";
      Map<String, String> request = Map.of("url", url);
      Map<String, Object> response = mlRestTemplate.postForObject(endpoint, request, Map.class);
      if (response != null) {
        return (String) response.get("excerpt");
      }
    } catch (Exception e) {
      log.warn("ML service unavailable for {}: {}", url, e.getMessage());
    }
    return null;
  }
}

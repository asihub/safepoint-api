package com.safepoint.api.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class SamhsaService {

  private final RestTemplate restTemplate;

  private static final String SAMHSA_URL = "https://findtreatment.gov/locator/listing";
  private static final double DEFAULT_RADIUS_METERS = 16093.0;
  private static final double MAX_RADIUS_METERS = 160934.0;

  public SamhsaService(@Qualifier("samhsaRestTemplate") RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  /**
   * Finds mental health treatment facilities near a given location.
   * Uses POST /locator/listing with form-encoded payload.
   */
  public List<Map<String, Object>> findFacilities(
      double latitude,
      double longitude,
      String insurance,
      int    limit,
      double radiusMeters,
      String serviceType) {

    double radius = Math.min(Math.max(radiusMeters, 1000), MAX_RADIUS_METERS);

    // SA = Substance Abuse, MH = Mental Health, default MH
    String sType = "sa".equalsIgnoreCase(serviceType) ? "SA" : "MH";

    // Build form-encoded body
    MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
    body.add("sType",      sType);
    body.add("sAddr",      latitude + "," + longitude);
    body.add("limitType",  "2");   // 2 = distance-based
    body.add("limitValue", String.valueOf((int) radius));
    body.add("pageSize",   String.valueOf(limit));
    body.add("page",       "1");
    body.add("sort",       "0");   // 0 = sort by distance

    String filterPay = mapInsuranceToFilter(insurance);
    if (filterPay != null) {
      body.add("filterPay", filterPay);
    }

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

    try {
      log.info("SAMHSA POST /locator/listing: sType={} radius={}m insurance={}", sType, (int) radius, insurance);

      ResponseEntity<Map> response = restTemplate.postForEntity(SAMHSA_URL, request, Map.class);

      if (response.getBody() == null) return Collections.emptyList();

      @SuppressWarnings("unchecked")
      List<Map<String, Object>> rows = (List<Map<String, Object>>) response.getBody().get("rows");

      return rows != null ? rows : Collections.emptyList();

    } catch (Exception e) {
      log.error("SAMHSA API error: {}", e.getMessage());
      return Collections.emptyList();
    }
  }

  public List<Map<String, Object>> findFacilities(
      double latitude, double longitude, String insurance, int limit, double radiusMeters) {
    return findFacilities(latitude, longitude, insurance, limit, radiusMeters, "MH");
  }

  public List<Map<String, Object>> findFacilities(
      double latitude, double longitude, String insurance, int limit) {
    return findFacilities(latitude, longitude, insurance, limit, DEFAULT_RADIUS_METERS, "MH");
  }

  private String mapInsuranceToFilter(String insurance) {
    if (insurance == null || insurance.equalsIgnoreCase("UNKNOWN")) return null;
    return switch (insurance.toUpperCase()) {
      case "MEDICAID" -> "MD";
      case "MEDICARE" -> "MC";
      case "PRIVATE"  -> "PI";
      case "NONE"     -> "SF";
      default         -> null;
    };
  }
}

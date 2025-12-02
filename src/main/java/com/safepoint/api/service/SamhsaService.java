package com.safepoint.api.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class SamhsaService {

  private final RestTemplate restTemplate;

  // Correct SAMHSA API endpoint as per FindTreatment.gov Developer Guide v1.10 (Jan 2025)
  private static final String SAMHSA_API_URL =
      "https://findtreatment.gov/locator/exportsAsJson/v2";

  public SamhsaService(@Qualifier("samhsaRestTemplate") RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  /**
   * Fetches nearby mental health treatment facilities from the SAMHSA FindTreatment.gov API.
   * Uses limitType=2 (distance in meters) with a 40km radius.
   *
   * @param latitude   user latitude
   * @param longitude  user longitude
   * @param insurance  insurance type filter (MEDICAID, MEDICARE, PRIVATE, NONE, UNKNOWN)
   * @param maxResults max number of results
   */
  public List<Map<String, Object>> getFacilities(double latitude,
                                                 double longitude,
                                                 String insurance,
                                                 int maxResults) {
    try {
      // Format: "lat,lng" (quoted as per API docs)
      String sAddr = latitude + "," + longitude;

      UriComponentsBuilder builder = UriComponentsBuilder
          .fromUriString(SAMHSA_API_URL)
          .queryParam("sAddr", sAddr)
          .queryParam("limitType", 2)          // distance-based search
          .queryParam("limitValue", 40000)     // 40km radius
          .queryParam("pageSize", maxResults)
          .queryParam("page", 1)
          .queryParam("sort", 0);

      // Apply insurance/payment filter if not UNKNOWN
      if (insurance != null && !insurance.equalsIgnoreCase("UNKNOWN")) {
        String payCode = mapInsuranceToPayCode(insurance);
        if (payCode != null) {
          builder.queryParam("filterPay", payCode);
        }
      }

      String url = builder.toUriString();
      log.info("Calling SAMHSA API: {}", url);

      ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
          url,
          HttpMethod.GET,
          null,
          new ParameterizedTypeReference<Map<String, Object>>() {}
      );

      if (response.getBody() == null) return Collections.emptyList();

      @SuppressWarnings("unchecked")
      List<Map<String, Object>> rows =
          (List<Map<String, Object>>) response.getBody().get("rows");

      return rows != null ? rows : Collections.emptyList();

    } catch (Exception e) {
      log.error("SAMHSA API error: {}", e.getMessage());
      return Collections.emptyList();
    }
  }

  /**
   * Maps insurance type to SAMHSA filterPay code.
   * See FindTreatment.gov API documentation for full list.
   */
  private String mapInsuranceToPayCode(String insurance) {
    return switch (insurance.toUpperCase()) {
      case "MEDICAID" -> "4";
      case "MEDICARE" -> "5";
      case "PRIVATE"  -> "6";
      case "NONE"     -> "1";  // self-pay / sliding fee
      default         -> null;
    };
  }
}
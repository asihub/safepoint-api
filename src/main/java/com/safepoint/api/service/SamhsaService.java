package com.safepoint.api.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
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

  @Value("${samhsa.api.url}")
  private String samhsaUrl;

  @Value("${samhsa.api.key:}")
  private String apiKey;

  /**
   * Explicit constructor with @Qualifier to resolve ambiguous RestTemplate beans.
   */
  public SamhsaService(@Qualifier("samhsaRestTemplate") RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  /**
   * Fetches nearby mental health treatment facilities from the SAMHSA locator API.
   *
   * @param latitude     user latitude
   * @param longitude    user longitude
   * @param insurance    insurance type filter (MEDICAID, MEDICARE, PRIVATE, NONE, UNKNOWN)
   * @param maxResults   maximum number of results to return
   * @return list of facility maps or empty list on error
   */
  public List<Map<String, Object>> getFacilities(double latitude,
                                                 double longitude,
                                                 String insurance,
                                                 int maxResults) {
    try {
      UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(samhsaUrl)
          .queryParam("sAddr", latitude + "," + longitude)
          .queryParam("dist", 25)
          .queryParam("limit", maxResults)
          .queryParam("output", "json");

      // Apply insurance filter if not UNKNOWN
      if (insurance != null && !insurance.equalsIgnoreCase("UNKNOWN")) {
        String paymentCode = mapInsuranceToSamhsaCode(insurance);
        if (paymentCode != null) {
          builder.queryParam("pay", paymentCode);
        }
      }

      if (!apiKey.isBlank()) {
        builder.queryParam("apikey", apiKey);
      }

      String url = builder.toUriString();

      @SuppressWarnings("unchecked")
      Map<String, Object> response = restTemplate.getForObject(url, Map.class);

      if (response == null) return Collections.emptyList();

      @SuppressWarnings("unchecked")
      List<Map<String, Object>> rows = (List<Map<String, Object>>) response.get("rows");

      return rows != null ? rows : Collections.emptyList();

    } catch (Exception e) {
      log.error("SAMHSA API error: {}", e.getMessage());
      return Collections.emptyList();
    }
  }

  /**
   * Maps our insurance type string to the SAMHSA API payment code.
   * https://findtreatment.samhsa.gov/locator/doc
   */
  private String mapInsuranceToSamhsaCode(String insurance) {
    return switch (insurance.toUpperCase()) {
      case "MEDICAID" -> "4";
      case "MEDICARE" -> "5";
      case "PRIVATE"  -> "6";
      case "NONE"     -> "1";
      default         -> null;
    };
  }
}
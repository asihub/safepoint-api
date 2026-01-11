package com.safepoint.api.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
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

  private static final String SAMHSA_URL =
      "https://findtreatment.gov/locator/exportsAsJson/v2";

  // Default radius — 16 km (~10 miles)
  private static final double DEFAULT_RADIUS_METERS = 16093.0;

  // Max allowed radius to prevent abuse — 100 miles
  private static final double MAX_RADIUS_METERS = 160934.0;

  public SamhsaService(@Qualifier("samhsaRestTemplate") RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  /**
   * Finds mental health treatment facilities near a given location.
   *
   * @param latitude     user latitude
   * @param longitude    user longitude
   * @param insurance    insurance type filter (MEDICAID, MEDICARE, PRIVATE, NONE, UNKNOWN)
   * @param limit        max number of results to return
   * @param radiusMeters search radius in meters (SAMHSA limitValue)
   */
  public List<Map<String, Object>> findFacilities(
      double latitude,
      double longitude,
      String insurance,
      int limit,
      double radiusMeters,
      String serviceType) {

    // Clamp radius to safe range
    double radius = Math.min(Math.max(radiusMeters, 1000), MAX_RADIUS_METERS);

    UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(SAMHSA_URL)
        .queryParam("sAddr", latitude + "," + longitude)
        .queryParam("limitType", 2)              // 2 = distance-based
        .queryParam("limitValue", radius)
        .queryParam("pageSize", limit)
        .queryParam("page", 1)
        .queryParam("sort", 0);             // 0 = sort by distance

    // Service type: mh = mental health, sa = substance abuse, both = both
    if (serviceType != null && !serviceType.isBlank()) {
      builder.queryParam("sType", serviceType);
    }

    // Insurance type filter
    String filterPay = mapInsuranceToFilter(insurance);
    if (filterPay != null) {
      builder.queryParam("filterPay", filterPay);
    }

    try {
      String url = builder.toUriString();
      log.info("SAMHSA query: radius={}m insurance={} sType={}", (int) radius, insurance, serviceType);

      @SuppressWarnings("unchecked")
      Map<String, Object> response = restTemplate.getForObject(url, Map.class);

      if (response == null) return Collections.emptyList();

      @SuppressWarnings("unchecked")
      List<Map<String, Object>> rows = (List<Map<String, Object>>) response.get("rows");
      if (rows == null) return Collections.emptyList();

      // Client-side insurance filter — SAMHSA filterPay is unreliable
      // Filter by PAY service entry in the services array
      if (filterPay != null) {
        rows = filterByInsurance(rows, filterPay);
      }

      return rows;

    } catch (Exception e) {
      log.error("SAMHSA API error: {}", e.getMessage());
      return Collections.emptyList();
    }
  }

  /**
   * Overload with default radius.
   */
  public List<Map<String, Object>> findFacilities(
      double latitude, double longitude, String insurance, int limit) {
    return findFacilities(latitude, longitude, insurance, limit, DEFAULT_RADIUS_METERS, "mh");
  }

  /**
   * Filters facilities by insurance type using the services array in the response.
   * SAMHSA's filterPay parameter is unreliable so we filter client-side.
   * PAY service entry contains human-readable payment types like "Medicaid", "Medicare", etc.
   */
  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> filterByInsurance(List<Map<String, Object>> rows, String filterPay) {
    String keyword = switch (filterPay) {
      case "MD" -> "medicaid";
      case "MC" -> "medicare";
      case "PI" -> "private health";
      case "SF" -> "sliding fee";
      default -> null;
    };
    if (keyword == null) return rows;

    final String kw = keyword;
    return rows.stream()
        .filter(facility -> {
          Object svcs = facility.get("services");
          if (!(svcs instanceof List)) return false;
          List<Map<String, Object>> services = (List<Map<String, Object>>) svcs;
          return services.stream().anyMatch(s -> {
            String f2 = (String) s.get("f2");
            String f3 = (String) s.get("f3");
            return "PAY".equals(f2) && f3 != null && f3.toLowerCase().contains(kw);
          });
        })
        .collect(java.util.stream.Collectors.toList());
  }

  private String mapInsuranceToFilter(String insurance) {
    if (insurance == null || insurance.equalsIgnoreCase("UNKNOWN")) return null;
    return switch (insurance.toUpperCase()) {
      case "MEDICAID" -> "MD";
      case "MEDICARE" -> "MC";
      case "PRIVATE" -> "PI";
      case "NONE" -> "SF"; // Sliding fee / no insurance
      default -> null;
    };
  }
}

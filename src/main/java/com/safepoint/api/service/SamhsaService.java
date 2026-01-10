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
     * @param latitude      user latitude
     * @param longitude     user longitude
     * @param insurance     insurance type filter (MEDICAID, MEDICARE, PRIVATE, NONE, UNKNOWN)
     * @param limit         max number of results to return
     * @param radiusMeters  search radius in meters (SAMHSA limitValue)
     */
    public List<Map<String, Object>> findFacilities(
            double latitude,
            double longitude,
            String insurance,
            int    limit,
            double radiusMeters) {

        // Clamp radius to safe range
        double radius = Math.min(Math.max(radiusMeters, 1000), MAX_RADIUS_METERS);

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(SAMHSA_URL)
            .queryParam("sAddr",      latitude + "," + longitude)
            .queryParam("limitType",  2)              // 2 = distance-based
            .queryParam("limitValue", radius)
            .queryParam("pageSize",   limit)
            .queryParam("page",       1)
            .queryParam("sort",       0);             // 0 = sort by distance

        // Insurance type filter
        String filterPay = mapInsuranceToFilter(insurance);
        if (filterPay != null) {
            builder.queryParam("filterPay", filterPay);
        }

        try {
            String url = builder.toUriString();
            log.info("SAMHSA query: radius={}m insurance={}", (int) radius, insurance);

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
     * Overload with default radius.
     */
    public List<Map<String, Object>> findFacilities(
            double latitude, double longitude, String insurance, int limit) {
        return findFacilities(latitude, longitude, insurance, limit, DEFAULT_RADIUS_METERS);
    }

    private String mapInsuranceToFilter(String insurance) {
        if (insurance == null || insurance.equalsIgnoreCase("UNKNOWN")) return null;
        return switch (insurance.toUpperCase()) {
            case "MEDICAID" -> "MD";
            case "MEDICARE" -> "MC";
            case "PRIVATE"  -> "PI";
            case "NONE"     -> "SF"; // Sliding fee / no insurance
            default         -> null;
        };
    }
}

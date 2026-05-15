package com.safepoint.api.config;

import com.safepoint.api.service.WellbeingResourceService;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.stereotype.Component;

/**
 * Actuator endpoint for manually triggering a wellbeing resource URL availability check.
 * Exposed at POST /actuator/wellbeingCheck — internal network only (firewalled at nginx).
 */
@Endpoint(id = "wellbeingCheck")
@Component
public class WellbeingCheckEndpoint {

  private final WellbeingResourceService service;

  public WellbeingCheckEndpoint(WellbeingResourceService service) {
    this.service = service;
  }

  @WriteOperation
  public String run() {
    service.checkAllUrlAvailability();
    return "triggered";
  }
}

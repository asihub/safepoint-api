package com.safepoint.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class AppConfig {

  @Value("${ml.service.timeout-ms:10000}")
  private int mlTimeoutMs;

  /**
   * RestTemplate for calling the internal Python ML service.
   * Timeout is configurable via application.yml.
   */
  @Bean("mlRestTemplate")
  public RestTemplate mlRestTemplate() {
    return new RestTemplate();
  }

  /**
   * RestTemplate for calling the external SAMHSA API.
   * Longer timeout to account for external network latency.
   */
  @Bean("samhsaRestTemplate")
  public RestTemplate samhsaRestTemplate() {
    return new RestTemplate();
  }
}
package com.safepoint.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/version")
@Tag(name = "Version", description = "Service version info")
public class VersionController {

    @Value("${app.version:0.0.0}")
    private String version;

    @Value("${app.updated-at:unknown}")
    private String updatedAt;

    @GetMapping
    @Operation(summary = "Get API version and build date")
    public Map<String, String> getVersion() {
        return Map.of(
            "service",    "safepoint-api",
            "version",    version,
            "updatedAt",  updatedAt
        );
    }
}

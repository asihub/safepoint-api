package com.safepoint.api.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO for creating or updating a safety plan.
 * Based on the Stanley-Brown Safety Planning Intervention protocol.
 */
@Data
@Schema(description = "Personal safety plan — six-step Stanley-Brown protocol")
public class SafetyPlanDto {

    @NotBlank(message = "User code is required")
    @Schema(description = "Human-readable anonymous user code", example = "blue-river-42")
    private String userCode;

    @NotBlank(message = "PIN is required")
    @Schema(description = "User PIN (4-6 digits)", example = "1234")
    private String pin;

    @Schema(description = "Step 1: Personal warning signs that a crisis may be approaching",
            example = "Feeling isolated, not sleeping, negative self-talk")
    private String warningSigns;

    @Schema(description = "Step 2: Internal coping strategies I can use on my own",
            example = "Go for a walk, listen to music, deep breathing")
    private String copingStrategies;

    @Schema(description = "Step 3: People and settings that provide distraction and support",
            example = "Coffee shop, my friend Maria, the library")
    private String socialDistractions;

    @Schema(description = "Step 4: Trusted people I can ask for help (name + contact)",
            example = "Mom: 555-1234, John: 555-5678")
    private String trustedContacts;

    @Schema(description = "Step 5: Professional resources and crisis lines",
            example = "988 Suicide & Crisis Lifeline, Dr. Smith: 555-9999")
    private String professionalResources;

    @Schema(description = "Step 6: Steps to make my environment safer",
            example = "Give medications to a trusted person, remove firearms from home")
    private String environmentSafety;
}

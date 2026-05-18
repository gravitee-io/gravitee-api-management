package io.gravitee.gamma.module.authz.entityimport.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ScimConnectorRequest(
    @NotBlank
    @Pattern(regexp = "^[a-z0-9][a-z0-9-]*$", message = "name must be lowercase alphanumerics with optional dashes, no spaces")
    String name,
    @NotBlank String url,
    String token,
    Boolean importUsers,
    Boolean importGroups,
    @Min(value = 10, message = "intervalSeconds must be >= 10")
    @Max(value = 86_400, message = "intervalSeconds must be <= 86400 (1 day)")
    Integer intervalSeconds
) {}

package io.gravitee.gamma.module.authz.entityimport.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ScimConnectorRequest(
    @NotBlank @Pattern(regexp = "^[a-z0-9][a-z0-9-]*$",
        message = "name must be lowercase alphanumerics with optional dashes, no spaces") String name,
    @NotBlank String url,
    String token,
    Boolean importUsers,
    Boolean importGroups
) {}

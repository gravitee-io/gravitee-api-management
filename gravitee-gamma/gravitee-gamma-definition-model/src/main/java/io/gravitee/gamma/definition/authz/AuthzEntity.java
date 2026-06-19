/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gamma.definition.authz;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Wire schema for authz entity events (PUBLISH_AUTHZ_ENTITY / UNPUBLISH_AUTHZ_ENTITY).
 *
 * <p>Shared contract between the publisher (module-authz plugin) and consumers
 * (gateway-services-sync). See {@link AuthzPolicy} for design notes.
 *
 * <p>Sensitive attribute values are redacted by the publisher before emission — wire
 * payloads never carry secrets.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthzEntity implements Serializable {

    @JsonProperty
    private String id;

    @JsonProperty(required = true)
    @NotBlank
    private String entityId;

    @JsonProperty(required = true)
    @NotNull
    private AuthzEntityKind kind;

    @JsonProperty
    private Map<String, Object> attributes;

    @JsonProperty
    private List<String> parents;

    @JsonProperty
    private String source;

    @JsonProperty
    private String environmentId;

    /** ISO-8601 timestamp string (e.g. "2024-01-01T00:00:00Z"). Kept as String to avoid coupling
     *  to a specific Jackson time module on the gateway side. */
    @JsonProperty
    private String updatedAt;

    /**
     * Engine type name (e.g. {@code "User"}, {@code "Doc"}). Optional on the wire so that
     * older publishers stay compatible — when null the gateway falls back to the kind-default
     * ({@code "Principal"} / {@code "Resource"}). Appended to the end of the field list so the
     * Lombok-generated {@code @AllArgsConstructor} preserves binary compatibility with
     * pre-typed-entity-type consumers up to the first 8 positional arguments.
     */
    @JsonProperty
    private String entityType;

    @JsonProperty("targetPdpIds")
    private Set<String> targetPdpIds;
}

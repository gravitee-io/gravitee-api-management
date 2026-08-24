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
import java.io.Serializable;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Wire schema for authz schema events (PUBLISH_AUTHZ_SCHEMA / UNPUBLISH_AUTHZ_SCHEMA).
 *
 * <p>Shared contract between the publisher (module-authz plugin) and consumers
 * (gateway-services-sync), mirroring {@link AuthzPolicy}. Several documents may target one
 * engine; the PDP composes them, so nothing here is a per-engine singleton.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthzSchema implements Serializable {

    @JsonProperty(required = true)
    @NotBlank
    private String id;

    @JsonProperty(required = true)
    @NotBlank
    private String name;

    @JsonProperty(required = true)
    @NotBlank
    private String schemaText;

    @JsonProperty
    private String environmentId;

    /** ISO-8601 timestamp string (e.g. "2024-01-01T00:00:00Z"). Kept as String to avoid coupling
     *  to a specific Jackson time module on the gateway side. */
    @JsonProperty
    private String updatedAt;

    @JsonProperty("targetPdpIds")
    private Set<String> targetPdpIds;
}

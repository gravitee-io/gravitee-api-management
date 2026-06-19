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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Wire schema for authz PDP provisioning events (PUBLISH_AUTHZ_PDP / UNPUBLISH_AUTHZ_PDP).
 *
 * <p>Shared contract between the publisher (module-authz plugin) and consumers
 * (the node-level PDP Service in gateway-services-sync). See {@link AuthzPolicy} for design notes.
 *
 * <p>A PDP is provisioned for exactly one {@code targetPdpId} (singular), unlike {@link AuthzPolicy}
 * / {@link AuthzEntity} which carry a {@code targetPdpIds} set of scopes they apply to.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthzPdp implements Serializable {

    @JsonProperty(required = true)
    @NotBlank
    private String id;

    @JsonProperty(required = true)
    @NotBlank
    private String name;

    @JsonProperty("targetPdpId")
    @NotBlank
    private String targetPdpId;

    @JsonProperty
    private String environmentId;

    /** Stable sharding tag a PDP targets. Placement mirrors API sharding: an untagged gateway is a
     *  catch-all that hosts every PDP engine, while a tagged gateway hosts a PDP whose tag it carries (and
     *  is not excluded via "!tag"). Null/blank means the PDP itself is untagged — hosted only on untagged
     *  (catch-all) gateways. Tag your gateways to narrow which engines they host. */
    @JsonProperty
    private String tag;

    /** ISO-8601 timestamp string (e.g. "2024-01-01T00:00:00Z"). Kept as String to avoid coupling
     *  to a specific Jackson time module on the gateway side. */
    @JsonProperty
    private String updatedAt;
}

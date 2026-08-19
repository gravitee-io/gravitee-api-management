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
package io.gravitee.apim.plugin.gamma.api.automation;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Optional;

/**
 * How a Gamma module exposes resources on the Automation API.
 *
 * <p>A module implements this once, as a Spring bean of its plugin context; the module handler copies it
 * into the host context and the Automation API mounts it at
 * {@code /automation/organizations/{orgId}/environments/{envId}/{module}/{kind}} with the uniform verbs:
 * {@code PUT} on the collection with the {@code hrid} carried in the body (optionally {@code ?dryRun=true}),
 * {@code GET} and {@code DELETE} on {@code {kind}/{hrid}}.
 *
 * <p>The Automation API owns the conventions and enforces them before a call reaches the module: the
 * license feature and the kind's permission are checked, {@code spec.hrid} is present and well-formed,
 * and {@code id} is derived deterministically from the organization, module, kind, HRID and environment.
 * After the call it stamps {@code hrid}, {@code id}, {@code environmentId}, {@code organizationId} and
 * {@code errors} onto the returned view, so a module never stores the HRID. The module owns the typed
 * contract: it publishes its own OpenAPI fragment, validates the spec against it, and maps it to its domain.
 *
 * <p>Payloads are JSON so the contract stays neutral to every module's shape. A view must never carry a
 * credential value, whatever form the spec used to supply it.
 */
public interface GammaAutomationPort {
    /**
     * @return the path segment under {@code /environments/{envId}} this module answers on — its plugin id,
     *     e.g. {@code aim}
     */
    String module();

    /**
     * @return the license feature the whole module surface requires (e.g. {@code gamma-aim-module}); empty
     *     when the surface is not license-gated
     */
    Optional<String> licenseFeature();

    /**
     * @param path the collection path under the module mount, e.g. {@code catalog/mcp-servers}
     * @return the kind served at that path, or empty when the module serves nothing there
     */
    Optional<ResourceKind> kind(String path);

    /**
     * Dry run of {@link #upsert}: runs the same validation and persists nothing.
     *
     * @return the redacted preview of the state the spec would produce, plus every finding
     */
    UpsertResult<ObjectNode> validate(AutomationContext context, ResourceKind kind, String id, ObjectNode spec);

    /**
     * Converge the resource identified by {@code id} on {@code spec}: create it or update it, and treat an
     * unchanged spec as a no-op. Severe findings mean nothing was persisted.
     *
     * @return the persisted state, or the redacted preview when severe findings prevented the apply
     */
    UpsertResult<ObjectNode> upsert(AutomationContext context, ResourceKind kind, String id, ObjectNode spec);

    Optional<ObjectNode> findById(AutomationContext context, ResourceKind kind, String id);

    /**
     * @return {@code true} when the resource existed and was deleted, {@code false} when there was nothing to
     *     delete
     */
    boolean deleteById(AutomationContext context, ResourceKind kind, String id);

    /**
     * @return the module's automation OpenAPI document (YAML), describing every kind it serves under its
     *     mount. It must be self-contained and use module-prefixed component names so it can be merged into
     *     the Automation API document without collisions. Empty when the module publishes none.
     */
    Optional<String> openApiFragment();
}

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
package io.gravitee.definition.model.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;

/** Shared profile catalog used by runtime policy resolution and both management UIs. */
public final class ContextManagementProfiles {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final JsonNode CATALOG = load();

    private ContextManagementProfiles() {}

    private static JsonNode load() {
        try (var input = ContextManagementProfiles.class.getResourceAsStream("/context-profiles.json")) {
            if (input == null) throw new IllegalStateException("Missing context profile catalog");
            return MAPPER.readTree(input);
        } catch (IOException error) {
            throw new IllegalStateException("Cannot read context profile catalog", error);
        }
    }

    public static JsonNode catalog() {
        return CATALOG.deepCopy();
    }

    public static Integer compactThreshold(ContextManagementPolicy.Profile profile) {
        JsonNode target = CATALOG.path(profile.name()).path("compactThreshold");
        return target.isNull() || target.isMissingNode() ? null : target.intValue();
    }

    public static RequestControls resolve(ContextManagementPolicy policy, String provider, RequestControls custom) {
        if (policy == null || policy.getProfile() == ContextManagementPolicy.Profile.CUSTOM) return custom;
        JsonNode profile = CATALOG.path(policy.getProfile().name());
        ObjectNode controls = profile.path("requestControls").deepCopy();
        JsonNode providerControls = provider == null ? null : profile.path("providerRequestControls").get(provider);
        if (providerControls instanceof ObjectNode object) controls.setAll(object);
        return MAPPER.convertValue(controls, RequestControls.class);
    }
}

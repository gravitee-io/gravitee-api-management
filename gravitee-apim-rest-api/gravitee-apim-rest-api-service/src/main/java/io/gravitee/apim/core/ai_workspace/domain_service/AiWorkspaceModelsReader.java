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
package io.gravitee.apim.core.ai_workspace.domain_service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.ai_workspace.model.AiWorkspaceModelInfo;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads model name and prices from an LLM proxy endpoint configuration.
 * The same document holds provider credentials; this reader never copies that block.
 */
public final class AiWorkspaceModelsReader {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private AiWorkspaceModelsReader() {}

    public static List<AiWorkspaceModelInfo> read(Api definition) {
        if (definition == null || definition.getEndpointGroups() == null) {
            return List.of();
        }
        List<AiWorkspaceModelInfo> models = new ArrayList<>();
        for (EndpointGroup group : definition.getEndpointGroups()) {
            if (group.getEndpoints() == null) {
                continue;
            }
            for (Endpoint endpoint : group.getEndpoints()) {
                models.addAll(readConfiguration(endpoint.getConfiguration()));
            }
        }
        return List.copyOf(models);
    }

    static List<AiWorkspaceModelInfo> readConfiguration(String configuration) {
        if (configuration == null || configuration.isBlank()) {
            return List.of();
        }
        JsonNode models;
        try {
            models = MAPPER.readTree(configuration).path("models");
        } catch (Exception e) {
            return List.of();
        }
        if (!models.isArray()) {
            return List.of();
        }
        List<AiWorkspaceModelInfo> result = new ArrayList<>();
        for (JsonNode model : models) {
            if (!model.path("name").isTextual() || model.path("name").asText().isBlank()) {
                continue;
            }
            result.add(
                new AiWorkspaceModelInfo(
                    model.path("name").asText(),
                    model.path("inputPrice").isNumber() ? model.path("inputPrice").asDouble() : null,
                    model.path("outputPrice").isNumber() ? model.path("outputPrice").asDouble() : null
                )
            );
        }
        return result;
    }
}

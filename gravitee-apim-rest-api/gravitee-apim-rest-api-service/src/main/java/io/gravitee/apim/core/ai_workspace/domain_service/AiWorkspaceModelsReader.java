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
import io.gravitee.apim.core.ai_workspace.model.AiWorkspaceModel;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.CustomLog;

/**
 * Model name and prices from the workspace LLM proxy endpoint configuration.
 * Provider, target, authentication and every other configuration field are ignored.
 * Aliases, aliasOnly and aliasRequiresPrefix are proxy routing options, not catalog fields, so they are left out.
 * The same name is returned once: a failover group repeats it on every endpoint, and the first declaration is kept.
 */
@CustomLog
public final class AiWorkspaceModelsReader {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private AiWorkspaceModelsReader() {}

    public static List<AiWorkspaceModel> read(String environmentId, List<Api> apis) {
        if (apis == null) {
            return List.of();
        }
        Map<String, AiWorkspaceModel> models = new LinkedHashMap<>();
        for (Api api : apis) {
            if (api == null || api.getType() != ApiType.LLM_PROXY || !sameEnvironment(environmentId, api)) {
                continue;
            }
            if (!(api.getApiDefinitionValue() instanceof io.gravitee.definition.model.v4.Api definition)) {
                continue;
            }
            if (definition.getEndpointGroups() == null) {
                continue;
            }
            for (EndpointGroup group : definition.getEndpointGroups()) {
                if (group == null || group.getEndpoints() == null) {
                    continue;
                }
                for (Endpoint endpoint : group.getEndpoints()) {
                    if (endpoint != null) {
                        for (AiWorkspaceModel model : readConfiguration(endpoint.getConfiguration(), api.getId())) {
                            models.putIfAbsent(model.name(), model);
                        }
                    }
                }
            }
        }
        return List.copyOf(models.values());
    }

    private static List<AiWorkspaceModel> readConfiguration(String configuration, String apiId) {
        if (configuration == null || configuration.isBlank()) {
            return List.of();
        }
        JsonNode models;
        try {
            models = MAPPER.readTree(configuration).path("models");
        } catch (Exception exception) {
            log.warn("Could not read LLM proxy models for api {}", apiId);
            return List.of();
        }
        if (!models.isArray()) {
            return List.of();
        }
        List<AiWorkspaceModel> result = new ArrayList<>();
        for (JsonNode model : models) {
            if (model == null || !model.isObject()) {
                continue;
            }
            JsonNode name = model.get("name");
            if (name == null || !name.isTextual() || name.asText().isBlank()) {
                continue;
            }
            // aliases are not copied: they rename this model for routing and are not a second catalog entry
            result.add(new AiWorkspaceModel(name.asText(), price(model, "inputPrice"), price(model, "outputPrice")));
        }
        return result;
    }

    private static Double price(JsonNode model, String field) {
        JsonNode value = model.get(field);
        if (value == null || !value.isNumber()) {
            return null;
        }
        return value.doubleValue();
    }

    private static boolean sameEnvironment(String environmentId, Api api) {
        return environmentId == null || api.getEnvironmentId() == null || environmentId.equals(api.getEnvironmentId());
    }
}

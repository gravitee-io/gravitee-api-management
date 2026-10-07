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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads model name and prices from LLM proxy endpoint configuration.
 * Only {@code models[].name}, {@code inputPrice} and {@code outputPrice} are taken —
 * authentication and other endpoint fields are ignored.
 */
public final class AiWorkspaceModelsReader {

    private static final Logger log = LoggerFactory.getLogger(AiWorkspaceModelsReader.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private AiWorkspaceModelsReader() {}

    public static List<AiWorkspaceModel> read(String environmentId, List<Api> apis) {
        if (apis == null || apis.isEmpty()) {
            return List.of();
        }
        Map<String, AiWorkspaceModel> byName = new LinkedHashMap<>();
        for (Api api : apis) {
            if (api == null || api.getType() != ApiType.LLM_PROXY || !sameEnvironment(environmentId, api)) {
                continue;
            }
            var definition = api.getApiDefinitionHttpV4();
            if (definition == null || definition.getEndpointGroups() == null) {
                continue;
            }
            for (EndpointGroup group : definition.getEndpointGroups()) {
                if (group == null || group.getEndpoints() == null) {
                    continue;
                }
                for (Endpoint endpoint : group.getEndpoints()) {
                    readEndpoint(endpoint).forEach(model -> byName.putIfAbsent(model.name(), model));
                }
            }
        }
        return List.copyOf(byName.values());
    }

    private static List<AiWorkspaceModel> readEndpoint(Endpoint endpoint) {
        if (endpoint == null) {
            return List.of();
        }
        return readConfiguration(endpoint.getConfiguration());
    }

    static List<AiWorkspaceModel> readConfiguration(String configuration) {
        if (configuration == null || configuration.isBlank()) {
            return List.of();
        }
        JsonNode modelsNode;
        try {
            modelsNode = MAPPER.readTree(configuration).path("models");
        } catch (Exception exception) {
            log.warn("Could not read LLM proxy endpoint models configuration", exception);
            return List.of();
        }
        if (!modelsNode.isArray()) {
            return List.of();
        }
        List<AiWorkspaceModel> models = new ArrayList<>();
        for (JsonNode modelNode : modelsNode) {
            if (modelNode == null || !modelNode.isObject()) {
                continue;
            }
            String name = text(modelNode.get("name"));
            if (name == null || name.isBlank()) {
                continue;
            }
            models.add(new AiWorkspaceModel(name, price(modelNode.get("inputPrice")), price(modelNode.get("outputPrice"))));
        }
        return List.copyOf(models);
    }

    private static String price(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        if (node.isNumber()) {
            return node.asText();
        }
        String text = node.asText(null);
        return text == null || text.isBlank() ? null : text;
    }

    private static String text(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        return node.asText(null);
    }

    private static boolean sameEnvironment(String environmentId, Api api) {
        return environmentId == null || api.getEnvironmentId() == null || environmentId.equals(api.getEnvironmentId());
    }
}

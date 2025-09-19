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
package io.gravitee.rest.api.service.impl.upgrade.upgrader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.node.api.upgrader.Upgrader;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApiFieldFilter;
import io.gravitee.repository.management.model.Environment;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ApiEndpointWeightUpgrader implements Upgrader {

    @Lazy
    @Autowired
    private ApiRepository apiRepository;

    @Lazy
    @Autowired
    private EnvironmentRepository environmentRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private final List<String> apisUpdated = new ArrayList<>();

    @Override
    public int getOrder() {
        return UpgraderOrder.API_ENDPOINT_WEIGHT_UPGRADER;
    }

    @Override
    public boolean upgrade() {
        try {
            for (Environment env : environmentRepository.findAll()) {
                fixApiEndpointWeights(new ExecutionContext(env));
            }
            if (!apisUpdated.isEmpty()) {
                log.info("{} updated {} APIs: {}", getClass().getSimpleName(), apisUpdated.size(), apisUpdated);
            }
            return true;
        } catch (Exception e) {
            log.error("Error applying {}", getClass().getSimpleName(), e);
            return false;
        }
    }

    private void fixApiEndpointWeights(ExecutionContext ctx) {
        apiRepository
            .search(
                getDefaultApiCriteriaBuilder().environmentId(ctx.getEnvironmentId()).build(),
                null,
                new ApiFieldFilter.Builder().excludePicture().build()
            )
            .forEach(api -> {
                try {
                    DefinitionVersion defVersion = api.getDefinitionVersion();
                    if (defVersion == DefinitionVersion.V1) return;
                    if (defVersion == null) defVersion = DefinitionVersion.V2;

                    String definition = api.getDefinition();
                    boolean updated = false;

                    if (defVersion == DefinitionVersion.V4) {
                        Api v4Api = objectMapper.readValue(definition, Api.class);
                        updated = fixEndpointWeightsV4(v4Api);
                        if (updated) {
                            api.setDefinition(objectMapper.writeValueAsString(v4Api));
                        }
                    } else {
                        if (defVersion != DefinitionVersion.FEDERATED) {
                            JsonNode rootNode = objectMapper.readTree(definition);
                            updated = fixEndpointWeightsV2(rootNode);
                            if (updated) {
                                api.setDefinition(objectMapper.writeValueAsString(rootNode));
                            }
                        }
                    }
                    if (updated) {
                        api.setUpdatedAt(new Date());
                        apiRepository.update(api);
                        apisUpdated.add(api.getId());
                    }
                } catch (Exception e) {
                    log.warn("Skipping API [{}] due to error: {}", api.getId(), e.getMessage(), e);
                }
            });
    }

    private boolean fixEndpointWeightsV2(JsonNode root) {
        boolean updated = false;

        for (JsonNode group : root.at("/proxy/groups")) {
            JsonNode endpoints = group.get("endpoints");
            if (endpoints == null || !endpoints.isArray()) continue;

            for (JsonNode endpoint : endpoints) {
                JsonNode weightNode = endpoint.get("weight");
                if (weightNode != null && weightNode.isInt() && weightNode.intValue() < 1) {
                    ((ObjectNode) endpoint).put("weight", 1);
                    updated = true;
                }
            }
        }
        return updated;
    }

    private boolean fixEndpointWeightsV4(Api v4Api) {
        boolean updated = false;
        if (v4Api.getEndpointGroups() == null) return false;

        for (var group : v4Api.getEndpointGroups()) {
            for (var ep : group.getEndpoints()) {
                int weight = ep.getWeight();
                if (weight < 1) {
                    ep.setWeight(1);
                    updated = true;
                }
            }
        }
        return updated;
    }

    private ApiCriteria.Builder getDefaultApiCriteriaBuilder() {
        return new ApiCriteria.Builder().definitionVersion(
            Arrays.asList(null, DefinitionVersion.V1, DefinitionVersion.V2, DefinitionVersion.V4)
        );
    }
}

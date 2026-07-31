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
package io.gravitee.apim.core.cluster.domain_service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.ApiFieldFilter;
import io.gravitee.apim.core.api.model.ApiSearchCriteria;
import io.gravitee.apim.core.api.query_service.ApiQueryService;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.nativeapi.NativeApi;
import io.gravitee.definition.model.v4.nativeapi.NativeEndpoint;
import io.gravitee.definition.model.v4.nativeapi.NativeEndpointGroup;
import java.util.List;
import java.util.Optional;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

/**
 * Resolves which Kafka Native APIs are bound to a Kafka Virtual Cluster through their
 * {@code native-kafka-virtual-cluster} endpoint ({@code configuration.virtualClusterCrossId}).
 *
 * <p>Backing service for the virtual-cluster lifecycle guards: a started API bound to a virtual
 * cluster must never be left pointing at a cluster that gets undeployed or emptied of its backends.
 */
@CustomLog
@DomainService
@RequiredArgsConstructor
public class VirtualClusterBoundApisQueryService {

    /** Endpoint plugin id a Kafka Native API uses to target a virtual cluster. */
    public static final String NATIVE_KAFKA_VIRTUAL_CLUSTER_ENDPOINT_TYPE = "native-kafka-virtual-cluster";
    private static final String VIRTUAL_CLUSTER_CROSS_ID_FIELD = "virtualClusterCrossId";

    private final ApiQueryService apiQueryService;
    private final ObjectMapper objectMapper;

    /**
     * Native APIs in the given environment that are STARTED and whose endpoint binds the given
     * virtual cluster crossId.
     *
     * <p>The STARTED filter is pushed into the search criteria to shrink the result set at the
     * store level, and re-applied in memory so the guard stays correct even if an underlying store
     * ignores running-state filtering.
     */
    public List<Api> findStartedBoundApis(String environmentId, String virtualClusterCrossId) {
        if (virtualClusterCrossId == null) {
            return List.of();
        }
        var criteria = ApiSearchCriteria.builder()
            .environmentId(environmentId)
            .definitionVersion(List.of(DefinitionVersion.V4))
            .state(Api.LifecycleState.STARTED)
            .build();
        return apiQueryService
            .search(criteria, null, ApiFieldFilter.builder().pictureExcluded(true).build())
            .filter(Api::isNative)
            .filter(api -> api.getLifecycleState() == Api.LifecycleState.STARTED)
            .filter(api ->
                extractVirtualClusterCrossId(api.getApiDefinitionNativeV4(), objectMapper).filter(virtualClusterCrossId::equals).isPresent()
            )
            .toList();
    }

    /**
     * The virtual cluster crossId a native API definition binds to, if any: read from the first
     * {@code native-kafka-virtual-cluster} endpoint's {@code virtualClusterCrossId} field. Never
     * throws — a null definition or malformed endpoint configuration yields {@link Optional#empty()}.
     */
    public static Optional<String> extractVirtualClusterCrossId(NativeApi definition, ObjectMapper objectMapper) {
        if (definition == null || definition.getEndpointGroups() == null) {
            return Optional.empty();
        }
        for (NativeEndpointGroup group : definition.getEndpointGroups()) {
            if (group == null || group.getEndpoints() == null) {
                continue;
            }
            for (NativeEndpoint endpoint : group.getEndpoints()) {
                if (endpoint == null || !NATIVE_KAFKA_VIRTUAL_CLUSTER_ENDPOINT_TYPE.equals(endpoint.getType())) {
                    continue;
                }
                Optional<String> crossId = readVirtualClusterCrossId(endpoint.getConfiguration(), objectMapper);
                if (crossId.isPresent()) {
                    return crossId;
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<String> readVirtualClusterCrossId(String configuration, ObjectMapper objectMapper) {
        if (configuration == null || configuration.isBlank()) {
            return Optional.empty();
        }
        try {
            JsonNode node = objectMapper.readTree(configuration);
            JsonNode value = node.get(VIRTUAL_CLUSTER_CROSS_ID_FIELD);
            if (value != null && !value.isNull() && !value.asText().isBlank()) {
                return Optional.of(value.asText());
            }
        } catch (JsonProcessingException e) {
            // Malformed endpoint configuration: treat as "not bound" rather than failing the caller,
            // but trace it — such an API becomes invisible to the lifecycle guards.
            log.warn("Unable to parse native-kafka-virtual-cluster endpoint configuration; treating API as not bound", e);
        }
        return Optional.empty();
    }
}

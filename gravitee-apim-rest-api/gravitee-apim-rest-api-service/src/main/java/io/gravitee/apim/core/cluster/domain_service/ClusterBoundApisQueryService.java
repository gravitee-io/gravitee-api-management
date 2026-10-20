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
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.nativeapi.NativeApi;
import io.gravitee.definition.model.v4.nativeapi.NativeEndpoint;
import io.gravitee.definition.model.v4.nativeapi.NativeEndpointGroup;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

/**
 * Resolves which v4 APIs use a cluster, through their endpoints:
 * <ul>
 *     <li>a Kafka Native API, through a {@code native-kafka-virtual-cluster} endpoint ({@code configuration.virtualClusterCrossId})
 *     or a {@code native-kafka-cluster} endpoint ({@code configuration.clusterCrossId} and {@code connectionCrossId});</li>
 *     <li>a Message API, through a {@code kafka} endpoint in {@code CLUSTER} mode (see {@link KafkaClusterBindings}).</li>
 * </ul>
 *
 * <p>Backing service for the cluster lifecycle guards: a started API must never be left pointing at a cluster, or a
 * connection, that gets undeployed or removed.
 */
@CustomLog
@DomainService
@RequiredArgsConstructor
public class ClusterBoundApisQueryService {

    /** Endpoint plugin id a Kafka Native API uses to target a virtual cluster. */
    public static final String NATIVE_KAFKA_VIRTUAL_CLUSTER_ENDPOINT_TYPE = "native-kafka-virtual-cluster";
    /** Endpoint plugin id a Kafka Native API uses to target a connection of a registered cluster. */
    public static final String NATIVE_KAFKA_CLUSTER_ENDPOINT_TYPE = "native-kafka-cluster";
    private static final String VIRTUAL_CLUSTER_CROSS_ID_FIELD = "virtualClusterCrossId";
    private static final String CLUSTER_CROSS_ID_FIELD = "clusterCrossId";
    private static final String CONNECTION_CROSS_ID_FIELD = "connectionCrossId";

    /**
     * A cluster an API uses.
     *
     * @param connectionCrossId the connection of a registered cluster the API uses; {@code null} for a virtual cluster,
     *     which has no connection, or when the endpoint does not name one
     */
    public record ClusterUse(String clusterCrossId, String connectionCrossId) {}

    private final ApiQueryService apiQueryService;
    private final ObjectMapper objectMapper;

    /**
     * The v4 APIs in the given environment that are STARTED and use a cluster matching {@code use}, sorted by name.
     *
     * <p>The STARTED filter and the API types that can use a cluster (Native and Message) are pushed into the search
     * criteria so the store does not load every API of the environment; STARTED is re-applied in memory so the guard
     * stays correct even if an underlying store ignores running-state filtering.
     */
    public List<Api> findStartedApisUsing(String environmentId, Predicate<ClusterUse> use) {
        var criteria = ApiSearchCriteria.builder()
            .environmentId(environmentId)
            .definitionVersion(List.of(DefinitionVersion.V4))
            .state(Api.LifecycleState.STARTED)
            .apiTypes(List.of(ApiType.NATIVE, ApiType.MESSAGE))
            .build();
        return apiQueryService
            .search(criteria, null, ApiFieldFilter.builder().pictureExcluded(true).build())
            .filter(api -> api.getLifecycleState() == Api.LifecycleState.STARTED)
            .filter(api -> clusterUses(api, objectMapper).stream().anyMatch(use))
            .sorted((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(String.valueOf(a.getName()), String.valueOf(b.getName())))
            .toList();
    }

    /** The clusters {@code api} uses. Never throws: an unreadable endpoint configuration uses no cluster. */
    public static List<ClusterUse> clusterUses(Api api, ObjectMapper objectMapper) {
        if (api.getType() == ApiType.NATIVE && api.getApiDefinitionValue() instanceof NativeApi definition) {
            return clusterUses(definition, objectMapper);
        }
        if (api.getType() == ApiType.MESSAGE && api.getApiDefinitionValue() instanceof io.gravitee.definition.model.v4.Api definition) {
            return KafkaClusterBindings.of(definition.getEndpointGroups(), objectMapper)
                .stream()
                .filter(binding -> binding.isCluster() && !binding.clusterCrossId().isEmpty())
                .map(binding -> new ClusterUse(binding.clusterCrossId(), binding.connectionCrossId()))
                .toList();
        }
        return List.of();
    }

    /** The clusters the endpoints of a Kafka Native API definition use. */
    public static List<ClusterUse> clusterUses(NativeApi definition, ObjectMapper objectMapper) {
        List<ClusterUse> uses = new ArrayList<>();
        if (definition == null || definition.getEndpointGroups() == null) {
            return uses;
        }
        for (NativeEndpointGroup group : definition.getEndpointGroups()) {
            if (group == null || group.getEndpoints() == null) {
                continue;
            }
            for (NativeEndpoint endpoint : group.getEndpoints()) {
                if (endpoint == null) {
                    continue;
                }
                if (NATIVE_KAFKA_VIRTUAL_CLUSTER_ENDPOINT_TYPE.equals(endpoint.getType())) {
                    read(endpoint.getConfiguration(), objectMapper)
                        .flatMap(node -> text(node, VIRTUAL_CLUSTER_CROSS_ID_FIELD))
                        .ifPresent(crossId -> uses.add(new ClusterUse(crossId, null)));
                } else if (NATIVE_KAFKA_CLUSTER_ENDPOINT_TYPE.equals(endpoint.getType())) {
                    read(endpoint.getConfiguration(), objectMapper).ifPresent(node ->
                        text(node, CLUSTER_CROSS_ID_FIELD).ifPresent(crossId ->
                            uses.add(new ClusterUse(crossId, text(node, CONNECTION_CROSS_ID_FIELD).orElse(null)))
                        )
                    );
                }
            }
        }
        return uses;
    }

    private static Optional<JsonNode> read(String configuration, ObjectMapper objectMapper) {
        if (configuration == null || configuration.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(objectMapper.readTree(configuration));
        } catch (JsonProcessingException e) {
            // Malformed endpoint configuration: treat as "not bound" rather than failing the caller,
            // but trace it — such an API becomes invisible to the lifecycle guards.
            log.warn("Unable to parse Kafka Native cluster endpoint configuration; treating API as not bound", e);
            return Optional.empty();
        }
    }

    private static Optional<String> text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || value.asText().isBlank()) {
            return Optional.empty();
        }
        return Optional.of(value.asText());
    }
}

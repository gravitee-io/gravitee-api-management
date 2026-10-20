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
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.cluster.model.ClusterLifecycleState;
import io.gravitee.apim.core.cluster.query_service.ClusterQueryService;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.nativeapi.NativeApi;
import io.gravitee.rest.api.service.exceptions.ApiNotDeployableException;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

/**
 * Guards start/deploy of an API bound to a cluster, which can only be started or deployed while that cluster is itself
 * deployed. Otherwise the API would sit at the gateway pointing at a cluster it cannot resolve:
 * <ul>
 *     <li>a Kafka Native API bound to a virtual cluster via a {@code native-kafka-virtual-cluster} endpoint
 *     ({@code configuration.virtualClusterCrossId}), or to a registered cluster via a {@code native-kafka-cluster}
 *     endpoint ({@code configuration.clusterCrossId});</li>
 *     <li>a Message API whose {@code kafka} endpoint group is in {@code CLUSTER} mode ({@code clusterCrossId}).</li>
 * </ul>
 */
@CustomLog
@DomainService
@RequiredArgsConstructor
public class ValidateApiClusterBindingService {

    private final ClusterQueryService clusterQueryService;
    private final ObjectMapper objectMapper;

    /**
     * Throws {@link ApiNotDeployableException} when {@code apiDefinition} is bound to a cluster that is not currently
     * deployed. No-op for other API types, APIs not bound to a cluster, or definitions that cannot be parsed.
     */
    public void validateDeployable(String apiId, String environmentId, ApiType apiType, String apiDefinition) {
        if (apiType == ApiType.MESSAGE && apiDefinition != null) {
            validateMessageApiDeployable(apiId, environmentId, apiDefinition);
            return;
        }
        if (apiType != ApiType.NATIVE || apiDefinition == null) {
            return;
        }
        NativeApi nativeDefinition;
        try {
            nativeDefinition = objectMapper.readValue(apiDefinition, NativeApi.class);
        } catch (JsonProcessingException e) {
            // Unparseable definition: leave existing start/deploy behaviour unchanged, but trace it —
            // a corrupted definition silently bypasses the cluster binding guard.
            log.warn("Unable to parse native API definition of API {}; skipping cluster binding validation", apiId, e);
            return;
        }
        for (ClusterBoundApisQueryService.ClusterUse use : ClusterBoundApisQueryService.clusterUses(nativeDefinition, objectMapper)) {
            if (!isDeployed(use.clusterCrossId(), environmentId)) {
                throw new ApiNotDeployableException(
                    "The API {" +
                        apiId +
                        "} cannot be deployed or started because its cluster {" +
                        use.clusterCrossId() +
                        "} is not deployed"
                );
            }
        }
    }

    private void validateMessageApiDeployable(String apiId, String environmentId, String apiDefinition) {
        Api definition;
        try {
            definition = objectMapper.readValue(apiDefinition, Api.class);
        } catch (JsonProcessingException e) {
            log.warn("Unable to parse message API definition of API {}; skipping Kafka cluster binding validation", apiId, e);
            return;
        }
        for (String crossId : KafkaClusterBindings.clusterCrossIds(definition.getEndpointGroups(), objectMapper)) {
            if (!isDeployed(crossId, environmentId)) {
                throw new ApiNotDeployableException(
                    "The API {" + apiId + "} cannot be deployed or started because its Kafka cluster {" + crossId + "} is not deployed"
                );
            }
        }
    }

    // PENDING means the cluster is deployed at the gateway with not-yet-redeployed edits
    // (a cluster only reaches PENDING from DEPLOYED), so it is a valid target.
    private boolean isDeployed(String crossId, String environmentId) {
        return clusterQueryService
            .findByCrossIdAndEnvironmentId(crossId, environmentId)
            .map(
                cluster ->
                    cluster.getLifecycleState() == ClusterLifecycleState.DEPLOYED ||
                    cluster.getLifecycleState() == ClusterLifecycleState.PENDING
            )
            .orElse(false);
    }
}

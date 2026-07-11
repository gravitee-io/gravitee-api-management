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
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.nativeapi.NativeApi;
import io.gravitee.rest.api.service.exceptions.ApiNotDeployableException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

/**
 * Guards start/deploy of a Kafka Native API: an API bound to a virtual cluster via a
 * {@code native-kafka-virtual-cluster} endpoint ({@code configuration.virtualClusterCrossId}) can
 * only be started or deployed while that virtual cluster is itself deployed. Otherwise the API would
 * sit at the gateway pointing at a cluster with no backends.
 */
@DomainService
@RequiredArgsConstructor
public class ValidateApiClusterBindingService {

    private final ClusterQueryService clusterQueryService;
    private final ObjectMapper objectMapper;

    /**
     * Throws {@link ApiNotDeployableException} when {@code apiDefinition} is a native API bound to a
     * virtual cluster that is not currently deployed. No-op for non-native APIs, APIs not bound to a
     * virtual cluster, or definitions that cannot be parsed.
     */
    public void validateDeployable(String apiId, String environmentId, ApiType apiType, String apiDefinition) {
        if (apiType != ApiType.NATIVE || apiDefinition == null) {
            return;
        }
        NativeApi nativeDefinition;
        try {
            nativeDefinition = objectMapper.readValue(apiDefinition, NativeApi.class);
        } catch (JsonProcessingException e) {
            // Unparseable definition: leave existing start/deploy behaviour unchanged.
            return;
        }
        Optional<String> virtualClusterCrossId = VirtualClusterBoundApisQueryService.extractVirtualClusterCrossId(
            nativeDefinition,
            objectMapper
        );
        if (virtualClusterCrossId.isEmpty()) {
            return;
        }
        String crossId = virtualClusterCrossId.get();
        boolean deployed = clusterQueryService
            .findByCrossIdAndEnvironmentId(crossId, environmentId)
            .map(cluster -> cluster.getLifecycleState() == ClusterLifecycleState.DEPLOYED)
            .orElse(false);
        if (!deployed) {
            throw new ApiNotDeployableException(
                "The API {" + apiId + "} cannot be deployed or started because its virtual cluster {" + crossId + "} is not deployed"
            );
        }
    }
}

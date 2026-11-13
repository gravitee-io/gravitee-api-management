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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.cluster.domain_service.ClusterBoundApisQueryService.ClusterUse;
import io.gravitee.apim.core.deployment.domain_service.ApiDeploymentValidator;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.endpointgroup.AbstractEndpointGroup;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.definition.model.v4.nativeapi.NativeApi;
import io.gravitee.rest.api.service.exceptions.ApiNotDeployableException;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import jakarta.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;

/**
 * Guards start and deploy of an API bound to a cluster, and the save of a Kafka Native API: the gateway must resolve that
 * cluster and the connection it uses ({@link ClusterResolutionDomainService}). Otherwise the API would sit at the gateway
 * pointing at a cluster it cannot resolve:
 * <ul>
 *     <li>a Kafka Native API bound to a virtual cluster via a {@code native-kafka-virtual-cluster} endpoint
 *     ({@code configuration.virtualClusterCrossId}), through each of its backends, or to a registered cluster via a
 *     {@code native-kafka-cluster} endpoint ({@code configuration.clusterCrossId} and {@code connectionCrossId});</li>
 *     <li>a Message API whose {@code kafka} endpoint group is in {@code CLUSTER} mode ({@code clusterCrossId}, and the
 *     {@code connectionCrossId} of each endpoint), whose connections must also be usable by Message APIs
 *     ({@link ValidateKafkaClusterBindingDomainService}).</li>
 * </ul>
 */
@DomainService
@RequiredArgsConstructor
public class ValidateApiClusterBindingService implements ApiDeploymentValidator {

    private final ClusterResolutionDomainService clusterResolutionDomainService;
    private final ValidateKafkaClusterBindingDomainService validateKafkaClusterBindingDomainService;
    private final ObjectMapper objectMapper;

    /**
     * Throws {@link ApiNotDeployableException} when the gateway cannot resolve a cluster or connection the definition
     * of {@code api} uses. No-op for other API types, APIs not bound to a cluster, or definitions that cannot be
     * parsed.
     */
    @Override
    public void validateDeployable(io.gravitee.apim.core.api.model.Api api) {
        var definition = api.getApiDefinitionValue();
        if (api.getType() == ApiType.MESSAGE && definition instanceof Api messageDefinition) {
            validateMessageApiStartable(api.getId(), api.getEnvironmentId(), messageDefinition.getEndpointGroups());
            return;
        }
        if (definition instanceof NativeApi nativeDefinition) {
            validateResolvable(
                api.getId(),
                api.getEnvironmentId(),
                ClusterBoundApisQueryService.clusterUses(nativeDefinition, objectMapper)
            );
        }
    }

    /**
     * Throws {@link ApiNotDeployableException} when an API of {@code apiType} whose endpoint groups are
     * {@code endpointGroups}, as an import declares them, uses a cluster or connection the gateway cannot resolve. Lets an
     * import refuse to start such an API before persisting anything.
     */
    public void validateStartable(
        String apiId,
        String environmentId,
        ApiType apiType,
        List<? extends AbstractEndpointGroup<?>> endpointGroups
    ) {
        if (apiType == ApiType.MESSAGE) {
            validateMessageApiStartable(apiId, environmentId, messageEndpointGroups(endpointGroups));
            return;
        }
        validateResolvable(apiId, environmentId, ClusterBoundApisQueryService.clusterUses(apiType, endpointGroups, objectMapper));
    }

    /**
     * Throws {@link InvalidDataException} when a Kafka Native API whose endpoint groups are {@code endpointGroups} uses a
     * cluster or connection the gateway cannot resolve and {@code previousEndpointGroups} did not already use: an API keeps
     * a binding its cluster no longer resolves, and stays editable, until it is started. Message APIs are checked on save
     * by {@link ValidateKafkaClusterBindingDomainService}.
     *
     * @param previousEndpointGroups the endpoint groups of the saved API, {@code null} on create
     */
    public void validateSavable(
        String environmentId,
        ApiType apiType,
        @Nullable List<? extends AbstractEndpointGroup<?>> previousEndpointGroups,
        List<? extends AbstractEndpointGroup<?>> endpointGroups
    ) {
        if (apiType != ApiType.NATIVE) {
            return;
        }
        var kept = previousEndpointGroups == null
            ? new HashSet<ClusterUse>()
            : new HashSet<>(ClusterBoundApisQueryService.clusterUses(apiType, previousEndpointGroups, objectMapper));
        var added = ClusterBoundApisQueryService.clusterUses(apiType, endpointGroups, objectMapper)
            .stream()
            .filter(use -> !kept.contains(use))
            .toList();
        clusterResolutionDomainService
            .unresolvedReason(environmentId, added)
            .ifPresent(reason -> {
                throw new InvalidDataException("The API cannot be saved because " + reason + ".");
            });
    }

    private void validateMessageApiStartable(String apiId, String environmentId, List<EndpointGroup> endpointGroups) {
        // Unlike ClusterBoundApisQueryService.messageClusterUses, a CLUSTER group naming no cluster is kept, and refused.
        List<ClusterUse> uses = KafkaClusterBindings.of(endpointGroups, objectMapper)
            .stream()
            .filter(KafkaClusterBindings.Binding::isCluster)
            .map(binding -> new ClusterUse(binding.clusterCrossId(), binding.connectionCrossId()))
            .toList();
        validateResolvable(apiId, environmentId, uses);
        // Saving it only checked the bindings it changed: the connections it kept may have become unusable since.
        try {
            validateKafkaClusterBindingDomainService.validate(environmentId, endpointGroups);
        } catch (InvalidDataException e) {
            throw new ApiNotDeployableException("The API {" + apiId + "} cannot be deployed or started: " + e.getMessage());
        }
    }

    private static List<EndpointGroup> messageEndpointGroups(List<? extends AbstractEndpointGroup<?>> endpointGroups) {
        return endpointGroups == null
            ? null
            : endpointGroups.stream().filter(EndpointGroup.class::isInstance).map(EndpointGroup.class::cast).toList();
    }

    private void validateResolvable(String apiId, String environmentId, List<ClusterUse> uses) {
        clusterResolutionDomainService
            .unresolvedReason(environmentId, uses)
            .ifPresent(reason -> {
                throw new ApiNotDeployableException("The API {" + apiId + "} cannot be deployed or started because " + reason);
            });
    }
}

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
import io.gravitee.apim.core.cluster.domain_service.KafkaClusterBindings.Binding;
import io.gravitee.apim.core.cluster.domain_service.KafkaClusterBindings.DefaultConnection;
import io.gravitee.apim.core.cluster.model.Cluster;
import io.gravitee.apim.core.cluster.model.KafkaClusterConnection;
import io.gravitee.definition.model.cluster.ClusterType;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import jakarta.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;

/**
 * Validates the bindings of Message API {@code kafka} endpoints to registered Kafka clusters, on API save and on start or
 * deploy.
 *
 * <p>Each endpoint is checked against the connection it actually uses (its group's, or its own override when it does not
 * inherit), and each default connection a shared configuration names against its cluster: the cluster is deployed in the
 * API's environment and is a {@code KAFKA_CLUSTER}, the connection exists in it, and Message APIs can use its security
 * ({@link KafkaClusterConnection#messageApiUnsupportedReason()}). Checked against the deployed cluster, which is what the
 * gateways run, not the one being edited. The plugin schema already checks the shape (mode, required fields, no inline
 * security).
 */
@DomainService
@RequiredArgsConstructor
public class ValidateKafkaClusterBindingDomainService {

    private final DeployedClustersDomainService deployedClustersDomainService;
    private final ObjectMapper objectMapper;

    /** Validates every binding of {@code endpointGroups}. */
    public void validate(String environmentId, List<EndpointGroup> endpointGroups) {
        validate(environmentId, null, endpointGroups);
    }

    /**
     * Validates the bindings of {@code endpointGroups} to a cluster connection {@code previousEndpointGroups} did not
     * already use: an API keeps a binding its cluster no longer resolves, and stays editable, until it is started.
     *
     * @param previousEndpointGroups the endpoint groups of the saved API, {@code null} on create
     */
    public void validate(String environmentId, @Nullable List<EndpointGroup> previousEndpointGroups, List<EndpointGroup> endpointGroups) {
        Set<ConnectionUse> kept = previousEndpointGroups == null ? Set.of() : connectionUses(previousEndpointGroups);
        Map<String, Optional<Cluster>> clusters = new HashMap<>();
        for (Binding binding : KafkaClusterBindings.of(endpointGroups, objectMapper)) {
            if (!binding.isCluster() || kept.contains(new ConnectionUse(binding.clusterCrossId(), binding.connectionCrossId()))) {
                continue;
            }
            if (binding.connectionCrossId() == null) {
                throw new InvalidDataException(
                    binding.subject() +
                        " must set the connectionCrossId of a connection of Kafka cluster [" +
                        binding.clusterCrossId() +
                        "]."
                );
            }
            validateConnection(binding.subject(), binding.clusterCrossId(), binding.connectionCrossId(), environmentId, clusters);
        }
        for (DefaultConnection defaultConnection : KafkaClusterBindings.defaultConnections(endpointGroups, objectMapper)) {
            if (kept.contains(new ConnectionUse(defaultConnection.clusterCrossId(), defaultConnection.connectionCrossId()))) {
                continue;
            }
            validateConnection(
                defaultConnection.subject() + ", default connection",
                defaultConnection.clusterCrossId(),
                defaultConnection.connectionCrossId(),
                environmentId,
                clusters
            );
        }
    }

    private Set<ConnectionUse> connectionUses(List<EndpointGroup> endpointGroups) {
        Set<ConnectionUse> uses = new HashSet<>();
        KafkaClusterBindings.of(endpointGroups, objectMapper)
            .stream()
            .filter(Binding::isCluster)
            .forEach(binding -> uses.add(new ConnectionUse(binding.clusterCrossId(), binding.connectionCrossId())));
        KafkaClusterBindings.defaultConnections(endpointGroups, objectMapper).forEach(defaultConnection ->
            uses.add(new ConnectionUse(defaultConnection.clusterCrossId(), defaultConnection.connectionCrossId()))
        );
        return uses;
    }

    private record ConnectionUse(String clusterCrossId, String connectionCrossId) {}

    private void validateConnection(
        String subject,
        String clusterCrossId,
        String connectionCrossId,
        String environmentId,
        Map<String, Optional<Cluster>> clusters
    ) {
        Cluster cluster = clusters
            .computeIfAbsent(clusterCrossId, crossId -> deployedClustersDomainService.findByCrossId(environmentId, crossId))
            .orElseThrow(() ->
                new InvalidDataException(
                    subject +
                        ": Kafka cluster [" +
                        clusterCrossId +
                        "] is not deployed in this environment. Deploy it or pick a deployed cluster."
                )
            );
        if (cluster.getType() != ClusterType.KAFKA_CLUSTER) {
            throw new InvalidDataException(
                subject + ": cluster [" + clusterCrossId + "] is not a registered Kafka cluster (KAFKA_CLUSTER)."
            );
        }
        var configuration = cluster.getKafkaClusterConfiguration(objectMapper);
        KafkaClusterConnection connection = (configuration == null || configuration.connections() == null)
            ? null
            : configuration
                .connections()
                .stream()
                .filter(c -> connectionCrossId.equals(c.crossId()))
                .findFirst()
                .orElse(null);
        if (connection == null) {
            throw new InvalidDataException(
                subject +
                    ": connection [" +
                    connectionCrossId +
                    "] does not exist in the deployed Kafka cluster [" +
                    clusterCrossId +
                    "]. Pick another connection."
            );
        }
        String unsupportedReason = connection.messageApiUnsupportedReason();
        if (unsupportedReason != null) {
            throw new InvalidDataException(
                subject +
                    ": connection [" +
                    connectionCrossId +
                    "] of Kafka cluster [" +
                    clusterCrossId +
                    "] cannot be used by Message APIs. " +
                    unsupportedReason +
                    " Pick another connection or switch the group to Direct mode."
            );
        }
    }
}

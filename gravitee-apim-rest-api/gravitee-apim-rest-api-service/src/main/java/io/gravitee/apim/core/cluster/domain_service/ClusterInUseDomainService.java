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
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.cluster.domain_service.ClusterBoundApisQueryService.ClusterUse;
import io.gravitee.apim.core.cluster.model.Cluster;
import io.gravitee.apim.core.cluster.model.KafkaClusterConfiguration;
import io.gravitee.apim.core.cluster.model.KafkaClusterConnection;
import io.gravitee.apim.core.cluster.model.KafkaVirtualClusterConfiguration;
import io.gravitee.definition.model.cluster.ClusterType;
import io.gravitee.rest.api.service.exceptions.ClusterInUseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

/**
 * Refuses cluster changes that would break started APIs: undeploying or deleting a cluster they use, or removing a
 * connection they use. Stopped APIs never block.
 *
 * <p>A Kafka Native API may use a registered cluster through a virtual cluster: the virtual clusters deployed at the
 * gateway with a backend on that cluster (or connection) count as using it, through their started APIs.
 */
@DomainService
@RequiredArgsConstructor
public class ClusterInUseDomainService {

    /** Names at most this many APIs in an error message: {@link ClusterInUseException#getParameters()} lists them all. */
    private static final int NAMED_APIS = 3;

    private final ClusterBoundApisQueryService clusterBoundApisQueryService;
    private final DeployedClustersDomainService deployedClustersDomainService;
    private final ObjectMapper objectMapper;

    /** The started APIs using {@code cluster}, sorted by name: those that block undeploying or deleting it. */
    public List<Api> startedApisUsing(Cluster cluster, String environmentId) {
        if (cluster.getCrossId() == null) {
            return List.of();
        }
        Set<String> virtualClusters = virtualClustersWithBackendOn(environmentId, cluster, backend -> true);
        return clusterBoundApisQueryService.findStartedApisUsing(
            environmentId,
            use -> cluster.getCrossId().equals(use.clusterCrossId()) || virtualClusters.contains(use.clusterCrossId())
        );
    }

    /**
     * The started APIs using one of {@code connectionCrossIds}, connections of {@code cluster}, sorted by name: those that
     * block removing them.
     */
    public List<Api> startedApisUsingConnections(Cluster cluster, Set<String> connectionCrossIds, String environmentId) {
        if (cluster.getCrossId() == null || connectionCrossIds.isEmpty() || cluster.getType() != ClusterType.KAFKA_CLUSTER) {
            return List.of();
        }
        Set<String> virtualClusters = virtualClustersWithBackendOn(environmentId, cluster, connectionCrossIds::contains);
        // An API naming the cluster but no connection does not block: the gateway cannot connect it to any connection.
        Predicate<ClusterUse> usesConnectionDirectly = use ->
            cluster.getCrossId().equals(use.clusterCrossId()) && connectionCrossIds.contains(use.connectionCrossId());
        Predicate<ClusterUse> usesItThroughVirtualCluster = use -> virtualClusters.contains(use.clusterCrossId());
        return clusterBoundApisQueryService.findStartedApisUsing(environmentId, usesConnectionDirectly.or(usesItThroughVirtualCluster));
    }

    /**
     * @param operation how the refused change is named in the error message, e.g. {@code "undeploy"}
     * @throws ClusterInUseException when a started API uses {@code cluster}
     */
    public void checkNotUsed(Cluster cluster, String environmentId, String operation) {
        List<Api> apis = startedApisUsing(cluster, environmentId);
        if (!apis.isEmpty()) {
            throw inUse("Cannot " + operation + " cluster '" + cluster.getName() + "': " + startedApis(apis) + " it.", apis);
        }
    }

    /**
     * @param before connections of {@code cluster}, a registered cluster, before the change
     * @param after connections of {@code cluster} after the change; a connection of {@code before} whose cross id is
     *     missing here is removed
     * @throws ClusterInUseException when a started API uses a removed connection
     */
    public void checkRemovedConnectionsNotUsed(
        Cluster cluster,
        KafkaClusterConfiguration before,
        KafkaClusterConfiguration after,
        String environmentId,
        String operation
    ) {
        List<KafkaClusterConnection> removedConnections = removedConnections(before, after);
        Set<String> removedCrossIds = removedConnections.stream().map(KafkaClusterConnection::crossId).collect(Collectors.toSet());
        List<Api> apis = startedApisUsingConnections(cluster, removedCrossIds, environmentId);
        if (!apis.isEmpty()) {
            String names = removedConnections
                .stream()
                .map(connection -> "'" + connection.name() + "'")
                .collect(Collectors.joining(", "));
            throw inUse(
                "Cannot " +
                    operation +
                    " cluster '" +
                    cluster.getName() +
                    "': it removes " +
                    (removedConnections.size() == 1 ? "connection " : "connections ") +
                    names +
                    ", and " +
                    startedApis(apis) +
                    (removedConnections.size() == 1 ? " it." : " them."),
                apis
            );
        }
    }

    private static List<KafkaClusterConnection> removedConnections(KafkaClusterConfiguration before, KafkaClusterConfiguration after) {
        if (before == null || before.connections() == null) {
            return List.of();
        }
        Set<String> kept = after == null || after.connections() == null
            ? Set.of()
            : after.connections().stream().filter(Objects::nonNull).map(KafkaClusterConnection::crossId).collect(Collectors.toSet());
        return before
            .connections()
            .stream()
            .filter(connection -> connection != null && connection.crossId() != null && !kept.contains(connection.crossId()))
            .toList();
    }

    /** Cross ids of the virtual clusters deployed at the gateway with a backend on a matching connection of {@code cluster}. */
    private Set<String> virtualClustersWithBackendOn(String environmentId, Cluster cluster, Predicate<String> connectionCrossId) {
        if (cluster.getType() != ClusterType.KAFKA_CLUSTER) {
            return Set.of();
        }
        return deployedClustersDomainService
            .findAll(environmentId)
            .stream()
            .filter(deployed -> deployed.getType() == ClusterType.KAFKA_VIRTUAL_CLUSTER)
            .filter(deployed -> {
                KafkaVirtualClusterConfiguration configuration = deployed.getKafkaVirtualClusterConfiguration(objectMapper);
                return (
                    configuration != null &&
                    configuration.backends() != null &&
                    configuration
                        .backends()
                        .stream()
                        .filter(Objects::nonNull)
                        .anyMatch(
                            backend ->
                                cluster.getCrossId().equals(backend.clusterCrossId()) && connectionCrossId.test(backend.connectionCrossId())
                        )
                );
            })
            .map(Cluster::getCrossId)
            .collect(Collectors.toSet());
    }

    /** {@code "5 started APIs (Audit, Orders, Payments and 2 more) use"}: the subject and verb of the error message. */
    private static String startedApis(List<Api> apis) {
        String names = apis.stream().limit(NAMED_APIS).map(Api::getName).collect(Collectors.joining(", "));
        if (apis.size() > NAMED_APIS) {
            names += " and " + (apis.size() - NAMED_APIS) + " more";
        }
        return apis.size() + (apis.size() == 1 ? " started API" : " started APIs") + " (" + names + (apis.size() == 1 ? ") uses" : ") use");
    }

    private static ClusterInUseException inUse(String message, List<Api> apis) {
        Map<String, String> apiNamesById = new LinkedHashMap<>();
        apis.forEach(api -> apiNamesById.put(api.getId(), api.getName()));
        return new ClusterInUseException(message + (apis.size() == 1 ? " Stop it first." : " Stop them first."), apiNamesById);
    }
}

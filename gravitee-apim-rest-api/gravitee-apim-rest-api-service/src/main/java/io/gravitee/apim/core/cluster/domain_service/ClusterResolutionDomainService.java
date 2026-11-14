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
import io.gravitee.apim.core.cluster.model.Cluster;
import io.gravitee.apim.core.cluster.model.ClusterLifecycleState;
import io.gravitee.apim.core.cluster.model.KafkaClusterConfiguration;
import io.gravitee.apim.core.cluster.model.KafkaVirtualClusterBackend;
import io.gravitee.apim.core.cluster.model.KafkaVirtualClusterConfiguration;
import io.gravitee.apim.core.cluster.query_service.ClusterQueryService;
import io.gravitee.definition.model.cluster.ClusterType;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

/**
 * Whether the gateway can resolve the clusters and connections an API or a virtual cluster uses. A cluster resolves when
 * it is deployed (DEPLOYED, or PENDING with edits not yet redeployed), is of the type its endpoint expects, and a
 * connection when the deployed version of its cluster, the one the gateway runs, has it. A virtual cluster resolves when
 * each of its backends, a registered cluster, does.
 *
 * <p>A use or a backend naming no connection is only checked for its cluster.
 */
@DomainService
@RequiredArgsConstructor
public class ClusterResolutionDomainService {

    private final ClusterQueryService clusterQueryService;
    private final DeployedClustersDomainService deployedClustersDomainService;
    private final ObjectMapper objectMapper;

    /** Why the gateway cannot resolve {@code uses}, worded to follow "because", or empty when it can. */
    public Optional<String> unresolvedReason(String environmentId, Collection<ClusterUse> uses) {
        if (uses.isEmpty()) {
            return Optional.empty();
        }
        var clusters = new DeployedClusters(environmentId);
        return uses.stream().map(clusters::unresolvedReason).flatMap(Optional::stream).findFirst();
    }

    /** Why the gateway cannot resolve one of {@code backends} of a virtual cluster, worded to follow "because it has", or empty. */
    public Optional<String> unresolvedBackendReason(String environmentId, Collection<KafkaVirtualClusterBackend> backends) {
        if (backends.isEmpty()) {
            return Optional.empty();
        }
        var clusters = new DeployedClusters(environmentId);
        return backends.stream().filter(Objects::nonNull).map(clusters::unresolvedReason).flatMap(Optional::stream).findFirst();
    }

    /** The clusters of an environment, the deployed versions read once. */
    private final class DeployedClusters {

        private final String environmentId;
        private Map<String, Cluster> deployedVersionsById;

        DeployedClusters(String environmentId) {
            this.environmentId = environmentId;
        }

        Optional<String> unresolvedReason(ClusterUse use) {
            String crossId = use.clusterCrossId();
            if (isBlank(crossId)) {
                return Optional.of("an endpoint names no cluster");
            }
            Lookup lookup = lookup(crossId);
            if (lookup.deployed() == null) {
                return Optional.of("its cluster {" + crossId + "} " + lookup.missingReason());
            }
            Cluster cluster = lookup.deployed();
            if (cluster.getType() != use.clusterType()) {
                return Optional.of("its cluster {" + crossId + "} is not " + describe(use.clusterType()));
            }
            if (cluster.getType() == ClusterType.KAFKA_VIRTUAL_CLUSTER) {
                KafkaVirtualClusterConfiguration configuration = cluster.getKafkaVirtualClusterConfiguration(objectMapper);
                if (configuration == null || configuration.backends() == null || configuration.backends().isEmpty()) {
                    return Optional.of("its virtual cluster {" + crossId + "} has no backend");
                }
                return configuration
                    .backends()
                    .stream()
                    .filter(Objects::nonNull)
                    .map(this::unresolvedReason)
                    .flatMap(Optional::stream)
                    .findFirst()
                    .map(reason -> "its virtual cluster {" + crossId + "} has " + reason);
            }
            if (!isBlank(use.connectionCrossId()) && !hasConnection(cluster, use.connectionCrossId())) {
                return Optional.of(
                    "its connection {" + use.connectionCrossId() + "} does not exist in the deployed cluster {" + crossId + "}"
                );
            }
            return Optional.empty();
        }

        Optional<String> unresolvedReason(KafkaVirtualClusterBackend backend) {
            String crossId = backend.clusterCrossId();
            if (isBlank(crossId)) {
                return Optional.of("a backend naming no cluster");
            }
            Lookup lookup = lookup(crossId);
            if (lookup.deployed() == null) {
                return Optional.of("a backend on cluster {" + crossId + "}, which " + lookup.missingReason());
            }
            if (lookup.deployed().getType() != ClusterType.KAFKA_CLUSTER) {
                return Optional.of("a backend on cluster {" + crossId + "}, which is not " + describe(ClusterType.KAFKA_CLUSTER));
            }
            if (!isBlank(backend.connectionCrossId()) && !hasConnection(lookup.deployed(), backend.connectionCrossId())) {
                return Optional.of(
                    "a backend on connection {" +
                        backend.connectionCrossId() +
                        "}, which does not exist in the deployed cluster {" +
                        crossId +
                        "}"
                );
            }
            return Optional.empty();
        }

        private Lookup lookup(String crossId) {
            Optional<Cluster> stored = clusterQueryService.findByCrossIdAndEnvironmentId(crossId, environmentId);
            if (
                stored.isEmpty() ||
                (stored.get().getLifecycleState() != ClusterLifecycleState.DEPLOYED &&
                    stored.get().getLifecycleState() != ClusterLifecycleState.PENDING)
            ) {
                return new Lookup(null, "is not deployed");
            }
            Cluster deployed = deployedVersionsById().get(stored.get().getId());
            // Deployed without a deployment event: the gateway does not have it either.
            return deployed == null ? new Lookup(null, "has no deployed version: redeploy it") : new Lookup(deployed, null);
        }

        private Map<String, Cluster> deployedVersionsById() {
            if (deployedVersionsById == null) {
                deployedVersionsById = deployedClustersDomainService
                    .findAll(environmentId)
                    .stream()
                    .filter(cluster -> cluster.getId() != null)
                    .collect(Collectors.toMap(Cluster::getId, Function.identity(), (first, second) -> first));
            }
            return deployedVersionsById;
        }

        private boolean hasConnection(Cluster cluster, String connectionCrossId) {
            KafkaClusterConfiguration configuration = cluster.getKafkaClusterConfiguration(objectMapper);
            return (
                configuration != null &&
                configuration.connections() != null &&
                configuration
                    .connections()
                    .stream()
                    .anyMatch(c -> c != null && connectionCrossId.equals(c.crossId()))
            );
        }
    }

    /**
     * @param deployed the version the gateway runs, {@code null} when it has none
     * @param missingReason why there is none, worded to follow the cluster
     */
    private record Lookup(Cluster deployed, String missingReason) {}

    private static String describe(ClusterType type) {
        return type == ClusterType.KAFKA_VIRTUAL_CLUSTER ? "a virtual cluster" : "a registered Kafka cluster";
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

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
package io.gravitee.gateway.handlers.cluster.manager.impl;

import io.gravitee.common.event.EventManager;
import io.gravitee.definition.model.cluster.ClusterEvent;
import io.gravitee.definition.model.cluster.ReactableCluster;
import io.gravitee.gateway.handlers.cluster.manager.ClusterManager;
import io.gravitee.secrets.api.discovery.Definition;
import io.gravitee.secrets.api.discovery.DefinitionMetadata;
import io.gravitee.secrets.api.event.SecretDiscoveryEvent;
import io.gravitee.secrets.api.event.SecretDiscoveryEventType;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import lombok.CustomLog;

/**
 * Holds the clusters deployed on this gateway, and their secret references as it does for APIs and shared policy groups:
 * discovered when a cluster version is deployed, revoked when it is superseded or undeployed. When a secret value
 * changes, the cluster is published again as an update, so that clients built from it are renewed.
 */
@CustomLog
public class ClusterManagerImpl implements ClusterManager {

    private final Map<String, ReactableCluster> clusters = new ConcurrentHashMap<>();
    private final EventManager eventManager;

    public static final String CLUSTER_DEFINITION_KIND = "cluster";

    public ClusterManagerImpl(EventManager eventManager) {
        this.eventManager = eventManager;

        eventManager.subscribeForEvents(
            event -> {
                if (
                    !(event.content() instanceof SecretDiscoveryEvent secretDiscoveryEvent) ||
                    !(secretDiscoveryEvent.definition() instanceof Definition definition) ||
                    !CLUSTER_DEFINITION_KIND.equals(definition.kind())
                ) {
                    return;
                }
                ReactableCluster cluster = clusters.get(definition.id());
                if (cluster == null) {
                    log.trace("Received SecretDiscoveryEvent for cluster {}, but not found in manager", definition.id());
                    return;
                }
                log.info("Secret value changed for cluster {}, updating it", definition.id());
                eventManager.publishEvent(ClusterEvent.UPDATE, cluster);
            },
            SecretDiscoveryEventType.VALUE_CHANGED
        );
    }

    @Override
    public boolean register(ReactableCluster cluster) {
        ReactableCluster deployedCluster = get(cluster.getId());

        boolean clusterToDeploy = deployedCluster == null;
        boolean clusterToUpdate = !clusterToDeploy && isNewer(deployedCluster, cluster);

        if (clusterToDeploy) {
            deploy(cluster);
            return true;
        }

        if (clusterToUpdate) {
            update(cluster);
            return true;
        }

        return false;
    }

    /**
     * Whether {@code candidate} supersedes the currently-deployed {@code current}. The monotonic
     * {@code version} (bumped on every redeploy) is the authoritative discriminator: {@code deployedAt}
     * is only second-resolution in the event payload, so two redeploys within the same wall-clock second
     * carry an equal {@code deployedAt} and a {@code deployedAt}-only comparison would silently drop the
     * second update. A versioned side always supersedes an unversioned one (version was introduced
     * later, so its presence implies a newer producer). Falls back to {@code deployedAt} only when
     * both versions are absent (older events).
     */
    private static boolean isNewer(ReactableCluster current, ReactableCluster candidate) {
        if (candidate.getVersion() != null && current.getVersion() != null) {
            return candidate.getVersion() > current.getVersion();
        }
        if (candidate.getVersion() != null) {
            return true;
        }
        if (current.getVersion() != null) {
            return false;
        }
        // Fallback for events where both versions are absent: compare deployedAt.
        if (candidate.getDeployedAt() == null) {
            return false;
        }
        if (current.getDeployedAt() == null) {
            return true;
        }
        return current.getDeployedAt().before(candidate.getDeployedAt());
    }

    @Override
    public void unregister(String clusterId) {
        ReactableCluster cluster = clusters.remove(clusterId);
        if (cluster != null) {
            eventManager.publishEvent(ClusterEvent.UNDEPLOY, cluster);
            publishSecretDiscoveryEvent(SecretDiscoveryEventType.REVOKE, cluster);
            log.info("Cluster [{}] has been undeployed", clusterId);
        }
    }

    @Override
    public ReactableCluster get(String clusterId) {
        return clusters.get(clusterId);
    }

    @Override
    public ReactableCluster get(String crossId, String environmentId) {
        if (crossId == null || environmentId == null) {
            return null;
        }
        return clusters
            .values()
            .stream()
            .filter(cluster -> crossId.equals(cluster.getCrossId()) && environmentId.equals(cluster.getEnvironmentId()))
            .findFirst()
            .orElse(null);
    }

    @Override
    public Collection<ReactableCluster> clusters() {
        return clusters.values();
    }

    private void deploy(ReactableCluster cluster) {
        log.debug("Deployment of cluster {}", cluster.getName());
        publishSecretDiscoveryEvent(SecretDiscoveryEventType.DISCOVER, cluster);
        clusters.put(cluster.getId(), cluster);
        eventManager.publishEvent(ClusterEvent.DEPLOY, cluster);
        log.info("Cluster [{}] has been deployed", cluster.getId());
    }

    private void update(ReactableCluster cluster) {
        log.debug("Updating cluster {}", cluster.getName());
        publishSecretDiscoveryEvent(SecretDiscoveryEventType.DISCOVER, cluster);
        ReactableCluster previousCluster = clusters.put(cluster.getId(), cluster);
        eventManager.publishEvent(ClusterEvent.UPDATE, cluster);
        // Versionless clusters share one revision: revoking the previous one would revoke what was just discovered.
        if (previousCluster != null && !Objects.equals(revisionOf(previousCluster), revisionOf(cluster))) {
            publishSecretDiscoveryEvent(SecretDiscoveryEventType.REVOKE, previousCluster);
        }
        log.info("Cluster [{}] has been updated", cluster.getId());
    }

    private void publishSecretDiscoveryEvent(SecretDiscoveryEventType type, ReactableCluster cluster) {
        eventManager.publishEvent(
            type,
            new SecretDiscoveryEvent(cluster.getEnvironmentId(), cluster, new DefinitionMetadata(revisionOf(cluster)))
        );
    }

    private static String revisionOf(ReactableCluster cluster) {
        return cluster.getVersion() == null ? null : String.valueOf(cluster.getVersion());
    }
}

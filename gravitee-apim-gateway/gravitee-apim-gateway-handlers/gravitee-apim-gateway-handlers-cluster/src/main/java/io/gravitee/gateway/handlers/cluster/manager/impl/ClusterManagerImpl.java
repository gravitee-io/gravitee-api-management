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
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.CustomLog;

@CustomLog
public class ClusterManagerImpl implements ClusterManager {

    private final Map<String, ReactableCluster> clusters = new ConcurrentHashMap<>();
    private final EventManager eventManager;

    public ClusterManagerImpl(EventManager eventManager) {
        this.eventManager = eventManager;
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
     * second update. Falls back to {@code deployedAt} when versions are absent (older events).
     */
    private static boolean isNewer(ReactableCluster current, ReactableCluster candidate) {
        if (candidate.getVersion() != null && current.getVersion() != null) {
            return candidate.getVersion() > current.getVersion();
        }
        return (
            candidate.getDeployedAt() != null &&
            (current.getDeployedAt() == null || current.getDeployedAt().before(candidate.getDeployedAt()))
        );
    }

    @Override
    public void unregister(String clusterId) {
        ReactableCluster cluster = clusters.remove(clusterId);
        if (cluster != null) {
            eventManager.publishEvent(ClusterEvent.UNDEPLOY, cluster);
            log.info("Cluster [{}] has been undeployed", clusterId);
        }
    }

    @Override
    public ReactableCluster get(String clusterId) {
        return clusters.get(clusterId);
    }

    @Override
    public Collection<ReactableCluster> clusters() {
        return clusters.values();
    }

    private void deploy(ReactableCluster cluster) {
        log.debug("Deployment of cluster {}", cluster.getName());
        clusters.put(cluster.getId(), cluster);
        eventManager.publishEvent(ClusterEvent.DEPLOY, cluster);
        log.info("Cluster [{}] has been deployed", cluster.getId());
    }

    private void update(ReactableCluster cluster) {
        log.debug("Updating cluster {}", cluster.getName());
        clusters.put(cluster.getId(), cluster);
        eventManager.publishEvent(ClusterEvent.UPDATE, cluster);
        log.info("Cluster [{}] has been updated", cluster.getId());
    }
}

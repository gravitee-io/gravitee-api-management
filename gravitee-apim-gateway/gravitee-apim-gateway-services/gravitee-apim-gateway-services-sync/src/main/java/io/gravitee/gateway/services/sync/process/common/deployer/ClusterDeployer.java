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
package io.gravitee.gateway.services.sync.process.common.deployer;

import io.gravitee.definition.model.cluster.ReactableCluster;
import io.gravitee.gateway.handlers.cluster.manager.ClusterManager;
import io.gravitee.gateway.services.sync.process.common.model.SyncException;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.cluster.ClusterReactorDeployable;
import io.reactivex.rxjava3.core.Completable;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@CustomLog
public class ClusterDeployer implements Deployer<ClusterReactorDeployable> {

    private final ClusterManager clusterManager;

    @Override
    public Completable deploy(ClusterReactorDeployable deployable) {
        return Completable.fromRunnable(() -> {
            try {
                clusterManager.register(deployable.reactableCluster());
                log.debug("Cluster [{}] deployed", deployable.clusterId());
            } catch (Exception e) {
                throw new SyncException("An error occurred when trying to deploy cluster [" + deployable.clusterId() + "].", e);
            }
        });
    }

    @Override
    public Completable doAfterDeployment(ClusterReactorDeployable deployable) {
        return Completable.complete();
    }

    @Override
    public Completable undeploy(ClusterReactorDeployable deployable) {
        return Completable.fromRunnable(() -> {
            try {
                String clusterId = deployedClusterId(deployable);
                clusterManager.unregister(clusterId);
                log.debug("Cluster [{}] (cross id [{}]) undeployed", clusterId, deployable.clusterId());
            } catch (Exception e) {
                throw new SyncException("An error occurred when trying to undeploy cluster [" + deployable.clusterId() + "].", e);
            }
        });
    }

    /**
     * The management API publishes undeploy events with the cluster <em>cross id</em>, while the cluster manager is keyed
     * by cluster id: resolve the deployed cluster by cross id in the event's environment. When the event names no single
     * environment, or the cluster is not deployed on this gateway, the value is used as is and unregistering is a no-op.
     */
    private String deployedClusterId(ClusterReactorDeployable deployable) {
        if (deployable.environmentId() != null) {
            ReactableCluster deployed = clusterManager.get(deployable.clusterId(), deployable.environmentId());
            if (deployed != null) {
                return deployed.getId();
            }
        }
        return deployable.clusterId();
    }

    @Override
    public Completable doAfterUndeployment(ClusterReactorDeployable deployable) {
        return Completable.complete();
    }
}

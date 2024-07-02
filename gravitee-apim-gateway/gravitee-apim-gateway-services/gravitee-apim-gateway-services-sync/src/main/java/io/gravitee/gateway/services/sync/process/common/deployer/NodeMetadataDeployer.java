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

import static io.gravitee.node.api.Node.META_INSTALLATION;
import static io.gravitee.node.api.Node.META_ORGANIZATIONS;

import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.services.sync.process.common.model.SyncException;
import io.gravitee.gateway.services.sync.process.distributed.service.DistributedSyncService;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.node.NodeMetadataDeployable;
import io.gravitee.node.api.Node;
import io.reactivex.rxjava3.core.Completable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
@Slf4j
public class NodeMetadataDeployer implements Deployer<NodeMetadataDeployable> {

    private final Node node;
    private final GatewayConfiguration gatewayConfiguration;
    private final DistributedSyncService distributedSyncService;

    @Override
    public Completable deploy(final NodeMetadataDeployable deployable) {
        return Completable.fromRunnable(() -> {
            try {
                if (!gatewayConfiguration.useLegacyEnvironmentHrids()) {
                    node.metadata().put(META_ORGANIZATIONS, deployable.organizationIds());
                }
                node.metadata().put(META_INSTALLATION, deployable.installationId());
                log.debug("Node metadata updated");
            } catch (Exception e) {
                throw new SyncException("An error occurred when trying to update node metadata.", e);
            }
        });
    }

    @Override
    public Completable doAfterDeployment(final NodeMetadataDeployable deployable) {
        return distributedSyncService.distributeIfNeeded(deployable);
    }
}

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
package io.gravitee.gateway.services.sync.process.distributed.synchronizer.node;

import io.gravitee.gateway.services.sync.process.common.deployer.DeployerFactory;
import io.gravitee.gateway.services.sync.process.common.deployer.NodeMetadataDeployer;
import io.gravitee.gateway.services.sync.process.common.synchronizer.Order;
import io.gravitee.gateway.services.sync.process.distributed.fetcher.DistributedEventFetcher;
import io.gravitee.gateway.services.sync.process.distributed.mapper.NodeMetadataMapper;
import io.gravitee.gateway.services.sync.process.distributed.synchronizer.AbstractDistributedSynchronizer;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.node.NodeMetadataDeployable;
import io.gravitee.repository.distributedsync.model.DistributedEvent;
import io.gravitee.repository.distributedsync.model.DistributedEventType;
import io.gravitee.repository.distributedsync.model.DistributedSyncAction;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class DistributedNodeMetadataSynchronizer extends AbstractDistributedSynchronizer<NodeMetadataDeployable, NodeMetadataDeployer> {

    private final DeployerFactory deployerFactory;
    private final NodeMetadataMapper nodeMetadataMapper;

    public DistributedNodeMetadataSynchronizer(
        final DistributedEventFetcher distributedEventFetcher,
        final ThreadPoolExecutor syncFetcherExecutor,
        final ThreadPoolExecutor syncDeployerExecutor,
        final DeployerFactory deployerFactory,
        final NodeMetadataMapper nodeMetadataMapper
    ) {
        super(distributedEventFetcher, syncFetcherExecutor, syncDeployerExecutor);
        this.deployerFactory = deployerFactory;
        this.nodeMetadataMapper = nodeMetadataMapper;
    }

    @Override
    public Completable synchronize(final Long from, final Long to) {
        if (from == -1) {
            return super.synchronize(from, to);
        } else {
            return Completable.complete();
        }
    }

    @Override
    protected DistributedEventType distributedEventType() {
        return DistributedEventType.NODE_METADATA;
    }

    @Override
    protected Set<DistributedSyncAction> syncActions(final boolean initialSync) {
        return INIT_SYNC_ACTIONS;
    }

    @Override
    protected Maybe<NodeMetadataDeployable> mapTo(final DistributedEvent distributedEvent) {
        return nodeMetadataMapper.to(distributedEvent);
    }

    @Override
    protected NodeMetadataDeployer createDeployer() {
        return deployerFactory.createNodeMetadataDeployer();
    }

    @Override
    public int order() {
        return Order.NODE_METADATA.index();
    }
}

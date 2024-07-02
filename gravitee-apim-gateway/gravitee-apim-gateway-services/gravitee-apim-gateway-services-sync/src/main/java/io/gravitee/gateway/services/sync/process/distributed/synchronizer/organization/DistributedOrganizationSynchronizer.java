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
package io.gravitee.gateway.services.sync.process.distributed.synchronizer.organization;

import io.gravitee.gateway.services.sync.process.common.deployer.DeployerFactory;
import io.gravitee.gateway.services.sync.process.common.deployer.OrganizationDeployer;
import io.gravitee.gateway.services.sync.process.common.synchronizer.Order;
import io.gravitee.gateway.services.sync.process.distributed.fetcher.DistributedEventFetcher;
import io.gravitee.gateway.services.sync.process.distributed.mapper.OrganizationMapper;
import io.gravitee.gateway.services.sync.process.distributed.synchronizer.AbstractDistributedSynchronizer;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.organization.OrganizationDeployable;
import io.gravitee.repository.distributedsync.model.DistributedEvent;
import io.gravitee.repository.distributedsync.model.DistributedEventType;
import io.gravitee.repository.distributedsync.model.DistributedSyncAction;
import io.reactivex.rxjava3.core.Maybe;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class DistributedOrganizationSynchronizer extends AbstractDistributedSynchronizer<OrganizationDeployable, OrganizationDeployer> {

    private final DeployerFactory deployerFactory;
    private final OrganizationMapper organizationMapper;

    public DistributedOrganizationSynchronizer(
        final DistributedEventFetcher distributedEventFetcher,
        final ThreadPoolExecutor syncFetcherExecutor,
        final ThreadPoolExecutor syncDeployerExecutor,
        final DeployerFactory deployerFactory,
        final OrganizationMapper organizationMapper
    ) {
        super(distributedEventFetcher, syncFetcherExecutor, syncDeployerExecutor);
        this.deployerFactory = deployerFactory;
        this.organizationMapper = organizationMapper;
    }

    @Override
    protected DistributedEventType distributedEventType() {
        return DistributedEventType.ORGANIZATION;
    }

    @Override
    protected Set<DistributedSyncAction> syncActions(final boolean initialSync) {
        return INIT_SYNC_ACTIONS;
    }

    @Override
    protected Maybe<OrganizationDeployable> mapTo(final DistributedEvent distributedEvent) {
        return organizationMapper.to(distributedEvent);
    }

    @Override
    protected OrganizationDeployer createDeployer() {
        return deployerFactory.createOrganizationDeployer();
    }

    @Override
    public int order() {
        return Order.ORGANIZATION.index();
    }
}

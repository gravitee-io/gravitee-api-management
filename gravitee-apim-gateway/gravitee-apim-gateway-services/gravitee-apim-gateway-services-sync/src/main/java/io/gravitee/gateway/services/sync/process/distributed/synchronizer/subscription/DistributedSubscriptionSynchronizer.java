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
package io.gravitee.gateway.services.sync.process.distributed.synchronizer.subscription;

import io.gravitee.gateway.services.sync.process.common.deployer.DeployerFactory;
import io.gravitee.gateway.services.sync.process.common.deployer.SubscriptionDeployer;
import io.gravitee.gateway.services.sync.process.common.model.SubscriptionDeployable;
import io.gravitee.gateway.services.sync.process.distributed.fetcher.DistributedEventFetcher;
import io.gravitee.gateway.services.sync.process.distributed.mapper.SubscriptionMapper;
import io.gravitee.gateway.services.sync.process.distributed.synchronizer.AbstractDistributedSynchronizer;
import io.gravitee.repository.distributedsync.model.DistributedEvent;
import io.gravitee.repository.distributedsync.model.DistributedEventType;
import io.reactivex.rxjava3.core.Maybe;
import java.util.concurrent.ThreadPoolExecutor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class DistributedSubscriptionSynchronizer extends AbstractDistributedSynchronizer<SubscriptionDeployable, SubscriptionDeployer> {

    private final DeployerFactory deployerFactory;
    private final SubscriptionMapper subscriptionMapper;

    public DistributedSubscriptionSynchronizer(
        final DistributedEventFetcher distributedEventFetcher,
        final ThreadPoolExecutor syncFetcherExecutor,
        final ThreadPoolExecutor syncDeployerExecutor,
        final DeployerFactory deployerFactory,
        final SubscriptionMapper subscriptionMapper
    ) {
        super(distributedEventFetcher, syncFetcherExecutor, syncDeployerExecutor);
        this.deployerFactory = deployerFactory;
        this.subscriptionMapper = subscriptionMapper;
    }

    @Override
    protected DistributedEventType distributedEventType() {
        return DistributedEventType.SUBSCRIPTION;
    }

    @Override
    protected Maybe<SubscriptionDeployable> mapTo(final DistributedEvent distributedEvent) {
        return subscriptionMapper.to(distributedEvent).cast(SubscriptionDeployable.class);
    }

    @Override
    protected SubscriptionDeployer createDeployer() {
        return deployerFactory.createSubscriptionDeployer();
    }

    @Override
    public int order() {
        return 10;
    }
}

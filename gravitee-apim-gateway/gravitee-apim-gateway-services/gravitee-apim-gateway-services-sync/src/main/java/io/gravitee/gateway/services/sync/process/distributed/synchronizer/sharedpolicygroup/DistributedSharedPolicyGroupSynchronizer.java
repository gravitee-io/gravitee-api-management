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
package io.gravitee.gateway.services.sync.process.distributed.synchronizer.sharedpolicygroup;

import io.gravitee.gateway.services.sync.process.common.deployer.DeployerFactory;
import io.gravitee.gateway.services.sync.process.common.deployer.SharedPolicyGroupDeployer;
import io.gravitee.gateway.services.sync.process.common.synchronizer.Order;
import io.gravitee.gateway.services.sync.process.distributed.fetcher.DistributedEventFetcher;
import io.gravitee.gateway.services.sync.process.distributed.mapper.SharedPolicyGroupMapper;
import io.gravitee.gateway.services.sync.process.distributed.synchronizer.AbstractDistributedSynchronizer;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.sharedpolicygroup.SharedPolicyGroupReactorDeployable;
import io.gravitee.repository.distributedsync.model.DistributedEvent;
import io.gravitee.repository.distributedsync.model.DistributedEventType;
import io.reactivex.rxjava3.core.Maybe;
import java.util.concurrent.ThreadPoolExecutor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class DistributedSharedPolicyGroupSynchronizer
    extends AbstractDistributedSynchronizer<SharedPolicyGroupReactorDeployable, SharedPolicyGroupDeployer> {

    private final DeployerFactory deployerFactory;
    private final SharedPolicyGroupMapper sharedPolicyGroupMapper;

    public DistributedSharedPolicyGroupSynchronizer(
        final DistributedEventFetcher distributedEventFetcher,
        final ThreadPoolExecutor syncFetcherExecutor,
        final ThreadPoolExecutor syncDeployerExecutor,
        final DeployerFactory deployerFactory,
        final SharedPolicyGroupMapper sharedPolicyGroupMapper
    ) {
        super(distributedEventFetcher, syncFetcherExecutor, syncDeployerExecutor);
        this.deployerFactory = deployerFactory;
        this.sharedPolicyGroupMapper = sharedPolicyGroupMapper;
    }

    @Override
    protected DistributedEventType distributedEventType() {
        return DistributedEventType.SHARED_POLICY_GROUP;
    }

    protected Maybe<SharedPolicyGroupReactorDeployable> mapTo(final DistributedEvent distributedEvent) {
        return sharedPolicyGroupMapper.to(distributedEvent);
    }

    @Override
    protected SharedPolicyGroupDeployer createDeployer() {
        return deployerFactory.createSharedPolicyGroupDeployer();
    }

    @Override
    public int order() {
        return Order.SHARED_POLICY_GROUP.index();
    }
}

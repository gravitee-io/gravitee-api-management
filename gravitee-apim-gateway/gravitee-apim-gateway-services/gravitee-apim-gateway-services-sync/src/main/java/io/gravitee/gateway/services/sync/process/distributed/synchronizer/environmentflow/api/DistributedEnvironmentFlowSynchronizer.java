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
package io.gravitee.gateway.services.sync.process.distributed.synchronizer.environmentflow.api;

import io.gravitee.gateway.services.sync.process.common.deployer.DeployerFactory;
import io.gravitee.gateway.services.sync.process.common.deployer.EnvironmentFlowDeployer;
import io.gravitee.gateway.services.sync.process.distributed.fetcher.DistributedEventFetcher;
import io.gravitee.gateway.services.sync.process.distributed.mapper.EnvironmentFlowMapper;
import io.gravitee.gateway.services.sync.process.distributed.synchronizer.AbstractDistributedSynchronizer;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.environmentflow.EnvironmentFlowReactorDeployable;
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
public class DistributedEnvironmentFlowSynchronizer
    extends AbstractDistributedSynchronizer<EnvironmentFlowReactorDeployable, EnvironmentFlowDeployer> {

    private final DeployerFactory deployerFactory;
    private final EnvironmentFlowMapper environmentFlowMapper;

    public DistributedEnvironmentFlowSynchronizer(
        final DistributedEventFetcher distributedEventFetcher,
        final ThreadPoolExecutor syncFetcherExecutor,
        final ThreadPoolExecutor syncDeployerExecutor,
        final DeployerFactory deployerFactory,
        final EnvironmentFlowMapper environmentFlowMapper
    ) {
        super(distributedEventFetcher, syncFetcherExecutor, syncDeployerExecutor);
        this.deployerFactory = deployerFactory;
        this.environmentFlowMapper = environmentFlowMapper;
    }

    @Override
    protected DistributedEventType distributedEventType() {
        return DistributedEventType.ENVIRONMENT_FLOW;
    }

    protected Maybe<EnvironmentFlowReactorDeployable> mapTo(final DistributedEvent distributedEvent) {
        return environmentFlowMapper.to(distributedEvent);
    }

    @Override
    protected EnvironmentFlowDeployer createDeployer() {
        return deployerFactory.createEnvironmentFlowDeployer();
    }

    @Override
    public int order() {
        return 2;
    }
}

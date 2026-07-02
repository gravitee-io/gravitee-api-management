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
package io.gravitee.gateway.services.sync.process.distributed.synchronizer.apiproduct;

import io.gravitee.gateway.services.sync.process.common.deployer.ApiProductDeployer;
import io.gravitee.gateway.services.sync.process.common.deployer.DeployerFactory;
import io.gravitee.gateway.services.sync.process.common.synchronizer.Order;
import io.gravitee.gateway.services.sync.process.distributed.fetcher.DistributedEventFetcher;
import io.gravitee.gateway.services.sync.process.distributed.mapper.ApiProductMapper;
import io.gravitee.gateway.services.sync.process.distributed.synchronizer.AbstractDistributedSynchronizer;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.apiproduct.ApiProductReactorDeployable;
import io.gravitee.repository.distributedsync.model.DistributedEvent;
import io.gravitee.repository.distributedsync.model.DistributedEventType;
import io.reactivex.rxjava3.core.Maybe;
import java.util.concurrent.ThreadPoolExecutor;
import lombok.CustomLog;

@CustomLog
public class DistributedApiProductSynchronizer extends AbstractDistributedSynchronizer<ApiProductReactorDeployable, ApiProductDeployer> {

    private final DeployerFactory deployerFactory;
    private final ApiProductMapper apiProductMapper;

    public DistributedApiProductSynchronizer(
        final DistributedEventFetcher distributedEventFetcher,
        final ThreadPoolExecutor syncFetcherExecutor,
        final ThreadPoolExecutor syncDeployerExecutor,
        final DeployerFactory deployerFactory,
        final ApiProductMapper apiProductMapper
    ) {
        super(distributedEventFetcher, syncFetcherExecutor, syncDeployerExecutor);
        this.deployerFactory = deployerFactory;
        this.apiProductMapper = apiProductMapper;
    }

    @Override
    protected DistributedEventType distributedEventType() {
        return DistributedEventType.API_PRODUCT;
    }

    @Override
    protected Maybe<ApiProductReactorDeployable> mapTo(final DistributedEvent distributedEvent) {
        return apiProductMapper.to(distributedEvent);
    }

    @Override
    protected ApiProductDeployer createDeployer() {
        return deployerFactory.createApiProductDeployer();
    }

    @Override
    public int order() {
        return Order.API_PRODUCT.index();
    }
}

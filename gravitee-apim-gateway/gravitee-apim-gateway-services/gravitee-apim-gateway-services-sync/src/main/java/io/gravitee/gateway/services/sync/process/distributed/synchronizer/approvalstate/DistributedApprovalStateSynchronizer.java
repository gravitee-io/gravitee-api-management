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
package io.gravitee.gateway.services.sync.process.distributed.synchronizer.approvalstate;

import io.gravitee.gateway.services.sync.process.common.deployer.ApprovalStateDeployer;
import io.gravitee.gateway.services.sync.process.common.deployer.DeployerFactory;
import io.gravitee.gateway.services.sync.process.common.synchronizer.Order;
import io.gravitee.gateway.services.sync.process.distributed.fetcher.DistributedEventFetcher;
import io.gravitee.gateway.services.sync.process.distributed.mapper.ApprovalStateMapper;
import io.gravitee.gateway.services.sync.process.distributed.synchronizer.AbstractDistributedSynchronizer;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.approvalstate.ApprovalStateDeployable;
import io.gravitee.repository.distributedsync.model.DistributedEvent;
import io.gravitee.repository.distributedsync.model.DistributedEventType;
import io.reactivex.rxjava3.core.Maybe;
import java.util.concurrent.ThreadPoolExecutor;

public class DistributedApprovalStateSynchronizer extends AbstractDistributedSynchronizer<ApprovalStateDeployable, ApprovalStateDeployer> {

    private final DeployerFactory deployerFactory;
    private final ApprovalStateMapper approvalStateMapper;

    public DistributedApprovalStateSynchronizer(
        final DistributedEventFetcher distributedEventFetcher,
        final ThreadPoolExecutor syncFetcherExecutor,
        final ThreadPoolExecutor syncDeployerExecutor,
        final DeployerFactory deployerFactory,
        final ApprovalStateMapper approvalStateMapper
    ) {
        super(distributedEventFetcher, syncFetcherExecutor, syncDeployerExecutor);
        this.deployerFactory = deployerFactory;
        this.approvalStateMapper = approvalStateMapper;
    }

    @Override
    protected DistributedEventType distributedEventType() {
        return DistributedEventType.APPROVAL_STATE;
    }

    @Override
    protected Maybe<ApprovalStateDeployable> mapTo(final DistributedEvent distributedEvent) {
        return approvalStateMapper.to(distributedEvent);
    }

    @Override
    protected ApprovalStateDeployer createDeployer() {
        return deployerFactory.createApprovalStateDeployer();
    }

    @Override
    public int order() {
        return Order.APPROVAL_STATE.index();
    }
}

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
package io.gravitee.gateway.services.sync.process.distributed.synchronizer.approvalrule;

import io.gravitee.gateway.services.sync.process.common.deployer.ApprovalRuleDeployer;
import io.gravitee.gateway.services.sync.process.common.deployer.DeployerFactory;
import io.gravitee.gateway.services.sync.process.common.synchronizer.Order;
import io.gravitee.gateway.services.sync.process.distributed.fetcher.DistributedEventFetcher;
import io.gravitee.gateway.services.sync.process.distributed.mapper.ApprovalRuleMapper;
import io.gravitee.gateway.services.sync.process.distributed.synchronizer.AbstractDistributedSynchronizer;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.approvalrule.ApprovalRuleDeployable;
import io.gravitee.repository.distributedsync.model.DistributedEvent;
import io.gravitee.repository.distributedsync.model.DistributedEventType;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import java.util.concurrent.ThreadPoolExecutor;
import lombok.CustomLog;

@CustomLog
public class DistributedApprovalRuleSynchronizer extends AbstractDistributedSynchronizer<ApprovalRuleDeployable, ApprovalRuleDeployer> {

    private final DeployerFactory deployerFactory;
    private final ApprovalRuleMapper approvalRuleMapper;

    public DistributedApprovalRuleSynchronizer(
        final DistributedEventFetcher distributedEventFetcher,
        final ThreadPoolExecutor syncFetcherExecutor,
        final ThreadPoolExecutor syncDeployerExecutor,
        final DeployerFactory deployerFactory,
        final ApprovalRuleMapper approvalRuleMapper
    ) {
        super(distributedEventFetcher, syncFetcherExecutor, syncDeployerExecutor);
        this.deployerFactory = deployerFactory;
        this.approvalRuleMapper = approvalRuleMapper;
    }

    /**
     * Commits once the whole pass is staged, so the rules of an environment are re-indexed once per pass.
     * {@code createDeployer()} returns a new, stateless instance here, separate from the one {@code super.synchronize()}
     * used for deploy/undeploy; both delegate to the same shared {@code ApprovalRuleManager}, so committing on
     * either one drains the same staged queue.
     */
    @Override
    public Completable synchronize(final Long from, final Long to) {
        return super.synchronize(from, to).andThen(Completable.defer(() -> createDeployer().commit()));
    }

    @Override
    protected DistributedEventType distributedEventType() {
        return DistributedEventType.APPROVAL_RULE;
    }

    @Override
    protected Maybe<ApprovalRuleDeployable> mapTo(final DistributedEvent distributedEvent) {
        return approvalRuleMapper.to(distributedEvent);
    }

    @Override
    protected ApprovalRuleDeployer createDeployer() {
        return deployerFactory.createApprovalRuleDeployer();
    }

    @Override
    public int order() {
        return Order.APPROVAL_RULE.index();
    }
}

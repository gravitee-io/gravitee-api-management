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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.approvalstate;

import io.gravitee.gateway.services.sync.process.common.deployer.ApprovalStateDeployer;
import io.gravitee.gateway.services.sync.process.common.deployer.DeployerFactory;
import io.gravitee.gateway.services.sync.process.common.synchronizer.Order;
import io.gravitee.gateway.services.sync.process.repository.fetcher.LatestEventFetcher;
import io.gravitee.gateway.services.sync.process.repository.service.EnvironmentService;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.approval.AbstractApprovalSynchronizer;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import io.reactivex.rxjava3.core.Maybe;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Synchronizes the approval states from the latest events: a published state is deployed, an unpublished one is
 * undeployed, as the subscription and API key synchronizers do. A failing pass, initial or not, is isolated so the sync
 * goes on, and its window is fetched again on the next pass: a missing state only makes a held call wait.
 */
public class ApprovalStateSynchronizer extends AbstractApprovalSynchronizer<ApprovalStateDeployable, ApprovalStateDeployer> {

    private final ApprovalStateMapper approvalStateMapper;
    private final DeployerFactory deployerFactory;

    public ApprovalStateSynchronizer(
        LatestEventFetcher eventsFetcher,
        ApprovalStateMapper approvalStateMapper,
        EnvironmentService environmentService,
        DeployerFactory deployerFactory,
        ThreadPoolExecutor syncFetcherExecutor,
        ThreadPoolExecutor syncDeployerExecutor
    ) {
        super(eventsFetcher, environmentService, syncFetcherExecutor, syncDeployerExecutor);
        this.approvalStateMapper = approvalStateMapper;
        this.deployerFactory = deployerFactory;
    }

    @Override
    public int order() {
        return Order.APPROVAL_STATE.index();
    }

    @Override
    protected Maybe<ApprovalStateDeployable> toDeploy(Event event) {
        return approvalStateMapper.toDeploy(event);
    }

    @Override
    protected Maybe<ApprovalStateDeployable> toUndeploy(Event event) {
        return approvalStateMapper.toUndeploy(event);
    }

    @Override
    protected ApprovalStateDeployer createDeployer() {
        return deployerFactory.createApprovalStateDeployer();
    }

    @Override
    protected Event.EventProperties eventProperty() {
        return Event.EventProperties.APPROVAL_REQUEST_ID;
    }

    @Override
    protected EventType publishType() {
        return EventType.PUBLISH_APPROVAL_STATE;
    }

    @Override
    protected EventType unpublishType() {
        return EventType.UNPUBLISH_APPROVAL_STATE;
    }

    @Override
    protected String environmentIdOf(ApprovalStateDeployable deployable) {
        return deployable.state().environmentId();
    }

    @Override
    protected ApprovalStateDeployable withOrganizationId(ApprovalStateDeployable deployable, String organizationId) {
        return deployable.organizationId(organizationId);
    }

    @Override
    protected String singularLabel() {
        return "approval state";
    }

    @Override
    protected String pluralLabel() {
        return "approval states";
    }

    @Override
    protected boolean isolatesFailedInitialPass() {
        return true;
    }
}

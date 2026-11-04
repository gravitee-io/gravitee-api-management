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
package io.gravitee.gateway.services.sync.process.distributed.mapper;

import io.gravitee.gateway.handlers.api.manager.DeployedApprovalState;
import io.gravitee.gateway.handlers.api.manager.impl.ApprovalStateJson;
import io.gravitee.gateway.handlers.api.manager.impl.ApprovalStateJson.Entry;
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.approvalstate.ApprovalStateDeployable;
import io.gravitee.repository.distributedsync.model.DistributedEvent;
import io.gravitee.repository.distributedsync.model.DistributedEventType;
import io.gravitee.repository.distributedsync.model.DistributedSyncAction;
import io.reactivex.rxjava3.core.Maybe;
import java.time.Clock;
import java.util.Date;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

/**
 * Maps approval states to and from distributed events, in the {@link ApprovalStateJson} form. A state already past its
 * {@code keepUntil} is distributed as a removal, so that the secondary nodes drop the copy they hold rather than keep it
 * until its own {@code keepUntil}.
 */
@RequiredArgsConstructor
@CustomLog
public class ApprovalStateMapper {

    private final ApprovalStateJson states;
    private final Clock clock;

    public Maybe<ApprovalStateDeployable> to(final DistributedEvent event) {
        return Maybe.fromCallable(() -> {
            try {
                SyncAction syncAction = SyncActionMapper.to(event.getSyncAction());
                if (syncAction == SyncAction.UNDEPLOY) {
                    return ApprovalStateDeployable.builder().approvalRequestId(event.getId()).syncAction(SyncAction.UNDEPLOY).build();
                }
                Entry entry = states.read(event.getPayload());
                return ApprovalStateDeployable.builder()
                    .approvalRequestId(entry.state().id())
                    .organizationId(entry.organizationId())
                    .state(entry.state())
                    .syncAction(syncAction)
                    .build();
            } catch (Exception e) {
                log.debug("Error while reading approval state from distributed event [{}]", event.getId(), e);
                return null;
            }
        });
    }

    public Maybe<DistributedEvent> to(final ApprovalStateDeployable deployable) {
        return Maybe.fromCallable(() -> {
            try {
                long now = clock.millis();
                DistributedEvent.DistributedEventBuilder builder = DistributedEvent.builder()
                    .id(deployable.approvalRequestId())
                    .type(DistributedEventType.APPROVAL_STATE)
                    .updatedAt(new Date(now));
                DeployedApprovalState state = deployable.state();
                if (deployable.syncAction() == SyncAction.DEPLOY && state.keepUntil() > now) {
                    return builder
                        .syncAction(DistributedSyncAction.DEPLOY)
                        .payload(states.write(deployable.organizationId(), state))
                        .build();
                }
                return builder.syncAction(DistributedSyncAction.UNDEPLOY).build();
            } catch (Exception e) {
                log.warn("Error while building distributed event from approval state [{}]", deployable.approvalRequestId(), e);
                return null;
            }
        });
    }
}

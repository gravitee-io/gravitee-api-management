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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gateway.handlers.sharedpolicygroup.ReactableSharedPolicyGroup;
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.sharedpolicygroup.SharedPolicyGroupReactorDeployable;
import io.gravitee.repository.distributedsync.model.DistributedEvent;
import io.gravitee.repository.distributedsync.model.DistributedEventType;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
@Slf4j
public class SharedPolicyGroupMapper {

    private final ObjectMapper objectMapper;

    public Maybe<SharedPolicyGroupReactorDeployable> to(final DistributedEvent event) {
        return Maybe.fromCallable(() -> {
            try {
                ReactableSharedPolicyGroup reactableSharedPolicyGroup = objectMapper.readValue(
                    event.getPayload(),
                    ReactableSharedPolicyGroup.class
                );

                return SharedPolicyGroupReactorDeployable
                    .builder()
                    .sharedPolicyGroupId(event.getId())
                    .reactableSharedPolicyGroup(reactableSharedPolicyGroup)
                    .syncAction(SyncActionMapper.to(event.getSyncAction()))
                    .build();
            } catch (Exception e) {
                log.warn("Error while determining shared policy group into event payload", e);
                return null;
            }
        });
    }

    public Flowable<DistributedEvent> to(final SharedPolicyGroupReactorDeployable deployable) {
        return Flowable.concatArray(toSharedPolicyGroupDistributedEvent(deployable));
    }

    private Flowable<DistributedEvent> toSharedPolicyGroupDistributedEvent(final SharedPolicyGroupReactorDeployable deployable) {
        return Flowable.fromCallable(() -> {
            try {
                DistributedEvent.DistributedEventBuilder builder = DistributedEvent
                    .builder()
                    .id(deployable.id())
                    .type(DistributedEventType.SHARED_POLICY_GROUP)
                    .syncAction(SyncActionMapper.to(deployable.syncAction()))
                    .updatedAt(new Date());
                if (deployable.syncAction() == SyncAction.DEPLOY) {
                    builder.payload(objectMapper.writeValueAsString(deployable.reactableSharedPolicyGroup()));
                }
                return builder.build();
            } catch (Exception e) {
                log.warn("Error while building distributed event from shared policy group reactor", e);
                return null;
            }
        });
    }
}

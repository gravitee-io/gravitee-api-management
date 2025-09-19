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
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.services.sync.process.common.model.SubscriptionDeployable;
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.subscription.SingleSubscriptionDeployable;
import io.gravitee.repository.distributedsync.model.DistributedEvent;
import io.gravitee.repository.distributedsync.model.DistributedEventType;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class SubscriptionMapper {

    private final ObjectMapper objectMapper;

    public Maybe<SingleSubscriptionDeployable> to(final DistributedEvent event) {
        return Maybe.fromCallable(() -> {
            try {
                final Subscription subscription = objectMapper.readValue(event.getPayload(), Subscription.class);
                return SingleSubscriptionDeployable.builder()
                    .subscription(subscription)
                    .syncAction(SyncActionMapper.to(event.getSyncAction()))
                    .build();
            } catch (Exception e) {
                log.warn("Error while determining subscription into event payload", e);
                return null;
            }
        });
    }

    public Flowable<DistributedEvent> to(final SubscriptionDeployable deployable) {
        return Flowable.fromIterable(deployable.subscriptions()).flatMapMaybe(subscription ->
            Maybe.fromCallable(() -> {
                try {
                    DistributedEvent.DistributedEventBuilder builder = DistributedEvent.builder()
                        .id(subscription.getId())
                        .type(DistributedEventType.SUBSCRIPTION)
                        .syncAction(SyncActionMapper.to(deployable.syncAction()))
                        .updatedAt(new Date())
                        .refType(DistributedEventType.API)
                        .refId(deployable.apiId());
                    if (deployable.syncAction() == SyncAction.DEPLOY) {
                        builder.payload(objectMapper.writeValueAsString(subscription));
                    }
                    return builder.build();
                } catch (Exception e) {
                    log.warn("Error while building distributed event from subscription", e);
                    return null;
                }
            })
        );
    }
}

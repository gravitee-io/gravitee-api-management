/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.services.sync.process.distributed.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.api.ApiReactorDeployable;
import io.gravitee.repository.distributedsync.model.DistributedEvent;
import io.gravitee.repository.distributedsync.model.DistributedEventType;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class ApiMapper {

    private final ObjectMapper objectMapper;
    private final SubscriptionMapper subscriptionMapper;
    private final ApiKeyMapper apiKeyMapper;

    public Maybe<ApiReactorDeployable> to(final DistributedEvent event) {
        return Maybe.fromCallable(() -> {
            try {
                ReactableApi<?> reactableApi = toReactable(event.getPayload());

                return ApiReactorDeployable
                    .builder()
                    .apiId(event.getId())
                    .reactableApi(reactableApi)
                    .syncAction(SyncActionMapper.to(event.getSyncAction()))
                    .build();
            } catch (Exception e) {
                log.warn("Error while determining api into event payload", e);
                return null;
            }
        });
    }

    private ReactableApi<?> toReactable(final String payload) throws JsonProcessingException {
        ReactableApi<?> reactableApi = null;
        if (payload != null && !payload.isBlank()) {
            try {
                reactableApi = objectMapper.readValue(payload, io.gravitee.gateway.reactive.handlers.api.v4.Api.class);
            } catch (Exception e) {
                reactableApi = objectMapper.readValue(payload, io.gravitee.gateway.handlers.api.definition.Api.class);
            }
        }
        return reactableApi;
    }

    public Flowable<DistributedEvent> to(final ApiReactorDeployable deployable) {
        return Flowable.concatArray(toApiDistributedEvent(deployable), subscriptionMapper.to(deployable), apiKeyMapper.to(deployable));
    }

    private Flowable<DistributedEvent> toApiDistributedEvent(final ApiReactorDeployable deployable) {
        return Flowable.fromCallable(() -> {
            try {
                DistributedEvent.DistributedEventBuilder builder = DistributedEvent
                    .builder()
                    .id(deployable.id())
                    .type(DistributedEventType.API)
                    .syncAction(SyncActionMapper.to(deployable.syncAction()))
                    .updatedAt(new Date());
                if (deployable.syncAction() == SyncAction.DEPLOY) {
                    builder.payload(objectMapper.writeValueAsString(deployable.reactableApi()));
                }
                return builder.build();
            } catch (Exception e) {
                log.warn("Error while building distributed event from api reactor", e);
                return null;
            }
        });
    }
}

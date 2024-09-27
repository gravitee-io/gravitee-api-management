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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.api.ApiReactorDeployable;
import io.gravitee.repository.distributedsync.model.DistributedEvent;
import io.gravitee.repository.distributedsync.model.DistributedEventType;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ApiMapper {

    private final ObjectMapper objectMapper;
    private final SubscriptionMapper subscriptionMapper;
    private final ApiKeyMapper apiKeyMapper;
    private final List<Function<String, ReactableApi<?>>> parsers;

    public ApiMapper(ObjectMapper objectMapper, SubscriptionMapper subscriptionMapper, ApiKeyMapper apiKeyMapper) {
        this.objectMapper = objectMapper;
        this.subscriptionMapper = subscriptionMapper;
        this.apiKeyMapper = apiKeyMapper;

        parsers =
            List.of(
                payload ->
                    parseAndAssert(
                        payload,
                        Api.class,
                        reactableApi ->
                            reactableApi.getDefinitionVersion() == DefinitionVersion.V1 ||
                            reactableApi.getDefinitionVersion() == DefinitionVersion.V2
                    ),
                payload ->
                    parseAndAssert(
                        payload,
                        io.gravitee.gateway.reactive.handlers.api.v4.Api.class,
                        reactableApi -> reactableApi.getDefinitionVersion() == DefinitionVersion.V4
                    )
            );
    }

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

    private ReactableApi<?> toReactable(final String payload) throws Exception {
        ReactableApi<?> reactableApi = null;
        if (payload != null && !payload.isBlank()) {
            Exception lastException = null;
            for (Function<String, ReactableApi<?>> parser : parsers) {
                try {
                    return parser.apply(payload);
                } catch (Exception e) {
                    lastException = e;
                    // continue to next parser
                }
            }
            throw lastException;
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

    /**
     * Try to parse a string payload into expected type and verify predicate is true.
     * @throws {@link RuntimeException} built from original exception as cause.
     * @return the payload parsed into the expected type
     */
    private ReactableApi<?> parseAndAssert(
        String payload,
        Class<? extends ReactableApi<?>> parseToClass,
        Predicate<ReactableApi<?>> assertion
    ) {
        ReactableApi<?> result;
        // try to map the payload into expected type
        try {
            result = this.objectMapper.readValue(payload, parseToClass);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        if (assertion.test(result)) {
            return result;
        }
        throw new RuntimeException(new IllegalStateException("Parsing predicate is false for API: " + result.getId()));
    }
}

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
package io.gravitee.plugin.endpoint.mock;

import io.gravitee.gateway.jupiter.api.ConnectorMode;
import io.gravitee.gateway.jupiter.api.connector.endpoint.async.EndpointAsyncConnector;
import io.gravitee.gateway.jupiter.api.context.ExecutionContext;
import io.gravitee.gateway.jupiter.api.context.InternalContextAttributes;
import io.gravitee.gateway.jupiter.api.message.DefaultMessage;
import io.gravitee.gateway.jupiter.api.message.Message;
import io.gravitee.plugin.endpoint.mock.configuration.MockEndpointConnectorConfiguration;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author GraviteeSource Team
 */
@AllArgsConstructor
@Slf4j
public class MockEndpointConnector implements EndpointAsyncConnector {

    static final Set<ConnectorMode> SUPPORTED_MODES = Set.of(ConnectorMode.PUBLISH, ConnectorMode.SUBSCRIBE);
    private static final String ENDPOINT_ID = "mock";
    protected final MockEndpointConnectorConfiguration configuration;

    @Override
    public String id() {
        return ENDPOINT_ID;
    }

    @Override
    public Set<ConnectorMode> supportedModes() {
        return SUPPORTED_MODES;
    }

    @Override
    public Completable connect(ExecutionContext ctx) {
        return Completable.defer(
            () -> {
                final Integer messagesLimitCount = ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_MESSAGES_LIMIT_COUNT);
                final Long messagesLimitDurationMs = ctx.getInternalAttribute(
                    InternalContextAttributes.ATTR_INTERNAL_MESSAGES_LIMIT_DURATION_MS
                );

                final String messagesResumeLastId = ctx.getInternalAttribute(
                    InternalContextAttributes.ATTR_INTERNAL_MESSAGES_RESUME_LASTID
                );

                final Integer configurationLimitCount = configuration.getMessageCount();

                final Integer limitCount = configurationLimitCount != null && messagesLimitCount != null
                    ? Integer.valueOf(Math.min(configurationLimitCount, messagesLimitCount))
                    : messagesLimitCount != null ? messagesLimitCount : configurationLimitCount;

                ctx.response().messages(generateMessageFlow(limitCount, messagesLimitDurationMs, messagesResumeLastId));
                return ctx
                    .request()
                    .onMessage(
                        message -> {
                            log.info("Received message: {}", message.content().toString());
                            return Maybe.empty();
                        }
                    );
            }
        );
    }

    private Flowable<Message> generateMessageFlow(
        final Integer messagesLimitCount,
        final Long messagesLimitDurationMs,
        final String lastId
    ) {
        final long stateInitValue = getStateInitValue(lastId);

        Flowable<Message> messageFlow = Flowable
            .<Message, Long>generate(
                () -> stateInitValue,
                (state, emitter) -> {
                    if (messagesLimitCount == null || (state - stateInitValue) < messagesLimitCount) {
                        emitter.onNext(new DefaultMessage(configuration.getMessageContent()).id(Long.toString(state)));
                    } else {
                        emitter.onComplete();
                    }
                    return state + 1;
                }
            )
            .delay(configuration.getMessageInterval(), TimeUnit.MILLISECONDS)
            .rebatchRequests(1);

        if (messagesLimitDurationMs != null) {
            messageFlow = messageFlow.take(messagesLimitDurationMs, TimeUnit.MILLISECONDS);
        }

        return messageFlow;
    }

    private long getStateInitValue(final String lastId) {
        long stateInitValue = 0L;
        if (lastId != null) {
            try {
                stateInitValue = Long.parseLong(lastId) + 1;
            } catch (NumberFormatException nfe) {
                log.warn("Unable to parse lastId: {}. Setting to 0", lastId);
            }
        }

        return stateInitValue;
    }
}

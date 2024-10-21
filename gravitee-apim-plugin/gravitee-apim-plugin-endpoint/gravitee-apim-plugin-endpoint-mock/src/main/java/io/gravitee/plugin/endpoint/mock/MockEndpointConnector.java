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
package io.gravitee.plugin.endpoint.mock;

import io.gravitee.common.http.HttpHeader;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.reactive.api.ConnectorMode;
import io.gravitee.gateway.reactive.api.connector.endpoint.async.EndpointAsyncConnector;
import io.gravitee.gateway.reactive.api.context.ExecutionContext;
import io.gravitee.gateway.reactive.api.context.InternalContextAttributes;
import io.gravitee.gateway.reactive.api.message.DefaultMessage;
import io.gravitee.gateway.reactive.api.message.Message;
import io.gravitee.gateway.reactive.api.qos.Qos;
import io.gravitee.gateway.reactive.api.qos.QosCapability;
import io.gravitee.gateway.reactive.api.tracing.message.TracingMessage;
import io.gravitee.gateway.reactive.api.tracing.message.TracingMessageAttribute;
import io.gravitee.gateway.reactive.api.tracing.message.TracingMessageOperationType;
import io.gravitee.plugin.endpoint.mock.configuration.MockEndpointConnectorConfiguration;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

/**
 * @author GraviteeSource Team
 */
@AllArgsConstructor
@Slf4j
public class MockEndpointConnector extends EndpointAsyncConnector {

    static final Set<ConnectorMode> SUPPORTED_MODES = Set.of(ConnectorMode.PUBLISH, ConnectorMode.SUBSCRIBE);
    static final Set<Qos> SUPPORTED_QOS = Set.of(Qos.NONE, Qos.AUTO, Qos.AT_LEAST_ONCE, Qos.AT_MOST_ONCE);
    private static final Set<QosCapability> SUPPORTED_QOS_CAPABILITIES = Set.of(
        QosCapability.AUTO_ACK,
        QosCapability.MANUAL_ACK,
        QosCapability.RECOVER
    );
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
    public Set<Qos> supportedQos() {
        return SUPPORTED_QOS;
    }

    @Override
    public Set<QosCapability> supportedQosCapabilities() {
        return SUPPORTED_QOS_CAPABILITIES;
    }

    @Override
    public Completable subscribe(final ExecutionContext ctx) {
        return Completable.fromRunnable(() -> {
            final Integer messagesLimitCount = ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_MESSAGES_LIMIT_COUNT);
            final Long messagesLimitDurationMs = ctx.getInternalAttribute(
                InternalContextAttributes.ATTR_INTERNAL_MESSAGES_LIMIT_DURATION_MS
            );

            final String messagesResumeLastId = ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_MESSAGES_RECOVERY_LAST_ID);

            final Integer maximumPublishedMessages = configuration.getMessageCount();

            ctx
                .response()
                .messages(generateMessageFlow(messagesLimitCount, maximumPublishedMessages, messagesLimitDurationMs, messagesResumeLastId));
        });
    }

    @Override
    public Completable publish(final ExecutionContext ctx) {
        return Completable.defer(() ->
            ctx
                .request()
                .onMessage(message -> {
                    log.info("Received message: {}", message.content() != null ? message.content().toString() : null);
                    return Maybe.just(message);
                })
        );
    }

    private Flowable<Message> generateMessageFlow(
        final Integer messagesLimitCount,
        final Integer maximumPublishedMessages,
        final Long messagesLimitDurationMs,
        final String lastId
    ) {
        final long stateInitValue = getStateInitValue(lastId);

        Flowable<Message> messageFlow = Flowable
            .<Message, Long>generate(
                () -> stateInitValue,
                (state, emitter) -> {
                    if (
                        // If we have no published message limit or state is before the limit
                        (maximumPublishedMessages == null || state < maximumPublishedMessages) &&
                        // And the entrypoint has no limit or state minus lastId is less than limit, then emit a message
                        (messagesLimitCount == null || (state - stateInitValue) < messagesLimitCount)
                    ) {
                        DefaultMessage message = new DefaultMessage(configuration.getMessageContent()).id(Long.toString(state));
                        // handle optional params
                        if (configuration.getHeaders() != null) {
                            HttpHeaders headers = HttpHeaders.create();
                            configuration.getHeaders().forEach(h -> headers.add(h.getName(), h.getValue()));
                            message.headers(headers);
                        }
                        if (configuration.getMetadata() != null) {
                            message.metadata(
                                configuration.getMetadata().stream().collect(Collectors.toMap(HttpHeader::getName, HttpHeader::getValue))
                            );
                        }
                        message.tracingAttributes(
                            Map.of(
                                TracingMessageAttribute.MESSAGING_OPERATION_NAME.key(),
                                "receive",
                                TracingMessageAttribute.MESSAGING_OPERATION_TYPE.key(),
                                TracingMessageOperationType.RECEIVE.value(),
                                TracingMessageAttribute.MESSAGING_SYSTEM.key(),
                                "mock",
                                TracingMessageAttribute.MESSAGING_MESSAGE_BODY_SIZE.key(),
                                message.content() != null ? String.valueOf(message.content().length()) : "0"
                            )
                        );
                        emitter.onNext(message);
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

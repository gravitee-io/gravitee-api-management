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
package io.gravitee.plugin.entrypoint.sse;

import static io.gravitee.common.http.MediaType.MEDIA_TEXT_EVENT_STREAM;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.http.MediaType;
import io.gravitee.common.utils.RxHelper;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.jupiter.api.ConnectorMode;
import io.gravitee.gateway.jupiter.api.ListenerType;
import io.gravitee.gateway.jupiter.api.connector.entrypoint.async.EntrypointAsyncConnector;
import io.gravitee.gateway.jupiter.api.context.ExecutionContext;
import io.gravitee.gateway.jupiter.api.message.DefaultMessage;
import io.gravitee.gateway.jupiter.api.message.Message;
import io.gravitee.gateway.jupiter.api.qos.Qos;
import io.gravitee.gateway.jupiter.api.qos.QosCapability;
import io.gravitee.gateway.jupiter.api.qos.QosRequirement;
import io.gravitee.plugin.entrypoint.sse.configuration.SseEntrypointConnectorConfiguration;
import io.gravitee.plugin.entrypoint.sse.model.SseEvent;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class SseEntrypointConnector extends EntrypointAsyncConnector {

    public static final String HEADER_LAST_EVENT_ID = "Last-Event-ID";
    public static final Set<ConnectorMode> SUPPORTED_MODES = Set.of(ConnectorMode.SUBSCRIBE);
    static final Set<Qos> SUPPORTED_QOS = Set.of(Qos.NONE, Qos.AUTO);
    public static final ListenerType SUPPORTED_LISTENER_TYPE = ListenerType.HTTP;
    private static final String ENTRYPOINT_ID = "sse";
    private static final int RETRY_MIN_VALUE = 1000;
    private static final int RETRY_MAX_VALUE = 30000;
    private final Random random;
    protected QosRequirement qosRequirement;
    private SseEntrypointConnectorConfiguration configuration;

    @SuppressWarnings("java:S2245")
    public SseEntrypointConnector(final Qos qos, final SseEntrypointConnectorConfiguration configuration) {
        computeQosRequirement(qos);
        this.configuration = configuration;
        if (this.configuration == null) {
            this.configuration = new SseEntrypointConnectorConfiguration();
        }
        // Random doesn't require to be secured here and is only used for random retry time
        this.random = new Random();
    }

    protected void computeQosRequirement(final Qos qos) {
        QosRequirement.QosRequirementBuilder qosRequirementBuilder = QosRequirement.builder().qos(qos);
        if (qos == Qos.AUTO) {
            qosRequirementBuilder.capabilities(Set.of(QosCapability.AUTO_ACK));
        }

        this.qosRequirement = qosRequirementBuilder.build();
    }

    @Override
    public String id() {
        return ENTRYPOINT_ID;
    }

    @Override
    public ListenerType supportedListenerType() {
        return SUPPORTED_LISTENER_TYPE;
    }

    @Override
    public Set<ConnectorMode> supportedModes() {
        return SUPPORTED_MODES;
    }

    @Override
    public QosRequirement qosRequirement() {
        return qosRequirement;
    }

    @Override
    public int matchCriteriaCount() {
        return 2;
    }

    @Override
    public boolean matches(final ExecutionContext ctx) {
        final String acceptHeader = ctx.request().headers().get(HttpHeaderNames.ACCEPT);

        return ctx.request().method().equals(HttpMethod.GET) && MediaType.parseMediaTypes(acceptHeader).contains(MEDIA_TEXT_EVENT_STREAM);
    }

    @Override
    public Completable handleRequest(final ExecutionContext ctx) {
        return Completable.complete();
    }

    @Override
    public Completable handleResponse(final ExecutionContext ctx) {
        return Completable.fromRunnable(
            () -> {
                // Set required sse headers.
                ctx.response().headers().add(HttpHeaderNames.CONTENT_TYPE, "text/event-stream;charset=UTF-8");
                ctx.response().headers().add(HttpHeaderNames.CONNECTION, "keep-alive");
                ctx.response().headers().add(HttpHeaderNames.CACHE_CONTROL, "no-cache");
                ctx.response().headers().add(HttpHeaderNames.TRANSFER_ENCODING, "chunked");

                // Assign the chunks that come from the transformation of messages.
                ctx.response().chunks(messagesToBuffers(ctx));
            }
        );
    }

    @Override
    public SseEntrypointConnector preStop() {
        emitStopMessage();
        return this;
    }

    private Flowable<Buffer> messagesToBuffers(ExecutionContext ctx) {
        final AtomicLong lastBufferTime = new AtomicLong(0);
        final Flowable<Buffer> messageBufferFlow = messageBufferFlow(ctx, lastBufferTime);
        final Flowable<Buffer> heartBeatFlow = heartBeatFlow(lastBufferTime);

        return heartBeatFlow.compose(RxHelper.mergeWithFirst(messageBufferFlow)).onErrorReturn(this::errorToBuffer);
    }

    private Flowable<Buffer> messageBufferFlow(ExecutionContext ctx, AtomicLong lastBufferTime) {
        return ctx
            .response()
            .messages()
            .compose(applyStopHook())
            .map(this::messageToBuffer)
            .startWithItem(Buffer.buffer(SseEvent.builder().retry(generateRandomRetry()).build().format()))
            .timestamp()
            .map(
                timed -> {
                    lastBufferTime.set(timed.time());
                    return timed.value();
                }
            );
    }

    private Flowable<Buffer> heartBeatFlow(AtomicLong lastBufferTime) {
        final int heartbeatIntervalInMs = configuration.getHeartbeatIntervalInMs();
        return Flowable
            .interval(heartbeatIntervalInMs, TimeUnit.MILLISECONDS)
            .timestamp()
            .flatMapMaybe(
                timed -> {
                    final long lastTime = lastBufferTime.get();
                    final long currentTime = timed.time();
                    if (currentTime - lastTime >= heartbeatIntervalInMs) {
                        lastBufferTime.set(currentTime);
                        return Maybe.just(Buffer.buffer(":\n\n"));
                    }
                    return Maybe.empty();
                }
            )
            .onBackpressureDrop();
    }

    private Buffer errorToBuffer(Throwable error) {
        DefaultMessage.DefaultMessageBuilder defaultMessageBuilder = DefaultMessage.builder().id(UUID.randomUUID().toString()).error(true);
        if (error.getMessage() != null) {
            defaultMessageBuilder.content(Buffer.buffer(error.getMessage().getBytes(StandardCharsets.UTF_8)));
        }
        return messageToBuffer(defaultMessageBuilder.build());
    }

    private Buffer messageToBuffer(Message message) {
        if (Objects.equals(STOP_MESSAGE_ID, message.id())) {
            return Buffer.buffer(SseEvent.builder().event("goaway").data(message.content().getBytes()).build().format());
        }

        HashMap<String, Object> comments = new HashMap<>();

        if (configuration.isHeadersAsComment() && message.headers() != null) {
            message.headers().toListValuesMap().forEach((key, value) -> comments.put(key, String.join(",", value)));
        }

        if (configuration.isMetadataAsComment() && message.metadata() != null) {
            comments.putAll(message.metadata());
        }
        SseEvent.SseEventBuilder sseEventBuilder = SseEvent.builder().id(message.id()).comments(comments);
        if (message.content() != null) {
            sseEventBuilder.data(message.content().getBytes());
        }
        if (message.error()) {
            sseEventBuilder.event("error");
        } else {
            sseEventBuilder.event("message");
        }

        return Buffer.buffer(sseEventBuilder.build().format());
    }

    private int generateRandomRetry() {
        return random.nextInt(RETRY_MAX_VALUE - RETRY_MIN_VALUE) + RETRY_MIN_VALUE;
    }
}

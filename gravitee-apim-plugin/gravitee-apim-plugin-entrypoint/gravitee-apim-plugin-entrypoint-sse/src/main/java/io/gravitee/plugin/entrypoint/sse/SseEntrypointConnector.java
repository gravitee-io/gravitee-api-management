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

import static io.gravitee.common.http.MediaType.TEXT_EVENT_STREAM;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.jupiter.api.ConnectorMode;
import io.gravitee.gateway.jupiter.api.ListenerType;
import io.gravitee.gateway.jupiter.api.connector.entrypoint.async.EntrypointAsyncConnector;
import io.gravitee.gateway.jupiter.api.context.ExecutionContext;
import io.gravitee.gateway.jupiter.api.context.MessageExecutionContext;
import io.gravitee.plugin.entrypoint.sse.configuration.SseEntrypointConnectorConfiguration;
import io.gravitee.plugin.entrypoint.sse.model.SseEvent;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class SseEntrypointConnector implements EntrypointAsyncConnector {

    public static final int RETRY_MIN_VALUE = 1000;
    public static final int RETRY_MAX_VALUE = 30000;
    static final Set<ConnectorMode> SUPPORTED_MODES = Set.of(ConnectorMode.SUBSCRIBE);
    private final Random random;
    private SseEntrypointConnectorConfiguration configuration;

    @SuppressWarnings("java:S2245")
    public SseEntrypointConnector(final SseEntrypointConnectorConfiguration configuration) {
        this.configuration = configuration;
        if (this.configuration == null) {
            this.configuration = new SseEntrypointConnectorConfiguration();
        }
        // Random doesn't require to be secured here and is only used for random retry time
        this.random = new Random();
    }

    @Override
    public ListenerType supportedListenerType() {
        return ListenerType.HTTP;
    }

    @Override
    public Set<ConnectorMode> supportedModes() {
        return SUPPORTED_MODES;
    }

    @Override
    public int matchCriteriaCount() {
        return 2;
    }

    @Override
    public boolean matches(final ExecutionContext ctx) {
        String acceptHeader = ctx.request().headers().get(HttpHeaderNames.ACCEPT);

        return (ctx.request().method().equals(HttpMethod.GET) && acceptHeader != null && acceptHeader.contains(TEXT_EVENT_STREAM));
    }

    @Override
    public Completable handleRequest(final ExecutionContext ctx) {
        return Completable.complete();
    }

    @Override
    public Completable handleResponse(final ExecutionContext ctx) {
        return Completable.defer(
            () -> {
                // set headers
                ctx.response().headers().add(HttpHeaderNames.CONTENT_TYPE, "text/event-stream;charset=UTF-8");
                ctx.response().headers().add(HttpHeaderNames.CONNECTION, "keep-alive");
                ctx.response().headers().add(HttpHeaderNames.CACHE_CONTROL, "no-cache");
                ctx.response().headers().add(HttpHeaderNames.TRANSFER_ENCODING, "chunked");

                return sendRetry(ctx)
                    .andThen(ctx.response().messages())
                    .flatMapSingle(
                        message -> {
                            HashMap<String, Object> comments = new HashMap<>();
                            if (configuration.isHeadersAsComment() && message.headers() != null) {
                                message.headers().toListValuesMap().forEach((key, value) -> comments.put(key, String.join(",", value)));
                            }
                            if (configuration.isMetadataAsComment() && message.metadata() != null) {
                                comments.putAll(message.metadata());
                            }
                            return ctx
                                .response()
                                .write(
                                    Buffer.buffer(
                                        SseEvent
                                            .builder()
                                            .id(UUID.randomUUID().toString())
                                            .event("message")
                                            .data(message.content().getBytes())
                                            .comments(comments)
                                            .build()
                                            .format()
                                    )
                                )
                                .andThen(ctx.response().write(Buffer.buffer("\n\n")))
                                .andThen(Single.just(message));
                        }
                    )
                    .onErrorResumeNext(
                        error -> {
                            log.error("Error when dealing with response messages", error);
                            return ctx
                                .response()
                                .end(
                                    Buffer.buffer(
                                        SseEvent
                                            .builder()
                                            .id(UUID.randomUUID().toString())
                                            .event("error")
                                            .data(error.getMessage().getBytes(StandardCharsets.UTF_8))
                                            .build()
                                            .format()
                                    )
                                )
                                .andThen(Flowable.error(error));
                        }
                    )
                    .ignoreElements()
                    .andThen(ctx.response().end());
            }
        );
    }

    private Completable sendRetry(final MessageExecutionContext ctx) {
        return ctx
            .response()
            .write(Buffer.buffer(SseEvent.builder().retry(generateRandomRetry()).build().format()))
            .andThen(ctx.response().write(Buffer.buffer("\n\n")))
            .andThen(ctx.response().writeHeaders());
    }

    private int generateRandomRetry() {
        return random.nextInt(RETRY_MAX_VALUE - RETRY_MIN_VALUE) + RETRY_MIN_VALUE;
    }
}

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
import io.gravitee.gateway.jupiter.api.context.MessageExecutionContext;
import io.gravitee.gateway.jupiter.api.entrypoint.async.EntrypointAsyncConnector;
import io.gravitee.plugin.entrypoint.sse.model.SseEvent;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@NoArgsConstructor
@Slf4j
public class SseEntrypointConnector implements EntrypointAsyncConnector {

    @Override
    public int matchesCriteria() {
        return 2;
    }

    @Override
    public boolean matches(final MessageExecutionContext executionContext) {
        String contentTypeHeader = executionContext.request().headers().get(HttpHeaderNames.CONTENT_TYPE);

        return (
            executionContext.request().method().equals(HttpMethod.GET) &&
            contentTypeHeader != null &&
            contentTypeHeader.contains(TEXT_EVENT_STREAM)
        );
    }

    @Override
    public Completable handleRequest(final MessageExecutionContext executionContext) {
        return Completable.complete();
    }

    @Override
    public Completable handleResponse(final MessageExecutionContext executionContext) {
        return Completable
            .fromRunnable(
                () -> {
                    // set headers
                    executionContext.response().headers().add(HttpHeaderNames.CONTENT_TYPE, "text/event-stream;charset=UTF-8");
                    executionContext.response().headers().add(HttpHeaderNames.CONNECTION, "keep-alive");
                    executionContext.response().headers().add(HttpHeaderNames.CACHE_CONTROL, "no-cache");
                }
            )
            .andThen(
                executionContext
                    .response()
                    .messages()
                    .doOnNext(message -> log.debug(String.format("Transform message to sse event %s", message)))
                    .flatMapSingle(
                        message ->
                            executionContext
                                .response()
                                .write(
                                    Buffer.buffer(
                                        SseEvent
                                            .builder()
                                            .id(UUID.randomUUID().toString())
                                            .event("message")
                                            .data(message.content().getBytes())
                                            .comments(message.metadata())
                                            .build()
                                            .format()
                                    )
                                )
                                .andThen(executionContext.response().write(Buffer.buffer("\n\n")))
                                .andThen(Single.just(message))
                    )
                    .onErrorResumeNext(
                        error -> {
                            log.error("Error when dealing with response messages", error);
                            return executionContext
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
                    .andThen(executionContext.response().end())
            );
    }
}

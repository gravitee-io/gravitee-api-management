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
package io.gravitee.plugin.entrypoint.http.post;

import static io.gravitee.common.http.HttpStatusCode.INTERNAL_SERVER_ERROR_500;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.utils.RxHelper;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.jupiter.api.ConnectorMode;
import io.gravitee.gateway.jupiter.api.ExecutionFailure;
import io.gravitee.gateway.jupiter.api.ListenerType;
import io.gravitee.gateway.jupiter.api.connector.entrypoint.async.EntrypointAsyncConnector;
import io.gravitee.gateway.jupiter.api.context.ExecutionContext;
import io.gravitee.gateway.jupiter.api.message.DefaultMessage;
import io.gravitee.gateway.jupiter.api.message.Message;
import io.gravitee.gateway.jupiter.api.qos.Qos;
import io.gravitee.gateway.jupiter.api.qos.QosOptions;
import io.gravitee.plugin.entrypoint.http.post.configuration.HttpPostEntrypointConnectorConfiguration;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class HttpPostEntrypointConnector extends EntrypointAsyncConnector {

    static final Set<ConnectorMode> SUPPORTED_MODES = Set.of(ConnectorMode.PUBLISH);
    static final Set<Qos> SUPPORTED_QOS = Set.of(Qos.NA);
    private static final String ENTRYPOINT_ID = "http-post";
    protected static final String STOPPING_MESSAGE = "Stopping, please reconnect";
    private final QosOptions qosOptions;
    private HttpPostEntrypointConnectorConfiguration configuration;

    public HttpPostEntrypointConnector(final HttpPostEntrypointConnectorConfiguration configuration) {
        this.qosOptions = QosOptions.builder().qos(Qos.NA).errorRecoverySupported(false).manualAckSupported(false).build();
        this.configuration = configuration;
        if (this.configuration == null) {
            this.configuration = new HttpPostEntrypointConnectorConfiguration();
        }
    }

    @Override
    public String id() {
        return ENTRYPOINT_ID;
    }

    @Override
    public Set<ConnectorMode> supportedModes() {
        return SUPPORTED_MODES;
    }

    @Override
    public ListenerType supportedListenerType() {
        return ListenerType.HTTP;
    }

    @Override
    public Set<Qos> supportedQos() {
        return SUPPORTED_QOS;
    }

    @Override
    public int matchCriteriaCount() {
        return 1;
    }

    @Override
    public boolean matches(final ExecutionContext ctx) {
        return (HttpMethod.POST == ctx.request().method());
    }

    @Override
    public QosOptions qosOptions() {
        return qosOptions;
    }

    @Override
    public Completable handleRequest(final ExecutionContext ctx) {
        return Completable.fromRunnable(
            () ->
                ctx
                    .request()
                    .messages(
                        ctx
                            .request()
                            .body()
                            .<Message>map(
                                buffer -> {
                                    DefaultMessage.DefaultMessageBuilder messageBuilder = DefaultMessage.builder().content(buffer);
                                    if (configuration.isRequestHeadersToMessage()) {
                                        messageBuilder.headers(HttpHeaders.create(ctx.request().headers()));
                                    }
                                    return messageBuilder.build();
                                }
                            )
                            .toFlowable()
                            .compose(
                                RxHelper.mergeWithFirst(
                                    stopHook.flatMap(
                                        message ->
                                            ctx.interruptMessagesWith(
                                                new ExecutionFailure(INTERNAL_SERVER_ERROR_500).message(STOPPING_MESSAGE)
                                            )
                                    )
                                )
                            )
                    )
        );
    }

    @Override
    public Completable handleResponse(final ExecutionContext ctx) {
        // Start consuming incoming messages
        return Completable.defer(
            () ->
                Completable.mergeArray(
                    ctx.request().messages().ignoreElements(),
                    Completable.fromRunnable(() -> ctx.response().chunks(processResponseMessages(ctx)))
                )
        );
    }

    @Override
    public void doStop() {
        emitStopMessage();
    }

    private Flowable<Buffer> processResponseMessages(final ExecutionContext ctx) {
        return ctx
            .response()
            .messages()
            .compose(applyStopHook())
            .filter(Message::error)
            .map(
                message -> {
                    Integer statusCode = (Integer) message.metadata().getOrDefault("statusCode", INTERNAL_SERVER_ERROR_500);
                    ctx.response().status(statusCode);
                    HttpResponseStatus httpResponseStatus = HttpResponseStatus.valueOf(statusCode);
                    if (httpResponseStatus != null) {
                        ctx.response().reason(httpResponseStatus.reasonPhrase());
                    }
                    Buffer content = message.content();
                    if (content != null) {
                        if (message.headers() != null) {
                            if (message.headers().contains(HttpHeaderNames.CONTENT_TYPE)) {
                                ctx
                                    .response()
                                    .headers()
                                    .set(HttpHeaderNames.CONTENT_TYPE, message.headers().get(HttpHeaderNames.CONTENT_TYPE));
                            }
                            if (
                                (!ctx.response().headers().contains(HttpHeaderNames.TRANSFER_ENCODING)) &&
                                message.headers().contains(HttpHeaderNames.CONTENT_LENGTH)
                            ) {
                                ctx
                                    .response()
                                    .headers()
                                    .set(HttpHeaderNames.CONTENT_LENGTH, message.headers().get(HttpHeaderNames.CONTENT_LENGTH));
                            }
                        }
                        return content;
                    }
                    return Buffer.buffer();
                }
            )
            .switchIfEmpty(
                Flowable.defer(
                    () -> {
                        ctx.response().status(HttpResponseStatus.ACCEPTED.code());
                        ctx.response().reason(HttpResponseStatus.ACCEPTED.reasonPhrase());
                        return Flowable.empty();
                    }
                )
            );
    }
}

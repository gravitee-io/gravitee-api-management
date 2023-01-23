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

import static io.gravitee.common.http.HttpHeadersValues.CONNECTION_CLOSE;
import static io.gravitee.common.http.HttpHeadersValues.CONNECTION_GO_AWAY;
import static io.gravitee.common.http.HttpStatusCode.SERVICE_UNAVAILABLE_503;
import static io.gravitee.gateway.api.http.HttpHeaderNames.CONNECTION;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.http.HttpVersion;
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
import io.gravitee.gateway.jupiter.api.qos.QosRequirement;
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
    static final Set<Qos> SUPPORTED_QOS = Set.of(Qos.NONE, Qos.AUTO);
    static final ListenerType SUPPORTED_LISTENER_TYPE = ListenerType.HTTP;
    private static final String ENTRYPOINT_ID = "http-post";
    private final QosRequirement qosRequirement;
    private HttpPostEntrypointConnectorConfiguration configuration;

    public HttpPostEntrypointConnector(final Qos qos, final HttpPostEntrypointConnectorConfiguration configuration) {
        this.qosRequirement = QosRequirement.builder().qos(qos).build();
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
        return SUPPORTED_LISTENER_TYPE;
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
    public QosRequirement qosRequirement() {
        return qosRequirement;
    }

    @Override
    public Completable handleRequest(final ExecutionContext ctx) {
        return Completable.fromRunnable(
            () ->
                ctx
                    .request()
                    .messages(
                        stopHook
                            .flatMap(message -> interruptWithStopMessage(ctx, message))
                            .compose(
                                RxHelper.mergeWithFirst(
                                    ctx
                                        .request()
                                        .body()
                                        .<Message>map(
                                            buffer -> {
                                                DefaultMessage.DefaultMessageBuilder messageBuilder = DefaultMessage
                                                    .builder()
                                                    .content(buffer);
                                                if (configuration.isRequestHeadersToMessage()) {
                                                    messageBuilder.headers(HttpHeaders.create(ctx.request().headers()));
                                                }
                                                return messageBuilder.build();
                                            }
                                        )
                                        .toFlowable()
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

    private Flowable<Message> interruptWithStopMessage(ExecutionContext ctx, Message message) {
        if (ctx.request().version() == HttpVersion.HTTP_2) {
            ctx.response().headers().set(CONNECTION, CONNECTION_GO_AWAY);
        } else {
            ctx.response().headers().set(CONNECTION, CONNECTION_CLOSE);
        }

        return ctx.interruptMessagesWith(
            new ExecutionFailure(SERVICE_UNAVAILABLE_503).message(message.content().toString()).key(message.id())
        );
    }

    private Flowable<Buffer> processResponseMessages(final ExecutionContext ctx) {
        return stopHook
            .flatMap(message -> interruptWithStopMessage(ctx, message))
            .compose(RxHelper.mergeWithFirst(ctx.response().messages().filter(Message::error)))
            .map(
                message -> {
                    final Integer statusCode = (Integer) message.metadata().getOrDefault("statusCode", SERVICE_UNAVAILABLE_503);
                    final HttpResponseStatus httpResponseStatus = HttpResponseStatus.valueOf(statusCode);

                    ctx.response().status(statusCode);

                    if (httpResponseStatus != null) {
                        ctx.response().reason(httpResponseStatus.reasonPhrase());
                    }

                    final Buffer content = message.content();

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

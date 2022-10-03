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
package io.gravitee.plugin.entrypoint.websocket;

import static io.gravitee.plugin.entrypoint.websocket.WebSocketCloseStatus.*;

import io.gravitee.gateway.jupiter.api.ConnectorMode;
import io.gravitee.gateway.jupiter.api.ListenerType;
import io.gravitee.gateway.jupiter.api.connector.entrypoint.async.EntrypointAsyncConnector;
import io.gravitee.gateway.jupiter.api.context.ExecutionContext;
import io.gravitee.gateway.jupiter.api.message.DefaultMessage;
import io.gravitee.gateway.jupiter.api.message.Message;
import io.gravitee.gateway.jupiter.api.ws.WebSocket;
import io.gravitee.plugin.entrypoint.websocket.configuration.WebSocketEntrypointConnectorConfiguration;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import java.util.Objects;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class WebSocketEntrypointConnector extends EntrypointAsyncConnector {

    static final Set<ConnectorMode> SUPPORTED_MODES = Set.of(ConnectorMode.PUBLISH, ConnectorMode.SUBSCRIBE);
    private static final String ENTRYPOINT_ID = "websocket";
    protected final WebSocketEntrypointConnectorConfiguration configuration;
    private WebSocketCloseStatus closeStatus;

    public WebSocketEntrypointConnector(final WebSocketEntrypointConnectorConfiguration configuration) {
        this.configuration = configuration;
        this.closeStatus = NORMAL_CLOSURE;
    }

    @Override
    public String id() {
        return ENTRYPOINT_ID;
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
        // 4 criteria: http 1.x version, GET method, Connection Upgrade, Upgrade websocket.
        return 4;
    }

    @Override
    public boolean matches(final ExecutionContext ctx) {
        return ctx.request().isWebSocket();
    }

    @Override
    public Completable handleRequest(final ExecutionContext ctx) {
        return Completable.defer(
            () ->
                ctx
                    .request()
                    .webSocket()
                    .upgrade()
                    .doOnSuccess(webSocket -> ctx.request().messages(prepareRequestMessages(webSocket)))
                    .ignoreElement()
        );
    }

    @Override
    public Completable handleResponse(final ExecutionContext ctx) {
        return Completable
            .defer(() -> Completable.mergeArray(ctx.request().messages().ignoreElements(), processResponseMessages(ctx)))
            .andThen(Completable.defer(() -> close(ctx, NORMAL_CLOSURE)))
            .onErrorResumeNext(t -> close(ctx, t));
    }

    @Override
    public EntrypointAsyncConnector preStop() {
        emitStopMessage();
        return this;
    }

    private Flowable<Message> prepareRequestMessages(WebSocket webSocket) {
        if (configuration.getPublisher().isEnabled()) {
            return webSocket.read().map(buffer -> new DefaultMessage().content(buffer)).compose(applyStopHook());
        } else {
            return Flowable.empty();
        }
    }

    private Completable processResponseMessages(ExecutionContext ctx) {
        if (configuration.getSubscriber().isEnabled()) {
            return ctx
                .response()
                .messages()
                .compose(applyStopHook())
                .flatMapCompletable(
                    message -> {
                        if (!message.error()) {
                            return ctx.request().webSocket().write(message.content());
                        } else {
                            if (Objects.equals(STOP_MESSAGE_ID, message.id())) {
                                return Completable.error(new WebSocketException(TRY_AGAIN_LATER, TRY_AGAIN_LATER.reasonText()));
                            }
                            return Completable.error(new WebSocketException(SERVER_ERROR, message.content().toString()));
                        }
                    }
                );
        } else {
            return Completable.complete();
        }
    }

    private Completable close(ExecutionContext ctx, Throwable e) {
        if (e instanceof WebSocketException) {
            return close(ctx, ((WebSocketException) e).getCloseStatus());
        } else {
            return close(ctx, SERVER_ERROR);
        }
    }

    private Completable close(ExecutionContext ctx, WebSocketCloseStatus closeStatus) {
        return ctx.request().webSocket().close(closeStatus.code(), closeStatus.reasonText());
    }
}

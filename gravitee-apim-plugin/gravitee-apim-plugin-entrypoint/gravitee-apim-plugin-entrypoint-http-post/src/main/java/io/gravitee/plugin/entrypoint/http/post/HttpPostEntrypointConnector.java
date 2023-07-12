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
package io.gravitee.plugin.entrypoint.http.post;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.jupiter.api.ApiType;
import io.gravitee.gateway.jupiter.api.ConnectorMode;
import io.gravitee.gateway.jupiter.api.ListenerType;
import io.gravitee.gateway.jupiter.api.connector.entrypoint.async.EntrypointAsyncConnector;
import io.gravitee.gateway.jupiter.api.context.ExecutionContext;
import io.gravitee.gateway.jupiter.api.context.MessageExecutionContext;
import io.gravitee.gateway.jupiter.api.message.DefaultMessage;
import io.gravitee.gateway.jupiter.api.message.Message;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import java.util.Map;
import java.util.Set;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@NoArgsConstructor
@Slf4j
public class HttpPostEntrypointConnector implements EntrypointAsyncConnector {

    static final Set<ConnectorMode> SUPPORTED_MODES = Set.of(ConnectorMode.PUBLISH);

    @Override
    public Set<ConnectorMode> supportedModes() {
        return SUPPORTED_MODES;
    }

    @Override
    public ListenerType supportedListenerType() {
        return ListenerType.HTTP;
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
    public Completable handleRequest(final ExecutionContext ctx) {
        return Completable.fromRunnable(() ->
            ctx
                .request()
                .messages(
                    ctx
                        .request()
                        .body()
                        .<Message>map(buffer ->
                            DefaultMessage.builder().headers(HttpHeaders.create(ctx.request().headers())).content(buffer).build()
                        )
                        .toFlowable()
                )
        );
    }

    @Override
    public Completable handleResponse(final ExecutionContext ctx) {
        return Completable.defer(() -> ctx.response().end());
    }
}

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
package io.gravitee.gateway.jupiter.debug.reactor;

import io.gravitee.common.event.EventManager;
import io.gravitee.common.http.IdGenerator;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.debug.vertx.VertxHttpServerRequestDebugDecorator;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.env.HttpRequestTimeoutConfiguration;
import io.gravitee.gateway.jupiter.debug.reactor.context.DebugRequestExecutionContext;
import io.gravitee.gateway.jupiter.debug.vertx.TimeoutServerResponseDebugDecorator;
import io.gravitee.gateway.jupiter.http.vertx.VertxHttpServerRequest;
import io.gravitee.gateway.jupiter.reactor.DefaultHttpRequestDispatcher;
import io.gravitee.gateway.jupiter.reactor.handler.EntrypointResolver;
import io.gravitee.gateway.jupiter.reactor.handler.context.DefaultRequestExecutionContext;
import io.gravitee.gateway.jupiter.reactor.processor.NotFoundProcessorChainFactory;
import io.gravitee.gateway.jupiter.reactor.processor.PlatformProcessorChainFactory;
import io.gravitee.gateway.reactor.handler.ReactorHandlerRegistry;
import io.gravitee.gateway.reactor.processor.RequestProcessorChainFactory;
import io.gravitee.gateway.reactor.processor.ResponseProcessorChainFactory;
import io.vertx.core.Vertx;
import io.vertx.reactivex.core.http.HttpServerRequest;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DebugHttpRequestDispatcher extends DefaultHttpRequestDispatcher {

    public DebugHttpRequestDispatcher(
        GatewayConfiguration gatewayConfiguration,
        EntrypointResolver entrypointResolver,
        IdGenerator idGenerator,
        ComponentProvider globalComponentProvider,
        RequestProcessorChainFactory requestProcessorChainFactory,
        ResponseProcessorChainFactory responseProcessorChainFactory,
        PlatformProcessorChainFactory platformProcessorChainFactory,
        NotFoundProcessorChainFactory notFoundProcessorChainFactory,
        boolean tracingEnabled,
        HttpRequestTimeoutConfiguration httpRequestTimeoutConfiguration,
        Vertx vertx
    ) {
        super(
            gatewayConfiguration,
            entrypointResolver,
            idGenerator,
            globalComponentProvider,
            requestProcessorChainFactory,
            responseProcessorChainFactory,
            platformProcessorChainFactory,
            notFoundProcessorChainFactory,
            tracingEnabled,
            httpRequestTimeoutConfiguration,
            vertx
        );
    }

    @Override
    protected DefaultRequestExecutionContext createExecutionContext(VertxHttpServerRequest request) {
        return new DebugRequestExecutionContext(request, request.response());
    }

    @Override
    protected io.gravitee.gateway.http.vertx.VertxHttpServerRequest createV3Request(
        final HttpServerRequest httpServerRequest,
        final IdGenerator idGenerator
    ) {
        io.gravitee.gateway.http.vertx.VertxHttpServerRequest v3Request = super.createV3Request(httpServerRequest, idGenerator);
        return new VertxHttpServerRequestDebugDecorator(v3Request, idGenerator);
    }

    @Override
    protected Response createV3TimeoutResponse(Vertx vertx, io.gravitee.gateway.http.vertx.VertxHttpServerRequest request, long timeoutId) {
        return new TimeoutServerResponseDebugDecorator(vertx, request.create(), timeoutId);
    }
}

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
package io.gravitee.gateway.reactive.debug.reactor;

import io.gravitee.common.http.IdGenerator;
import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.debug.vertx.VertxHttpServerRequestDebugDecorator;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.env.RequestClientAuthConfiguration;
import io.gravitee.gateway.env.RequestTimeoutConfiguration;
import io.gravitee.gateway.reactive.core.context.DefaultExecutionContext;
import io.gravitee.gateway.reactive.debug.reactor.context.DebugExecutionContext;
import io.gravitee.gateway.reactive.debug.reactor.processor.DebugPlatformProcessorChainFactory;
import io.gravitee.gateway.reactive.http.vertx.VertxHttpServerRequest;
import io.gravitee.gateway.reactive.reactor.DefaultHttpRequestDispatcher;
import io.gravitee.gateway.reactive.reactor.handler.HttpAcceptorResolver;
import io.gravitee.gateway.reactive.reactor.processor.NotFoundProcessorChainFactory;
import io.gravitee.gateway.reactor.processor.RequestProcessorChainFactory;
import io.gravitee.gateway.reactor.processor.ResponseProcessorChainFactory;
import io.vertx.core.Vertx;
import io.vertx.rxjava3.core.http.HttpServerRequest;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DebugHttpRequestDispatcher extends DefaultHttpRequestDispatcher {

    public DebugHttpRequestDispatcher(
        GatewayConfiguration gatewayConfiguration,
        HttpAcceptorResolver httpAcceptorResolver,
        IdGenerator idGenerator,
        ComponentProvider globalComponentProvider,
        RequestProcessorChainFactory requestProcessorChainFactory,
        ResponseProcessorChainFactory responseProcessorChainFactory,
        DebugPlatformProcessorChainFactory platformProcessorChainFactory,
        NotFoundProcessorChainFactory notFoundProcessorChainFactory,
        boolean tracingEnabled,
        RequestTimeoutConfiguration requestTimeoutConfiguration,
        RequestClientAuthConfiguration requestClientAuthConfiguration,
        Vertx vertx
    ) {
        super(
            gatewayConfiguration,
            httpAcceptorResolver,
            idGenerator,
            globalComponentProvider,
            requestProcessorChainFactory,
            responseProcessorChainFactory,
            platformProcessorChainFactory,
            notFoundProcessorChainFactory,
            tracingEnabled,
            requestTimeoutConfiguration,
            requestClientAuthConfiguration,
            vertx
        );
    }

    @Override
    protected DefaultExecutionContext createExecutionContext(VertxHttpServerRequest request) {
        return new DebugExecutionContext(request, request.response());
    }

    @Override
    protected io.gravitee.gateway.http.vertx.VertxHttpServerRequest createV3Request(
        final HttpServerRequest httpServerRequest,
        final IdGenerator idGenerator
    ) {
        io.gravitee.gateway.http.vertx.VertxHttpServerRequest v3Request = super.createV3Request(httpServerRequest, idGenerator);
        return new VertxHttpServerRequestDebugDecorator(v3Request, idGenerator);
    }
}

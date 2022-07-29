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
package io.gravitee.gateway.reactor.impl;

import io.gravitee.common.event.EventManager;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.context.SimpleExecutionContext;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.reactor.Reactor;
import io.gravitee.gateway.reactor.handler.AcceptorResolver;
import io.gravitee.gateway.reactor.handler.HttpAcceptorHandler;
import io.gravitee.gateway.reactor.handler.ReactorHandlerRegistry;
import io.gravitee.gateway.reactor.processor.NotFoundProcessorChainFactory;
import io.gravitee.gateway.reactor.processor.RequestProcessorChainFactory;
import io.gravitee.gateway.reactor.processor.ResponseProcessorChainFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DefaultReactor implements Reactor {

    private final Logger LOGGER = LoggerFactory.getLogger(DefaultReactor.class);

    private final EventManager eventManager;
    private final ReactorHandlerRegistry reactorHandlerRegistry;
    private final GatewayConfiguration gatewayConfiguration;
    private final RequestProcessorChainFactory requestProcessorChainFactory;
    private final ResponseProcessorChainFactory responseProcessorChainFactory;
    private final NotFoundProcessorChainFactory notFoundProcessorChainFactory;
    private final AcceptorResolver acceptorResolver;

    public DefaultReactor(
        final EventManager eventManager,
        final @Qualifier("v3EntrypointResolver") AcceptorResolver acceptorResolver,
        final @Qualifier("reactorHandlerRegistry") ReactorHandlerRegistry reactorHandlerRegistry,
        final GatewayConfiguration gatewayConfiguration,
        final @Qualifier("v3RequestProcessorChainFactory") RequestProcessorChainFactory requestProcessorChainFactory,
        final @Qualifier("v3ResponseProcessorChainFactory") ResponseProcessorChainFactory responseProcessorChainFactory,
        final @Qualifier("v3NotFoundProcessorChainFactory") NotFoundProcessorChainFactory notFoundProcessorChainFactory
    ) {
        this.eventManager = eventManager;
        this.acceptorResolver = acceptorResolver;
        this.reactorHandlerRegistry = reactorHandlerRegistry;
        this.gatewayConfiguration = gatewayConfiguration;
        this.requestProcessorChainFactory = requestProcessorChainFactory;
        this.responseProcessorChainFactory = responseProcessorChainFactory;
        this.notFoundProcessorChainFactory = notFoundProcessorChainFactory;
    }

    @Override
    public void route(Request serverRequest, Response serverResponse, Handler<ExecutionContext> handler) {
        LOGGER.debug("Receiving a request {} for path {}", serverRequest.id(), serverRequest.path());

        // Prepare invocation execution context
        ExecutionContext context = new SimpleExecutionContext(serverRequest, serverResponse);

        // Set gateway tenant
        gatewayConfiguration.tenant().ifPresent(tenant -> serverRequest.metrics().setTenant(tenant));

        // Set gateway zone
        gatewayConfiguration.zone().ifPresent(zone -> serverRequest.metrics().setZone(zone));

        // Prepare handler chain
        requestProcessorChainFactory
            .create()
            .handler(
                ctx -> {
                    HttpAcceptorHandler entrypoint = acceptorResolver.resolve(ctx);

                    if (entrypoint != null) {
                        entrypoint
                            .target()
                            .handle(
                                ctx,
                                context1 -> {
                                    // Ensure that response has been ended before going further
                                    context1.response().endHandler(avoid -> processResponse(context1, handler)).end();
                                }
                            );
                    } else {
                        processNotFound(ctx, handler);
                    }
                }
            )
            .errorHandler(__ -> processResponse(context, handler))
            .exitHandler(__ -> processResponse(context, handler))
            .handle(context);
    }

    private void processNotFound(ExecutionContext context, Handler<ExecutionContext> handler) {
        notFoundProcessorChainFactory.create().handler(handler).handle(context);
    }

    private void processResponse(ExecutionContext context, Handler<ExecutionContext> handler) {
        responseProcessorChainFactory.create().handler(handler).handle(context);
    }
}

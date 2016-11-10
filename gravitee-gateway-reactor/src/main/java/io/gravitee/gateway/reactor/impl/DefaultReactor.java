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

import io.gravitee.common.event.Event;
import io.gravitee.common.event.EventListener;
import io.gravitee.common.event.EventManager;
import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpHeadersValues;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.common.service.AbstractService;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.reactor.Reactable;
import io.gravitee.gateway.reactor.Reactor;
import io.gravitee.gateway.reactor.ReactorEvent;
import io.gravitee.gateway.reactor.handler.ReactorHandler;
import io.gravitee.gateway.reactor.handler.ReactorHandlerRegistry;
import io.gravitee.gateway.reactor.handler.ReactorHandlerResolver;
import io.gravitee.gateway.reactor.handler.ResponseTimeHandler;
import io.gravitee.gateway.reactor.handler.reporter.ReporterHandler;
import io.gravitee.gateway.reactor.handler.transaction.TransactionHandlerFactory;
import io.gravitee.gateway.report.ReporterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

/**
 * @author David BRASSELY (david at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DefaultReactor extends AbstractService implements
        Reactor, EventListener<ReactorEvent, Reactable> {

    private final Logger LOGGER = LoggerFactory.getLogger(DefaultReactor.class);

    @Autowired
    private EventManager eventManager;

    @Autowired
    private Environment environment;

    @Autowired
    private ReactorHandlerRegistry reactorHandlerRegistry;

    @Autowired
    private ReactorHandlerResolver reactorHandlerResolver;

    @Autowired
    private ReporterService reporterService;

    @Autowired
    private TransactionHandlerFactory transactionHandlerFactory;

    @Override
    public void route(Request serverRequest, Response serverResponse, final Handler<Response> handler) {
        LOGGER.debug("Receiving a request {} for path {}", serverRequest.id(), serverRequest.path());

        // Prepare handler chain
        Handler<Request> requestHandlerChain = transactionHandlerFactory.create(request -> {
            ReactorHandler reactorHandler = reactorHandlerResolver.resolve(request);
            if (reactorHandler != null) {
                // Prepare the handler chain
                Handler<Response> responseHandlerChain = new ResponseTimeHandler(request,
                        new ReporterHandler(reporterService, request, handler));

                reactorHandler.handle(request, serverResponse, responseHandlerChain);
            } else {
                LOGGER.debug("No handler can be found for request {}, returning NOT_FOUND (404)", request.path());
                sendNotFound(serverResponse, handler);
            }
        });

        // And handle the request
        requestHandlerChain.handle(serverRequest);
    }

    private void sendNotFound(Response serverResponse, Handler<Response> handler) {
        // Send a NOT_FOUND HTTP status code (404)
        serverResponse.status(HttpStatusCode.NOT_FOUND_404);

        String message = environment.getProperty("http.errors[404].message", "No context-path matches the request URI.");
        serverResponse.headers().set(HttpHeaders.CONTENT_LENGTH, Integer.toString(message.length()));
        serverResponse.headers().set(HttpHeaders.CONTENT_TYPE, "text/plain");
        serverResponse.headers().set(HttpHeaders.CONNECTION, HttpHeadersValues.CONNECTION_CLOSE);
        serverResponse.write(Buffer.buffer(message));

        serverResponse.end();
        handler.handle(serverResponse);
    }

    @Override
    public void onEvent(Event<ReactorEvent, Reactable> event) {
        switch (event.type()) {
            case DEPLOY:
                reactorHandlerRegistry.create(event.content());
                break;
            case UPDATE:
                reactorHandlerRegistry.update(event.content());
                break;
            case UNDEPLOY:
                reactorHandlerRegistry.remove(event.content());
                break;
        }
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();

        eventManager.subscribeForEvents(this, ReactorEvent.class);
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();

        reactorHandlerRegistry.clear();
    }
}

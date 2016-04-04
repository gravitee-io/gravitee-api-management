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
package io.gravitee.gateway.core.reactor;

import io.gravitee.common.event.Event;
import io.gravitee.common.event.EventListener;
import io.gravitee.common.event.EventManager;
import io.gravitee.common.service.AbstractService;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.core.Reactor;
import io.gravitee.gateway.core.definition.Api;
import io.gravitee.gateway.core.event.ApiEvent;
import io.gravitee.gateway.core.reactor.handler.ReactorHandler;
import io.gravitee.gateway.core.reactor.handler.ReactorHandlerRegistry;
import io.gravitee.gateway.core.reactor.handler.ReactorHandlerResolver;
import io.gravitee.gateway.core.reporter.ReporterService;
import io.gravitee.gateway.core.service.ServiceManager;
import io.gravitee.plugin.core.api.PluginRegistry;
import io.gravitee.plugin.core.internal.PluginEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.concurrent.CompletableFuture;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class GraviteeReactor extends AbstractService implements
        Reactor, EventListener<ApiEvent, Api> {

    private final Logger LOGGER = LoggerFactory.getLogger(GraviteeReactor.class);

    @Autowired
    private EventManager eventManager;

    @Autowired
    @Qualifier("notFoundHandler")
    private ReactorHandler notFoundHandler;

    @Autowired
    private ReactorHandlerRegistry reactorHandlerRegistry;

    @Autowired
    private ReactorHandlerResolver reactorHandlerResolver;

    @Autowired
    private ReporterService reporterService;

    public CompletableFuture<Response> process(Request serverRequest, Response serverResponse) {
        LOGGER.debug("Receiving a request {} for path {}", serverRequest.id(), serverRequest.path());

        ReactorHandler reactorHandler = reactorHandlerResolver.resolve(serverRequest);
        reactorHandler = (reactorHandler != null) ? reactorHandler : notFoundHandler;

        // Prepare the handler chain
        //TODO: how to handle this with CompletableFuture ?
        /*
        handler = new ResponseTimeHandler(request,
                new ReporterHandler(reporterService, request, handler));
                */

        return reactorHandler.handle(serverRequest, serverResponse);
    }

    @Override
    public void onEvent(Event<ApiEvent, Api> event) {
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

    public void setReactorHandlerRegistry(ReactorHandlerRegistry reactorHandlerRegistry) {
        this.reactorHandlerRegistry = reactorHandlerRegistry;
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();

        applicationContext.getBean(PluginEventListener.class).start();
        applicationContext.getBean(PluginRegistry.class).start();

        eventManager.subscribeForEvents(this, ApiEvent.class);

        applicationContext.getBean(ServiceManager.class).start();
        applicationContext.getBean(ReporterService.class).start();
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();

        applicationContext.getBean(PluginRegistry.class).stop();
        applicationContext.getBean(PluginEventListener.class).stop();

        reactorHandlerRegistry.clear();

        applicationContext.getBean(ServiceManager.class).stop();
        applicationContext.getBean(ReporterService.class).stop();
    }
}

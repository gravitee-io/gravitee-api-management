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
package io.gravitee.gateway.reactor.handler.impl;

import io.gravitee.definition.model.ExecutionMode;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.reactor.Reactable;
import io.gravitee.gateway.reactor.handler.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DefaultReactorHandlerRegistry implements ReactorHandlerRegistry {

    private final Logger logger = LoggerFactory.getLogger(DefaultReactorHandlerRegistry.class);

    private final ReactorHandlerFactoryManager handlerFactoryManager;

    private final Map<Reactable, ReactableHttpAcceptors> handlers = new ConcurrentHashMap<>();
    private final HttpAcceptorHandlerComparator entryPointComparator = new HttpAcceptorHandlerComparator();
    private List<HttpAcceptorHandler> registeredEntrypoints = new ArrayList<>();

    public DefaultReactorHandlerRegistry(ReactorHandlerFactoryManager handlerFactoryManager) {
        this.handlerFactoryManager = handlerFactoryManager;
    }

    @Override
    public void create(Reactable reactable) {
        logger.debug("Creating a new handler for {}", reactable);

        ReactorHandler handler = prepare(reactable);
        if (handler != null) {
            register(handler);
        }
    }

    private void register(ReactorHandler handler) {
        logger.debug("Registering a new handler: {}", handler);

        // Associate the handler to the http acceptor
        List<HttpAcceptorHandler> httpAcceptorHandlers = handler
            .reactable()
            .httpAcceptors()
            .stream()
            .map((Function<HttpAcceptor, HttpAcceptorHandler>) httpAcceptor -> new DefaultHttpAcceptorHandler(handler, httpAcceptor))
            .collect(Collectors.toList());

        handlers.put(handler.reactable(), new ReactableHttpAcceptors(handler, httpAcceptorHandlers));
        addHttpAcceptors(httpAcceptorHandlers);
    }

    private ReactorHandler prepare(Reactable reactable) {
        logger.debug("Preparing a new handler for: {}", reactable);
        ReactorHandler handler = handlerFactoryManager.create(reactable);
        if (handler != null) {
            try {
                handler.start();
            } catch (Exception ex) {
                logger.error("Unable to register handler: " + handler, ex);
                return null;
            }
        }

        return handler;
    }

    @Override
    public void update(Reactable reactable) {
        logger.debug("Updating handler for: {}", reactable);

        ReactableHttpAcceptors reactableHttpAcceptors = handlers.get(reactable);

        if (reactableHttpAcceptors != null) {
            ReactorHandler currentHandler = reactableHttpAcceptors.handler;
            logger.debug("Handler is already deployed: {}", currentHandler);

            ReactorHandler newHandler = prepare(reactable);

            // Do not update handler if the new is not correctly initialized
            if (newHandler != null) {
                ReactableHttpAcceptors previousHandler = handlers.remove(reactable);

                // Register the new handler before removing the previous http acceptor to avoid 404, especially on high throughput.
                register(newHandler);
                removeEntrypoints(previousHandler.httpAcceptorHandlers);

                try {
                    logger.debug("Stopping previous handler for: {}", reactable);
                    previousHandler.handler.stop();
                } catch (Exception ex) {
                    logger.error("Unable to stop handler", ex);
                }
            }
        } else {
            create(reactable);
        }
    }

    @Override
    public void remove(Reactable reactable) {
        final ReactableHttpAcceptors reactableHttpAcceptors = handlers.get(reactable);
        remove(reactable, reactableHttpAcceptors, true);
    }

    @Override
    public void clear() {
        Iterator<Map.Entry<Reactable, ReactableHttpAcceptors>> reactableIte = handlers.entrySet().iterator();
        while (reactableIte.hasNext()) {
            final Map.Entry<Reactable, ReactableHttpAcceptors> next = reactableIte.next();
            remove(next.getKey(), next.getValue(), false);
            reactableIte.remove();
        }
    }

    @Override
    public boolean contains(Reactable reactable) {
        return handlers.containsKey(reactable);
    }

    private void remove(Reactable reactable, ReactableHttpAcceptors reactableHttpAcceptors, boolean remove) {
        if (reactableHttpAcceptors != null) {
            try {
                // Remove the entrypoints before stopping the handler to avoid 500 errors.
                removeEntrypoints(reactableHttpAcceptors.httpAcceptorHandlers);
                reactableHttpAcceptors.handler.stop();

                if (remove) {
                    handlers.remove(reactable);
                }
                logger.debug("Handler has been unregistered from the proxy");
            } catch (Exception e) {
                logger.error("Unable to un-register handler", e);
            }
        }
    }

    @Override
    public Collection<HttpAcceptorHandler> getHttpAcceptorHandlers() {
        return registeredEntrypoints;
    }

    private void addHttpAcceptors(List<HttpAcceptorHandler> reactableEntrypoints) {
        synchronized (this) {
            final ArrayList<HttpAcceptorHandler> handlerEntrypoints = new ArrayList<>(registeredEntrypoints);
            handlerEntrypoints.addAll(reactableEntrypoints);
            handlerEntrypoints.sort(entryPointComparator);

            registeredEntrypoints = handlerEntrypoints;
        }
    }

    private void removeEntrypoints(List<HttpAcceptorHandler> previousEntrypoints) {
        synchronized (this) {
            final ArrayList<HttpAcceptorHandler> handlerEntrypoints = new ArrayList<>(registeredEntrypoints);
            handlerEntrypoints.removeAll(previousEntrypoints);
            handlerEntrypoints.sort(entryPointComparator);

            registeredEntrypoints = handlerEntrypoints;
        }
    }

    private static class ReactableHttpAcceptors {

        private final ReactorHandler handler;

        private final List<HttpAcceptorHandler> httpAcceptorHandlers;

        public ReactableHttpAcceptors(ReactorHandler handler, List<HttpAcceptorHandler> httpAcceptorHandlers) {
            this.handler = handler;
            this.httpAcceptorHandlers = httpAcceptorHandlers;
        }
    }

    private static class DefaultHttpAcceptorHandler implements HttpAcceptorHandler {

        private final ReactorHandler handler;
        private final HttpAcceptor httpAcceptor;

        public DefaultHttpAcceptorHandler(ReactorHandler handler, HttpAcceptor httpAcceptor) {
            this.handler = handler;
            this.httpAcceptor = httpAcceptor;
        }

        @Override
        public ReactorHandler target() {
            return handler;
        }

        @Override
        public ExecutionMode executionMode() {
            return handler.executionMode();
        }

        @Override
        public String path() {
            return httpAcceptor.path();
        }

        @Override
        public String host() {
            return httpAcceptor.host();
        }

        @Override
        public int priority() {
            return httpAcceptor.priority();
        }

        @Override
        public boolean accept(Request request) {
            return httpAcceptor.accept(request);
        }

        @Override
        public boolean accept(String host, String path) {
            return httpAcceptor.accept(host, path);
        }

        @Override
        public String toString() {
            return httpAcceptor.toString();
        }
    }
}

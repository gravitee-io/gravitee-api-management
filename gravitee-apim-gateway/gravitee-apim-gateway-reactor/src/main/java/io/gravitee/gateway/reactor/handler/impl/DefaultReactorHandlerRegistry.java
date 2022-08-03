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
import io.gravitee.gateway.jupiter.reactor.v4.reactor.ReactorFactoryManager;
import io.gravitee.gateway.reactor.Reactable;
import io.gravitee.gateway.reactor.handler.HttpAcceptor;
import io.gravitee.gateway.reactor.handler.HttpAcceptorHandler;
import io.gravitee.gateway.reactor.handler.ReactorHandler;
import io.gravitee.gateway.reactor.handler.ReactorHandlerRegistry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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

    private final ReactorFactoryManager reactorFactoryManager;

    private final Map<Reactable, List<ReactableHttpAcceptors>> handlers = new ConcurrentHashMap<>();
    private final HttpAcceptorHandlerComparator httpAcceptorHandlerComparator = new HttpAcceptorHandlerComparator();
    private List<HttpAcceptorHandler> registeredHttpAcceptors = new ArrayList<>();

    public DefaultReactorHandlerRegistry(final ReactorFactoryManager reactorFactoryManager) {
        this.reactorFactoryManager = reactorFactoryManager;
    }

    @Override
    public void create(Reactable reactable) {
        logger.debug("Creating a new handler for {}", reactable);

        List<ReactorHandler> reactorHandlers = prepare(reactable);
        if (!reactorHandlers.isEmpty()) {
            reactorHandlers.forEach(this::register);
        }
    }

    private List<ReactorHandler> prepare(Reactable reactable) {
        logger.debug("Preparing a new reactor handler for: {}", reactable);
        List<ReactorHandler> reactorHandlers = reactorFactoryManager.create(reactable);
        List<ReactorHandler> startedReactorHandlers = new ArrayList<>();
        if (reactorHandlers != null) {
            reactorHandlers.forEach(
                reactorHandler -> {
                    try {
                        reactorHandler.start();
                        startedReactorHandlers.add(reactorHandler);
                    } catch (Exception ex) {
                        logger.error("Unable to start the new reactor handler: " + reactorHandler, ex);
                    }
                }
            );
        }

        return startedReactorHandlers;
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

        List<ReactableHttpAcceptors> httpAcceptors = handlers.getOrDefault(handler.reactable(), new ArrayList<>());
        httpAcceptors.add(new ReactableHttpAcceptors(handler, httpAcceptorHandlers));
        handlers.put(handler.reactable(), httpAcceptors);

        addHttpAcceptors(httpAcceptorHandlers);
    }

    @Override
    public void update(Reactable reactable) {
        logger.debug("Updating handler for: {}", reactable);

        List<ReactableHttpAcceptors> reactableHttpAcceptors = handlers.get(reactable);

        if (reactableHttpAcceptors != null && !reactableHttpAcceptors.isEmpty()) {
            if (logger.isDebugEnabled()) {
                reactableHttpAcceptors.forEach(
                    reactableHttpAcceptor -> {
                        logger.debug("Handler is already deployed: {}", reactableHttpAcceptor.handler);
                    }
                );
            }

            List<ReactorHandler> newReactorHandlers = prepare(reactable);

            // Do not update handler if the new ones are not correctly initialized
            if (!newReactorHandlers.isEmpty()) {
                // Remove any handlers for the current reactable
                List<ReactableHttpAcceptors> previousReactableHttpAcceptors = handlers.remove(reactable);

                // Register the new handler before removing the previous http acceptor to avoid 404, especially on high throughput.
                newReactorHandlers.forEach(this::register);

                removeHttpAcceptors(reactable, previousReactableHttpAcceptors);
            }
        } else {
            create(reactable);
        }
    }

    @Override
    public void remove(Reactable reactable) {
        final List<ReactableHttpAcceptors> reactableHttpAcceptors = handlers.get(reactable);
        remove(reactable, reactableHttpAcceptors, true);
    }

    @Override
    public void clear() {
        Iterator<Map.Entry<Reactable, List<ReactableHttpAcceptors>>> reactableIte = handlers.entrySet().iterator();
        while (reactableIte.hasNext()) {
            final Map.Entry<Reactable, List<ReactableHttpAcceptors>> next = reactableIte.next();
            remove(next.getKey(), next.getValue(), false);
            reactableIte.remove();
        }
    }

    @Override
    public boolean contains(Reactable reactable) {
        return handlers.containsKey(reactable);
    }

    private void remove(
        final Reactable reactable,
        final List<ReactableHttpAcceptors> reactableHttpAcceptors,
        final boolean removeReactable
    ) {
        if (reactableHttpAcceptors != null) {
            try {
                removeHttpAcceptors(reactable, reactableHttpAcceptors);

                if (removeReactable) {
                    handlers.remove(reactable);
                }
                logger.debug("Handler has been unregistered from the proxy");
            } catch (Exception e) {
                logger.error("Unable to un-register handler", e);
            }
        }
    }

    private void removeHttpAcceptors(final Reactable reactable, final List<ReactableHttpAcceptors> reactableHttpAcceptors) {
        // Remove the http acceptors before stopping the handler to avoid 500 errors.
        List<HttpAcceptorHandler> httpAcceptorHandlers = reactableHttpAcceptors
            .stream()
            .flatMap(reactableHttpAcceptor -> reactableHttpAcceptor.httpAcceptorHandlers.stream())
            .collect(Collectors.toList());

        remove(httpAcceptorHandlers);

        reactableHttpAcceptors.forEach(
            reactableHttpAcceptor -> {
                try {
                    logger.debug("Stopping previous handler for: {}", reactable);
                    reactableHttpAcceptor.handler.stop();
                } catch (Exception ex) {
                    logger.error("Unable to stop handler", ex);
                }
            }
        );
    }

    @Override
    public Collection<HttpAcceptorHandler> getHttpAcceptorHandlers() {
        return registeredHttpAcceptors;
    }

    private void addHttpAcceptors(List<HttpAcceptorHandler> reactableEntrypoints) {
        synchronized (this) {
            final List<HttpAcceptorHandler> httpAcceptorHandlers = new ArrayList<>(registeredHttpAcceptors);
            httpAcceptorHandlers.addAll(reactableEntrypoints);
            httpAcceptorHandlers.sort(httpAcceptorHandlerComparator);

            registeredHttpAcceptors = httpAcceptorHandlers;
        }
    }

    private void remove(List<HttpAcceptorHandler> previousEntrypoints) {
        synchronized (this) {
            final List<HttpAcceptorHandler> httpAcceptorHandlers = new ArrayList<>(registeredHttpAcceptors);
            httpAcceptorHandlers.removeAll(previousEntrypoints);
            httpAcceptorHandlers.sort(httpAcceptorHandlerComparator);

            registeredHttpAcceptors = httpAcceptorHandlers;
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

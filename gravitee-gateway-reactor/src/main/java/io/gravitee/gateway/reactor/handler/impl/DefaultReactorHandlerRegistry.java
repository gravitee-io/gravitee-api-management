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

import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.reactor.Reactable;
import io.gravitee.gateway.reactor.handler.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DefaultReactorHandlerRegistry implements ReactorHandlerRegistry {

    private final Logger logger = LoggerFactory.getLogger(DefaultReactorHandlerRegistry.class);

    @Autowired
    private ReactorHandlerFactoryManager handlerFactoryManager;

    private final Map<Reactable, ReactorHandler> handlers = new ConcurrentHashMap<>();
    private final Map<Reactable, List<HandlerEntrypoint>> entrypointByReactable = new ConcurrentHashMap<>();

    private final List<HandlerEntrypoint> registeredEntrypoints = new ArrayList<>();
    private final HandlerEntryPointComparator entryPointComparator = new HandlerEntryPointComparator();

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
        handlers.put(handler.reactable(), handler);

        // Associate the handler to the entrypoints
        List<HandlerEntrypoint> reactableEntrypoints = handler
            .reactable()
            .entrypoints()
            .stream()
            .map(
                new Function<Entrypoint, HandlerEntrypoint>() {
                    @Override
                    public HandlerEntrypoint apply(Entrypoint entrypoint) {
                        return new HandlerEntrypoint() {
                            @Override
                            public ReactorHandler target() {
                                return handler;
                            }

                            @Override
                            public String path() {
                                return entrypoint.path();
                            }

                            @Override
                            public String host() {
                                return entrypoint.host();
                            }

                            @Override
                            public int priority() {
                                return entrypoint.priority();
                            }

                            @Override
                            public boolean accept(Request request) {
                                return entrypoint.accept(request);
                            }
                        };
                    }
                }
            )
            .collect(Collectors.toList());

        entrypointByReactable.put(handler.reactable(), reactableEntrypoints);
        addEntrypoints(reactableEntrypoints);
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

        ReactorHandler currentHandler = handlers.get(reactable);

        if (currentHandler != null) {
            logger.debug("Handler is already deployed: {}", currentHandler);

            ReactorHandler newHandler = prepare(reactable);

            // Do not update handler if the new is not correctly initialized
            if (newHandler != null) {
                ReactorHandler previousHandler = handlers.remove(reactable);
                List<HandlerEntrypoint> previousEntrypoints = entrypointByReactable.remove(previousHandler.reactable());

                // Register the new handler before removing the previous entrypoints to avoid 404, especially on high throughput.
                register(newHandler);
                removeEntrypoints(previousEntrypoints);

                try {
                    logger.debug("Stopping previous handler for: {}", reactable);
                    previousHandler.stop();
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
        ReactorHandler handler = handlers.get(reactable);

        remove(reactable, handler, true);
    }

    @Override
    public void clear() {
        Iterator<Map.Entry<Reactable, ReactorHandler>> reactableIte = handlers.entrySet().iterator();
        while (reactableIte.hasNext()) {
            final Map.Entry<Reactable, ReactorHandler> next = reactableIte.next();
            remove(next.getKey(), next.getValue(), false);
            reactableIte.remove();
        }
    }

    private void remove(Reactable reactable, ReactorHandler handler, boolean remove) {
        if (handler != null) {
            try {
                List<HandlerEntrypoint> previousEntrypoints = entrypointByReactable.remove(handler.reactable());

                // Remove the entrypoints before stopping the handler to avoid 500 errors.
                removeEntrypoints(previousEntrypoints);
                handler.stop();

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
    public Collection<HandlerEntrypoint> getEntrypoints() {
        return registeredEntrypoints;
    }

    private void addEntrypoints(List<HandlerEntrypoint> reactableEntrypoints) {
        synchronized (registeredEntrypoints) {
            registeredEntrypoints.addAll(reactableEntrypoints);
            registeredEntrypoints.sort(entryPointComparator);
        }
    }

    private void removeEntrypoints(List<HandlerEntrypoint> previousEntrypoints) {
        synchronized (registeredEntrypoints) {
            registeredEntrypoints.removeAll(previousEntrypoints);
            registeredEntrypoints.sort(entryPointComparator);
        }
    }
}

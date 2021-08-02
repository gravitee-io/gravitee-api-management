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

    private final Map<Reactable, ReactorHandler> handlers = new HashMap<>();
    private final Map<Reactable, List<HandlerEntrypoint>> entrypointByReactable = new HashMap<>();

    private final List<HandlerEntrypoint> registeredEntrypoints = new ArrayList<>();

    @Override
    public void create(Reactable reactable) {
        logger.info("Creating a new handler for {}", reactable);

        ReactorHandler handler = prepare(reactable);
        if (handler != null) {
            register(handler);
        }
    }

    private void register(ReactorHandler handler) {
        logger.info("Registering a new handler: {}", handler);
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
        registeredEntrypoints.addAll(reactableEntrypoints);
        registeredEntrypoints.sort(Comparator.comparingInt(Entrypoint::priority).reversed());
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
        logger.info("Updating handler for: {}", reactable);

        ReactorHandler currentHandler = handlers.get(reactable);

        if (currentHandler != null) {
            logger.info("Handler is already deployed: {}", currentHandler);

            ReactorHandler newHandler = prepare(reactable);

            // Do not update handler if the new is not correctly initialized
            if (newHandler != null) {
                ReactorHandler previousHandler = handlers.remove(reactable);
                List<HandlerEntrypoint> previousEntrypoints = entrypointByReactable.remove(previousHandler.reactable());
                registeredEntrypoints.removeAll(previousEntrypoints);

                register(newHandler);

                try {
                    logger.info("Stopping previous handler for: {}", reactable);
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
            remove(reactableIte.next().getKey(), reactableIte.next().getValue(), false);
            reactableIte.remove();
        }
    }

    @Override
    public boolean contains(Reactable reactable) {
        return handlers.containsKey(reactable);
    }

    private void remove(Reactable reactable, ReactorHandler handler, boolean remove) {
        if (handler != null) {
            try {
                handler.stop();
                List<HandlerEntrypoint> previousEntrypoints = entrypointByReactable.remove(handler.reactable());
                registeredEntrypoints.removeAll(previousEntrypoints);
                registeredEntrypoints.sort(Comparator.comparingInt(Entrypoint::priority).reversed());

                if (remove) {
                    handlers.remove(reactable);
                }
                logger.info("Handler has been unregistered from the proxy");
            } catch (Exception e) {
                logger.error("Unable to un-register handler", e);
            }
        }
    }

    @Override
    public List<HandlerEntrypoint> getEntrypoints() {
        return registeredEntrypoints;
    }
}

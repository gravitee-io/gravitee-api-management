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
package io.gravitee.gateway.reactor.handler.impl;

import io.gravitee.gateway.jupiter.reactor.v4.reactor.ReactorFactoryManager;
import io.gravitee.gateway.reactor.Reactable;
import io.gravitee.gateway.reactor.handler.Acceptor;
import io.gravitee.gateway.reactor.handler.ReactorHandler;
import io.gravitee.gateway.reactor.handler.ReactorHandlerRegistry;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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

    private final Map<Reactable, List<ReactableAcceptors>> handlers = new ConcurrentHashMap<>();

    private final Map<Class<? extends Acceptor<?>>, List<? extends Acceptor<?>>> acceptors = new ConcurrentHashMap<>();

    public DefaultReactorHandlerRegistry(final ReactorFactoryManager reactorFactoryManager) {
        this.reactorFactoryManager = reactorFactoryManager;
    }

    @Override
    public void create(Reactable reactable) {
        logger.debug("Creating a new handler for {}", reactable);

        List<ReactorHandler> reactorHandlers = prepare(reactable);
        if (!reactorHandlers.isEmpty()) {
            reactorHandlers.forEach(reactorHandler -> register(reactable, reactorHandler));
        }
    }

    private List<ReactorHandler> prepare(Reactable reactable) {
        logger.debug("Preparing a new reactor handler for: {}", reactable);
        List<ReactorHandler> reactorHandlers = reactorFactoryManager.create(reactable);
        List<ReactorHandler> startedReactorHandlers = new ArrayList<>();
        if (reactorHandlers != null) {
            reactorHandlers.forEach(reactorHandler -> {
                try {
                    reactorHandler.start();
                    startedReactorHandlers.add(reactorHandler);
                } catch (Exception ex) {
                    logger.error("Unable to start the new reactor handler: " + reactorHandler, ex);
                }
            });
        }

        return startedReactorHandlers;
    }

    private void register(Reactable reactable, ReactorHandler handler) {
        logger.debug("Registering a new handler: {}", handler);

        // Associate the handler to the acceptors
        List<Acceptor<?>> acceptors = handler.acceptors();

        List<ReactableAcceptors> reactableAcceptors = handlers.getOrDefault(reactable, new ArrayList<>());
        reactableAcceptors.add(new ReactableAcceptors(handler, acceptors));
        handlers.put(reactable, reactableAcceptors);

        addAcceptors(acceptors);
    }

    @Override
    public void update(Reactable reactable) {
        logger.debug("Updating handler for: {}", reactable);

        List<ReactableAcceptors> reactableAcceptors = handlers.get(reactable);

        if (reactableAcceptors != null && !reactableAcceptors.isEmpty()) {
            if (logger.isDebugEnabled()) {
                reactableAcceptors.forEach(reactableHttpAcceptor -> {
                    logger.debug("Handler is already deployed: {}", reactableHttpAcceptor.handler);
                });
            }

            List<ReactorHandler> newReactorHandlers = prepare(reactable);

            // Do not update handler if the new ones are not correctly initialized
            if (!newReactorHandlers.isEmpty()) {
                // Remove any handlers for the current reactable
                List<ReactableAcceptors> previousReactableAcceptors = handlers.remove(reactable);

                // Register the new handler before removing the previous http acceptor to avoid 404, especially on high throughput.
                newReactorHandlers.forEach(reactorHandler -> register(reactable, reactorHandler));

                removeAcceptors(reactable, previousReactableAcceptors);
            }
        } else {
            create(reactable);
        }
    }

    @Override
    public void remove(Reactable reactable) {
        final List<ReactableAcceptors> reactableAcceptors = handlers.get(reactable);
        remove(reactable, reactableAcceptors, true);
    }

    @Override
    public void clear() {
        Iterator<Map.Entry<Reactable, List<ReactableAcceptors>>> reactableIte = handlers.entrySet().iterator();
        while (reactableIte.hasNext()) {
            final Map.Entry<Reactable, List<ReactableAcceptors>> next = reactableIte.next();
            remove(next.getKey(), next.getValue(), false);
            reactableIte.remove();
        }
    }

    @Override
    public boolean contains(Reactable reactable) {
        return handlers.containsKey(reactable);
    }

    private void remove(final Reactable reactable, final List<ReactableAcceptors> reactableAcceptors, final boolean removeReactable) {
        if (reactableAcceptors != null) {
            try {
                removeAcceptors(reactable, reactableAcceptors);

                if (removeReactable) {
                    handlers.remove(reactable);
                }
                logger.debug("Handler has been unregistered from the proxy");
            } catch (Exception e) {
                logger.error("Unable to un-register handler", e);
            }
        }
    }

    private void removeAcceptors(final Reactable reactable, final List<ReactableAcceptors> reactableAcceptors) {
        // Remove the http acceptors before stopping the handler to avoid 500 errors.
        List<Acceptor> acceptorHandlers = reactableAcceptors
            .stream()
            .flatMap(reactableAcceptor -> reactableAcceptor.acceptors.stream())
            .collect(Collectors.toList());

        remove(acceptorHandlers);

        reactableAcceptors.forEach(reactableHttpAcceptor -> {
            try {
                logger.debug("Stopping previous handler for: {}", reactable);
                reactableHttpAcceptor.handler.stop();
            } catch (Exception ex) {
                logger.error("Unable to stop handler", ex);
            }
        });
    }

    @Override
    public <T extends Acceptor<T>> Collection<T> getAcceptors(Class<T> acceptorType) {
        Collection<T> acceptorsType = (Collection<T>) acceptors.get(acceptorType);

        if (acceptorsType == null) {
            return Collections.emptyList();
        }

        return acceptorsType;
    }

    private Map<Class<? extends Acceptor<?>>, Class<? extends Acceptor<?>>> acceptorsMapping = new ConcurrentHashMap<>();

    private Class<? extends Acceptor<?>> resolve(Class<? extends Acceptor> acceptor) {
        return acceptorsMapping.computeIfAbsent(
            (Class<? extends Acceptor<?>>) acceptor,
            aClass -> {
                Class<?>[] acceptorClasses = aClass.getInterfaces();
                for (Class<?> acceptorClass : acceptorClasses) {
                    if (Acceptor.class.isAssignableFrom(acceptorClass)) {
                        return (Class<? extends Acceptor<?>>) acceptorClass;
                    }
                }

                return null;
            }
        );
    }

    private void addAcceptors(List<? extends Acceptor> newAcceptors) {
        if (!newAcceptors.isEmpty()) {
            synchronized (this) {
                Class<? extends Acceptor<?>> acceptorType = resolve(newAcceptors.get(0).getClass());

                List registeredAcceptors = acceptors.get(acceptorType);

                if (registeredAcceptors == null) {
                    registeredAcceptors = new ArrayList<>();
                }

                registeredAcceptors.addAll(newAcceptors);
                Collections.sort(registeredAcceptors);

                acceptors.put(acceptorType, registeredAcceptors);
            }
        }
    }

    private void remove(List<Acceptor> previousAcceptors) {
        synchronized (this) {
            Class<? extends Acceptor<?>> acceptorType = resolve(previousAcceptors.get(0).getClass());
            List<? extends Acceptor> registeredAcceptors = acceptors.get(acceptorType);

            registeredAcceptors.removeAll(previousAcceptors);
            Collections.sort(registeredAcceptors);
        }
    }

    private static class ReactableAcceptors {

        private final ReactorHandler handler;

        private final List<Acceptor<?>> acceptors;

        public ReactableAcceptors(ReactorHandler handler, List<Acceptor<?>> acceptors) {
            this.handler = handler;
            this.acceptors = acceptors;
        }
    }
}

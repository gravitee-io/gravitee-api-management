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
import java.util.concurrent.ConcurrentSkipListSet;
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

    private final Map<Reactable, ReactableEntrypoints> handlers = new ConcurrentHashMap<>();
    private final Set<HandlerEntrypoint> entrypoints = new ConcurrentSkipListSet<>(new PriorityComparator());

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

        // Associate the handler to the entrypoints
        List<HandlerEntrypoint> reactableEntrypoints = handler
            .reactable()
            .entrypoints()
            .stream()
            .map((Function<Entrypoint, HandlerEntrypoint>) entrypoint -> new DefaultHandlerEntrypoint(handler, entrypoint))
            .collect(Collectors.toList());

        handlers.put(handler.reactable(), new ReactableEntrypoints(handler, reactableEntrypoints));
        entrypoints.addAll(reactableEntrypoints);
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

        ReactableEntrypoints reactableEntrypoints = handlers.get(reactable);

        if (reactableEntrypoints != null) {
            ReactorHandler currentHandler = reactableEntrypoints.handler;
            logger.debug("Handler is already deployed: {}", currentHandler);

            ReactorHandler newHandler = prepare(reactable);

            // Do not update handler if the new is not correctly initialized
            if (newHandler != null) {
                ReactableEntrypoints previousHandler = handlers.remove(reactable);
                entrypoints.removeAll(previousHandler.entrypoints);

                register(newHandler);

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
        final ReactableEntrypoints reactableEntrypoints = handlers.get(reactable);
        remove(reactable, reactableEntrypoints, true);
    }

    @Override
    public void clear() {
        Iterator<Map.Entry<Reactable, ReactableEntrypoints>> reactableIte = handlers.entrySet().iterator();
        while (reactableIte.hasNext()) {
            final Map.Entry<Reactable, ReactableEntrypoints> next = reactableIte.next();
            remove(next.getKey(), next.getValue(), false);
            reactableIte.remove();
        }
    }

    @Override
    public boolean contains(Reactable reactable) {
        return handlers.containsKey(reactable);
    }

    private void remove(Reactable reactable, ReactableEntrypoints reactableEntrypoints, boolean remove) {
        if (reactableEntrypoints != null) {
            try {
                reactableEntrypoints.handler.stop();
                List<HandlerEntrypoint> previousEntrypoints = reactableEntrypoints.entrypoints;
                entrypoints.removeIf(previousEntrypoints::contains);

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
        return entrypoints;
    }

    /**
     * Comparator used to sort {@link HandlerEntrypoint} in a centralized entrypoints collection.
     * The final entrypoint collection is a {@link ConcurrentSkipListSet} which rely on this comparator to add / remove entry keeping entries ordered.
     *
     * Entrypoint are first sorted by path and, in case of equality in path, the entrypoint priority is used (higher priority first).
     *
     */
    private static class PriorityComparator implements Comparator<HandlerEntrypoint> {

        @Override
        public int compare(HandlerEntrypoint o1, HandlerEntrypoint o2) {
            if (o1.equals(o2)) {
                return 0;
            }

            final int pathCompare = o1.path().compareTo(o2.path());

            if (pathCompare == 0) {
                if (o1.priority() <= o2.priority()) {
                    return 1;
                }
                return -1;
            }

            return pathCompare;
        }
    }

    private static class ReactableEntrypoints {

        private final ReactorHandler handler;

        private final List<HandlerEntrypoint> entrypoints;

        public ReactableEntrypoints(ReactorHandler handler, List<HandlerEntrypoint> entrypoints) {
            this.handler = handler;
            this.entrypoints = entrypoints;
        }
    }

    private static class DefaultHandlerEntrypoint implements HandlerEntrypoint {

        private final ReactorHandler handler;
        private final Entrypoint entrypoint;

        public DefaultHandlerEntrypoint(ReactorHandler handler, Entrypoint entrypoint) {
            this.handler = handler;
            this.entrypoint = entrypoint;
        }

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

        @Override
        public String toString() {
            return entrypoint.toString();
        }
    }
}

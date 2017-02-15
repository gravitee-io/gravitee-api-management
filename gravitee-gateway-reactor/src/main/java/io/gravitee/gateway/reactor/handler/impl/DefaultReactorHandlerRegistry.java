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

import io.gravitee.common.spring.factory.SpringFactoriesLoader;
import io.gravitee.gateway.reactor.Reactable;
import io.gravitee.gateway.reactor.handler.ReactorHandler;
import io.gravitee.gateway.reactor.handler.ReactorHandlerFactory;
import io.gravitee.gateway.reactor.handler.ReactorHandlerRegistry;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DefaultReactorHandlerRegistry extends SpringFactoriesLoader<ReactorHandlerFactory>
        implements ReactorHandlerRegistry {

    private Collection<ReactorHandlerFactory> reactorHandlerFactories;

    private final ConcurrentMap<String, ReactorHandler> handlers = new ConcurrentHashMap<>();
    private final ConcurrentMap<Object, String> contextPaths = new ConcurrentHashMap<>();

    public void create(Reactable reactable) {
        logger.info("Register a new handler for {} on path {}", reactable.item(), reactable.contextPath());

        ReactorHandler handler = create0(reactable);
        if (handler != null) {
            try {
                handler.start();
                handlers.putIfAbsent(handler.contextPath(), handler);
                contextPaths.putIfAbsent(reactable, handler.contextPath());
            } catch (Exception ex) {
                logger.error("Unable to register handler", ex);
            }
        }
    }

    @Override
    public void update(Reactable reactable) {
        String contextPath = contextPaths.get(reactable);
        if (contextPath != null) {
            ReactorHandler handler = handlers.get(contextPath);
            if (handler != null) {
                remove(reactable);
                create(reactable);
            }
        } else {
            create(reactable);
        }
    }

    @Override
    public void remove(Reactable reactable) {
        String contextPath = contextPaths.remove(reactable);
        if (contextPath != null) {
            ReactorHandler handler = handlers.remove(contextPath);

            if (handler != null) {
                try {
                    handler.stop();
                    handlers.remove(handler.contextPath());
                    logger.info("API has been unregistered");
                } catch (Exception e) {
                    logger.error("Unable to un-register handler", e);
                }
            }
        }
    }

    @Override
    public void clear() {
        handlers.forEach((s, handler) -> {
            try {
                handler.stop();
                handlers.remove(handler.contextPath());
            } catch (Exception e) {
                logger.error("Unable to un-register handler", e);
            }
        });
        contextPaths.clear();
    }

    @Override
    public Collection<ReactorHandler> getReactorHandlers() {
        return handlers.values();
    }

    private ReactorHandler create0(Reactable reactable) {
        if (reactorHandlerFactories == null) {
            reactorHandlerFactories = (Collection<ReactorHandlerFactory>) getFactoriesInstances();
        }

        return reactorHandlerFactories.iterator().next().create(reactable.item());
    }

    @Override
    protected Class<ReactorHandlerFactory> getObjectType() {
        return ReactorHandlerFactory.class;
    }
}

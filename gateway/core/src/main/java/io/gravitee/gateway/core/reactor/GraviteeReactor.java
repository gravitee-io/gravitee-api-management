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

import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.core.Reactor;
import io.gravitee.gateway.core.event.Event;
import io.gravitee.gateway.core.event.EventListener;
import io.gravitee.gateway.core.event.EventManager;
import io.gravitee.gateway.core.handler.Handler;
import io.gravitee.gateway.core.registry.RegistryEvent;
import io.gravitee.model.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public abstract class GraviteeReactor<T> implements Reactor<T>, EventListener<RegistryEvent, Api> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GraviteeReactor.class);

    @Autowired
    private EventManager eventManager;

    private ConcurrentMap<String, Handler> handlers = new ConcurrentHashMap();

    @PostConstruct
    public void init() {
        eventManager.subscribeForEvents(this, RegistryEvent.class);
    }

    @Override
    public void onEvent(Event<RegistryEvent, Api> event) {
        switch(event.type()) {
            case START:
                break;
            case STOP:
                break;
        }
    }

    protected void addHandler(Api api) {
        LOGGER.info("API {} has been enabled in reactor", api);

        handlers.putIfAbsent(api.getPublicURI().getPath(), null);
    }

    protected void removeHandler(Api api) {
        LOGGER.info("API {} has been disabled (or removed) in reactor", api);

        handlers.remove(api.getPublicURI().getPath());
    }
}

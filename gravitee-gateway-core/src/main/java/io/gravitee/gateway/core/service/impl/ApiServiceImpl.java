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
package io.gravitee.gateway.core.service.impl;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import io.gravitee.gateway.api.Repository;
import io.gravitee.gateway.core.event.EventManager;
import io.gravitee.gateway.core.service.ApiLifecycleEvent;
import io.gravitee.gateway.core.service.ApiService;
import io.gravitee.model.Api;
import io.gravitee.model.ApiState;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class ApiServiceImpl implements ApiService {

    private final Logger LOGGER = LoggerFactory.getLogger(ApiServiceImpl.class);

    @Autowired
    private EventManager eventManager;

    @Autowired
    private Repository repository;

    @Override
    public boolean start(String name) {
        Api api = repository.get(name);
        if (api == null) {
            return false;
        } else {
            eventManager.publishEvent(ApiLifecycleEvent.START, api);
            return true;
        }
    }

    @Override
    public boolean stop(String name) {
        Api api = repository.get(name);
        if (api == null) {
            return false;
        } else {
            eventManager.publishEvent(ApiLifecycleEvent.STOP, api);
            return true;
        }
    }

    @Override
    public boolean reload(final String name) {
        stop(name);
        return start(name);
    }

    @Override
    public void startAll() {
        LOGGER.info("Starting APIs... ");
        repository.listAll().forEach(api -> {
            if (ApiState.STARTED.equals(api.getState())) {
                start(api.getName());
            }
        });
        LOGGER.info("Starting APIs... DONE");
    }

    public Api get(final String name) {
        return repository.get(name);
    }

    @Override
    public Set<Api> listAll() {
        return repository.listAll();
    }

    @Override
    public boolean create(Api api) {
        return repository.create(api);
    }
}

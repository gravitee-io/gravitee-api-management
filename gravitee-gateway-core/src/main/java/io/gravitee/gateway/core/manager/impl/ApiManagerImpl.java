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
package io.gravitee.gateway.core.manager.impl;

import io.gravitee.common.event.EventManager;
import io.gravitee.gateway.core.definition.ApiDefinition;
import io.gravitee.gateway.core.manager.ApiEvent;
import io.gravitee.gateway.core.manager.ApiManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class ApiManagerImpl implements ApiManager {

    private final Logger logger = LoggerFactory.getLogger(ApiManagerImpl.class);

    @Autowired
    private EventManager eventManager;

    private final Map<String, ApiDefinition> apis = new HashMap<>();

    @Override
    public void deploy(ApiDefinition apiDefinition) {
        logger.debug("Trying to deploy API {}", apiDefinition);

        // TODO: validate API definition before triggering an event
        apis.put(apiDefinition.getName(), apiDefinition);
        eventManager.publishEvent(ApiEvent.DEPLOY, apiDefinition);
    }

    @Override
    public void update(ApiDefinition apiDefinition) {
        logger.debug("Trying to update API {}", apiDefinition);
        ApiDefinition cachedApi = apis.get(apiDefinition.getName());

        // Update only if certain fields has been updated:
        // - Lifecycle
        // - TargetURL
        // - PublicURL

        // TODO: validate API definition before triggering an event
        apis.put(apiDefinition.getName(), apiDefinition);
        eventManager.publishEvent(ApiEvent.UPDATE, apiDefinition);
    }

    @Override
    public void undeploy(String apiName) {
        logger.debug("API {} has been undeployed", apiName);
        apis.remove(apiName);
        eventManager.publishEvent(ApiEvent.UNDEPLOY, apis.get(apiName));
    }

    @Override
    public Collection<ApiDefinition> apis() {
        return apis.values();
    }

    @Override
    public ApiDefinition get(String name) {
        return apis.get(name);
    }

    public void setEventManager(EventManager eventManager) {
        this.eventManager = eventManager;
    }
}

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
package io.gravitee.management.services.sync;

import io.gravitee.common.component.Lifecycle;
import io.gravitee.common.event.EventManager;
import io.gravitee.management.model.ApiEntity;
import io.gravitee.management.service.event.ApiEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiManager {

    private final Logger logger = LoggerFactory.getLogger(ApiManager.class);

    @Autowired
    private EventManager eventManager;

    private final Map<String, ApiEntity> apis = new HashMap<>();

    public void deploy(ApiEntity api) {
        logger.info("Deployment of {}", api);

            apis.put(api.getId(), api);

            if (api.getState() == Lifecycle.State.STARTED) {
                eventManager.publishEvent(ApiEvent.DEPLOY, api);
            } else {
                logger.debug("{} is not enabled. Skip deployment.", api);
            }
    }

    public void update(ApiEntity api) {
        apis.put(api.getId(), api);
            eventManager.publishEvent(ApiEvent.UPDATE, api);
    }

    public void undeploy(String apiId) {
        ApiEntity currentApi = apis.remove(apiId);
        if (currentApi != null) {
            logger.info("Undeployment of {}", currentApi);

            eventManager.publishEvent(ApiEvent.UNDEPLOY, currentApi);
            logger.info("{} has been undeployed", apiId);
        }
    }

    public Collection<ApiEntity> apis() {
        return apis.values();
    }

    public ApiEntity get(String name) {
        return apis.get(name);
    }

    public void setEventManager(EventManager eventManager) {
        this.eventManager = eventManager;
    }
}

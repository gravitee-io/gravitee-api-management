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
package io.gravitee.rest.api.services.sync;

import io.gravitee.common.event.EventManager;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.LifecycleState;
import io.gravitee.rest.api.service.event.ApiEvent;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class ApiManager {

    private final Map<String, Api> apis = new HashMap<>();

    @Autowired
    private EventManager eventManager;

    public void deploy(Api api) {
<<<<<<< HEAD
        logger.info("Deployment of {}", api);
=======
        log.info("Deployment of API id[{}] name[{}] version[{}]", api.getId(), api.getName(), api.getVersion());
>>>>>>> d0302258df (refactor(logging): use @Slf4j, remove unused loggers, and add contextual logs for silent failures)

        apis.put(api.getId(), api);

        if (api.getLifecycleState() == LifecycleState.STARTED) {
            eventManager.publishEvent(ApiEvent.DEPLOY, api);
        } else {
<<<<<<< HEAD
            logger.debug("{} is not enabled. Skip deployment.", api);
=======
            log.debug("API id[{}] name[{}] version[{}] is not enabled. Skip deployment.", api.getId(), api.getName(), api.getVersion());
>>>>>>> d0302258df (refactor(logging): use @Slf4j, remove unused loggers, and add contextual logs for silent failures)
        }
    }

    public void update(Api api) {
        apis.put(api.getId(), api);
        eventManager.publishEvent(ApiEvent.UPDATE, api);
    }

    public void undeploy(String apiId) {
        Api currentApi = apis.remove(apiId);
        if (currentApi != null) {
<<<<<<< HEAD
            logger.info("Undeployment of {}", currentApi);

            eventManager.publishEvent(ApiEvent.UNDEPLOY, currentApi);
            logger.info("{} has been undeployed", apiId);
=======
            log.info("Un-deploying API id[{}] name[{}] version[{}]", currentApi.getId(), currentApi.getName(), currentApi.getVersion());

            eventManager.publishEvent(ApiEvent.UNDEPLOY, currentApi);
            log.info("API id[{}] has been un-deployed", apiId);
>>>>>>> d0302258df (refactor(logging): use @Slf4j, remove unused loggers, and add contextual logs for silent failures)
        }
    }

    public void setEventManager(EventManager eventManager) {
        this.eventManager = eventManager;
    }

    public Collection<Api> apis() {
        return apis.values();
    }

    public Api get(String name) {
        return apis.get(name);
    }
}

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
package io.gravitee.gateway.core.sync.impl;

import io.gravitee.gateway.core.manager.ApiManager;
import io.gravitee.gateway.core.model.Api;
import io.gravitee.gateway.core.model.ApiLifecycleState;
import io.gravitee.repository.api.ApiRepository;
import io.gravitee.repository.exceptions.TechnicalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class SyncManager {

    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(SyncManager.class);

    @Autowired
    private ApiRepository apiRepository;

    @Autowired
    private ApiManager apiManager;

    public void refresh() {
        logger.debug("Refreshing gateway state...");
        try {
            Set<io.gravitee.repository.model.Api> apis = apiRepository.findAll();

            Map<String, Api> apisMap = apis.stream()
                    .map(api -> convert(api))
                    .collect(Collectors.toMap(Api::getName, api -> api));

            // Determine APIs to remove
            Set<String> apiToRemove = apiManager.apis().keySet().stream()
                    .filter(apiName -> !apisMap.containsKey(apiName))
                    .collect(Collectors.toSet());

            apiToRemove.stream().forEach(apiName -> apiManager.remove(apiName));

            /*
            // Determine APIs to update
            apisMap.keySet().stream()
                    .filter(apiName -> apiManager.apis().containsKey(apiName))
                    .forEach(apiName -> {
                        // Get local cached API
                        ApiDefinition cachedApi = apiManager.apis().get(apiName);

                        // Get API from store
                        Api remoteApi = apisMap.get(apiName);

                        if (cachedApi.getUpdatedAt().before(remoteApi.getUpdatedAt())) {
                            apiManager.update(remoteApi);
                        }
                    });

            // Determine APIs to create
            apisMap.keySet().stream()
                    .filter(apiName -> !apiManager.apis().containsKey(apiName))
                    .forEach(apiName -> apiManager.add(apisMap.get(apiName)));
            */

        } catch (TechnicalException te) {
            logger.error("Unable to sync instance", te);
        }
    }

    private Api convert(io.gravitee.repository.model.Api remoteApi) {
        Api api = new Api();

        api.setName(remoteApi.getName());
        api.setPublicURI(remoteApi.getPublicURI());
        api.setTargetURI(remoteApi.getTargetURI());
        api.setCreatedAt(remoteApi.getCreatedAt());
        api.setUpdatedAt(remoteApi.getUpdatedAt());
        api.setState(ApiLifecycleState.valueOf(remoteApi.getLifecycleState().name()));

        return api;
    }

    public void setApiRepository(ApiRepository apiRepository) {
        this.apiRepository = apiRepository;
    }

    public void setApiManager(ApiManager apiManager) {
        this.apiManager = apiManager;
    }
}

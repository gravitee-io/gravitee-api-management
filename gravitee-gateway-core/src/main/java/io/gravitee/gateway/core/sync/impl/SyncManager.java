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

import io.gravitee.gateway.core.definition.*;
import io.gravitee.gateway.core.manager.ApiManager;
import io.gravitee.repository.api.ApiRepository;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.model.LifecycleState;
import io.gravitee.repository.model.PolicyConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
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

            Map<String, ApiDefinition> apisMap = apis.stream()
                    .map(this::convert)
                    .collect(Collectors.toMap(ApiDefinition::getName, api -> api));

            // Determine APIs to undeploy
            Set<String> apiToRemove = apiManager.apis().stream()
                    .filter(apiName -> !apisMap.containsKey(apiName))
                    .map(ApiDefinition::getName)
                    .collect(Collectors.toSet());

            apiToRemove.stream().forEach(apiManager::undeploy);

            // Determine APIs to update
            apisMap.keySet().stream()
                    .filter(apiName ->  apiManager.get(apiName) != null)
                    .forEach(apiName -> {
                        // Get local cached API
                        ApiDefinition deployedApi = apiManager.get(apiName);

                        // Get API from store
                        ApiDefinition remoteApi = apisMap.get(apiName);

                        if (deployedApi.getDeployedAt().before(remoteApi.getDeployedAt())) {
                            // Should be updated so prepare the complete definition.
                            try {
                                prepare(remoteApi);
                                apiManager.update(remoteApi);
                            } catch (TechnicalException te) {
                                logger.error("Unable to prepare API definition for {}", remoteApi.getName(), te);
                            }
                        }
                    });

            // Determine APIs to create
            apisMap.keySet().stream()
                    .filter(apiName ->  apiManager.get(apiName) == null)
                    .forEach(apiName -> {
                        ApiDefinition newApi = apisMap.get(apiName);
                        // Should be updated so prepare the complete definition.
                        try {
                            prepare(newApi);
                            apiManager.deploy(newApi);
                        } catch (TechnicalException te) {
                            logger.error("Unable to prepare API definition for {}", newApi.getName(), te);
                        }
                    });

        } catch (TechnicalException te) {
            logger.error("Unable to sync instance", te);
        }
    }

    private ApiDefinition convert(io.gravitee.repository.model.Api remoteApi) {
        ApiDefinition api = new ApiDefinition();

        api.setName(remoteApi.getName());
        api.setEnabled(remoteApi.getLifecycleState() == LifecycleState.STARTED);
        api.setDeployedAt(remoteApi.getUpdatedAt());

        ProxyDefinition proxyDefinition = new ProxyDefinition();
        proxyDefinition.setContextPath(remoteApi.getPublicURI().getPath());
        proxyDefinition.setTarget(remoteApi.getTargetURI());
        proxyDefinition.setStripContextPath(false);
        api.setProxy(proxyDefinition);

        return api;
    }

    private void prepare(ApiDefinition apiDefinition) throws TechnicalException {
        logger.debug("Loading complete API definition for API {}", apiDefinition.getName());

        List<PolicyConfiguration> policyConfigurationList = apiRepository.findPoliciesByApi(apiDefinition.getName());

        if (policyConfigurationList != null) {
            PathDefinition pathDefinition = new PathDefinition();
            pathDefinition.setPath("/*");

            MethodDefinition methodDefinition = new MethodDefinition();
            pathDefinition.getMethods().add(methodDefinition);

            policyConfigurationList.stream().forEachOrdered(policyConfiguration -> {
                PolicyDefinition policyDefinition = new PolicyDefinition();
                policyDefinition.setName(policyConfiguration.getPolicy());
                policyDefinition.setConfiguration(policyConfiguration.getConfiguration());
                methodDefinition.getPolicies().add(policyDefinition);
            });

            apiDefinition.getPaths().put(pathDefinition.getPath(), pathDefinition);
        }
    }

    public void setApiRepository(ApiRepository apiRepository) {
        this.apiRepository = apiRepository;
    }

    public void setApiManager(ApiManager apiManager) {
        this.apiManager = apiManager;
    }
}

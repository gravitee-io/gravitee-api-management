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

import io.gravitee.gateway.core.event.EventManager;
import io.gravitee.gateway.core.manager.ApiEvent;
import io.gravitee.gateway.core.manager.ApiManager;
import io.gravitee.gateway.core.model.Api;
import io.gravitee.repository.api.ApiRepository;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.model.PolicyConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.function.Consumer;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class ApiManagerImpl implements ApiManager {

    private final Logger logger = LoggerFactory.getLogger(ApiManagerImpl.class);

    @Autowired
    private EventManager eventManager;

    @Autowired
    private ApiRepository apiRepository;

    @Override
    public void add(Api api) {
        enhance(api);
        eventManager.publishEvent(ApiEvent.CREATE, api);
    }

    @Override
    public void update(Api api) {
        enhance(api);
        eventManager.publishEvent(ApiEvent.UPDATE, api);
    }

    @Override
    public void remove(Api api) {
        eventManager.publishEvent(ApiEvent.REMOVE, api);
    }

    private void enhance(Api api) {
        try {
            List<PolicyConfiguration> policies = apiRepository.findPoliciesByApi(api.getName());
            policies.stream().forEach(new Consumer<PolicyConfiguration>() {
                @Override
                public void accept(PolicyConfiguration policyConfiguration) {
                    // TODO: check that the policy is locally installed
                    String policy = policyConfiguration.getPolicy();

                    // TODO: validate configuration against installed policy
                    String configuration = policyConfiguration.getConfiguration();

                }
            });
        } catch (TechnicalException e) {
            e.printStackTrace();
        }
    }
}

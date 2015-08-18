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
package io.gravitee.management.service.impl;

import io.gravitee.management.model.PolicyEntity;
import io.gravitee.management.service.PolicyService;
import io.gravitee.plugin.PluginRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Set;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
@Component
public class PolicyServiceImpl implements PolicyService {

    /**
     * Logger.
     */
    private final Logger LOGGER = LoggerFactory.getLogger(PolicyServiceImpl.class);

    @Autowired
    private PluginRegistry pluginRegistry;

    @Override
    public Set<PolicyEntity> findAll() {
        pluginRegistry.init();
        return null;
        /*
        try {
            LOGGER.debug("Find all policies");
            Set<Policy> policies = policyRepository.findAll();
            Set<PolicyEntity> policyEntities = new HashSet<>(policies.size());

            for(Policy policy : policies) {
                policyEntities.add(convert(policy));
            }

            return policyEntities;
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find all policies", ex);
            throw new TechnicalManagementException("An error occurs while trying to find all policies", ex);
        }
        */
    }

    /*
    private PolicyEntity convert(Policy policy) {
        PolicyEntity entity = new PolicyEntity();

        entity.setId(policy.getId());
        entity.setName(policy.getName());
        entity.setDescription(policy.getDescription());
        entity.setVersion(policy.getVersion());

        return entity;
    }*/
}

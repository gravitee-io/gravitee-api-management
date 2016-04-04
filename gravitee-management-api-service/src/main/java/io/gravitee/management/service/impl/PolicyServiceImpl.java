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

import io.gravitee.management.model.PluginEntity;
import io.gravitee.management.model.PolicyDevelopmentEntity;
import io.gravitee.management.model.PolicyEntity;
import io.gravitee.management.model.PolicyType;
import io.gravitee.management.service.PolicyService;
import io.gravitee.management.service.exceptions.PolicyConfigurationNotFoundException;
import io.gravitee.management.service.exceptions.PolicyNotFoundException;
import io.gravitee.management.service.exceptions.TechnicalManagementException;
import io.gravitee.plugin.core.api.Plugin;
import io.gravitee.plugin.policy.PolicyDefinition;
import io.gravitee.plugin.policy.PolicyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
@Component
public class PolicyServiceImpl extends TransactionalService implements PolicyService {

    /**
     * Logger.
     */
    private final Logger LOGGER = LoggerFactory.getLogger(PolicyServiceImpl.class);

    @Autowired
    private PolicyManager policyManager;

    @Override
    public Set<PolicyEntity> findAll() {
        try {
            LOGGER.debug("List all policies");
            final Collection<PolicyDefinition> policyDefinitions = policyManager.getPolicyDefinitions();

            return policyDefinitions.stream()
                    .map(policyDefinition -> convert(policyDefinition, false))
                    .collect(Collectors.toSet());
        } catch (Exception ex) {
            LOGGER.error("An error occurs while trying to list all policies", ex);
            throw new TechnicalManagementException("An error occurs while trying to list all policies", ex);
        }
    }

    @Override
    public PolicyEntity findById(String policyId) {
        LOGGER.debug("Find policy by ID: {}", policyId);
        PolicyDefinition policyDefinition = policyManager.getPolicyDefinition(policyId);

        if (policyDefinition == null) {
            throw new PolicyNotFoundException(policyId);
        }

        return convert(policyDefinition, true);
    }

    @Override
    public String getSchema(String policyId) {
        try {
            LOGGER.debug("Find policy schema by ID: {}", policyId);
            String configuration = policyManager.getPolicyConfiguration(policyId);

            if (configuration == null) {
                throw new PolicyConfigurationNotFoundException(policyId);
            }

            return configuration;
        } catch (IOException ioex) {
            LOGGER.error("An error occurs while trying to get policy schema for policy {}", policyId, ioex);
            throw new TechnicalManagementException("An error occurs while trying to get policy schema for policy " + policyId, ioex);
        }
    }

    private PolicyEntity convert(PolicyDefinition policyDefinition, boolean withPlugin) {
        PolicyEntity entity = new PolicyEntity();

        entity.setId(policyDefinition.id());
        entity.setDescription(policyDefinition.plugin().manifest().description());
        entity.setName(policyDefinition.plugin().manifest().name());
        entity.setVersion(policyDefinition.plugin().manifest().version());

        if (policyDefinition.onRequestMethod() != null && policyDefinition.onResponseMethod() != null) {
            entity.setType(PolicyType.REQUEST_RESPONSE);
        } else if (policyDefinition.onRequestMethod() != null) {
            entity.setType(PolicyType.REQUEST);
        } else if (policyDefinition.onResponseMethod() != null) {
            entity.setType(PolicyType.RESPONSE);
        }

        if (withPlugin) {
            // Plugin information
            Plugin plugin = policyDefinition.plugin();
            PluginEntity pluginEntity = new PluginEntity();

            pluginEntity.setClassName(plugin.clazz());
            pluginEntity.setPath(plugin.path().toString());
            pluginEntity.setType(plugin.type().toString().toLowerCase());
            pluginEntity.setDependencies(plugin.dependencies());

            entity.setPlugin(pluginEntity);

            // Policy development information
            PolicyDevelopmentEntity developmentEntity = new PolicyDevelopmentEntity();
            developmentEntity.setClassName(policyDefinition.policy().getName());

            if (policyDefinition.configuration() != null) {
                developmentEntity.setConfiguration(policyDefinition.configuration().getName());
            }

            if (policyDefinition.onRequestMethod() != null) {
                developmentEntity.setOnRequestMethod(policyDefinition.onRequestMethod().toGenericString());
            }

            if (policyDefinition.onResponseMethod() != null) {
                developmentEntity.setOnResponseMethod(policyDefinition.onResponseMethod().toGenericString());
            }

            entity.setDevelopment(developmentEntity);
        }

        return entity;
    }
}

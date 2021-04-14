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
package io.gravitee.rest.api.service.impl;

import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.core.api.Plugin;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.rest.api.model.PluginEntity;
import io.gravitee.rest.api.model.PolicyDevelopmentEntity;
import io.gravitee.rest.api.model.PolicyEntity;
import io.gravitee.rest.api.service.PolicyService;
import io.gravitee.rest.api.service.exceptions.PolicyNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class PolicyServiceImpl extends TransactionalService implements PolicyService {

    /**
     * Logger.
     */
    private final Logger LOGGER = LoggerFactory.getLogger(PolicyServiceImpl.class);

    @Autowired
    private ConfigurablePluginManager<PolicyPlugin> policyManager;

    @Override
    public Set<PolicyEntity> findAll() {
        try {
            LOGGER.debug("List all policies");
            final Collection<PolicyPlugin> policyDefinitions = policyManager.findAll();

            return policyDefinitions.stream().map(policyDefinition -> convert(policyDefinition, false)).collect(Collectors.toSet());
        } catch (Exception ex) {
            LOGGER.error("An error occurs while trying to list all policies", ex);
            throw new TechnicalManagementException("An error occurs while trying to list all policies", ex);
        }
    }

    @Override
    public PolicyEntity findById(String policyId) {
        LOGGER.debug("Find policy by ID: {}", policyId);
        PolicyPlugin policyDefinition = policyManager.get(policyId);

        if (policyDefinition == null) {
            throw new PolicyNotFoundException(policyId);
        }

        return convert(policyDefinition, true);
    }

    @Override
    public String getSchema(String policyId) {
        try {
            LOGGER.debug("Find policy schema by ID: {}", policyId);
            return policyManager.getSchema(policyId);
        } catch (IOException ioex) {
            LOGGER.error("An error occurs while trying to get policy schema for policy {}", policyId, ioex);
            throw new TechnicalManagementException("An error occurs while trying to get policy schema for policy " + policyId, ioex);
        }
    }

    private PolicyEntity convert(PolicyPlugin policyPlugin, boolean withPlugin) {
        PolicyEntity entity = new PolicyEntity();

        entity.setId(policyPlugin.id());
        entity.setDescription(policyPlugin.manifest().description());
        entity.setName(policyPlugin.manifest().name());
        entity.setVersion(policyPlugin.manifest().version());

        /*
        if (policyDefinition.onRequestMethod() != null && policyDefinition.onResponseMethod() != null) {
            entity.setType(PolicyType.REQUEST_RESPONSE);
        } else if (policyDefinition.onRequestMethod() != null) {
            entity.setType(PolicyType.REQUEST);
        } else if (policyDefinition.onResponseMethod() != null) {
            entity.setType(PolicyType.RESPONSE);
        }
        */

        if (withPlugin) {
            // Plugin information
            Plugin plugin = policyPlugin;
            PluginEntity pluginEntity = new PluginEntity();

            pluginEntity.setPlugin(plugin.clazz());
            pluginEntity.setPath(plugin.path().toString());
            pluginEntity.setType(plugin.type().toString().toLowerCase());
            pluginEntity.setDependencies(plugin.dependencies());

            entity.setPlugin(pluginEntity);

            // Policy development information
            PolicyDevelopmentEntity developmentEntity = new PolicyDevelopmentEntity();
            developmentEntity.setClassName(policyPlugin.policy().getName());

            /*
            if (policy.configuration() != null) {
                developmentEntity.setConfiguration(policyDefinition.configuration().getName());
            }

            if (policyDefinition.onRequestMethod() != null) {
                developmentEntity.setOnRequestMethod(policyDefinition.onRequestMethod().toGenericString());
            }

            if (policyDefinition.onResponseMethod() != null) {
                developmentEntity.setOnResponseMethod(policyDefinition.onResponseMethod().toGenericString());
            }
            */

            entity.setDevelopment(developmentEntity);
        }

        return entity;
    }
}

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

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ListProcessingReport;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import io.gravitee.definition.model.Policy;
import io.gravitee.plugin.core.api.Plugin;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.rest.api.model.PluginEntity;
import io.gravitee.rest.api.model.PolicyDevelopmentEntity;
import io.gravitee.rest.api.model.PolicyEntity;
import io.gravitee.rest.api.service.PolicyService;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

import static io.gravitee.rest.api.service.validator.PolicyCleaner.clearNullValues;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class PolicyServiceImpl extends AbstractPluginService<PolicyPlugin, PolicyEntity> implements PolicyService {

    @Autowired
    private JsonSchemaFactory jsonSchemaFactory;

    @Override
    public Set<PolicyEntity> findAll() {
        return super.list()
                .stream()
                .map(policyDefinition -> convert(policyDefinition, false))
                .collect(Collectors.toSet());
    }

    @Override
    public PolicyEntity findById(String policyId) {
        PolicyPlugin policyDefinition = super.get(policyId);
        return convert(policyDefinition, true);
    }

    @Override
    public void validatePolicyConfiguration(Policy policy) {

        if (policy != null && policy.getConfiguration() != null) {
            String schema = getSchema(policy.getName());

            try {
                // At least, validate json.
                String safePolicyConfiguration = clearNullValues(policy.getConfiguration());
                JsonNode jsonConfiguration = JsonLoader.fromString(safePolicyConfiguration);

                if (schema != null && !schema.equals("")) {
                    // Validate json against schema when defined.
                    JsonNode jsonSchema = JsonLoader.fromString(schema);
                    ListProcessingReport report = (ListProcessingReport) jsonSchemaFactory.getValidator().validate(jsonSchema, jsonConfiguration, true);
                    if (!report.isSuccess()) {
                        String msg = "";
                        if (report.iterator().hasNext()) {
                            msg = " : " + report.iterator().next().getMessage();
                        }
                        throw new InvalidDataException("Invalid policy configuration" + msg);
                    }
                }

                policy.setConfiguration(safePolicyConfiguration);

            } catch (IOException | ProcessingException e) {
                throw new InvalidDataException("Unable to validate policy configuration", e);
            }
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

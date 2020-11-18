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
import io.github.classgraph.ClassGraph;
import io.github.classgraph.MethodInfoList;
import io.github.classgraph.ScanResult;
import io.gravitee.definition.model.Policy;
import io.gravitee.definition.model.flow.Step;
import io.gravitee.plugin.core.api.Plugin;
import io.gravitee.plugin.core.api.PluginClassLoader;
import io.gravitee.plugin.policy.PolicyClassLoaderFactory;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.policy.api.annotations.OnRequest;
import io.gravitee.policy.api.annotations.OnRequestContent;
import io.gravitee.policy.api.annotations.OnResponse;
import io.gravitee.policy.api.annotations.OnResponseContent;
import io.gravitee.rest.api.model.PluginEntity;
import io.gravitee.rest.api.model.PolicyDevelopmentEntity;
import io.gravitee.rest.api.model.PolicyEntity;
import io.gravitee.rest.api.service.PolicyService;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
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

    @Autowired
    private PolicyClassLoaderFactory policyClassLoaderFactory;

    private final Map<String, PolicyDevelopmentEntity> policies = new HashMap<>();

    @Override
    public Set<PolicyEntity> findAll() {
        return super.list()
                .stream()
                .map(policyDefinition -> convert(policyDefinition, true))
                .collect(Collectors.toSet());
    }

    @Override
    public PolicyEntity findById(String policyId) {
        PolicyPlugin policyDefinition = super.get(policyId);
        return convert(policyDefinition, true);
    }

    private String validatePolicyConfiguration(String policyName, String configuration) {
        if (policyName != null && configuration != null) {
            String schema = getSchema(policyName);

            try {
                // At least, validate json.
                String safePolicyConfiguration = clearNullValues(configuration);
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

                return safePolicyConfiguration;

            } catch (IOException | ProcessingException e) {
                throw new InvalidDataException("Unable to validate policy configuration", e);
            }
        }
        return configuration;
    }

    @Override
    public void validatePolicyConfiguration(Step step) {
        if (step != null) {
            step.setConfiguration(validatePolicyConfiguration(step.getPolicy(), step.getConfiguration()));
        }
    }

    @Override
    public void validatePolicyConfiguration(Policy policy) {
        if (policy != null) {
            policy.setConfiguration(validatePolicyConfiguration(policy.getName(), policy.getConfiguration()));
        }
    }

    private PolicyEntity convert(PolicyPlugin policyPlugin, Boolean withPlugin) {
        PolicyEntity entity = new PolicyEntity();

        entity.setId(policyPlugin.id());
        entity.setDescription(policyPlugin.manifest().description());
        entity.setName(policyPlugin.manifest().name());
        entity.setVersion(policyPlugin.manifest().version());
        entity.setCategory(policyPlugin.manifest().category());

        if (withPlugin) {
            // Plugin information
            PluginEntity pluginEntity = new PluginEntity();

            pluginEntity.setPlugin(policyPlugin.clazz());
            pluginEntity.setPath(policyPlugin.path().toString());
            pluginEntity.setType(((Plugin) policyPlugin).type().toLowerCase());
            pluginEntity.setDependencies(policyPlugin.dependencies());

            entity.setPlugin(pluginEntity);
            entity.setDevelopment(loadPolicy(policyPlugin));
        }

        return entity;
    }

    private PolicyDevelopmentEntity loadPolicy(PolicyPlugin policy) {
        return policies.computeIfAbsent(policy.id(), new Function<String, PolicyDevelopmentEntity>() {
            @Override
            public PolicyDevelopmentEntity apply(String s) {
                // Policy development information
                PolicyDevelopmentEntity developmentEntity = new PolicyDevelopmentEntity();
                developmentEntity.setClassName(policy.policy().getName());

                ScanResult scan = null;
                PluginClassLoader policyClassLoader = null;

                try {
                    policyClassLoader = policyClassLoaderFactory.getOrCreateClassLoader(policy);

                    scan = new ClassGraph()
                            .enableMethodInfo()
                            .enableAnnotationInfo()
                            .acceptClasses(policy.policy().getName())
                            .ignoreParentClassLoaders()
                            .overrideClassLoaders(policyClassLoader)
                            .scan(1);

                    MethodInfoList methodInfo = scan.getClassInfo(policy.policy().getName()).getMethodInfo();

                    MethodInfoList filter = methodInfo.filter(methodInfo1 -> methodInfo1.hasAnnotation(OnRequest.class.getName())
                            || methodInfo1.hasAnnotation(OnRequestContent.class.getName()));

                    if (!filter.isEmpty()) {
                        developmentEntity.setOnRequestMethod(filter.get(0).getName());
                    }

                    filter = methodInfo.filter(methodInfo12 -> methodInfo12.hasAnnotation(OnResponse.class.getName())
                            || methodInfo12.hasAnnotation(OnResponseContent.class.getName()));

                    if (!filter.isEmpty()) {
                        developmentEntity.setOnResponseMethod(filter.get(0).getName());
                    }

                    return developmentEntity;
                } catch (Throwable ex) {
                    logger.error("An unexpected error occurs while loading policy", ex);
                    return null;
                } finally {
                    if (policyClassLoader != null) {
                        try {
                            policyClassLoader.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    if (scan != null) {
                        scan.close();
                    }
                }
            }
        });
    }
}

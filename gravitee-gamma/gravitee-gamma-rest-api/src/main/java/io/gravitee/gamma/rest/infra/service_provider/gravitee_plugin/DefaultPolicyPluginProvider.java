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
package io.gravitee.gamma.rest.infra.service_provider.gravitee_plugin;

import static java.util.stream.Collectors.toMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gamma.rest.core.gravitee_plugin.model.DeclaredFlows;
import io.gravitee.gamma.rest.core.gravitee_plugin.model.PlatformPolicyPlugin;
import io.gravitee.gamma.rest.core.gravitee_plugin.model.PolicyCapabilities;
import io.gravitee.gamma.rest.core.gravitee_plugin.model.PolicyFlowType;
import io.gravitee.gamma.rest.core.gravitee_plugin.model.ResourceReference;
import io.gravitee.gamma.rest.core.gravitee_plugin.port.service_provider.PolicyPluginProvider;
import io.gravitee.gateway.reactive.api.policy.guardrail.GuardrailPolicy;
import io.gravitee.node.api.license.License;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.policy.PolicyPlugin;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class DefaultPolicyPluginProvider extends AbstractPluginProvider<PolicyPlugin<?>> implements PolicyPluginProvider {

    /** The {@code gioConfig.uiType} with which a policy schema marks a property holding a resource name. */
    private static final String RESOURCE_PICKER = "resource-type";

    private static final ObjectMapper SCHEMA_MAPPER = new ObjectMapper();

    public DefaultPolicyPluginProvider(ConfigurablePluginManager<PolicyPlugin<?>> pluginManager, LicenseManager licenseManager) {
        super(pluginManager, licenseManager);
    }

    @Override
    public Map<String, PolicyCapabilities> findAllCapabilities() {
        return pluginManager
            .findAll(true)
            .stream()
            .collect(
                toMap(PolicyPlugin::id, plugin ->
                    new PolicyCapabilities(GuardrailPolicy.class.isAssignableFrom(plugin.policy()), flows(plugin.manifest().properties()))
                )
            );
    }

    /** Filters before converting: the policies left out are not converted, their icon not read. */
    @Override
    public List<PlatformPolicyPlugin> findAllPolicies(String organizationId, Predicate<DeclaredFlows> filter) {
        var license = license(organizationId);
        var policies = new ArrayList<PlatformPolicyPlugin>();
        for (var plugin : pluginManager.findAll(true)) {
            var flows = flows(plugin.manifest().properties());
            if (filter.test(flows)) {
                policies.add(toPolicy(plugin, flows, license));
            }
        }
        return policies;
    }

    @Override
    public PlatformPolicyPlugin findPolicyById(String organizationId, String policyId) {
        var plugin = getPlugin(policyId);
        return toPolicy(plugin, flows(plugin.manifest().properties()), license(organizationId));
    }

    @Override
    public List<ResourceReference> findResourceReferences(String policyId) {
        if (pluginManager.get(policyId, true) == null) {
            return List.of();
        }
        var schema = getSchema(policyId);
        if (schema == null || schema.isBlank()) {
            return List.of();
        }
        JsonNode root;
        try {
            root = SCHEMA_MAPPER.readTree(schema);
        } catch (JsonProcessingException e) {
            throw technicalError("schema", policyId, e);
        }
        var references = new ArrayList<ResourceReference>();
        collectResourceReferences(root, "", references);
        return references;
    }

    /**
     * Walks the nested {@code properties} of an object schema. Composed branches ({@code oneOf}, {@code items}, ...)
     * are not entered: no policy declares a resource picker there, and the console would not offer one.
     */
    private static void collectResourceReferences(JsonNode schema, String pointer, List<ResourceReference> references) {
        for (var property : schema.path("properties").properties()) {
            var path = pointer + "/" + property.getKey().replace("~", "~0").replace("/", "~1");
            var gioConfig = property.getValue().path("gioConfig");
            if (RESOURCE_PICKER.equals(gioConfig.path("uiType").asText())) {
                references.add(new ResourceReference(path, acceptedTypes(gioConfig.path("uiTypeProps").path("resourceType"))));
            } else {
                collectResourceReferences(property.getValue(), path, references);
            }
        }
    }

    /** A single type or a list of them, as the console pickers read {@code uiTypeProps.resourceType}. */
    private static List<String> acceptedTypes(JsonNode resourceType) {
        if (resourceType.isTextual()) {
            return List.of(resourceType.textValue());
        }
        var types = new ArrayList<String>();
        resourceType.forEach(type -> {
            if (type.isTextual()) {
                types.add(type.textValue());
            }
        });
        return types;
    }

    private PlatformPolicyPlugin toPolicy(PolicyPlugin<?> plugin, DeclaredFlows flows, License license) {
        return new PlatformPolicyPlugin(convert(plugin, license), flows);
    }

    /** The phases of each flow the manifest declares ({@code llm_proxy=REQUEST,RESPONSE}), trimmed but neither validated nor reduced. */
    private static DeclaredFlows flows(Map<String, String> properties) {
        var flows = new EnumMap<PolicyFlowType, List<String>>(PolicyFlowType.class);
        if (properties == null) {
            return new DeclaredFlows(flows);
        }
        for (var flow : PolicyFlowType.values()) {
            declaredPhases(properties, flow).ifPresent(phases ->
                flows.put(
                    flow,
                    Arrays.stream(phases.split(","))
                        .map(String::trim)
                        .filter(phase -> !phase.isEmpty())
                        .distinct()
                        .toList()
                )
            );
        }
        return new DeclaredFlows(flows);
    }

    /** The first non-blank value between the key of the flow and its deprecated key. */
    private static Optional<String> declaredPhases(Map<String, String> properties, PolicyFlowType flow) {
        return Stream.concat(Stream.of(flow.manifestKey()), flow.deprecatedManifestKey().stream())
            .map(properties::get)
            .filter(phases -> phases != null && !phases.isBlank())
            .findFirst();
    }
}

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
package io.gravitee.apim.core.flow.domain_service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.flow.step.Step;
import java.util.Set;
import org.springframework.lang.Nullable;

/**
 * Structural validation for XML Validation policy configurations with registry source.
 * Shared between {@link FlowValidationDomainService} and legacy {@code FlowValidationServiceImpl}.
 */
public final class XmlValidationPolicyChecker {

    private static final String XML_VALIDATION_POLICY_ID = "xml-validation";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final Set<ApiType> SUPPORTED_API_TYPES = Set.of(ApiType.PROXY);

    private XmlValidationPolicyChecker() {}

    /**
     * Validates registry-sourced XML validation policy structural configuration.
     *
     * @return {@code null} if the step does not require validation or passes all checks;
     *         a non-null error message otherwise.
     */
    public static String validateRegistryConfiguration(ApiType apiType, Step step) {
        return validateRegistryConfiguration(apiType, step, null);
    }

    /**
     * Validates registry-sourced XML validation policy structural configuration, optionally
     * verifying that the referenced resource name exists on the API.
     *
     * @param apiResourceNames resource names declared on the API; when non-null, the checker
     *                         verifies that {@code registryResource} references an existing name.
     * @return {@code null} if the step does not require validation or passes all checks;
     *         a non-null error message otherwise.
     */
    public static String validateRegistryConfiguration(ApiType apiType, Step step, @Nullable Set<String> apiResourceNames) {
        if (!XML_VALIDATION_POLICY_ID.equals(step.getPolicy())) {
            return null;
        }
        try {
            JsonNode config = OBJECT_MAPPER.readTree(step.getConfiguration());
            String schemaSource = config.path("schemaSource").asText("inline");
            if (!"registry".equalsIgnoreCase(schemaSource)) {
                return null;
            }
            if (!SUPPORTED_API_TYPES.contains(apiType)) {
                return "XML Validation with schemaSource=registry is only supported on HTTP proxy APIs";
            }
            if (
                isBlank(config, "registryResource") ||
                isBlank(config, "groupId") ||
                isBlank(config, "artifactId") ||
                isBlank(config, "version")
            ) {
                return "XML Validation registry source requires registryResource, groupId, artifactId, and version";
            }
            if (apiResourceNames != null && !apiResourceNames.isEmpty()) {
                String resourceName = config.get("registryResource").asText();
                if (!apiResourceNames.contains(resourceName)) {
                    return "XML Validation references unknown resource '" + resourceName + "'";
                }
            }
            if (config.hasNonNull("xsdSchema") && !config.get("xsdSchema").asText("").isBlank()) {
                return "XML Validation schemaSource=registry cannot be combined with inline xsdSchema";
            }
            return null;
        } catch (Exception e) {
            return "Invalid XML Validation policy configuration: " + e.getMessage();
        }
    }

    private static boolean isBlank(JsonNode config, String field) {
        JsonNode node = config.get(field);
        return node == null || node.isNull() || node.asText("").isBlank();
    }
}

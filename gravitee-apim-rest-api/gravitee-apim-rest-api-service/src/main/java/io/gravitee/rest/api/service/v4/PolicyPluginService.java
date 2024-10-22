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
package io.gravitee.rest.api.service.v4;

import io.gravitee.rest.api.model.platform.plugin.SchemaDisplayFormat;
import io.gravitee.rest.api.model.v4.policy.ApiProtocolType;
import io.gravitee.rest.api.model.v4.policy.PolicyPluginEntity;
import io.gravitee.rest.api.service.PluginService;
import java.util.Set;

public interface PolicyPluginService extends PluginService<PolicyPluginEntity> {
    /**
     * Looks for the policy plugin from its id and then validate the configuration
     * @param policyPluginId is the id of the policy
     * @param configuration is the configuration to validate
     * @return the validated configuration
     */
    String validatePolicyConfiguration(final String policyPluginId, final String configuration);

    /**
     * Validate the configuration for the given policy plugin
     * @param policyPluginEntity is the policy plugin for which configuration will be validated
     * @param configuration is the configuration to validate
     * @return the validated configuration
     */
    String validatePolicyConfiguration(final PolicyPluginEntity policyPluginEntity, final String configuration);

    /**
     * Get the schema form for the given policy plugin
     * @param policyPluginId is the id of the policy
     * @param apiProtocolType the protocol type to get the schema for
     * @param schemaDisplayFormat the format of the schema to return
     * @return the configuration schema form
     */
    String getSchema(String policyPluginId, ApiProtocolType apiProtocolType, SchemaDisplayFormat schemaDisplayFormat);

    /**
     * Get the documentation for the given policy plugin
     * @param policyPluginId is the id of the policy
     * @param apiProtocolType the protocol type to get the documentation for
     * @return the documentation
     */
    String getDocumentation(String policyPluginId, ApiProtocolType apiProtocolType);
}

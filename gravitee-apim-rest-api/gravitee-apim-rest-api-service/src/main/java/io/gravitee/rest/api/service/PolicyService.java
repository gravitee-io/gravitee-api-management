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
package io.gravitee.rest.api.service;

import io.gravitee.definition.model.Policy;
import io.gravitee.definition.model.flow.Step;
import io.gravitee.rest.api.model.PolicyEntity;
import io.gravitee.rest.api.model.platform.plugin.SchemaDisplayFormat;
import java.util.Set;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface PolicyService extends PluginService<PolicyEntity> {
    void validatePolicyConfiguration(Policy policy);

    String validatePolicyConfiguration(String policyName, String configuration);

    void validatePolicyConfiguration(Step step);

    Set<PolicyEntity> findAll(Boolean withResource);

    String getSchema(String plugin, SchemaDisplayFormat schemaDisplayFormat);
}

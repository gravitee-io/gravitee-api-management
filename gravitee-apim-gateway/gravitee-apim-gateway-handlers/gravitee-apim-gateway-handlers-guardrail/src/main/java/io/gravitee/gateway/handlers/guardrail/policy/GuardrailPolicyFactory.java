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
package io.gravitee.gateway.handlers.guardrail.policy;

import io.gravitee.definition.model.guardrail.GuardrailPolicyConfiguration;
import io.gravitee.gateway.policy.PolicyManifest;
import io.gravitee.gateway.policy.PolicyMetadata;
import io.gravitee.gateway.policy.PolicyPluginFactory;
import io.gravitee.gateway.reactive.api.ExecutionPhase;
import io.gravitee.gateway.reactive.api.policy.http.HttpPolicy;
import io.gravitee.gateway.reactive.core.condition.ExpressionLanguageConditionFilter;
import io.gravitee.gateway.reactive.policy.HttpConditionalPolicy;
import io.gravitee.gateway.reactive.policy.HttpPolicyFactory;
import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.policy.api.PolicyConfiguration;
import org.springframework.util.StringUtils;

/**
 * Builds {@link GuardrailPolicy} instances named after their flow step, so traces and logs show the step rather than
 * the generic policy id.
 *
 * @author GraviteeSource Team
 */
public class GuardrailPolicyFactory extends HttpPolicyFactory {

    public GuardrailPolicyFactory(
        Configuration configuration,
        PolicyPluginFactory policyPluginFactory,
        ExpressionLanguageConditionFilter<HttpConditionalPolicy> filter
    ) {
        super(configuration, policyPluginFactory, filter);
    }

    @Override
    public boolean accept(PolicyManifest policyManifest) {
        return GuardrailPolicy.POLICY_ID.equals(policyManifest.id());
    }

    @Override
    protected HttpPolicy createPolicy(
        ExecutionPhase phase,
        PolicyManifest policyManifest,
        PolicyConfiguration policyConfiguration,
        PolicyMetadata policyMetadata
    ) {
        GuardrailPolicyConfiguration configuration = (GuardrailPolicyConfiguration) policyConfiguration;
        if (configuration == null || !StringUtils.hasText(configuration.getGuardrailId())) {
            // Without an id, every call would answer that the Guardrail is not deployed, pointing to the sync rather than to the step.
            throw new IllegalArgumentException(String.format("Guardrail step [%s] has no guardrailId", policyMetadata.getName()));
        }
        String stepName = (String) policyMetadata.metadata().get(PolicyMetadata.MetadataKeys.STEP_NAME);
        HttpPolicy policy = new GuardrailPolicy(policyMetadata.getName(), stepName, configuration);
        return decoratePolicy(policyMetadata, policy);
    }
}

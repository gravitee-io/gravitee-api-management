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
package fakes;

import io.gravitee.apim.core.plugin.model.PolicyPlugin;
import io.gravitee.apim.core.policy.domain_service.PolicyValidationDomainService;
import io.gravitee.apim.core.policy.exception.UnexpectedPoliciesException;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import java.util.List;

public class FakePolicyValidationDomainService implements PolicyValidationDomainService {

    @Override
    public String validateAndSanitizeConfiguration(String policyName, String configuration) {
        if (policyName.contains("throw_invalid_data_exception")) {
            throw new InvalidDataException("Invalid configuration for policy " + policyName);
        }
        return configuration;
    }

    @Override
    public void validatePoliciesExecutionPhase(List<String> policyIds, ApiType apiType, PolicyPlugin.ExecutionPhase phase)
        throws UnexpectedPoliciesException {
        policyIds.forEach(policyId -> {
            if (policyId.contains("throw_unexpected_policy_exception")) {
                throw new UnexpectedPoliciesException(List.of(policyId), apiType.name(), phase.name());
            }
        });
    }
}

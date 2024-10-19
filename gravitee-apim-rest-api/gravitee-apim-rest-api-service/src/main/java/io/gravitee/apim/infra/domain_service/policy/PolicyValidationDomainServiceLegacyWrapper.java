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
package io.gravitee.apim.infra.domain_service.policy;

import io.gravitee.apim.core.plugin.model.FlowPhase;
import io.gravitee.apim.core.plugin.model.PolicyPlugin;
import io.gravitee.apim.core.policy.domain_service.PolicyValidationDomainService;
import io.gravitee.apim.core.policy.exception.UnexpectedPoliciesException;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.rest.api.model.v4.policy.ApiProtocolType;
import io.gravitee.rest.api.model.v4.policy.PolicyPluginEntity;
import io.gravitee.rest.api.service.v4.PolicyPluginService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class PolicyValidationDomainServiceLegacyWrapper implements PolicyValidationDomainService {

    private final PolicyPluginService policyPluginService;

    @Override
    public String validateAndSanitizeConfiguration(String policyName, String configuration) {
        return policyPluginService.validatePolicyConfiguration(policyName, configuration);
    }

    @Override
    public void validatePoliciesFlowPhase(List<String> policyIds, ApiType apiType, FlowPhase phase) throws UnexpectedPoliciesException {
        Map<String, PolicyPluginEntity> policiesMap = policyPluginService
            .findAll()
            .stream()
            .collect(Collectors.toMap(PolicyPluginEntity::getId, Function.identity()));

        List<String> policyNamesUnexpected = new ArrayList<>();
        policyIds.forEach(policyId -> {
            if (policiesMap.containsKey(policyId)) {
                PolicyPluginEntity policy = policiesMap.get(policyId);

                if (apiType.equals(ApiType.PROXY)) {
                    if (
                        policy.getFlowPhaseCompatibility(ApiProtocolType.HTTP_PROXY) == null ||
                        policy.getFlowPhaseCompatibility(ApiProtocolType.HTTP_PROXY).stream().noneMatch(p -> p.name().equals(phase.name()))
                    ) {
                        policyNamesUnexpected.add(policy.getName());
                    }
                }
                if (apiType.equals(ApiType.MESSAGE)) {
                    if (
                        policy.getFlowPhaseCompatibility(ApiProtocolType.HTTP_MESSAGE) == null ||
                        policy
                            .getFlowPhaseCompatibility(ApiProtocolType.HTTP_MESSAGE)
                            .stream()
                            .noneMatch(p -> p.name().equals(phase.name()))
                    ) {
                        policyNamesUnexpected.add(policy.getName());
                    }
                }
            }
        });

        if (!policyNamesUnexpected.isEmpty()) {
            throw new UnexpectedPoliciesException(policyNamesUnexpected, apiType.name(), phase.name());
        }
    }
}

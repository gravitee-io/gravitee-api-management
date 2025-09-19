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
package io.gravitee.apim.core.shared_policy_group.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.policy.domain_service.PolicyValidationDomainService;
import io.gravitee.apim.core.shared_policy_group.crud_service.SharedPolicyGroupCrudService;
import io.gravitee.apim.core.shared_policy_group.exception.SharedPolicyGroupDuplicateCrossIdException;
import io.gravitee.apim.core.shared_policy_group.exception.SharedPolicyGroupInvalidPhaseException;
import io.gravitee.apim.core.shared_policy_group.model.SharedPolicyGroup;
import io.gravitee.apim.core.validation.Validator;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import java.util.ArrayList;
import lombok.RequiredArgsConstructor;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
@DomainService
@RequiredArgsConstructor
public class ValidateCreateSharedPolicyGroupDomainService implements Validator<ValidateCreateSharedPolicyGroupDomainService.Input> {

    private final SharedPolicyGroupCrudService sharedPolicyGroupCrudService;
    private final PolicyValidationDomainService policyValidationDomainService;

    public record Input(AuditInfo auditInfo, SharedPolicyGroup sharedPolicyGroup) implements Validator.Input {}

    @Override
    public Result<Input> validateAndSanitize(Input input) {
        var errors = new ArrayList<Error>();
        var sanitizedBuilder = input.sharedPolicyGroup().toBuilder();

        try {
            validate(input.sharedPolicyGroup, input.auditInfo.environmentId());
        } catch (Exception e) {
            errors.add(Error.severe(e.getMessage()));
        }

        return Validator.Result.ofBoth(new Input(input.auditInfo(), sanitizedBuilder.build()), errors);
    }

    public void validate(SharedPolicyGroup sharedPolicyGroup, String environmentId) {
        validateCreateSharedPolicyGroup(sharedPolicyGroup, environmentId);
    }

    private void validateCreateSharedPolicyGroup(SharedPolicyGroup sharedPolicyGroupToCreate, String environmentId) {
        if (!sharedPolicyGroupToCreate.hasName()) {
            throw new InvalidDataException("Name is required.");
        }
        if (sharedPolicyGroupToCreate.getApiType() == null) {
            throw new InvalidDataException("ApiType is required.");
        }
        if (sharedPolicyGroupToCreate.getPhase() == null) {
            throw new InvalidDataException("Phase is required.");
        }
        if (!sharedPolicyGroupToCreate.hasValidPhase()) {
            throw new SharedPolicyGroupInvalidPhaseException(
                sharedPolicyGroupToCreate.getPhase().name(),
                sharedPolicyGroupToCreate.getApiType().name()
            );
        }

        ensureSharedPolicyGroupDoesNotExist(sharedPolicyGroupToCreate, environmentId);
    }

    private void ensureSharedPolicyGroupDoesNotExist(SharedPolicyGroup sharedPolicyGroupToCreate, String environmentId) {
        this.sharedPolicyGroupCrudService.findByEnvironmentIdAndCrossId(environmentId, sharedPolicyGroupToCreate.getCrossId()).ifPresent(
            sharedPolicyGroup -> {
                throw new SharedPolicyGroupDuplicateCrossIdException(sharedPolicyGroupToCreate.getCrossId(), environmentId);
            }
        );

        // Validate and sanitize policies configuration
        if (sharedPolicyGroupToCreate.getSteps() != null) {
            sharedPolicyGroupToCreate
                .getSteps()
                .stream()
                .filter(Step::isEnabled)
                .forEach(step ->
                    step.setConfiguration(
                        policyValidationDomainService.validateAndSanitizeConfiguration(step.getPolicy(), step.getConfiguration())
                    )
                );

            policyValidationDomainService.validatePoliciesFlowPhase(
                sharedPolicyGroupToCreate.getSteps().stream().map(Step::getPolicy).toList(),
                sharedPolicyGroupToCreate.getApiType(),
                sharedPolicyGroupToCreate.getPhase()
            );
        }
    }
}

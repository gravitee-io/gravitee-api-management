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
package io.gravitee.apim.core.shared_policy_group.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.shared_policy_group.crud_service.SharedPolicyGroupCrudService;
import io.gravitee.apim.core.shared_policy_group.domain_service.ValidateSharedPolicyGroupCRDDomainService;
import io.gravitee.apim.core.shared_policy_group.model.SharedPolicyGroup;
import io.gravitee.apim.core.shared_policy_group.model.SharedPolicyGroupCRD;
import io.gravitee.apim.core.shared_policy_group.model.SharedPolicyGroupCRDStatus;
import io.gravitee.apim.core.validation.Validator;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
@UseCase
@RequiredArgsConstructor
public class ImportSharedPolicyGroupCRDCRDUseCase {

    private final CreateSharedPolicyGroupUseCase createSharedPolicyGroupUseCase;
    private final UpdateSharedPolicyGroupUseCase updateSharedPolicyGroupUseCase;
    private final DeploySharedPolicyGroupUseCase deploySharedPolicyGroupUseCase;
    private final ValidateSharedPolicyGroupCRDDomainService validateSharedPolicyGroupCRDDomainService;
    private final SharedPolicyGroupCrudService sharedPolicyGroupCrudService;

    public Output execute(Input input) {
        var validationResult = validateSharedPolicyGroupCRDDomainService.validateAndSanitize(
            new ValidateSharedPolicyGroupCRDDomainService.Input(input.auditInfo, input.crd)
        );

        validationResult
            .severe()
            .ifPresent(errors -> {
                throw new ValidationDomainException(
                    String.format(
                        "Unable to import because of errors [%s]",
                        String.join(",", errors.stream().map(Validator.Error::getMessage).toList())
                    )
                );
            });

        Optional<SharedPolicyGroup> sharedPolicyGroup = sharedPolicyGroupCrudService.findByEnvironmentIdAndCrossId(
            input.auditInfo.environmentId(),
            input.crd.getCrossId()
        );

        if (sharedPolicyGroup.isPresent()) {
            return updateSharedPolicyGroup(input);
        } else {
            return createSharedPolicyGroup(input);
        }
    }

    public record Output(SharedPolicyGroupCRDStatus status) {}

    public record Input(AuditInfo auditInfo, SharedPolicyGroupCRD crd) {}

    private Output createSharedPolicyGroup(Input input) {
        // Create a new Shared Policy Group
        var output = createSharedPolicyGroupUseCase.execute(
            new CreateSharedPolicyGroupUseCase.Input(input.crd().toCreateSharedPolicyGroup(), input.auditInfo)
        );

        final Output result = new Output(
            new SharedPolicyGroupCRDStatus(
                output.sharedPolicyGroup().getCrossId(),
                output.sharedPolicyGroup().getId(),
                input.auditInfo.organizationId(),
                input.auditInfo.environmentId(),
                null
            )
        );

        // Deploy the newly created Shared Policy group
        deploySharedPolicyGroupUseCase.execute(
            new DeploySharedPolicyGroupUseCase.Input(output.sharedPolicyGroup().getId(), input.auditInfo.environmentId(), input.auditInfo)
        );

        return result;
    }

    private Output updateSharedPolicyGroup(Input input) {
        var output = updateSharedPolicyGroupUseCase.execute(
            new UpdateSharedPolicyGroupUseCase.Input(
                input.crd.getSharedPolicyGroupId(),
                input.crd.toUpdateSharedPolicyGroup(),
                input.auditInfo
            )
        );

        // Deploy the SGP if the existing status is not DEPLOYED
        if (output.sharedPolicyGroup().getLifecycleState() != SharedPolicyGroup.SharedPolicyGroupLifecycleState.DEPLOYED) {
            // Deploy the newly created Shared Policy group
            deploySharedPolicyGroupUseCase.execute(
                new DeploySharedPolicyGroupUseCase.Input(
                    output.sharedPolicyGroup().getId(),
                    input.auditInfo.environmentId(),
                    input.auditInfo
                )
            );
        }

        return new Output(
            new SharedPolicyGroupCRDStatus(
                output.sharedPolicyGroup().getCrossId(),
                output.sharedPolicyGroup().getId(),
                input.auditInfo.organizationId(),
                input.auditInfo.environmentId(),
                null
            )
        );
    }
}

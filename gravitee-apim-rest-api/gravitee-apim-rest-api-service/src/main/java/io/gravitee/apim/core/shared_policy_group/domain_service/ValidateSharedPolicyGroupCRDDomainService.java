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
import io.gravitee.apim.core.shared_policy_group.crud_service.SharedPolicyGroupCrudService;
import io.gravitee.apim.core.shared_policy_group.model.SharedPolicyGroupCRD;
import io.gravitee.apim.core.validation.Validator;
import io.gravitee.rest.api.service.common.IdBuilder;
import java.util.ArrayList;
import lombok.RequiredArgsConstructor;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
@DomainService
@RequiredArgsConstructor
public class ValidateSharedPolicyGroupCRDDomainService implements Validator<ValidateSharedPolicyGroupCRDDomainService.Input> {

    private final SharedPolicyGroupCrudService sharedPolicyGroupCrudService;
    private final ValidateCreateSharedPolicyGroupDomainService validateCreateSharedPolicyGroupDomainService;
    private final ValidateUpdateSharedPolicyGroupDomainService validateUpdateSharedPolicyGroupDomainService;

    public record Input(AuditInfo auditInfo, SharedPolicyGroupCRD crd) implements Validator.Input {}

    @Override
    public Result<Input> validateAndSanitize(Input input) {
        var errors = new ArrayList<Error>();

        if (input.crd().getCrossId() == null) {
            IdBuilder idBuilder = IdBuilder.builder(input.auditInfo, input.crd.getHrid());
            input.crd().setCrossId(idBuilder.buildCrossId());
            input.crd().setSharedPolicyGroupId(idBuilder.buildId());
        } else {
            IdBuilder idBuilder = IdBuilder.builder(input.auditInfo, input.crd.getCrossId());
            input.crd().setSharedPolicyGroupId(idBuilder.buildId());
            // ID are sent by GKO upgraded resources, so HRID does not make sense
            // to avoid confusion when looking into the database, we remove it
            input.crd().setHrid(null);
        }

        var sanitizedBuilder = input.crd.toBuilder();

        sharedPolicyGroupCrudService
            .findByEnvironmentIdAndCrossId(input.auditInfo.environmentId(), input.crd.getCrossId())
            .ifPresentOrElse(
                spg -> {
                    input.crd.setSharedPolicyGroupId(spg.getId());
                    validateUpdateSharedPolicyGroupDomainService
                        .validateAndSanitize(
                            new ValidateUpdateSharedPolicyGroupDomainService.Input(input.auditInfo, input.crd.toSharedPolicyGroup())
                        )
                        .peek(i -> {}, errors::addAll);
                },
                () ->
                    validateCreateSharedPolicyGroupDomainService
                        .validateAndSanitize(
                            new ValidateCreateSharedPolicyGroupDomainService.Input(input.auditInfo, input.crd.toSharedPolicyGroup())
                        )
                        .peek(i -> {}, errors::addAll)
            );

        return Validator.Result.ofBoth(new Input(input.auditInfo(), sanitizedBuilder.build()), errors);
    }
}

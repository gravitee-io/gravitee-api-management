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
package io.gravitee.apim.core.application.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.application.model.crd.ApplicationCRDSpec;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.group.domain_service.ValidateGroupsDomainService;
import io.gravitee.apim.core.member.domain_service.ValidateCRDMembersDomainService;
import io.gravitee.apim.core.validation.Validator;
import java.util.ArrayList;
import lombok.RequiredArgsConstructor;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DomainService
@RequiredArgsConstructor
public class ValidateApplicationCRDDomainService implements Validator<ValidateApplicationCRDDomainService.Input> {

    public record Input(AuditInfo auditInfo, ApplicationCRDSpec spec) implements Validator.Input {}

    private final ValidateGroupsDomainService groupsValidator;

    private final ValidateCRDMembersDomainService membersValidator;

    @Override
    public Result<ValidateApplicationCRDDomainService.Input> validateAndSanitize(ValidateApplicationCRDDomainService.Input input) {
        var errors = new ArrayList<Error>();
        var sanitizedBuilder = input.spec().toBuilder();

        groupsValidator
            .validateAndSanitize(new ValidateGroupsDomainService.Input(input.auditInfo.environmentId(), input.spec().getGroups()))
            .peek(sanitized -> sanitizedBuilder.groups(sanitized.groups()), errors::addAll);

        membersValidator
            .validateAndSanitize(new ValidateCRDMembersDomainService.Input(input.auditInfo.organizationId(), input.spec.getMembers()))
            .peek(sanitized -> sanitizedBuilder.members(sanitized.members()), errors::addAll);

        return Validator.Result.ofBoth(new Input(input.auditInfo(), sanitizedBuilder.build()), errors);
    }
}

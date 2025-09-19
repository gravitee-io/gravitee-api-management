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
package io.gravitee.apim.core.group.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.group.crud_service.GroupCrudService;
import io.gravitee.apim.core.group.domain_service.ValidateGroupCRDDomainService;
import io.gravitee.apim.core.group.model.crd.GroupCRDSpec;
import io.gravitee.apim.core.group.model.crd.GroupCRDStatus;
import io.gravitee.apim.core.group.query_service.GroupQueryService;
import io.gravitee.apim.core.member.domain_service.CRDMembersDomainService;
import io.gravitee.apim.core.validation.Validator;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@UseCase
@Slf4j
public class ImportGroupCRDUseCase {

    public record Input(AuditInfo auditInfo, GroupCRDSpec spec) implements Validator.Input {}

    public record Output(GroupCRDStatus status) {}

    private final ValidateGroupCRDDomainService validationService;
    private final GroupQueryService queryService;
    private final GroupCrudService crudService;
    private final CRDMembersDomainService membersService;

    public ImportGroupCRDUseCase(
        ValidateGroupCRDDomainService validationService,
        GroupQueryService queryService,
        GroupCrudService crudService,
        CRDMembersDomainService membersService
    ) {
        this.validationService = validationService;
        this.queryService = queryService;
        this.crudService = crudService;
        this.membersService = membersService;
    }

    public Output execute(Input input) {
        return queryService
            .findById(input.spec.getId())
            .map(existing -> this.update(input))
            .orElseGet(() -> this.create(input));
    }

    private Output create(Input input) {
        var validationResult = validationService.validateAndSanitize(new ValidateGroupCRDDomainService.Input(input.auditInfo, input.spec));

        validationResult
            .severe()
            .ifPresent(errors -> {
                throw new TechnicalDomainException(errors.iterator().next().getMessage());
            });

        var errors = validationResult.errors().map(GroupCRDStatus.Errors::fromErrorList).orElse(GroupCRDStatus.Errors.EMPTY);

        crudService.create(input.spec.toGroup(input.auditInfo.environmentId()));
        membersService.updateGroupMembers(input.auditInfo, input.spec.getId(), input.spec.getMembers());
        return new Output(new GroupCRDStatus(input.spec.getId(), input.spec.getMembers().size(), errors));
    }

    private Output update(Input input) {
        var validationResult = validationService.validateAndSanitize(new ValidateGroupCRDDomainService.Input(input.auditInfo, input.spec));

        validationResult
            .severe()
            .ifPresent(errors -> {
                throw new TechnicalDomainException(errors.iterator().next().getMessage());
            });

        var errors = validationResult.errors().map(GroupCRDStatus.Errors::fromErrorList).orElse(GroupCRDStatus.Errors.EMPTY);

        crudService.update(input.spec.toGroup(input.auditInfo.environmentId()));
        membersService.updateGroupMembers(input.auditInfo, input.spec.getId(), input.spec.getMembers());
        return new Output(new GroupCRDStatus(input.spec.getId(), input.spec.getMembers().size(), errors));
    }
}

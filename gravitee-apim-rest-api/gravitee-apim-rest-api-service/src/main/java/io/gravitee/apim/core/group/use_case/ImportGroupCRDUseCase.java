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
import java.util.List;
import lombok.CustomLog;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@UseCase
@CustomLog
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
        var validationResult = validationService
            .validateAndSanitize(new ValidateGroupCRDDomainService.Input(input.auditInfo, input.spec))
            .map(sanitized -> new Input(sanitized.auditInfo(), sanitized.spec()));

        validationResult
            .severe()
            .ifPresent(errors -> {
                throw new TechnicalDomainException(errors.iterator().next().getMessage());
            });

        var warnings = validationResult.warning().orElseGet(List::of);
        var sanitizedInput = validationResult.value().orElseThrow(() -> new TechnicalDomainException("Unable to sanitize CRD spec"));

        var status = queryService
            .findById(sanitizedInput.spec.getId())
            .map(existing -> this.update(sanitizedInput))
            .orElseGet(() -> this.create(sanitizedInput));

        status.setErrors(GroupCRDStatus.Errors.fromErrorList(warnings));

        return new Output(status);
    }

    private GroupCRDStatus create(Input input) {
        crudService.create(input.spec.toGroup(input.auditInfo.environmentId()));
        return syncGroupMemberships(input);
    }

    private GroupCRDStatus update(Input input) {
        crudService.update(input.spec.toGroup(input.auditInfo.environmentId()));
        return syncGroupMemberships(input);
    }

    private GroupCRDStatus syncGroupMemberships(Input input) {
        membersService.updateGroupDefaultRoles(
            input.auditInfo,
            input.spec.getId(),
            input.spec.getApiRole(),
            input.spec.getApplicationRole(),
            input.spec.getApiProductRole()
        );
        membersService.updateGroupMembers(input.auditInfo, input.spec.getId(), input.spec.getMembers());
        return GroupCRDStatus.builder().id(input.spec.getId()).members(input.spec.getMembers().size()).build();
    }
}

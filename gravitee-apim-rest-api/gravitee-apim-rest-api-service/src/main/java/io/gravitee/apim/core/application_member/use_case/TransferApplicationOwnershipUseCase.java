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
package io.gravitee.apim.core.application_member.use_case;

import static io.gravitee.rest.api.model.permissions.SystemRole.PRIMARY_OWNER;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.membership.domain_service.MembershipDomainService;
import io.gravitee.apim.core.membership.exception.RoleNotFoundException;
import io.gravitee.apim.core.membership.model.TransferOwnership;
import io.gravitee.apim.core.membership.query_service.RoleQueryService;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.common.ReferenceContext;
import io.gravitee.rest.api.service.exceptions.SinglePrimaryOwnerException;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class TransferApplicationOwnershipUseCase {

    private final MembershipDomainService membershipDomainService;
    private final RoleQueryService roleQueryService;

    public record Input(
        String applicationId,
        String newPrimaryOwnerId,
        String newPrimaryOwnerReference,
        String previousOwnerNewRole,
        String organizationId
    ) {}

    public record Output() {}

    public Output execute(Input input) {
        validateRole(input.previousOwnerNewRole(), input.organizationId());

        membershipDomainService.transferOwnership(
            TransferOwnership.builder()
                .newPrimaryOwnerId(input.newPrimaryOwnerId())
                .userReference(input.newPrimaryOwnerReference())
                .currentPrimaryOwnerNewRole(input.previousOwnerNewRole())
                .build(),
            RoleScope.APPLICATION,
            input.applicationId()
        );
        return new Output();
    }

    private void validateRole(String roleName, String organizationId) {
        if (PRIMARY_OWNER.name().equals(roleName)) {
            throw new SinglePrimaryOwnerException(RoleScope.APPLICATION);
        }

        var roleContext = ReferenceContext.builder().referenceType(ReferenceContext.Type.ORGANIZATION).referenceId(organizationId).build();
        roleQueryService.findApplicationRole(roleName, roleContext).orElseThrow(() -> new RoleNotFoundException(roleName, roleContext));
    }
}

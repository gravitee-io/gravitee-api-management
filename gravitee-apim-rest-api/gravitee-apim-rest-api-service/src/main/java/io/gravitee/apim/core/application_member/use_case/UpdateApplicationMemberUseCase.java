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
import io.gravitee.apim.core.exception.NotFoundDomainException;
import io.gravitee.apim.core.member.model.Member;
import io.gravitee.apim.core.member.model.MembershipMember;
import io.gravitee.apim.core.member.model.MembershipMemberType;
import io.gravitee.apim.core.member.model.MembershipReference;
import io.gravitee.apim.core.member.model.MembershipReferenceType;
import io.gravitee.apim.core.member.model.MembershipRole;
import io.gravitee.apim.core.member.query_service.MemberQueryService;
import io.gravitee.apim.core.membership.exception.RoleNotFoundException;
import io.gravitee.apim.core.membership.query_service.RoleQueryService;
import io.gravitee.rest.api.service.common.ReferenceContext;
import io.gravitee.rest.api.service.exceptions.SinglePrimaryOwnerException;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class UpdateApplicationMemberUseCase {

    private final MemberQueryService memberQueryService;
    private final RoleQueryService roleQueryService;

    public record Input(String applicationId, String memberId, String roleName, String organizationId) {}

    public record Output(Member updatedMember) {}

    public Output execute(Input input) {
        validateRole(input.roleName(), input.organizationId());

        var reference = new MembershipReference(MembershipReferenceType.APPLICATION, input.applicationId());
        var member = MembershipMember.builder().memberId(input.memberId()).memberType(MembershipMemberType.USER).build();
        var role = MembershipRole.builder().scope(io.gravitee.apim.core.member.model.RoleScope.APPLICATION).name(input.roleName()).build();

        var updatedMember = memberQueryService.updateRoleToMemberOnReference(reference, member, role);
        if (updatedMember == null) {
            throw new NotFoundDomainException("Application member not found: " + input.memberId(), input.memberId());
        }

        return new Output(updatedMember);
    }

    private void validateRole(String roleName, String organizationId) {
        if (PRIMARY_OWNER.name().equals(roleName)) {
            throw new SinglePrimaryOwnerException(io.gravitee.rest.api.model.permissions.RoleScope.APPLICATION);
        }

        var roleContext = ReferenceContext.builder().referenceType(ReferenceContext.Type.ORGANIZATION).referenceId(organizationId).build();
        roleQueryService.findApplicationRole(roleName, roleContext).orElseThrow(() -> new RoleNotFoundException(roleName, roleContext));
    }
}

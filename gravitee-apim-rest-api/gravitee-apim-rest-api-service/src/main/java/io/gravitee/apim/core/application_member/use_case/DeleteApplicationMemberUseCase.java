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
import io.gravitee.apim.core.member.model.MembershipMemberType;
import io.gravitee.apim.core.member.model.MembershipReferenceType;
import io.gravitee.apim.core.member.query_service.MemberQueryService;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.exceptions.SinglePrimaryOwnerException;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class DeleteApplicationMemberUseCase {

    private final MemberQueryService memberQueryService;

    public record Input(String applicationId, String memberId) {}

    public record Output() {}

    public Output execute(Input input) {
        var member = memberQueryService.getUserMember(MembershipReferenceType.APPLICATION, input.applicationId(), input.memberId());
        if (member == null) {
            throw new NotFoundDomainException("Application member not found: " + input.memberId(), input.memberId());
        }

        var isPrimaryOwner =
            member.getRoles() != null &&
            member
                .getRoles()
                .stream()
                .anyMatch(role -> PRIMARY_OWNER.name().equals(role.getName()));
        if (isPrimaryOwner) {
            throw new SinglePrimaryOwnerException(RoleScope.APPLICATION);
        }

        memberQueryService.deleteReferenceMember(
            MembershipReferenceType.APPLICATION,
            input.applicationId(),
            MembershipMemberType.USER,
            input.memberId()
        );
        return new Output();
    }
}

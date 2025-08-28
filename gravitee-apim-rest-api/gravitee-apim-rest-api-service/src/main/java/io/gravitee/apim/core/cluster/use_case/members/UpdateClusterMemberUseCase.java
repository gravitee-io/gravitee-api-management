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
package io.gravitee.apim.core.cluster.use_case.members;

import static io.gravitee.rest.api.model.permissions.SystemRole.PRIMARY_OWNER;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.member.model.Member;
import io.gravitee.apim.core.member.model.MembershipMember;
import io.gravitee.apim.core.member.model.MembershipMemberType;
import io.gravitee.apim.core.member.model.MembershipReference;
import io.gravitee.apim.core.member.model.MembershipReferenceType;
import io.gravitee.apim.core.member.model.MembershipRole;
import io.gravitee.apim.core.member.query_service.MemberQueryService;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.exceptions.SinglePrimaryOwnerException;
import lombok.AllArgsConstructor;

@UseCase
@AllArgsConstructor
public class UpdateClusterMemberUseCase {

    private final MemberQueryService memberQueryService;

    public record Input(String newRole, String memberId, String clusterId) {}

    public record Output(Member updatedMember) {}

    public Output execute(Input input) {
        validate(input.newRole);

        var reference = new MembershipReference(MembershipReferenceType.CLUSTER, input.clusterId);
        var member = MembershipMember.builder().memberId(input.memberId).memberType(MembershipMemberType.USER).build();
        var role = MembershipRole.builder().scope(io.gravitee.apim.core.member.model.RoleScope.CLUSTER).name(input.newRole).build();
        Member updatedMember = memberQueryService.updateRoleToMemberOnReference(reference, member, role);

        return new Output(updatedMember);
    }

    private void validate(String newRole) {
        if (PRIMARY_OWNER.name().equals(newRole)) {
            throw new SinglePrimaryOwnerException(RoleScope.CLUSTER);
        }
    }
}

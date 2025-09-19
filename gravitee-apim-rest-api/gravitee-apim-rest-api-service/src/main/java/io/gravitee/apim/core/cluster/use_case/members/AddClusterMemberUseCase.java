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

import static io.gravitee.rest.api.model.permissions.RoleScope.CLUSTER;
import static io.gravitee.rest.api.model.permissions.SystemRole.PRIMARY_OWNER;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.membership.crud_service.MembershipCrudService;
import io.gravitee.apim.core.membership.model.AddMember;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.membership.model.Role;
import io.gravitee.apim.core.membership.query_service.RoleQueryService;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.exceptions.SinglePrimaryOwnerException;
import java.time.ZonedDateTime;
import lombok.AllArgsConstructor;

@UseCase
@AllArgsConstructor
public class AddClusterMemberUseCase {

    private final MembershipCrudService membershipCrudService;
    private final RoleQueryService roleQueryService;

    public record Input(AuditInfo audit, AddMember addMember, String clusterId) {}

    public record Output() {}

    public Output execute(Input input) {
        validateMembership(input.addMember);

        Role role = roleQueryService
            .findByScopeAndName(Role.Scope.CLUSTER, input.addMember.getRoleName(), input.audit.organizationId())
            .orElseThrow();
        ZonedDateTime now = TimeProvider.now();
        Membership membership = Membership.builder()
            .id(UuidString.generateRandom())
            .referenceType(Membership.ReferenceType.CLUSTER)
            .referenceId(input.clusterId)
            .memberType(Membership.Type.USER)
            .memberId(input.addMember.getUserId())
            .roleId(role.getId())
            .createdAt(now)
            .updatedAt(now)
            .build();
        membershipCrudService.create(membership);

        return new Output();
    }

    private void validateMembership(AddMember addMember) {
        if (PRIMARY_OWNER.name().equals(addMember.getRoleName())) {
            throw new SinglePrimaryOwnerException(CLUSTER);
        }
    }
}

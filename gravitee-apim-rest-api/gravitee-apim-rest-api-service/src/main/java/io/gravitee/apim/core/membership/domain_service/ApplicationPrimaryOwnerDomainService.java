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
package io.gravitee.apim.core.membership.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.group.query_service.GroupQueryService;
import io.gravitee.apim.core.membership.exception.ApiPrimaryOwnerNotFoundException;
import io.gravitee.apim.core.membership.exception.ApplicationPrimaryOwnerNotFoundException;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.apim.core.membership.model.Role;
import io.gravitee.apim.core.membership.query_service.MembershipQueryService;
import io.gravitee.apim.core.membership.query_service.RoleQueryService;
import io.gravitee.apim.core.user.crud_service.UserCrudService;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.service.common.ReferenceContext;
import java.util.Optional;

@DomainService
public class ApplicationPrimaryOwnerDomainService {

    private final GroupQueryService groupQueryService;
    private final MembershipQueryService membershipQueryService;
    private final RoleQueryService roleQueryService;
    private final UserCrudService userCrudService;

    public ApplicationPrimaryOwnerDomainService(
        GroupQueryService groupQueryService,
        MembershipQueryService membershipQueryService,
        RoleQueryService roleQueryService,
        UserCrudService userCrudService
    ) {
        this.groupQueryService = groupQueryService;
        this.membershipQueryService = membershipQueryService;
        this.roleQueryService = roleQueryService;
        this.userCrudService = userCrudService;
    }

    public PrimaryOwnerEntity getApplicationPrimaryOwner(final String organizationId, String applicationId)
        throws ApiPrimaryOwnerNotFoundException {
        return findPrimaryOwnerRole(organizationId)
            .flatMap(role ->
                findApplicationPrimaryOwnerMembership(applicationId, role).flatMap(membership ->
                    switch (membership.getMemberType()) {
                        case USER -> findUserPrimaryOwner(membership);
                        case GROUP -> findGroupPrimaryOwner(membership, role.getId());
                    }
                )
            )
            .orElseThrow(() -> new ApplicationPrimaryOwnerNotFoundException(applicationId));
    }

    private Optional<Role> findPrimaryOwnerRole(String organizationId) {
        return roleQueryService.findApplicationRole(
            SystemRole.PRIMARY_OWNER.name(),
            ReferenceContext.builder().referenceType(ReferenceContext.Type.ORGANIZATION).referenceId(organizationId).build()
        );
    }

    private Optional<Membership> findApplicationPrimaryOwnerMembership(String apiId, Role role) {
        return membershipQueryService
            .findByReferenceAndRoleId(Membership.ReferenceType.APPLICATION, apiId, role.getId())
            .stream()
            .findFirst();
    }

    private Optional<PrimaryOwnerEntity> findUserPrimaryOwner(Membership membership) {
        return userCrudService
            .findBaseUserById(membership.getMemberId())
            .map(user ->
                PrimaryOwnerEntity.builder()
                    .id(user.getId())
                    .displayName(user.displayName())
                    .email(user.getEmail())
                    .type(PrimaryOwnerEntity.Type.USER)
                    .build()
            );
    }

    private Optional<PrimaryOwnerEntity> findGroupPrimaryOwner(Membership membership, String primaryOwnerRoleId) {
        var group = groupQueryService.findById(membership.getMemberId());
        var user = findPrimaryOwnerGroupMember(membership.getMemberId(), primaryOwnerRoleId).flatMap(m ->
            userCrudService.findBaseUserById(m.getMemberId())
        );

        return group.map(value ->
            PrimaryOwnerEntity.builder()
                .id(value.getId())
                .displayName(value.getName())
                .type(PrimaryOwnerEntity.Type.GROUP)
                .email(user.map(BaseUserEntity::getEmail).orElse(null))
                .build()
        );
    }

    private Optional<Membership> findPrimaryOwnerGroupMember(String groupId, String primaryOwnerRoleId) {
        return membershipQueryService
            .findByReferenceAndRoleId(Membership.ReferenceType.GROUP, groupId, primaryOwnerRoleId)
            .stream()
            .findFirst();
    }
}

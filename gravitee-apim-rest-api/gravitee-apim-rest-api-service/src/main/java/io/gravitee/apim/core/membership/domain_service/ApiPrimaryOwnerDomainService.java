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
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.ApiAuditLogEntity;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.audit.model.event.MembershipAuditEvent;
import io.gravitee.apim.core.group.query_service.GroupQueryService;
import io.gravitee.apim.core.membership.crud_service.MembershipCrudService;
import io.gravitee.apim.core.membership.exception.ApiPrimaryOwnerNotFoundException;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.apim.core.membership.model.Role;
import io.gravitee.apim.core.membership.query_service.MembershipQueryService;
import io.gravitee.apim.core.membership.query_service.RoleQueryService;
import io.gravitee.apim.core.user.crud_service.UserCrudService;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.service.common.ReferenceContext;
import io.gravitee.rest.api.service.common.UuidString;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class ApiPrimaryOwnerDomainService {

    private final AuditDomainService auditService;
    private final GroupQueryService groupQueryService;
    private final MembershipCrudService membershipCrudService;
    private final MembershipQueryService membershipQueryService;
    private final RoleQueryService roleQueryService;
    private final UserCrudService userCrudService;

    public PrimaryOwnerEntity getApiPrimaryOwner(final String organizationId, String apiId) throws ApiPrimaryOwnerNotFoundException {
        return findPrimaryOwnerRole(organizationId)
            .flatMap(role ->
                findApiPrimaryOwnerMembership(apiId, role).flatMap(membership ->
                    switch (membership.getMemberType()) {
                        case USER -> findUserPrimaryOwner(membership);
                        case GROUP -> findGroupPrimaryOwner(membership, role.getId());
                    }
                )
            )
            .orElseThrow(() -> new ApiPrimaryOwnerNotFoundException(apiId));
    }

    public void createApiPrimaryOwnerMembership(String apiId, PrimaryOwnerEntity primaryOwner, AuditInfo auditInfo) {
        findPrimaryOwnerRole(auditInfo.organizationId()).ifPresent(role -> {
            var membership = Membership.builder()
                .id(UuidString.generateRandom())
                .referenceId(apiId)
                .referenceType(Membership.ReferenceType.API)
                .roleId(role.getId())
                .memberId(primaryOwner.id())
                .memberType(Membership.Type.valueOf(primaryOwner.type().name()))
                .createdAt(TimeProvider.now())
                .updatedAt(TimeProvider.now())
                .build();
            membershipCrudService.create(membership);

            createAuditLog(membership, auditInfo);
        });
    }

    private Optional<Role> findPrimaryOwnerRole(String organizationId) {
        return roleQueryService.findApiRole(
            SystemRole.PRIMARY_OWNER.name(),
            ReferenceContext.builder().referenceType(ReferenceContext.Type.ORGANIZATION).referenceId(organizationId).build()
        );
    }

    private Optional<Membership> findApiPrimaryOwnerMembership(String apiId, Role role) {
        return membershipQueryService.findByReferenceAndRoleId(Membership.ReferenceType.API, apiId, role.getId()).stream().findFirst();
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

    private void createAuditLog(Membership membership, AuditInfo auditInfo) {
        Map<AuditProperties, String> properties = switch (membership.getMemberType()) {
            case USER -> Map.of(AuditProperties.USER, membership.getMemberId());
            case GROUP -> Map.of(AuditProperties.GROUP, membership.getMemberId());
        };

        auditService.createApiAuditLog(
            ApiAuditLogEntity.builder()
                .organizationId(auditInfo.organizationId())
                .environmentId(auditInfo.environmentId())
                .apiId(membership.getReferenceId())
                .event(MembershipAuditEvent.MEMBERSHIP_CREATED)
                .actor(auditInfo.actor())
                .newValue(membership)
                .createdAt(membership.getCreatedAt())
                .properties(properties)
                .build()
        );
    }
}

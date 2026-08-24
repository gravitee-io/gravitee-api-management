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
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.audit.model.event.MembershipAuditEvent;
import io.gravitee.apim.core.group.model.Group;
import io.gravitee.apim.core.group.query_service.GroupQueryService;
import io.gravitee.apim.core.membership.crud_service.MembershipCrudService;
import io.gravitee.apim.core.membership.exception.ApiProductPrimaryOwnerNotFoundException;
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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class ApiProductPrimaryOwnerDomainService {

    private final AuditDomainService auditService;
    private final GroupQueryService groupQueryService;
    private final MembershipCrudService membershipCrudService;
    private final MembershipQueryService membershipQueryService;
    private final RoleQueryService roleQueryService;
    private final UserCrudService userCrudService;

    public void createApiProductPrimaryOwnerMembership(String apiProductId, PrimaryOwnerEntity primaryOwner, AuditInfo auditInfo) {
        findPrimaryOwnerRole(auditInfo.organizationId()).ifPresent(role -> {
            var membership = Membership.builder()
                .id(UuidString.generateRandom())
                .referenceId(apiProductId)
                .referenceType(Membership.ReferenceType.API_PRODUCT)
                .roleId(role.getId())
                .memberId(primaryOwner.id())
                .memberType(Membership.Type.valueOf(primaryOwner.type().name()))
                .createdAt(TimeProvider.now())
                .updatedAt(TimeProvider.now())
                .build();
            membershipCrudService.create(membership);
            createApiProductAuditLog(membership, auditInfo);
        });
    }

    public void assignPrimaryOwner(String apiProductId, PrimaryOwnerEntity primaryOwner, AuditInfo auditInfo) {
        createApiProductPrimaryOwnerMembership(apiProductId, primaryOwner, auditInfo);
    }

    public PrimaryOwnerEntity getApiProductPrimaryOwner(final String organizationId, String apiProductId)
        throws ApiProductPrimaryOwnerNotFoundException {
        return findPrimaryOwnerRole(organizationId)
            .flatMap(role ->
                findApiProductPrimaryOwnerMembership(apiProductId, role).flatMap(membership ->
                    switch (membership.getMemberType()) {
                        case USER -> findUserPrimaryOwner(membership);
                        case GROUP -> findGroupPrimaryOwner(membership, role.getId());
                    }
                )
            )
            .orElseThrow(() -> new ApiProductPrimaryOwnerNotFoundException(apiProductId));
    }

    /**
     * The primary owner of each of several API Products, resolved together.
     *
     * <p>Costs a fixed handful of queries however many products are asked for, so listing a page does not
     * cost three round trips per row. Products with no primary owner are absent from the result rather
     * than raising: a listing renders the rest of the row instead of failing.</p>
     */
    public Map<String, PrimaryOwnerEntity> getApiProductPrimaryOwners(final String organizationId, Set<String> apiProductIds) {
        if (apiProductIds.isEmpty()) {
            return Map.of();
        }
        return findPrimaryOwnerRole(organizationId)
            .map(role -> resolvePrimaryOwners(apiProductIds, role))
            .orElseGet(Map::of);
    }

    private Map<String, PrimaryOwnerEntity> resolvePrimaryOwners(Set<String> apiProductIds, Role role) {
        Collection<Membership> memberships = membershipQueryService.findByReferencesAndRoleId(
            Membership.ReferenceType.API_PRODUCT,
            List.copyOf(apiProductIds),
            role.getId()
        );
        if (memberships.isEmpty()) {
            return Map.of();
        }

        Map<Membership.Type, Set<String>> memberIdsByType = memberships
            .stream()
            .collect(Collectors.groupingBy(Membership::getMemberType, Collectors.mapping(Membership::getMemberId, Collectors.toSet())));
        Set<String> userIds = memberIdsByType.getOrDefault(Membership.Type.USER, Set.of());
        Set<String> groupIds = memberIdsByType.getOrDefault(Membership.Type.GROUP, Set.of());

        Map<String, BaseUserEntity> usersById = findUsersById(userIds);
        Map<String, PrimaryOwnerEntity> groupOwnersById = findGroupOwnersById(groupIds, role.getId());

        Map<String, PrimaryOwnerEntity> ownersByApiProductId = new HashMap<>();
        memberships.forEach(membership -> {
            PrimaryOwnerEntity owner = switch (membership.getMemberType()) {
                case USER -> Optional.ofNullable(usersById.get(membership.getMemberId())).map(this::toUserPrimaryOwner).orElse(null);
                case GROUP -> groupOwnersById.get(membership.getMemberId());
            };
            if (owner != null) {
                ownersByApiProductId.putIfAbsent(membership.getReferenceId(), owner);
            }
        });
        return ownersByApiProductId;
    }

    private Map<String, BaseUserEntity> findUsersById(Set<String> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userCrudService
            .findBaseUsersByIds(List.copyOf(userIds))
            .stream()
            .collect(Collectors.toMap(BaseUserEntity::getId, Function.identity(), (first, second) -> first));
    }

    /**
     * A group's own primary-owner member supplies the contact email, so groups cost one extra membership
     * query for the whole batch rather than one per group.
     */
    private Map<String, PrimaryOwnerEntity> findGroupOwnersById(Set<String> groupIds, String primaryOwnerRoleId) {
        if (groupIds.isEmpty()) {
            return Map.of();
        }
        Collection<Membership> groupMemberships = membershipQueryService.findByReferencesAndRoleId(
            Membership.ReferenceType.GROUP,
            List.copyOf(groupIds),
            primaryOwnerRoleId
        );
        Map<String, String> memberIdByGroupId = groupMemberships
            .stream()
            .collect(Collectors.toMap(Membership::getReferenceId, Membership::getMemberId, (first, second) -> first));
        Map<String, BaseUserEntity> groupMembersById = findUsersById(Set.copyOf(memberIdByGroupId.values()));

        return groupQueryService
            .findByIds(groupIds)
            .stream()
            .collect(
                Collectors.toMap(Group::getId, group ->
                    PrimaryOwnerEntity.builder()
                        .id(group.getId())
                        .displayName(group.getName())
                        .type(PrimaryOwnerEntity.Type.GROUP)
                        .email(
                            Optional.ofNullable(memberIdByGroupId.get(group.getId()))
                                .map(groupMembersById::get)
                                .map(BaseUserEntity::getEmail)
                                .orElse(null)
                        )
                        .build()
                )
            );
    }

    private PrimaryOwnerEntity toUserPrimaryOwner(BaseUserEntity user) {
        return PrimaryOwnerEntity.builder()
            .id(user.getId())
            .displayName(user.displayName())
            .email(user.getEmail())
            .type(PrimaryOwnerEntity.Type.USER)
            .build();
    }

    private Optional<Membership> findApiProductPrimaryOwnerMembership(String apiProductId, Role role) {
        return membershipQueryService
            .findByReferenceAndRoleId(Membership.ReferenceType.API_PRODUCT, apiProductId, role.getId())
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

    private Optional<Role> findPrimaryOwnerRole(String organizationId) {
        return roleQueryService.findApiProductRole(
            SystemRole.PRIMARY_OWNER.name(),
            ReferenceContext.builder().referenceType(ReferenceContext.Type.ORGANIZATION).referenceId(organizationId).build()
        );
    }

    private void createApiProductAuditLog(Membership membership, AuditInfo auditInfo) {
        Map<AuditProperties, String> properties = switch (membership.getMemberType()) {
            case USER -> Map.of(AuditProperties.USER, membership.getMemberId());
            case GROUP -> Map.of(AuditProperties.GROUP, membership.getMemberId());
        };
        auditService.createApiProductAuditLog(
            io.gravitee.apim.core.audit.model.ApiProductAuditLogEntity.builder()
                .organizationId(auditInfo.organizationId())
                .environmentId(auditInfo.environmentId())
                .apiProductId(membership.getReferenceId())
                .event(MembershipAuditEvent.MEMBERSHIP_CREATED)
                .actor(auditInfo.actor())
                .newValue(membership)
                .createdAt(membership.getCreatedAt())
                .properties(properties)
                .build()
        );
    }
}

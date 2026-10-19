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
package io.gravitee.apim.infra.domain_service.member;

import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.group.model.crd.GroupCRDSpec;
import io.gravitee.apim.core.member.domain_service.CRDMembersDomainService;
import io.gravitee.apim.core.member.model.RoleScope;
import io.gravitee.apim.core.member.model.SystemRole;
import io.gravitee.apim.core.member.model.crd.MemberCRD;
import io.gravitee.apim.core.utils.CollectionUtils;
import io.gravitee.apim.core.utils.StringUtils;
import io.gravitee.apim.infra.adapter.GroupCRDAdapter;
import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;
import io.gravitee.rest.api.service.exceptions.RoleNotFoundException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 *
 * This class assumes validated and sanitized input and is thus to be used by kubernetes resources use cases only
 */
@CustomLog
@Service
@RequiredArgsConstructor
public class CRDMembersDomainServiceImpl implements CRDMembersDomainService {

    private final MembershipService membershipService;
    private final RoleService roleService;
    private final PermissionService permissionService;

    private static final List<RoleScope> GROUP_DEFAULT_ROLE_SCOPES = List.of(RoleScope.API, RoleScope.APPLICATION, RoleScope.API_PRODUCT);

    @Override
    public void createApiMembers(AuditInfo auditInfo, String apiId, Set<MemberCRD> members) {
        replaceMembers(auditInfo, apiId, RoleScope.API, MembershipReferenceType.API, members);
    }

    @Override
    public void updateApiMembers(AuditInfo auditInfo, String apiId, Set<MemberCRD> members) {
        if (!canManageMembers(auditInfo, RolePermission.API_MEMBER, apiId)) {
            // checkApiMembersUpdate already rejected any member change: there is nothing to write, and the owner stays
            return;
        }
        replaceMembers(auditInfo, apiId, RoleScope.API, MembershipReferenceType.API, members);
    }

    @Override
    public void checkApiMembersUpdate(AuditInfo auditInfo, String apiId, Set<MemberCRD> members) {
        if (!canManageMembers(auditInfo, RolePermission.API_MEMBER, apiId)) {
            requireUnchangedMembers(auditInfo, apiId, RoleScope.API, MembershipReferenceType.API, members);
        }
    }

    @Override
    public void updateApplicationMembers(AuditInfo auditInfo, String applicationId, Set<MemberCRD> members) {
        if (!canManageMembers(auditInfo, RolePermission.APPLICATION_MEMBER, applicationId)) {
            // checkApplicationMembersUpdate already rejected any member change: there is nothing to write, and the owner stays
            return;
        }
        replaceMembers(auditInfo, applicationId, RoleScope.APPLICATION, MembershipReferenceType.APPLICATION, members);
    }

    @Override
    public void checkApplicationMembersUpdate(AuditInfo auditInfo, String applicationId, Set<MemberCRD> members) {
        if (!canManageMembers(auditInfo, RolePermission.APPLICATION_MEMBER, applicationId)) {
            requireUnchangedMembers(auditInfo, applicationId, RoleScope.APPLICATION, MembershipReferenceType.APPLICATION, members);
        }
    }

    @Override
    public void updateGroupMembers(AuditInfo auditInfo, String groupId, Set<GroupCRDSpec.Member> members) {
        var apiMembers = GroupCRDAdapter.INSTANCE.toApiMemberCRDSet(members);
        var applicationMembers = GroupCRDAdapter.INSTANCE.toApplicationMemberCRDSet(members);
        var integrationMembers = GroupCRDAdapter.INSTANCE.toIntegrationMemberCRDSet(members);

        updateMembers(auditInfo, groupId, RoleScope.API, MembershipReferenceType.GROUP, apiMembers);
        updateMembers(auditInfo, groupId, RoleScope.APPLICATION, MembershipReferenceType.GROUP, applicationMembers);
        updateMembers(auditInfo, groupId, RoleScope.INTEGRATION, MembershipReferenceType.GROUP, integrationMembers);

        deleteOrphans(auditInfo, groupId, RoleScope.API, MembershipReferenceType.GROUP, apiMembers);
        deleteOrphans(auditInfo, groupId, RoleScope.APPLICATION, MembershipReferenceType.GROUP, applicationMembers);
        deleteOrphans(auditInfo, groupId, RoleScope.INTEGRATION, MembershipReferenceType.GROUP, integrationMembers);
    }

    @Override
    public void updateGroupDefaultRoles(
        AuditInfo auditInfo,
        String groupId,
        String apiRole,
        String applicationRole,
        String apiProductRole
    ) {
        var executionContext = new ExecutionContext(auditInfo.organizationId(), auditInfo.environmentId());

        if (!StringUtils.isEmpty(apiRole)) {
            updateDefaultRole(executionContext, groupId, apiRole, RoleScope.API);
        }

        if (!StringUtils.isEmpty(applicationRole)) {
            updateDefaultRole(executionContext, groupId, applicationRole, RoleScope.APPLICATION);
        }

        if (!StringUtils.isEmpty(apiProductRole)) {
            updateDefaultRole(executionContext, groupId, apiProductRole, RoleScope.API_PRODUCT);
        }
    }

    @Override
    public void updateGroupDefaultRoles(AuditInfo auditInfo, String groupId, Map<RoleScope, String> defaultMemberRoles) {
        var executionContext = new ExecutionContext(auditInfo.organizationId(), auditInfo.environmentId());

        for (var roleScope : GROUP_DEFAULT_ROLE_SCOPES) {
            var roleName = defaultMemberRoles.get(roleScope);
            if (StringUtils.isEmpty(roleName)) {
                clearDefaultRole(executionContext, groupId, roleScope);
            } else {
                updateDefaultRole(executionContext, groupId, roleName, roleScope);
            }
        }
    }

    private void clearDefaultRole(ExecutionContext executionContext, String groupId, RoleScope roleScope) {
        var referenceType = MembershipReferenceType.valueOf(roleScope.name());
        var hasDefaultRole = !membershipService.getRoles(referenceType, null, MembershipMemberType.GROUP, groupId).isEmpty();
        if (hasDefaultRole) {
            membershipService.deleteReferenceMember(executionContext, referenceType, null, MembershipMemberType.GROUP, groupId);
        }
    }

    private void updateDefaultRole(ExecutionContext executionContext, String groupId, String roleName, RoleScope roleScope) {
        if (SystemRole.PRIMARY_OWNER.name().equals(roleName)) {
            return;
        }

        var referenceType = MembershipReferenceType.valueOf(roleScope.name());
        var currentRole = membershipService
            .getRoles(referenceType, null, MembershipMemberType.GROUP, groupId)
            .stream()
            .findFirst()
            .map(RoleEntity::getName)
            .orElse(null);

        if (roleName.equals(currentRole)) {
            return;
        }

        if (currentRole != null) {
            membershipService.deleteReferenceMember(executionContext, referenceType, null, MembershipMemberType.GROUP, groupId);
        }

        membershipService.addRoleToMemberOnReference(
            executionContext,
            new MembershipService.MembershipReference(referenceType, null),
            new MembershipService.MembershipMember(groupId, null, MembershipMemberType.GROUP),
            new MembershipService.MembershipRole(mapRoleScope(roleScope), roleName)
        );
    }

    private void updateMembers(
        AuditInfo auditInfo,
        String referenceId,
        RoleScope roleScope,
        MembershipReferenceType referenceType,
        Set<MemberCRD> members
    ) {
        var executionContext = new ExecutionContext(auditInfo.organizationId(), auditInfo.environmentId());

        var defaultRoles = roleService.findDefaultRoleByScopes(auditInfo.organizationId(), mapRoleScope(roleScope));

        var defaultRole = CollectionUtils.isNotEmpty(defaultRoles) ? defaultRoles.getFirst() : null;

        for (var member : members) {
            var memberRoleEntity = findMemberRole(auditInfo.organizationId(), roleScope, member, defaultRole);

            membershipService
                .getRoles(referenceType, referenceId, MembershipMemberType.USER, member.getId())
                .stream()
                .filter(role -> role.getScope().name().equals(roleScope.name()))
                .findFirst()
                .ifPresent(role ->
                    membershipService.removeRole(referenceType, referenceId, MembershipMemberType.USER, member.getId(), role.getId())
                );

            membershipService.addRoleToMemberOnReference(
                executionContext,
                referenceType,
                referenceId,
                MembershipMemberType.USER,
                member.getId(),
                memberRoleEntity.getId()
            );
        }

        log.debug("Members successfully created for {} [{}]", referenceType, referenceId);
    }

    private void replaceMembers(
        AuditInfo auditInfo,
        String referenceId,
        RoleScope roleScope,
        MembershipReferenceType referenceType,
        Set<MemberCRD> members
    ) {
        updateMembers(auditInfo, referenceId, roleScope, referenceType, members);
        deleteOrphans(auditInfo, referenceId, roleScope, referenceType, members);
        transferOwnerShip(auditInfo, referenceId, roleScope, referenceType);
    }

    private boolean canManageMembers(AuditInfo auditInfo, RolePermission memberPermission, String referenceId) {
        return permissionService.hasPermission(
            new ExecutionContext(auditInfo.organizationId(), auditInfo.environmentId()),
            auditInfo.actor().userId(),
            memberPermission,
            referenceId,
            RolePermissionAction.UPDATE
        );
    }

    private void requireUnchangedMembers(
        AuditInfo auditInfo,
        String referenceId,
        RoleScope roleScope,
        MembershipReferenceType referenceType,
        Set<MemberCRD> members
    ) {
        var executionContext = new ExecutionContext(auditInfo.organizationId(), auditInfo.environmentId());
        var defaultRoles = roleService.findDefaultRoleByScopes(auditInfo.organizationId(), mapRoleScope(roleScope));
        var defaultRole = CollectionUtils.isNotEmpty(defaultRoles) ? defaultRoles.getFirst() : null;
        var poRole = roleService.findPrimaryOwnerRoleByOrganization(auditInfo.organizationId(), mapRoleScope(roleScope));

        Map<String, Set<String>> declaredRoles = members
            .stream()
            .collect(
                Collectors.toMap(
                    MemberCRD::getId,
                    member -> Set.of(findMemberRole(auditInfo.organizationId(), roleScope, member, defaultRole).getId()),
                    CRDMembersDomainServiceImpl::union
                )
            );

        // Members other than the primary owner, as deleteOrphans sees them: a spec naming the primary owner is a change.
        // The caller is left out too, since the spec may not declare them (ValidateCRDMembersDomainService rejects it).
        Map<String, Set<String>> currentRoles = membershipService
            .getMembersByReference(executionContext, referenceType, referenceId)
            .stream()
            .filter(member -> member.getRoles().stream().noneMatch(poRole::equals))
            .filter(member -> !member.getId().equals(auditInfo.actor().userId()))
            .collect(
                Collectors.toMap(
                    MemberEntity::getId,
                    member -> member.getRoles().stream().map(RoleEntity::getId).collect(Collectors.toSet()),
                    CRDMembersDomainServiceImpl::union
                )
            );

        if (!declaredRoles.equals(currentRoles)) {
            throw new ForbiddenAccessException();
        }
    }

    private static Set<String> union(Set<String> left, Set<String> right) {
        var roles = new HashSet<>(left);
        roles.addAll(right);
        return roles;
    }

    private void deleteOrphans(
        AuditInfo auditInfo,
        String referenceId,
        RoleScope roleScope,
        MembershipReferenceType referenceType,
        Set<MemberCRD> members
    ) {
        log.debug("Deleting orphan members");

        var executionContext = new ExecutionContext(auditInfo.organizationId(), auditInfo.environmentId());

        var scope = io.gravitee.rest.api.model.permissions.RoleScope.valueOf(roleScope.name());

        var poRole = roleService.findPrimaryOwnerRoleByOrganization(auditInfo.organizationId(), scope);

        var givenMemberIds = members.stream().map(MemberCRD::getId).collect(Collectors.toSet());

        var existingMemberIds = new LinkedHashSet<>(
            membershipService
                .getMembersByReference(executionContext, referenceType, referenceId)
                .stream()
                .filter(member -> member.getRoles().stream().noneMatch(poRole::equals))
                .map(MemberEntity::getId)
                .toList()
        );

        existingMemberIds.removeAll(givenMemberIds);

        existingMemberIds.forEach(memberId ->
            membershipService.deleteReferenceMember(executionContext, referenceType, referenceId, MembershipMemberType.USER, memberId)
        );
    }

    private void transferOwnerShip(AuditInfo auditInfo, String referenceId, RoleScope roleScope, MembershipReferenceType referenceType) {
        log.debug("Transferring owner ship to authenticated user {}", auditInfo.actor().userSourceId());

        var currentPrimaryOwner = membershipService.getPrimaryOwner(auditInfo.organizationId(), referenceType, referenceId);

        if (currentPrimaryOwner != null && currentPrimaryOwner.getMemberId().equals(auditInfo.actor().userId())) {
            log.debug("User {} is already the primary owner for {} {}", auditInfo.actor().userSourceId(), referenceType, referenceId);
            return;
        }

        var executionContext = new ExecutionContext(auditInfo.organizationId(), auditInfo.environmentId());
        var primaryOwner = new MembershipService.MembershipMember(auditInfo.actor().userId(), null, MembershipMemberType.USER);

        switch (roleScope) {
            case API -> membershipService.transferApiOwnership(executionContext, referenceId, primaryOwner, List.of());
            case APPLICATION -> membershipService.transferApplicationOwnership(executionContext, referenceId, primaryOwner, List.of());
            default -> throw new TechnicalDomainException(String.format("Unknown role scope [%s]", roleScope));
        }
    }

    private RoleEntity findMemberRole(String organizationId, RoleScope roleScope, MemberCRD member, RoleEntity defaultRole) {
        if (StringUtils.isEmpty(member.getRole())) {
            assertDefaultRoleForScope(defaultRole, roleScope);
            log.warn("There is no role associated with member [{}]. Default role will be applied", member.getSourceId());
        }

        var memberRoleEntity = StringUtils.isEmpty(member.getRole())
            ? defaultRole
            : findRoleEntity(organizationId, roleScope, member.getRole(), defaultRole);

        return Objects.requireNonNull(memberRoleEntity, "Unable to find role for member [" + member.getSourceId() + "]");
    }

    private RoleEntity findRoleEntity(String organizationId, RoleScope roleScope, String roleNameOrId, RoleEntity defaultRole) {
        try {
            return roleService
                .findByScopeAndName(mapRoleScope(roleScope), roleNameOrId, organizationId)
                .orElseGet(() -> roleService.findById(roleNameOrId));
        } catch (RoleNotFoundException e) {
            assertDefaultRoleForScope(defaultRole, roleScope);
            log.warn("Unable to find role [{}]. Using default role [{}]", roleNameOrId, defaultRole);
            return defaultRole;
        }
    }

    private io.gravitee.rest.api.model.permissions.RoleScope mapRoleScope(RoleScope roleScope) {
        return roleScope == null ? null : io.gravitee.rest.api.model.permissions.RoleScope.valueOf(roleScope.name());
    }

    private void assertDefaultRoleForScope(RoleEntity defaultRole, RoleScope roleScope) {
        if (defaultRole == null) {
            var explanation =
                "If an existing member role has not been explicitly set, a default role must be set for the role scope in your organization settings.";
            throw new ValidationDomainException(String.format("Unable to set role for scope [%s]. %s", roleScope, explanation));
        }
    }
}

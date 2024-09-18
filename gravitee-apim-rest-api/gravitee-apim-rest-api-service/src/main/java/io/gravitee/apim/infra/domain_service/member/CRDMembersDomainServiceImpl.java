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
import io.gravitee.apim.core.member.domain_service.CRDMembersDomainService;
import io.gravitee.apim.core.member.model.crd.MemberCRD;
import io.gravitee.apim.core.utils.StringUtils;
import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.RoleNotFoundException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 *
 * This class assumes validated and sanitized input and is thus to be used by kubernetes resources use cases only
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CRDMembersDomainServiceImpl implements CRDMembersDomainService {

    private final MembershipService membershipService;
    private final RoleService roleService;

    @Override
    public void updateApiMembers(AuditInfo auditInfo, String apiId, Set<MemberCRD> members) {
        updateMembers(auditInfo, apiId, RoleScope.API, MembershipReferenceType.API, members);
        deleteOrphans(auditInfo, apiId, RoleScope.API, MembershipReferenceType.API, members);
        transferOwnerShip(auditInfo, apiId, RoleScope.API, MembershipReferenceType.API);
    }

    @Override
    public void updateApplicationMembers(AuditInfo auditInfo, String applicationId, Set<MemberCRD> members) {
        updateMembers(auditInfo, applicationId, RoleScope.APPLICATION, MembershipReferenceType.APPLICATION, members);
        deleteOrphans(auditInfo, applicationId, RoleScope.APPLICATION, MembershipReferenceType.APPLICATION, members);
        transferOwnerShip(auditInfo, applicationId, RoleScope.APPLICATION, MembershipReferenceType.APPLICATION);
    }

    private void updateMembers(
        AuditInfo auditInfo,
        String referenceId,
        RoleScope roleScope,
        MembershipReferenceType referenceType,
        Set<MemberCRD> members
    ) {
        var executionContext = new ExecutionContext(auditInfo.organizationId(), auditInfo.environmentId());
        var defaultRole = roleService.findDefaultRoleByScopes(auditInfo.organizationId(), roleScope).iterator().next();

        for (var member : members) {
            if (StringUtils.isEmpty(member.getRole())) {
                log.warn("There is no role associated with member [{}]. Default role will be applied", member.getSourceId());
            }

            membershipService.deleteReferenceMember(
                executionContext,
                referenceType,
                referenceId,
                MembershipMemberType.USER,
                member.getId()
            );

            var memberRoleEntity = StringUtils.isEmpty(member.getRole())
                ? defaultRole
                : findRoleEntity(auditInfo.organizationId(), roleScope, member.getRole(), defaultRole);

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

    private void deleteOrphans(
        AuditInfo auditInfo,
        String referenceId,
        RoleScope roleScope,
        MembershipReferenceType referenceType,
        Set<MemberCRD> members
    ) {
        log.debug("Deleting orphan members");

        var executionContext = new ExecutionContext(auditInfo.organizationId(), auditInfo.environmentId());

        var poRole = roleService.findPrimaryOwnerRoleByOrganization(auditInfo.organizationId(), roleScope);

        var givenMemberIds = members.stream().map(MemberCRD::getId).collect(Collectors.toSet());

        var existingMemberIds = new HashSet<>(
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

    private RoleEntity findRoleEntity(String organizationId, RoleScope roleScope, String roleNameOrId, RoleEntity defaultRole) {
        try {
            return roleService
                .findByScopeAndName(roleScope, roleNameOrId, organizationId)
                .orElseGet(() -> roleService.findById(roleNameOrId));
        } catch (RoleNotFoundException e) {
            log.warn("Unable to find role [{}]. Using default role [{}]", roleNameOrId, defaultRole.getName());
            return defaultRole;
        }
    }
}

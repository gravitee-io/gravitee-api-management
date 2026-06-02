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
package io.gravitee.rest.api.service.impl.upgrade.upgrader;

import static io.gravitee.rest.api.service.common.DefaultRoleEntityDefinition.ROLE_API_PRODUCT_USER;
import static io.gravitee.rest.api.service.impl.upgrade.upgrader.UpgraderOrder.ASSIGN_DEFAULT_API_PRODUCT_ROLE_TO_GROUPS_UPGRADER;

import io.gravitee.node.api.upgrader.Upgrader;
import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.GroupRepository;
import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.api.RoleRepository;
import io.gravitee.repository.management.model.Group;
import io.gravitee.repository.management.model.Membership;
import io.gravitee.repository.management.model.MembershipMemberType;
import io.gravitee.repository.management.model.MembershipReferenceType;
import io.gravitee.repository.management.model.Role;
import io.gravitee.repository.management.model.RoleReferenceType;
import io.gravitee.repository.management.model.RoleScope;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.UuidString;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Backfills the default API_PRODUCT role membership for groups that pre-date the API_PRODUCT scope,
 * and assigns that default role to existing group members who do not yet have an API_PRODUCT role
 * on the group.
 *
 * <p>For every group across every environment, if no membership of the form
 * {@code (referenceType=API_PRODUCT, referenceId=null, memberType=GROUP, memberId=<groupId>)} exists,
 * this upgrader creates one with the {@code USER} role. This makes {@code GroupEntity.roles[API_PRODUCT]}
 * resolve to a real value going forward.
 *
 * <p>For every existing {@code USER} member of the group, if no {@code USER→GROUP} membership exists
 * with an API_PRODUCT-scoped role, this upgrader creates one using the group's default API_PRODUCT role.
 */
@Component
@CustomLog
public class AssignDefaultApiProductRoleToGroupsUpgrader implements Upgrader {

    private static final String DEFAULT_SOURCE = "system";

    private final EnvironmentRepository environmentRepository;
    private final GroupRepository groupRepository;
    private final MembershipRepository membershipRepository;
    private final RoleRepository roleRepository;

    @Autowired
    public AssignDefaultApiProductRoleToGroupsUpgrader(
        @Lazy EnvironmentRepository environmentRepository,
        @Lazy GroupRepository groupRepository,
        @Lazy MembershipRepository membershipRepository,
        @Lazy RoleRepository roleRepository
    ) {
        this.environmentRepository = environmentRepository;
        this.groupRepository = groupRepository;
        this.membershipRepository = membershipRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    public boolean upgrade() throws UpgraderException {
        return this.wrapException(this::applyUpgrade);
    }

    private boolean applyUpgrade() throws TechnicalException {
        environmentRepository
            .findAll()
            .forEach(environment -> {
                ExecutionContext executionContext = new ExecutionContext(environment);
                try {
                    backfillAllGroupsInEnvironment(executionContext);
                } catch (TechnicalException e) {
                    log.error(
                        "Error backfilling default API_PRODUCT role on groups for environment {}",
                        executionContext.getEnvironmentId(),
                        e
                    );
                }
            });
        return true;
    }

    private void backfillAllGroupsInEnvironment(ExecutionContext executionContext) throws TechnicalException {
        Optional<String> userRoleId = findApiProductUserRoleId(executionContext.getOrganizationId());
        if (userRoleId.isEmpty()) {
            log.warn(
                "API_PRODUCT '{}' role not found for organization {} — skipping group backfill (run ApiProductRolesUpgrader first)",
                ROLE_API_PRODUCT_USER.getName(),
                executionContext.getOrganizationId()
            );
            return;
        }

        Set<String> apiProductRoleIds = findAllApiProductRoleIds(executionContext.getOrganizationId());

        for (Group group : groupRepository.findAllByEnvironment(executionContext.getEnvironmentId())) {
            backfillGroupMembers(executionContext.getOrganizationId(), group.getId(), userRoleId.get(), apiProductRoleIds);
        }
    }

    private void backfillGroupMembers(String organizationId, String groupId, String defaultUserRoleId, Set<String> apiProductRoleIds)
        throws TechnicalException {
        String defaultRoleId = resolveOrCreateDefaultApiProductRoleId(groupId, defaultUserRoleId);

        List<Membership> groupMemberships = membershipRepository.findByReferenceIdAndReferenceType(groupId, MembershipReferenceType.GROUP);

        Set<String> userIdsWithApiProductRole = groupMemberships
            .stream()
            .filter(m -> MembershipMemberType.USER.equals(m.getMemberType()))
            .filter(m -> apiProductRoleIds.contains(m.getRoleId()))
            .map(Membership::getMemberId)
            .collect(Collectors.toSet());

        Set<String> userIds = groupMemberships
            .stream()
            .filter(m -> MembershipMemberType.USER.equals(m.getMemberType()))
            .map(Membership::getMemberId)
            .collect(Collectors.toSet());

        for (String userId : userIds) {
            if (userIdsWithApiProductRole.contains(userId)) {
                continue;
            }
            createMemberApiProductRoleMembership(userId, groupId, defaultRoleId);
            log.info("Assigned default API_PRODUCT role to user {} on group {} (organization {})", userId, groupId, organizationId);
        }
    }

    private Optional<String> findApiProductUserRoleId(String organizationId) throws TechnicalException {
        return roleRepository
            .findByScopeAndNameAndReferenceIdAndReferenceType(
                RoleScope.API_PRODUCT,
                ROLE_API_PRODUCT_USER.getName(),
                organizationId,
                RoleReferenceType.ORGANIZATION
            )
            .map(Role::getId);
    }

    private Set<String> findAllApiProductRoleIds(String organizationId) throws TechnicalException {
        return roleRepository
            .findByScopeAndReferenceIdAndReferenceType(RoleScope.API_PRODUCT, organizationId, RoleReferenceType.ORGANIZATION)
            .stream()
            .map(Role::getId)
            .collect(Collectors.toSet());
    }

    private String resolveOrCreateDefaultApiProductRoleId(String groupId, String defaultUserRoleId) throws TechnicalException {
        Optional<String> existingDefaultRoleId = findDefaultApiProductRoleId(groupId);
        if (existingDefaultRoleId.isPresent()) {
            return existingDefaultRoleId.get();
        }

        createDefaultApiProductRoleMembership(groupId, defaultUserRoleId);
        log.info("Assigned default API_PRODUCT role '{}' to group {}", ROLE_API_PRODUCT_USER.getName(), groupId);
        return defaultUserRoleId;
    }

    private Optional<String> findDefaultApiProductRoleId(String groupId) throws TechnicalException {
        return membershipRepository
            .findByMemberIdAndMemberTypeAndReferenceType(groupId, MembershipMemberType.GROUP, MembershipReferenceType.API_PRODUCT)
            .stream()
            .filter(m -> m.getReferenceId() == null)
            .map(Membership::getRoleId)
            .findFirst();
    }

    private void createDefaultApiProductRoleMembership(String groupId, String roleId) throws TechnicalException {
        Membership membership = new Membership(
            UuidString.generateRandom(),
            groupId,
            MembershipMemberType.GROUP,
            null,
            MembershipReferenceType.API_PRODUCT,
            roleId
        );
        Date now = new Date();
        membership.setCreatedAt(now);
        membership.setUpdatedAt(now);
        membership.setSource(DEFAULT_SOURCE);
        membershipRepository.create(membership);
    }

    private void createMemberApiProductRoleMembership(String userId, String groupId, String roleId) throws TechnicalException {
        Membership membership = new Membership(
            UuidString.generateRandom(),
            userId,
            MembershipMemberType.USER,
            groupId,
            MembershipReferenceType.GROUP,
            roleId
        );
        Date now = new Date();
        membership.setCreatedAt(now);
        membership.setUpdatedAt(now);
        membership.setSource(DEFAULT_SOURCE);
        membershipRepository.create(membership);
    }

    @Override
    public int getOrder() {
        return ASSIGN_DEFAULT_API_PRODUCT_ROLE_TO_GROUPS_UPGRADER;
    }
}

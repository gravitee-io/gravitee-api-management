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
package io.gravitee.rest.api.service.impl;

import static java.util.function.Predicate.not;

import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.permissions.*;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.*;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * @author Nicolas GERAUD(nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class PermissionServiceImpl extends AbstractService implements PermissionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionServiceImpl.class);

    public static final String ORGANIZATION_ADMIN = RoleScope.ORGANIZATION.name() + ':' + SystemRole.ADMIN.name();

    public static final Map<RoleScope, MembershipReferenceType> ROLE_SCOPE_TO_REFERENCE_TYPE = Map.ofEntries(
        Pair.of(RoleScope.API, MembershipReferenceType.API),
        Pair.of(RoleScope.APPLICATION, MembershipReferenceType.APPLICATION),
        Pair.of(RoleScope.ORGANIZATION, MembershipReferenceType.ORGANIZATION),
        Pair.of(RoleScope.ENVIRONMENT, MembershipReferenceType.ENVIRONMENT),
        Pair.of(RoleScope.GROUP, MembershipReferenceType.GROUP),
        Pair.of(RoleScope.INTEGRATION, MembershipReferenceType.INTEGRATION)
    );

    @Autowired
    MembershipService membershipService;

    @Autowired
    RoleService roleService;

    @Autowired
    UserService userService;

    @Override
    public boolean hasPermission(
        final ExecutionContext executionContext,
        RolePermission permission,
        String referenceId,
        RolePermissionAction... acls
    ) {
        return hasPermission(executionContext, getAuthenticatedUsername(), permission, referenceId, acls);
    }

    @Override
    public boolean hasPermission(
        final ExecutionContext executionContext,
        String userId,
        RolePermission permission,
        String referenceId,
        RolePermissionAction... acls
    ) {
        if (isOrganizationAdmin()) {
            LOGGER.debug("User [{}] has full access because of its ORGANIZATION ADMIN role", userId);
            return true;
        }

        MembershipReferenceType membershipReferenceType = ROLE_SCOPE_TO_REFERENCE_TYPE.get(permission.getScope());

        Map<String, char[]> permissions = membershipService.getUserMemberPermissions(
            executionContext,
            membershipReferenceType,
            referenceId,
            userId
        );

        return permissions != null && roleService.hasPermission(permissions, permission.getPermission(), acls);
    }

    @Override
    public boolean hasManagementRights(final ExecutionContext executionContext, String userId) {
        if (isOrganizationAdmin()) {
            LOGGER.debug("User [{}] has full access because of its ORGANIZATION ADMIN role", userId);
            return true;
        }
        UserEntity user = userService.findByIdWithRoles(executionContext, userId);
        return hasOrganizationManagementRole(user) || hasEnvironmentManagementRole(user) || hasApiManagementRole(executionContext, user);
    }

    public static boolean isOrganizationAdmin() {
        return (
            SecurityContextHolder.getContext().getAuthentication() != null &&
            SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(ORGANIZATION_ADMIN::equals)
        );
    }

    private boolean hasApiManagementRole(final ExecutionContext executionContext, UserEntity user) {
        return streamUserApiIds(executionContext, user)
            .flatMap(apiId -> streamApiUserPermissions(apiId, user))
            .filter(not(this::isApiRatingPermission))
            .anyMatch(this::isCreateOrUpdateOrDeletePermission);
    }

    private boolean hasEnvironmentManagementRole(UserEntity user) {
        return streamUserRolePermissions(user, RoleScope.ENVIRONMENT)
            .filter(not(this::isEnvironmentApplicationPermission))
            .anyMatch(this::isCreateOrUpdateOrDeletePermission);
    }

    private boolean hasOrganizationManagementRole(UserEntity user) {
        return streamUserRolePermissions(user, RoleScope.ORGANIZATION).anyMatch(this::isCreateOrUpdateOrDeletePermission);
    }

    private boolean isCreateOrUpdateOrDeletePermission(Map.Entry<String, char[]> permission) {
        String permissionString = new String(permission.getValue());
        return permissionString.contains("C") || permissionString.contains("U") || permissionString.contains("D");
    }

    private boolean isEnvironmentApplicationPermission(Map.Entry<String, char[]> permission) {
        return EnvironmentPermission.valueOf(permission.getKey()) == EnvironmentPermission.APPLICATION;
    }

    private boolean isApiRatingPermission(Map.Entry<String, char[]> permission) {
        ApiPermission apiPermission = ApiPermission.valueOf(permission.getKey());
        return apiPermission == ApiPermission.RATING || ApiPermission.RATING_ANSWER == apiPermission;
    }

    private Stream<String> streamUserApiIds(final ExecutionContext executionContext, UserEntity user) {
        return membershipService
            .findUserMembership(executionContext, MembershipReferenceType.API, user.getId())
            .stream()
            .map(UserMembership::getReference)
            .distinct();
    }

    private Stream<Map.Entry<String, char[]>> streamUserRolePermissions(UserEntity user, RoleScope scope) {
        return streamUserRoles(user).filter(role -> scope == role.getScope()).flatMap(role -> role.getPermissions().entrySet().stream());
    }

    private Stream<UserRoleEntity> streamUserRoles(UserEntity user) {
        return Stream.ofNullable(user.getRoles()).flatMap(Collection::stream);
    }

    private Stream<Map.Entry<String, char[]>> streamApiUserPermissions(String apiId, UserEntity user) {
        return membershipService
            .getRoles(MembershipReferenceType.API, apiId, MembershipMemberType.USER, user.getId())
            .stream()
            .flatMap(role -> role.getPermissions().entrySet().stream());
    }
}

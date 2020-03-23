/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.service.impl;

import io.gravitee.repository.management.model.MembershipDefaultReferenceId;
import io.gravitee.repository.management.model.MembershipReferenceType;
import io.gravitee.repository.management.model.RoleScope;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.UserMembership;
import io.gravitee.rest.api.model.permissions.ApiPermission;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Nicolas GERAUD(nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class PermissionServiceImpl extends AbstractService implements PermissionService {

    @Autowired
    MembershipService membershipService;

    @Autowired
    ApiService apiService;

    @Autowired
    ApplicationService applicationService;

    @Autowired
    RoleService roleService;

    @Autowired
    UserService userService;

    @Override
    public boolean hasPermission(RolePermission permission, String referenceId, RolePermissionAction... acls) {
        Optional<String> optionalReferenceId = Optional.ofNullable(referenceId);
        MembershipReferenceType membershipReferenceType;
        MembershipReferenceType groupMembershipReferenceType = null;
        io.gravitee.repository.management.model.RoleScope repoRoleScope;
        switch (permission.getScope()) {
            case MANAGEMENT:
                membershipReferenceType = MembershipReferenceType.MANAGEMENT;
                repoRoleScope = io.gravitee.repository.management.model.RoleScope.MANAGEMENT;
                break;
            case PORTAL:
                membershipReferenceType = MembershipReferenceType.PORTAL;
                repoRoleScope = io.gravitee.repository.management.model.RoleScope.PORTAL;
                break;
            case API:
                membershipReferenceType = MembershipReferenceType.API;
                groupMembershipReferenceType = MembershipReferenceType.GROUP;
                repoRoleScope = io.gravitee.repository.management.model.RoleScope.API;
                break;
            case APPLICATION:
                membershipReferenceType = MembershipReferenceType.APPLICATION;
                groupMembershipReferenceType = MembershipReferenceType.GROUP;
                repoRoleScope = io.gravitee.repository.management.model.RoleScope.APPLICATION;
                break;
            default:
                membershipReferenceType = null;
                repoRoleScope = null;
        }
        Set<RoleEntity> roles = Collections.emptySet();
        RoleEntity firstDegreeRole = membershipService.getRole(membershipReferenceType, optionalReferenceId.orElse(MembershipDefaultReferenceId.DEFAULT.name()), getAuthenticatedUsername(), repoRoleScope);
        if (firstDegreeRole != null) {
            roles = Collections.singleton(firstDegreeRole);
        } else if (groupMembershipReferenceType != null) {
            Set<String> groups;
            try {
                groups = apiService.findById(referenceId).getGroups();
            } catch (ApiNotFoundException | IllegalArgumentException ane) {
                groups = applicationService.findById(referenceId).getGroups();
            }

            if (groups != null && !groups.isEmpty()) {
                roles = membershipService.getRoles(groupMembershipReferenceType, groups, getAuthenticatedUsername(), repoRoleScope);
            }
        }
        for (RoleEntity roleEntity : roles) {
            if (roleService.hasPermission(roleEntity.getPermissions(), permission.getPermission(), acls)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasManagementRights(String userId) {
        UserEntity user = userService.findByIdWithRoles(userId);
        boolean hasManagementRights = (user.getRoles() != null && !user.getRoles().isEmpty());
        
        if (!hasManagementRights) {
            Set<String> userApisId = membershipService.findUserMembership(userId, MembershipReferenceType.API).stream().map(UserMembership::getReference).collect(Collectors.toSet());
            Set<RoleEntity> userApisRole = membershipService.getRoles(MembershipReferenceType.API, userApisId, userId, RoleScope.API);
            hasManagementRights = userApisRole.stream().anyMatch(roleEntity -> {
                
                boolean hasCreateUpdateOrDeletePermission = false;
                Map<String, char[]> rolePermissions = roleEntity.getPermissions();
                Iterator<String> iterator = rolePermissions.keySet().iterator();
                while(iterator.hasNext() && !hasCreateUpdateOrDeletePermission) {
                    String permissionName = iterator.next();
                    if (!ApiPermission.RATING.name().equals(permissionName) && !ApiPermission.RATING_ANSWER.name().equals(permissionName)) {
                        String permissionString = new String(rolePermissions.get(permissionName));
                        hasCreateUpdateOrDeletePermission = permissionString != null && !permissionString.isEmpty() && 
                                (permissionString.contains("C") || permissionString.contains("U") || permissionString.contains("D"));
                    }
                }
                return hasCreateUpdateOrDeletePermission;
            });
        }
        return hasManagementRights;
    }
}

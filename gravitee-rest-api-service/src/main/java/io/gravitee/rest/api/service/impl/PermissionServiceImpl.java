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

import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.permissions.ApiPermission;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
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
    RoleService roleService;

    @Autowired
    UserService userService;

    @Override
    public boolean hasPermission(RolePermission permission, String referenceId, RolePermissionAction... acls) {
        MembershipReferenceType membershipReferenceType;
        switch (permission.getScope()) {
            case API:
                membershipReferenceType = MembershipReferenceType.API;
                break;
            case APPLICATION:
                membershipReferenceType = MembershipReferenceType.APPLICATION;
                break;
            case ENVIRONMENT:
                membershipReferenceType = MembershipReferenceType.ENVIRONMENT;
                break;
            default:
                membershipReferenceType = null;
        }
        
        MemberEntity member = membershipService.getUserMember(membershipReferenceType, referenceId, getAuthenticatedUsername());
        if (member == null ) {
            return false;
        }
        return roleService.hasPermission(member.getPermissions(), permission.getPermission(), acls);
    }

    @Override
    public boolean hasManagementRights(String userId) {
        UserEntity user = userService.findByIdWithRoles(userId);
        boolean hasManagementRights = (user.getRoles() != null && !user.getRoles().isEmpty());
        
        if (!hasManagementRights) {
            Set<RoleEntity> userApisRole = membershipService.findUserMembership(MembershipReferenceType.API, userId)
                    .stream()
                    .map(UserMembership::getReference)
                    .distinct()
                    .flatMap(apiId -> membershipService.getRoles(MembershipReferenceType.API, apiId, MembershipMemberType.USER, userId).stream())
                    .collect(Collectors.toSet());
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

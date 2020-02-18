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
package io.gravitee.management.service.impl;

import io.gravitee.management.model.RoleEntity;
import io.gravitee.management.model.permissions.RolePermission;
import io.gravitee.management.model.permissions.RolePermissionAction;
import io.gravitee.management.service.*;
import io.gravitee.management.service.exceptions.ApiNotFoundException;
import io.gravitee.repository.management.model.MembershipDefaultReferenceId;
import io.gravitee.repository.management.model.MembershipReferenceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

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
        Set<RoleEntity> roles = new HashSet<>();
        RoleEntity firstDegreeRole = membershipService.getRole(membershipReferenceType, optionalReferenceId.orElse(MembershipDefaultReferenceId.DEFAULT.name()), getAuthenticatedUsername(), repoRoleScope);
        if (firstDegreeRole != null) {
            roles.add(firstDegreeRole);
        }

        if (groupMembershipReferenceType != null) {
            Set<String> groups;
            try {
                groups = apiService.findById(referenceId).getGroups();
            } catch (ApiNotFoundException | IllegalArgumentException ane) {
                groups = applicationService.findById(referenceId).getGroups();
            }

            if (groups != null && !groups.isEmpty()) {
                roles.addAll(membershipService.getRoles(groupMembershipReferenceType, groups, getAuthenticatedUsername(), repoRoleScope));
            }
        }
        for (RoleEntity roleEntity : roles) {
            if (roleService.hasPermission(roleEntity.getPermissions(), permission.getPermission(), acls)) {
                return true;
            }
        }
        return false;
    }
}

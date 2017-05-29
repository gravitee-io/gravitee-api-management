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
package io.gravitee.management.rest.resource;

import io.gravitee.management.model.RoleEntity;
import io.gravitee.management.model.permissions.SystemRole;
import io.gravitee.management.model.permissions.RolePermission;
import io.gravitee.management.model.permissions.RolePermissionAction;
import io.gravitee.management.service.ApiService;
import io.gravitee.management.service.ApplicationService;
import io.gravitee.management.service.MembershipService;
import io.gravitee.management.service.RoleService;
import io.gravitee.repository.management.model.MembershipDefaultReferenceId;
import io.gravitee.repository.management.model.MembershipReferenceType;
import sun.security.acl.GroupImpl;

import javax.inject.Inject;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;
import java.security.Principal;
import java.util.Optional;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractResource {

    @Context
    protected SecurityContext securityContext;

    @Inject
    MembershipService membershipService;

    @Inject
    RoleService roleService;

    @Inject
    ApiService apiService;

    @Inject
    ApplicationService applicationService;

    protected String getAuthenticatedUsername() {
        return securityContext.getUserPrincipal().getName();
    }

    protected Principal getAuthenticatedUser() {
        return securityContext.getUserPrincipal();
    }

    protected boolean isAuthenticated() {
        return securityContext.getUserPrincipal() != null;
    }

    protected boolean isAdmin() {
        return isUserInRole(SystemRole.ADMIN.name());
    }

    protected boolean isUserInRole(String role) {
        return securityContext.isUserInRole(role);
    }

    protected boolean hasPermission(RolePermission permission, RolePermissionAction... acls) {
        return hasPermission(permission, null, acls);
    }

    protected boolean hasPermission(RolePermission permission, String referenceId, RolePermissionAction... acls) {
        if (isAdmin()) {
            return true;
        }
        Optional<String> optionalReferenceId = Optional.ofNullable(referenceId);
        MembershipReferenceType membershipReferenceType;
        MembershipReferenceType groupMembershipReferenceType = null;

        switch (permission.getScope()) {
            case MANAGEMENT:
                membershipReferenceType = MembershipReferenceType.MANAGEMENT;
                break;
            case PORTAL:
                membershipReferenceType = MembershipReferenceType.PORTAL;
                break;
            case API:
                membershipReferenceType = MembershipReferenceType.API;
                groupMembershipReferenceType = MembershipReferenceType.API_GROUP;
                break;
            case APPLICATION:
                membershipReferenceType = MembershipReferenceType.APPLICATION;
                groupMembershipReferenceType = MembershipReferenceType.APPLICATION_GROUP;
                break;
            default:
                membershipReferenceType = null;
        }
        RoleEntity role = membershipService.getRole(membershipReferenceType, optionalReferenceId.orElse(MembershipDefaultReferenceId.DEFAULT.name()), getAuthenticatedUsername());
        if (role == null && groupMembershipReferenceType != null) {
            String groupId;
            if (MembershipReferenceType.API_GROUP.equals(groupMembershipReferenceType)) {
                groupId = apiService.findById(referenceId).getGroup().getId();
            } else {
                groupId = applicationService.findById(referenceId).getGroup().getId();
            }
            role = membershipService.getRole(groupMembershipReferenceType, groupId, getAuthenticatedUsername());
        }
        return roleService.hasPermission(role, permission.getPermission(), acls);
    }
}

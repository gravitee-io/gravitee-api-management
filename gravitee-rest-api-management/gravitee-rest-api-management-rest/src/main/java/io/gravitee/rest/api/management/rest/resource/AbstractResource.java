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
package io.gravitee.rest.api.management.rest.resource;

import io.gravitee.rest.api.idp.api.authentication.UserDetails;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.RoleService;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.inject.Inject;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractResource {

    public final static String ENVIRONMENT_ADMIN = RoleScope.ENVIRONMENT.name() + ':' + SystemRole.ADMIN.name();

    @Context
    protected SecurityContext securityContext;

    @Inject
    protected MembershipService membershipService;
    @Inject
    protected RoleService roleService;
    @Inject
    protected ApiService apiService;
    @Inject
    protected PermissionService permissionService;

    UserDetails getAuthenticatedUserDetails() {
        return (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    protected String getAuthenticatedUser() {
        return securityContext.getUserPrincipal().getName();
    }

    String getAuthenticatedUserOrNull() {
        return isAuthenticated() ? getAuthenticatedUser() : null;
    }

    protected boolean isAuthenticated() {
        return securityContext.getUserPrincipal() != null;
    }

    protected boolean isAdmin() {
        return isUserInRole(ENVIRONMENT_ADMIN);
    }

    private boolean isUserInRole(String role) {
        return securityContext.isUserInRole(role);
    }

    protected boolean hasPermission(RolePermission permission, RolePermissionAction... acls) {
        return hasPermission(permission, null, acls);
    }

    protected boolean hasPermission(RolePermission permission, String referenceId, RolePermissionAction... acls) {
        return isAuthenticated() && (isAdmin() || permissionService.hasPermission(permission, referenceId, acls));
    }

    protected boolean hasApiPermission(RolePermission permission, RolePermissionAction... acls) {
        return apiService.search(null).stream()
                .noneMatch(api -> isAuthenticated() && hasPermission(permission, api.getId(), acls));
    }
}

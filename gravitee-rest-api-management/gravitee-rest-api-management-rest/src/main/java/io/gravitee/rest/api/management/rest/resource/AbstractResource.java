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
import io.gravitee.rest.api.model.MembershipEntity;
import io.gravitee.rest.api.model.api.ApiQuery;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.inject.Inject;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.gravitee.rest.api.model.MembershipMemberType.USER;
import static io.gravitee.rest.api.model.MembershipReferenceType.API;
import static io.gravitee.rest.api.model.MembershipReferenceType.GROUP;

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

    protected boolean canReadAPIConfiguration() {
        if (!isAdmin()) {
            return retrieveApiMembership().findFirst().isPresent();
        }
        return true;
    }

    /**
     * @return The list of API Membership for the authenticated user (direct membership or through groups)
     */
    private Stream<MembershipEntity> retrieveApiMembership() {
        Stream<MembershipEntity> streamUserMembership = membershipService
                .getMembershipsByMemberAndReference(USER, getAuthenticatedUser(), API).stream();

        Stream<MembershipEntity> streamGroupMembership = membershipService
                .getMembershipsByMemberAndReference(USER, getAuthenticatedUser(), GROUP).stream()
                .filter(m -> m.getRoleId() != null && roleService.findById(m.getRoleId()).getScope().equals(RoleScope.API));

        return Stream.concat(streamUserMembership, streamGroupMembership);
    }

    protected void canReadAPI(final String api) {
        if (!isAdmin()) {
            // get memberships of the current user
            List<MembershipEntity> memberships = retrieveApiMembership().collect(Collectors.toList());
            Set<String> groups = memberships.stream().filter(m -> GROUP.equals(m.getReferenceType())).map(m -> m.getReferenceId()).collect(Collectors.toSet());
            Set<String> directMembers =  memberships.stream().filter(m -> API.equals(m.getReferenceType())).map(m -> m.getReferenceId()).collect(Collectors.toSet());

            // if the current user is member of the API, continue
            if (directMembers.contains(api)) {
                return;
            }

            // fetch group memberships
            final ApiQuery apiQuery = new ApiQuery();
            apiQuery.setGroups(new ArrayList<>(groups));
            final boolean canReadAPI = apiService.searchIds(apiQuery).contains(api);
            if (!canReadAPI) {
                throw new ForbiddenAccessException();
            }
        }
    }
}

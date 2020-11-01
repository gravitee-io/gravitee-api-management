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

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.PathParam;
import java.util.Optional;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = {"Roles"})
public class RoleUserResource extends AbstractResource  {

    @Inject
    private MembershipService membershipService;

    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Delete the role for a given user",
            notes = "User must have the MANAGEMENT_ROLE[DELETE] permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 204, message = "Role successfully removed"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.ORGANIZATION_ROLE, acls = RolePermissionAction.DELETE)
    })
    public void deleteRoleForUser(@PathParam("scope")RoleScope scope,
                       @PathParam("role") String role,
                       @PathParam("userId") String userId) {
        final Optional<RoleEntity> roleToRemove = roleService.findByScopeAndName(scope, role);
        if (roleToRemove.isPresent()) {
            String roleId = roleToRemove.get().getId();
            if (RoleScope.ORGANIZATION.equals(scope)) {
                membershipService.removeRole(
                        MembershipReferenceType.ORGANIZATION,
                        GraviteeContext.getCurrentOrganization(),
                        MembershipMemberType.USER,
                        userId,
                        roleId);
            } else if (RoleScope.ENVIRONMENT.equals(scope)) {
                membershipService.removeRole(
                        MembershipReferenceType.ENVIRONMENT,
                        GraviteeContext.getCurrentEnvironment(),
                        MembershipMemberType.USER,
                        userId,
                        roleId);
            }
        }
    }
}

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
import io.gravitee.rest.api.management.rest.model.RoleMembership;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.swagger.annotations.*;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = {"Roles"})
public class RoleUsersResource extends AbstractResource  {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private MembershipService membershipService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "List users with the given role",
            notes = "User must have the MANAGEMENT_ROLE[READ] permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 204, message = "List of user's memberships", response = MembershipListItem.class, responseContainer = "List"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.ORGANIZATION_ROLE, acls = RolePermissionAction.READ)
    })
    public List<MembershipListItem> getUsersPerRole(
            @PathParam("scope")RoleScope scope,
            @PathParam("role") String role) {
        if (RoleScope.ORGANIZATION.equals(scope) || RoleScope.ENVIRONMENT.equals(scope)) {
            Optional<RoleEntity> optRole = roleService.findByScopeAndName(scope, role);
            if(optRole.isPresent()) {
                MembershipReferenceType referenceType = null;
                String referenceId = null;
                
                if(RoleScope.ORGANIZATION.equals(scope)) {
                    referenceType = MembershipReferenceType.ORGANIZATION;
                    referenceId = GraviteeContext.getCurrentOrganization();
                } else if(RoleScope.ENVIRONMENT.equals(scope)) {
                    referenceType = MembershipReferenceType.ENVIRONMENT;
                    referenceId = GraviteeContext.getCurrentEnvironment();
                }
                Set<MemberEntity> members = membershipService.getMembersByReferenceAndRole(
                        referenceType,
                        referenceId,
                        optRole.get().getId());
    
                return members
                        .stream()
                        .filter(Objects::nonNull)
                        .map(MembershipListItem::new)
                        .sorted((a,b) -> {
                            if (a.getDisplayName() == null && b.getDisplayName() == null) {
                                return a.getId().compareToIgnoreCase(b.getId());
                            }
                            if (a.getDisplayName() == null) {
                                return -1;
                            }
                            if (b.getDisplayName() == null) {
                                return 1;
                            }
                            return a.getDisplayName().compareToIgnoreCase(b.getDisplayName());
                        })
                        .collect(Collectors.toList());
            }
        }

        return Collections.emptyList();
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Add or update a role for a user",
            notes = "User must have the MANAGEMENT_ROLE[CREATE] and MANAGEMENT_ROLE[UPDATE] permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 201, message = "Membership successfully created / updated"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.ORGANIZATION_ROLE, acls = RolePermissionAction.CREATE),
            @Permission(value = RolePermission.ORGANIZATION_ROLE, acls = RolePermissionAction.UPDATE),
    })
    public Response addRoleToUser(
            @ApiParam(name = "scope", required = true, allowableValues = "ORGANIZATION,ENVIRONMENT")
            @PathParam("scope")RoleScope roleScope,
            @PathParam("role") String roleName,
            @Valid @NotNull final RoleMembership roleMembership) {

        if (roleScope == null || roleName == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Role must be set").build();
        }
        MembershipReferenceType referenceType = null;
        String referenceId = null;
        
        if(RoleScope.ORGANIZATION.equals(roleScope)) {
            referenceType = MembershipReferenceType.ORGANIZATION;
            referenceId = GraviteeContext.getCurrentOrganization();
        } else if(RoleScope.ENVIRONMENT.equals(roleScope)) {
            referenceType = MembershipReferenceType.ENVIRONMENT;
            referenceId = GraviteeContext.getCurrentEnvironment();
        }
        if( referenceType == null || referenceId == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Can't determine context").build();
        }
        
        MemberEntity membership = membershipService.addRoleToMemberOnReference(
                new MembershipService.MembershipReference(referenceType, referenceId),
                new MembershipService.MembershipMember(roleMembership.getId(), roleMembership.getReference(), MembershipMemberType.USER),
                new MembershipService.MembershipRole(roleScope, roleName));

        return Response.created(URI.create("/users/" + membership.getId() + "/roles/" + membership.getId())).build();
    }

    @Path("{userId}")
    public RoleUserResource getRoleUserResource() {
        return resourceContext.getResource(RoleUserResource.class);
    }
}

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

import io.gravitee.common.http.MediaType;
import io.gravitee.management.model.MemberEntity;
import io.gravitee.management.model.permissions.RolePermission;
import io.gravitee.management.model.permissions.RolePermissionAction;
import io.gravitee.management.rest.model.RoleMembership;
import io.gravitee.management.rest.security.Permission;
import io.gravitee.management.rest.security.Permissions;
import io.gravitee.management.service.MembershipService;
import io.gravitee.repository.management.model.MembershipDefaultReferenceId;
import io.gravitee.repository.management.model.MembershipReferenceType;
import io.gravitee.repository.management.model.RoleScope;
import io.swagger.annotations.*;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = {"Roles"})
public class RoleUsersResource extends AbstractResource  {

    @Context
    private ResourceContext resourceContext;

    @Autowired
    private MembershipService membershipService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "List users with the given role",
            notes = "User must have the MANAGEMENT_ROLE[READ] permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 204, message = "List of user's memberships", response = MembershipListItem.class, responseContainer = "List"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.MANAGEMENT_ROLE, acls = RolePermissionAction.READ)
    })
    public List<MembershipListItem> listUsersPerRole(
            @PathParam("scope")RoleScope scope,
            @PathParam("role") String role) {
        if (RoleScope.MANAGEMENT.equals(scope) || RoleScope.PORTAL.equals(scope)) {
            Set<MemberEntity> members = membershipService.getMembers(
                    RoleScope.MANAGEMENT.equals(scope) ? MembershipReferenceType.MANAGEMENT : MembershipReferenceType.PORTAL,
                    MembershipDefaultReferenceId.DEFAULT.name(),
                    scope,
                    role);

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
            @Permission(value = RolePermission.MANAGEMENT_ROLE, acls = RolePermissionAction.CREATE),
            @Permission(value = RolePermission.MANAGEMENT_ROLE, acls = RolePermissionAction.UPDATE),
    })
    public Response addRoleToUser(
            @ApiParam(name = "scope", required = true, allowableValues = "MANAGEMENT,PORTAL,API,APPLICATION")
            @PathParam("scope")RoleScope roleScope,
            @PathParam("role") String roleName,
            @Valid @NotNull final RoleMembership roleMembership) {

        if (roleScope == null || roleName == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Role must be set").build();
        }

        MemberEntity membership = membershipService.addOrUpdateMember(
                new MembershipService.MembershipReference(
                        RoleScope.MANAGEMENT.equals(roleScope) ? MembershipReferenceType.MANAGEMENT : MembershipReferenceType.PORTAL,
                        MembershipDefaultReferenceId.DEFAULT.name()),
                new MembershipService.MembershipUser(roleMembership.getId(), roleMembership.getReference()),
                new MembershipService.MembershipRole(roleScope, roleName));

        return Response.created(URI.create("/users/" + membership.getId() + "/roles/" + membership.getId())).build();
    }

    @Path("{userId}")
    public RoleUserResource getRoleUserResource() {
        return resourceContext.getResource(RoleUserResource.class);
    }

    private final static class MembershipListItem {

        private final MemberEntity member;

        public MembershipListItem(final MemberEntity member) {
            this.member = member;
        }

        public String getId() {
            return member.getId();
        }

        public String getDisplayName() {
            return member.getDisplayName();
        }
    }
}

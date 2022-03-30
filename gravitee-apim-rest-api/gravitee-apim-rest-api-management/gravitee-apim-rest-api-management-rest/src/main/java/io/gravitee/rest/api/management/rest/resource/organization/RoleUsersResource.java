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
package io.gravitee.rest.api.management.rest.resource.organization;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.rest.model.RoleMembership;
import io.gravitee.rest.api.management.rest.resource.AbstractResource;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.*;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Tag(name = "Roles")
public class RoleUsersResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private MembershipService membershipService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "List users with the given role",
        description = "User must have the MANAGEMENT_ROLE[READ] permission to use this service"
    )
    @ApiResponse(
        responseCode = "204",
        description = "List of user's memberships",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            array = @ArraySchema(schema = @Schema(implementation = MembershipListItem.class))
        )
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.ORGANIZATION_ROLE, acls = RolePermissionAction.READ) })
    public List<MembershipListItem> getUsersPerRole(@PathParam("scope") RoleScope scope, @PathParam("role") String role) {
        if (RoleScope.ORGANIZATION.equals(scope)) {
            Optional<RoleEntity> optRole = roleService.findByScopeAndName(scope, role, GraviteeContext.getCurrentOrganization());
            if (optRole.isPresent()) {
                Set<MemberEntity> members = membershipService.getMembersByReferenceAndRole(
                    GraviteeContext.getExecutionContext(),
                    MembershipReferenceType.ORGANIZATION,
                    GraviteeContext.getCurrentOrganization(),
                    optRole.get().getId()
                );

                return members
                    .stream()
                    .filter(Objects::nonNull)
                    .map(MembershipListItem::new)
                    .sorted(
                        (a, b) -> {
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
                        }
                    )
                    .collect(Collectors.toList());
            }
        }

        return Collections.emptyList();
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Add or update a role for a user",
        description = "User must have the MANAGEMENT_ROLE[CREATE] and MANAGEMENT_ROLE[UPDATE] permission to use this service"
    )
    @ApiResponse(responseCode = "201", description = "Membership successfully created / updated")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions(
        {
            @Permission(value = RolePermission.ORGANIZATION_ROLE, acls = RolePermissionAction.CREATE),
            @Permission(value = RolePermission.ORGANIZATION_ROLE, acls = RolePermissionAction.UPDATE),
        }
    )
    public Response addRoleToUser(
        @Parameter(name = "scope", required = true, schema = @Schema(allowableValues = { "ORGANIZATION", "ENVIRONMENT" })) @PathParam(
            "scope"
        ) RoleScope roleScope,
        @PathParam("role") String roleName,
        @Valid @NotNull final RoleMembership roleMembership
    ) {
        if (roleScope == null || roleName == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Role must be set").build();
        }
        MembershipReferenceType referenceType = null;
        String referenceId = null;

        if (!RoleScope.ORGANIZATION.equals(roleScope)) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Can't determine context").build();
        }

        membershipService.addRoleToMemberOnReference(
            GraviteeContext.getExecutionContext(),
            new MembershipService.MembershipReference(MembershipReferenceType.ORGANIZATION, GraviteeContext.getCurrentOrganization()),
            new MembershipService.MembershipMember(roleMembership.getId(), roleMembership.getReference(), MembershipMemberType.USER),
            new MembershipService.MembershipRole(roleScope, roleName)
        );

        return Response.status(Response.Status.CREATED).build();
    }

    @Path("{userId}")
    public RoleUserResource getRoleUserResource() {
        return resourceContext.getResource(RoleUserResource.class);
    }
}

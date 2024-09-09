/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.management.rest.resource;

import static io.gravitee.rest.api.model.permissions.RolePermissionAction.*;
import static io.gravitee.rest.api.model.permissions.RoleScope.*;
import static io.gravitee.rest.api.model.permissions.SystemRole.PRIMARY_OWNER;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.rest.model.ApplicationMembership;
import io.gravitee.rest.api.management.rest.model.TransferOwnership;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.permissions.ApplicationPermission;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.SinglePrimaryOwnerException;
import io.gravitee.rest.api.service.exceptions.UserNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.core.Response;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Tag(name = "Application Memberships")
public class ApplicationMembersResource extends AbstractResource {

    @Inject
    private MembershipService membershipService;

    @Inject
    private ApplicationService applicationService;

    @Inject
    private UserService userService;

    @SuppressWarnings("UnresolvedRestParam")
    @PathParam("application")
    @Parameter(name = "application", hidden = true)
    private String application;

    @GET
    @Path("/permissions")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get application members", description = "User must have the APPLICATION_MEMBER permission to use this service")
    @ApiResponse(
        responseCode = "200",
        description = "Application member's permissions",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            array = @ArraySchema(schema = @Schema(implementation = MemberEntity.class))
        )
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public Response getApplicationMemberPermissions() {
        Map<String, char[]> permissions = new HashMap<>();
        if (isAuthenticated()) {
            if (isAdmin()) {
                final char[] rights = new char[] { CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId() };
                for (ApplicationPermission perm : ApplicationPermission.values()) {
                    permissions.put(perm.getName(), rights);
                }
            } else {
                final String username = getAuthenticatedUser();
                final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
                permissions =
                    membershipService.getUserMemberPermissions(
                        executionContext,
                        MembershipReferenceType.APPLICATION,
                        application,
                        username
                    );
            }
        }
        return Response.ok(permissions).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "List application members", description = "User must have the READ permission to use this service")
    @ApiResponse(
        responseCode = "200",
        description = "Application successfully deleted",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            array = @ArraySchema(schema = @Schema(implementation = MembershipListItem.class))
        )
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.APPLICATION_MEMBER, acls = RolePermissionAction.READ) })
    public List<MembershipListItem> getApplicationMembers() {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        applicationService.findById(executionContext, application);
        return membershipService
            .getMembersByReference(executionContext, MembershipReferenceType.APPLICATION, application)
            .stream()
            .map(MembershipListItem::new)
            .sorted(Comparator.comparing(MembershipListItem::getId))
            .collect(Collectors.toList());
    }

    @POST
    @Operation(
        summary = "Add or update an application member",
        description = "User must have the MANAGE_MEMBERS permission to use this service"
    )
    @ApiResponse(responseCode = "201", description = "Member has been added or updated successfully")
    @ApiResponse(responseCode = "400", description = "Membership parameter is not valid")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions(
        {
            @Permission(value = RolePermission.APPLICATION_MEMBER, acls = RolePermissionAction.CREATE),
            @Permission(value = RolePermission.APPLICATION_MEMBER, acls = RolePermissionAction.UPDATE),
        }
    )
    public Response addOrUpdateApplicationMember(@Valid @NotNull ApplicationMembership applicationMembership) {
        if (PRIMARY_OWNER.name().equals(applicationMembership.getRole())) {
            throw new SinglePrimaryOwnerException(APPLICATION);
        }

        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        applicationService.findById(executionContext, application);

        MembershipService.MembershipReference reference = new MembershipService.MembershipReference(
            MembershipReferenceType.APPLICATION,
            application
        );
        MembershipService.MembershipMember member = new MembershipService.MembershipMember(
            applicationMembership.getId(),
            applicationMembership.getReference(),
            MembershipMemberType.USER
        );
        MembershipService.MembershipRole role = new MembershipService.MembershipRole(APPLICATION, applicationMembership.getRole());

        MemberEntity membership = null;
        if (applicationMembership.getId() != null) {
            MemberEntity userMember = membershipService.getUserMember(
                executionContext,
                MembershipReferenceType.APPLICATION,
                application,
                applicationMembership.getId()
            );
            if (userMember != null && userMember.getRoles() != null && !userMember.getRoles().isEmpty()) {
                membership = membershipService.updateRoleToMemberOnReference(executionContext, reference, member, role);
            }
        }
        if (membership == null) {
            membershipService.addRoleToMemberOnReference(executionContext, reference, member, role);
        }

        return Response.status(Response.Status.CREATED).build();
    }

    @DELETE
    @Operation(summary = "Remove an application member", description = "User must have the MANAGE_MEMBERS permission to use this service")
    @ApiResponse(responseCode = "200", description = "Member has been removed successfully")
    @ApiResponse(responseCode = "400", description = "User does not exist")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.APPLICATION_MEMBER, acls = RolePermissionAction.DELETE) })
    public Response deleteApplicationMember(@Parameter(name = "user", required = true) @NotNull @QueryParam("user") String userId) {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        applicationService.findById(executionContext, application);
        try {
            userService.findById(executionContext, userId);
        } catch (UserNotFoundException unfe) {
            return Response.status(Response.Status.BAD_REQUEST).entity(unfe.getMessage()).build();
        }

        membershipService.deleteReferenceMember(
            executionContext,
            MembershipReferenceType.APPLICATION,
            application,
            MembershipMemberType.USER,
            userId
        );
        return Response.ok().build();
    }

    @POST
    @Path("transfer_ownership")
    @Operation(
        summary = "Transfer the ownership of the APPLICATION",
        description = "User must have the TRANSFER_OWNERSHIP permission to use this service"
    )
    @ApiResponse(responseCode = "200", description = "Ownership has been transferred successfully")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.APPLICATION_MEMBER, acls = RolePermissionAction.UPDATE) })
    public Response transferApplicationOwnership(@Valid @NotNull TransferOwnership transferOwnership) {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        List<RoleEntity> newRoles = new ArrayList<>();

        roleService
            .findByScopeAndName(APPLICATION, transferOwnership.getPoRole(), executionContext.getOrganizationId())
            .ifPresent(newRoles::add);

        applicationService.findById(executionContext, application);
        membershipService.transferApplicationOwnership(
            executionContext,
            application,
            new MembershipService.MembershipMember(transferOwnership.getId(), transferOwnership.getReference(), MembershipMemberType.USER),
            newRoles
        );
        return Response.ok().build();
    }
}

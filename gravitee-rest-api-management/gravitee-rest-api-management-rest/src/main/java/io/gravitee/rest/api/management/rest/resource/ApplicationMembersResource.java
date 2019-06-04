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
import io.gravitee.repository.management.model.MembershipReferenceType;
import io.gravitee.repository.management.model.RoleScope;
import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.model.MembershipListItem;
import io.gravitee.rest.api.model.permissions.ApplicationPermission;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.management.rest.model.ApplicationMembership;
import io.gravitee.rest.api.management.rest.model.TransferOwnership;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.exceptions.RoleNotFoundException;
import io.gravitee.rest.api.service.exceptions.SinglePrimaryOwnerException;
import io.gravitee.rest.api.service.exceptions.UserNotFoundException;
import io.swagger.annotations.*;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DELETE;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import static io.gravitee.rest.api.model.permissions.RolePermissionAction.*;
import static io.gravitee.rest.api.model.permissions.SystemRole.PRIMARY_OWNER;

import java.net.URI;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = {"Application"})
public class ApplicationMembersResource  extends AbstractResource {

    @Inject
    private MembershipService membershipService;

    @Inject
    private ApplicationService applicationService;
    @Inject
    private UserService userService;

    @GET
    @Path("/permissions")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get application members",
            notes = "User must have the APPLICATION_MEMBER permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Application member's permissions", response = MemberEntity.class, responseContainer = "List"),
            @ApiResponse(code = 500, message = "Internal server error")})
    public Response getPermissions(@PathParam("application") String application) {
        Map<String, char[]> permissions = new HashMap<>();
        if (isAuthenticated()) {
            final String username = getAuthenticatedUser();
            final ApplicationEntity applicationEntity = applicationService.findById(application);
            if (isAdmin()) {
                final char[] rights = new char[]{CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId()};
                for (ApplicationPermission perm: ApplicationPermission.values()) {
                    permissions.put(perm.getName(), rights);
                }
            } else {
                permissions = membershipService.getMemberPermissions(applicationEntity, username);
            }
        }
        return Response.ok(permissions).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "List application members",
            notes = "User must have the READ permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Application successfully deleted", response = MembershipListItem.class, responseContainer = "List"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.APPLICATION_MEMBER, acls = RolePermissionAction.READ)
    })
    public List<MembershipListItem> listApplicationMembers(@PathParam("application") String application) {
        applicationService.findById(application);
        return membershipService.getMembers(MembershipReferenceType.APPLICATION, application, RoleScope.APPLICATION, null)
                .stream()
                .map(MembershipListItem::new)
                .sorted(Comparator.comparing(MembershipListItem::getId))
                .collect(Collectors.toList());
    }

    @POST
    @ApiOperation(value = "Add or update an application member",
            notes = "User must have the MANAGE_MEMBERS permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 201, message = "Member has been added or updated successfully"),
            @ApiResponse(code = 400, message = "Membership parameter is not valid"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.APPLICATION_MEMBER, acls = RolePermissionAction.CREATE),
            @Permission(value = RolePermission.APPLICATION_MEMBER, acls = RolePermissionAction.UPDATE)
    })
    public Response addOrUpdateApplicationMember(
            @PathParam("application") String application,
            @Valid @NotNull ApplicationMembership applicationMembership) {

        if (PRIMARY_OWNER.name().equals(applicationMembership.getRole())) {
            throw new SinglePrimaryOwnerException(RoleScope.APPLICATION);
        }

        applicationService.findById(application);

        MemberEntity membership = membershipService.addOrUpdateMember(
                new MembershipService.MembershipReference(MembershipReferenceType.APPLICATION, application),
                new MembershipService.MembershipUser(applicationMembership.getId(), applicationMembership.getReference()),
                new MembershipService.MembershipRole(RoleScope.APPLICATION, applicationMembership.getRole()));

        return Response.created(URI.create("/applications/" + application + "/members/" + membership.getId())).build();
    }

    @DELETE
    @ApiOperation(value = "Remove an application member",
            notes = "User must have the MANAGE_MEMBERS permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Member has been removed successfully"),
            @ApiResponse(code = 400, message = "User does not exist"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.APPLICATION_MEMBER, acls = RolePermissionAction.DELETE)
    })
    public Response deleteApplicationMember(
            @PathParam("application") String application,
            @ApiParam(name = "user", required = true) @NotNull @QueryParam("user") String userId) {
        applicationService.findById(application);
        try {
            userService.findById(userId);
        } catch (UserNotFoundException unfe) {
            return Response.status(Response.Status.BAD_REQUEST).entity(unfe.getMessage()).build();
        }

        membershipService.deleteMember(MembershipReferenceType.APPLICATION, application, userId);
        return Response.ok().build();
    }

    @POST
    @Path("transfer_ownership")
    @ApiOperation(value = "Transfer the ownership of the APPLICATION",
            notes = "User must have the TRANSFER_OWNERSHIP permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Ownership has been transferred successfully"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.APPLICATION_MEMBER, acls = RolePermissionAction.UPDATE)
    })
    public Response transferOwnership(
            @PathParam("application") String application,
            @Valid @NotNull TransferOwnership transferOwnership) {
        io.gravitee.rest.api.model.RoleEntity newPORole = null;

        try {
            newPORole = roleService.findById(RoleScope.API, transferOwnership.getPoRole());
        } catch (RoleNotFoundException re) {
            //it doesn't matter
        }

        applicationService.findById(application);
        membershipService.transferApplicationOwnership(application, new MembershipService.MembershipUser(
                transferOwnership.getId(), transferOwnership.getReference()), newPORole);
        return Response.ok().build();
    }
}

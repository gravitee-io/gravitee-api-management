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
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.permissions.ApiPermission;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.management.rest.model.ApiMembership;
import io.gravitee.rest.api.management.rest.model.TransferOwnership;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.UserService;
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
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = {"API"})
public class ApiMembersResource extends AbstractResource {

    @Inject
    private UserService userService;

    @GET
    @Path("/permissions")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get API members",
            notes = "User must have the MANAGE_MEMBERS permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "API member's permissions", response = MemberEntity.class, responseContainer = "List"),
            @ApiResponse(code = 500, message = "Internal server error")})
    public Response getPermissions(@PathParam("api") String api) {
        final ApiEntity apiEntity = apiService.findById(api);
        Map<String, char[]> permissions = new HashMap<>();
        if (isAuthenticated()) {
            final String userId = getAuthenticatedUser();
            if (isAdmin()) {
                final char[] rights = new char[]{CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId()};
                for (ApiPermission perm: ApiPermission.values()) {
                    permissions.put(perm.getName(), rights);
                }
            } else {
                permissions = membershipService.getUserMemberPermissions(apiEntity, userId);
            }
        }
        return Response.ok(permissions).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "List API members",
            notes = "User must have the MANAGE_MEMBERS permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "List of API's members", response = MembershipListItem.class, responseContainer = "List"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.API_MEMBER, acls = RolePermissionAction.READ)
    })
    public List<MembershipListItem> listApiMembers(@PathParam("api") String api) {
        apiService.findById(api);
        return membershipService.getMembersByReference(MembershipReferenceType.API, api)
                .stream()
                .map(MembershipListItem::new)
                .sorted(Comparator.comparing(MembershipListItem::getId))
                .collect(Collectors.toList());
    }

    @POST
    @ApiOperation(value = "Add or update an API member",
            notes = "User must have the MANAGE_MEMBERS permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 201, message = "Member has been added or updated successfully"),
            @ApiResponse(code = 400, message = "Membership parameter is not valid"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.API_MEMBER, acls = RolePermissionAction.CREATE),
            @Permission(value = RolePermission.API_MEMBER, acls = RolePermissionAction.UPDATE)
    })
    public Response addOrUpdateApiMember(
            @PathParam("api") String api,
            @Valid @NotNull ApiMembership apiMembership) {

        if (PRIMARY_OWNER.name().equals(apiMembership.getRole())) {
            throw new SinglePrimaryOwnerException(RoleScope.API);
        }

        apiService.findById(api);

        MemberEntity membership = membershipService.addRoleToMemberOnReference(
                new MembershipService.MembershipReference(MembershipReferenceType.API, api),
                new MembershipService.MembershipMember(apiMembership.getId(), apiMembership.getReference(), MembershipMemberType.USER),
                new MembershipService.MembershipRole(RoleScope.API, apiMembership.getRole()));

        return Response.created(URI.create("/apis/" + api + "/members/" + membership.getId())).build();
    }

    @POST
    @Path("transfer_ownership")
    @ApiOperation(value = "Transfer the ownership of the API",
            notes = "User must have the TRANSFER_OWNERSHIP permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Ownership has been transferred successfully"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.API_MEMBER, acls = RolePermissionAction.UPDATE)
    })
    public Response transferOwnership(
            @PathParam("api") String api,
            @Valid @NotNull TransferOwnership transferOwnership) {
        List<RoleEntity> newRoles = new ArrayList<>();

        Optional<RoleEntity> optNewRole = roleService.findByScopeAndName(RoleScope.API, transferOwnership.getPoRole());
        if (optNewRole.isPresent()) {
            newRoles.add(optNewRole.get());
        } else {
            //it doesn't matter
        }

        apiService.findById(api);
        membershipService.transferApiOwnership(api, new MembershipService.MembershipMember(
                transferOwnership.getId(), transferOwnership.getReference(), MembershipMemberType.USER), newRoles);
        return Response.ok().build();
    }

    @DELETE
    @ApiOperation(value = "Remove an API member",
            notes = "User must have the MANAGE_MEMBERS permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Member has been removed successfully"),
            @ApiResponse(code = 400, message = "User does not exist"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.API_MEMBER, acls = RolePermissionAction.DELETE)
    })
    public Response deleteApiMember(
            @PathParam("api") String api,
            @ApiParam(name = "user", required = true) @NotNull @QueryParam("user") String userId) {
        apiService.findById(api);
        try {
            userService.findById(userId);
        } catch (UserNotFoundException unfe) {
            return Response.status(Response.Status.BAD_REQUEST).entity(unfe.getMessage()).build();
        }

        membershipService.deleteReferenceMember(MembershipReferenceType.API, api, MembershipMemberType.USER, userId);
        return Response.ok().build();
    }
}

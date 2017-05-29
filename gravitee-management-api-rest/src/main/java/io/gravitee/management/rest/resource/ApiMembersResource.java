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
import io.gravitee.management.idp.api.authentication.UserDetails;
import io.gravitee.management.model.ApiEntity;
import io.gravitee.management.model.MemberEntity;
import io.gravitee.management.model.Visibility;
import io.gravitee.management.model.permissions.ApiPermission;
import io.gravitee.management.model.permissions.RolePermission;
import io.gravitee.management.model.permissions.RolePermissionAction;
import io.gravitee.management.rest.security.Permission;
import io.gravitee.management.rest.security.Permissions;
import io.gravitee.management.service.ApiService;
import io.gravitee.management.service.MembershipService;
import io.gravitee.management.service.UserService;
import io.gravitee.management.service.exceptions.ForbiddenAccessException;
import io.gravitee.management.service.exceptions.SinglePrimaryOwnerException;
import io.gravitee.management.service.exceptions.UserNotFoundException;
import io.gravitee.repository.management.model.MembershipReferenceType;
import io.gravitee.repository.management.model.RoleScope;
import io.swagger.annotations.*;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DELETE;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.gravitee.management.model.permissions.RolePermissionAction.*;
import static io.gravitee.management.model.permissions.SystemRole.PRIMARY_OWNER;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = {"API"})
public class ApiMembersResource extends AbstractResource {

    @Inject
    private MembershipService membershipService;

    @Inject
    private ApiService apiService;

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
        final Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails) {
            final UserDetails details = ((UserDetails) principal);
            final String username = details.getUsername();
            if (username.equals(apiEntity.getPrimaryOwner().getUsername()) || isAdmin()) {
                final char[] rights = new char[]{CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId()};
                for (ApiPermission perm: ApiPermission.values()) {
                    permissions.put(perm.getName(), rights);
                }
            } else {
                MemberEntity member = membershipService.getMember(MembershipReferenceType.API, api, username);
                if (member == null && apiEntity.getGroup() != null) {
                    member = membershipService.getMember(MembershipReferenceType.API_GROUP, apiEntity.getGroup().getId(), username);
                }

                if (member != null) {
                    permissions = member.getPermissions();
                }
            }
        }
        return Response.ok(permissions).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "List API members",
            notes = "User must have the MANAGE_MEMBERS permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "List of API's members", response = MemberEntity.class, responseContainer = "List"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.API_MEMBER, acls = RolePermissionAction.READ)
    })
    public List<MemberEntity> listApiMembers(@PathParam("api") String api) {
        apiService.findById(api);
        return membershipService.getMembers(MembershipReferenceType.API, api, null, null).stream()
                .sorted(Comparator.comparing(MemberEntity::getUsername))
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

            @ApiParam(name = "user", required = true)
            @NotNull @QueryParam("user") String username,

            @ApiParam(name = "rolename", required = true)
            @NotNull @QueryParam("rolename") String roleName
    ) {
        apiService.findById(api);
        if (roleName == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Membership type must be set").build();
        }

        if (PRIMARY_OWNER.name().equals(roleName)) {
            throw new SinglePrimaryOwnerException(RoleScope.API);
        }

        membershipService.addOrUpdateMember(MembershipReferenceType.API, api, username, RoleScope.API, roleName);
        return Response.created(URI.create("/apis/" + api + "/members/" + username)).build();
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
    public Response transferOwnership(@PathParam("api") String api, @NotNull @QueryParam("user") String username) {
        apiService.findById(api);
        membershipService.transferApiOwnership(api, username);
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
            @ApiParam(name = "user", required = true) @NotNull @QueryParam("user") String username) {
        apiService.findById(api);
        try {
            userService.findByName(username, false);
        } catch (UserNotFoundException unfe) {
            return Response.status(Response.Status.BAD_REQUEST).entity(unfe.getMessage()).build();
        }

        membershipService.deleteMember(MembershipReferenceType.API, api, username);
        return Response.ok().build();
    }
}

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
import io.gravitee.management.model.MembershipType;
import io.gravitee.management.model.permissions.ApiPermission;
import io.gravitee.management.rest.resource.param.MembershipTypeParam;
import io.gravitee.management.rest.security.ApiPermissionsRequired;
import io.gravitee.management.service.ApiService;
import io.gravitee.management.service.MembershipService;
import io.gravitee.management.service.UserService;
import io.gravitee.management.service.exceptions.UserNotFoundException;
import io.gravitee.repository.management.model.MembershipReferenceType;
import io.swagger.annotations.*;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@ApiPermissionsRequired(ApiPermission.MANAGE_MEMBERS)
@Api(tags = {"API"})
public class ApiMembersResource {

    @Inject
    private MembershipService membershipService;

    @Inject
    private ApiService apiService;

    @Inject
    private UserService userService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "List API members",
            notes = "User must have the MANAGE_MEMBERS permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "List of API's members", response = MemberEntity.class, responseContainer = "List"),
            @ApiResponse(code = 500, message = "Internal server error")})
    public List<MemberEntity> listApiMembers(@PathParam("api") String api) {
        apiService.findById(api);
        return membershipService.getMembers(MembershipReferenceType.API, api, null).stream()
                .sorted((o1, o2) -> o1.getUsername().compareTo(o2.getUsername()))
                .collect(Collectors.toList());
    }

    @POST
    @ApiOperation(value = "Add or update an API member",
            notes = "User must have the MANAGE_MEMBERS permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 201, message = "Member has been added or updated successfully"),
            @ApiResponse(code = 400, message = "Membership parameter is not valid"),
            @ApiResponse(code = 500, message = "Internal server error")})
    public Response addOrUpdateApiMember(
            @PathParam("api") String api,
            @ApiParam(name = "user", required = true)
                @NotNull @QueryParam("user") String username,
            @ApiParam(name = "type", required = true, allowableValues = "PRIMARY_OWNER,OWNER,USER")
                @NotNull @QueryParam("type") MembershipTypeParam membershipType) {
        apiService.findById(api);
        if (membershipType.getValue() == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Membership type must be set").build();
        }

        if (MembershipType.PRIMARY_OWNER.equals(membershipType.getValue())) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Illegal membership type 'PRIMARY_OWNER'").build();
        }

        membershipService.addOrUpdateMember(MembershipReferenceType.API, api, username, membershipType.getValue());
        return Response.created(URI.create("/apis/" + api + "/members/" + username)).build();
    }

    @POST
    @Path("transfer_ownership")
    @ApiPermissionsRequired(ApiPermission.TRANSFER_OWNERSHIP)
    @ApiOperation(value = "Transfer the ownership of the API",
            notes = "User must have the TRANSFER_OWNERSHIP permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Ownership has been transferred successfully"),
            @ApiResponse(code = 500, message = "Internal server error")})
    public Response transferOwnership(@PathParam("api") String api, @NotNull @QueryParam("user") String username) {
        apiService.findById(api);
        // change previous primary owner privilege
        // TODO : API must have a single PRIMARY_OWNER, refactor getMembers() code part
        membershipService.getMembers(MembershipReferenceType.API, api, MembershipType.PRIMARY_OWNER)
                .forEach(m -> membershipService.addOrUpdateMember(MembershipReferenceType.API, api, m.getUsername(), MembershipType.OWNER));
        // set the new primary owner
        membershipService.addOrUpdateMember(MembershipReferenceType.API, api, username, MembershipType.PRIMARY_OWNER);
        return Response.ok().build();
    }

    @DELETE
    @ApiOperation(value = "Remove an API member",
            notes = "User must have the MANAGE_MEMBERS permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Member has been removed successfully"),
            @ApiResponse(code = 400, message = "User does not exist"),
            @ApiResponse(code = 500, message = "Internal server error")})
    public Response deleteApiMember(
            @PathParam("api") String api,
            @ApiParam(name = "user", required = true) @NotNull @QueryParam("user") String username) {
        apiService.findById(api);
        try {
            userService.findByName(username);
        } catch (UserNotFoundException unfe) {
            return Response.status(Response.Status.BAD_REQUEST).entity(unfe.getMessage()).build();
        }

        membershipService.deleteMember(MembershipReferenceType.API, api, username);
        return Response.ok().build();
    }
}

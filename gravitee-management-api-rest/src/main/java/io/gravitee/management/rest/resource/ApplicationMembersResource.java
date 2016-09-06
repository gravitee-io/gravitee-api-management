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
import io.gravitee.management.model.permissions.ApplicationPermission;
import io.gravitee.management.rest.resource.param.MembershipTypeParam;
import io.gravitee.management.rest.security.ApplicationPermissionsRequired;
import io.gravitee.management.service.ApplicationService;
import io.gravitee.management.service.UserService;
import io.gravitee.management.service.exceptions.UserNotFoundException;
import io.swagger.annotations.*;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
@Api(tags = {"Application"})
public class ApplicationMembersResource {

    @Inject
    private ApplicationService applicationService;

    @Inject
    private UserService userService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApplicationPermissionsRequired(ApplicationPermission.READ)
    @ApiOperation(value = "List application members",
            notes = "User must have the READ permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Application successfully deleted", response = MemberEntity.class, responseContainer = "List"),
            @ApiResponse(code = 500, message = "Internal server error")})
    public List<MemberEntity> listApplicationMembers(@PathParam("application") String application) {
        return applicationService.getMembers(application, null).stream()
                .sorted((o1, o2) -> o1.getUsername().compareTo(o2.getUsername()))
                .collect(Collectors.toList());
    }

    @POST
    @ApplicationPermissionsRequired(ApplicationPermission.MANAGE_MEMBERS)
    @ApiOperation(value = "Add or update an application member",
            notes = "User must have the MANAGE_MEMBERS permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 201, message = "Member has been added or updated successfully"),
            @ApiResponse(code = 400, message = "Membership parameter is not valid"),
            @ApiResponse(code = 500, message = "Internal server error")})
    public Response addOrUpdateApplicationMember(
            @PathParam("application") String application,
            @ApiParam(name = "user", required = true)
                @NotNull @QueryParam("user") String username,
            @ApiParam(name = "type", required = true, allowableValues = "PRIMARY_OWNER,OWNER,USER")
                @NotNull @QueryParam("type") MembershipTypeParam membershipType) {
        if (membershipType.getValue() == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        applicationService.addOrUpdateMember(application, username, membershipType.getValue());
        return Response.created(URI.create("/applications/" + application + "/members/" + username)).build();
    }

    @DELETE
    @ApplicationPermissionsRequired(ApplicationPermission.MANAGE_MEMBERS)
    @ApiOperation(value = "Remove an application member",
            notes = "User must have the MANAGE_MEMBERS permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Member has been removed successfully"),
            @ApiResponse(code = 400, message = "User does not exist"),
            @ApiResponse(code = 500, message = "Internal server error")})
    public Response deleteApplicationMember(
            @PathParam("application") String application,
            @ApiParam(name = "user", required = true) @NotNull @QueryParam("user") String username) {
        try {
            userService.findByName(username);
        } catch (UserNotFoundException unfe) {
            return Response.status(Response.Status.BAD_REQUEST).entity(unfe.getMessage()).build();
        }

        applicationService.deleteMember(application, username);
        return Response.ok().build();
    }
}

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

import io.gravitee.management.model.GroupEntity;
import io.gravitee.management.model.GroupEntityType;
import io.gravitee.management.model.MemberEntity;
import io.gravitee.management.model.MembershipType;
import io.gravitee.management.rest.resource.param.MembershipTypeParam;
import io.gravitee.management.service.GroupService;
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
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com) 
 * @author GraviteeSource Team
 */
public class GroupMembersResource extends AbstractResource {

    @Inject
    private UserService userService;

    @Inject
    private GroupService groupService;

    @Inject
    private MembershipService membershipService;

    @GET
    @Produces(io.gravitee.common.http.MediaType.APPLICATION_JSON)
    @ApiOperation(value = "List Group members")
    @ApiResponses({
            @ApiResponse(code = 200, message = "List of Group's members", response = MemberEntity.class, responseContainer = "List"),
            @ApiResponse(code = 500, message = "Internal server error")})
    public List<MemberEntity> getMembers(@PathParam("group") String group) {
        GroupEntity groupEntity = groupService.findById(group);
        return membershipService.getMembers(
                groupEntity.getType().equals(GroupEntityType.API) ?
                        MembershipReferenceType.API_GROUP :
                        MembershipReferenceType.APPLICATION_GROUP,
                group).stream()
                .sorted((o1, o2) -> o1.getUsername().compareTo(o2.getUsername()))
                .collect(Collectors.toList());
    }

    @POST
    @ApiOperation(value = "Add or update an Group member")
    @ApiResponses({
            @ApiResponse(code = 201, message = "Member has been added or updated successfully"),
            @ApiResponse(code = 400, message = "Membership parameter is not valid"),
            @ApiResponse(code = 500, message = "Internal server error")})
    public Response addOrUpdateMember(
            @PathParam("group") String group,
            @ApiParam(name = "user", required = true)
            @NotNull @QueryParam("user") String username,
            @ApiParam(name = "type", required = true, allowableValues = "PRIMARY_OWNER,OWNER,USER")
            @NotNull @QueryParam("type") MembershipTypeParam membershipType) {
        GroupEntity groupEntity = groupService.findById(group);
        if (membershipType.getValue() == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Membership type must be set").build();
        }

        if (MembershipType.PRIMARY_OWNER.equals(membershipType.getValue())) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Illegal membership type 'PRIMARY_OWNER'").build();
        }

        membershipService.addOrUpdateMember(
                groupEntity.getType().equals(GroupEntityType.API) ?
                        MembershipReferenceType.API_GROUP :
                        MembershipReferenceType.APPLICATION_GROUP,
                group,
                username,
                membershipType.getValue());
        return Response.created(URI.create("/groups/" + group + "/members/" + username)).build();
    }

    @DELETE
    @ApiOperation(value = "Remove a group member")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Member has been removed successfully"),
            @ApiResponse(code = 400, message = "User does not exist"),
            @ApiResponse(code = 500, message = "Internal server error")})
    public Response deleteMember(
            @PathParam("group") String group,
            @ApiParam(name = "user", required = true) @NotNull @QueryParam("user") String username) {
        GroupEntity groupEntity = groupService.findById(group);
        try {
            userService.findByName(username);
        } catch (UserNotFoundException unfe) {
            return Response.status(Response.Status.BAD_REQUEST).entity(unfe.getMessage()).build();
        }

        membershipService.deleteMember(
                groupEntity.getType().equals(GroupEntityType.API) ?
                        MembershipReferenceType.API_GROUP :
                        MembershipReferenceType.APPLICATION_GROUP,
                group,
                username);
        return Response.ok().build();
    }

}

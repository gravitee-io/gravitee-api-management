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
package io.gravitee.rest.api.management.rest.resource.organization;

import static io.gravitee.common.http.MediaType.APPLICATION_JSON;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.rest.resource.AbstractResource;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.pagedresult.Metadata;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.GraviteeContext;
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
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.*;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Defines the REST resources to manage Users.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author Florent CHAMFROY (forent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Tag(name = "Users")
public class UserResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private UserService userService;

    @Inject
    private GroupService groupService;

    @PathParam("userId")
    @Parameter(name = "userId", required = true)
    private String userId;

    @GET
    @Produces(APPLICATION_JSON)
    @Operation(summary = "Retrieve a user", description = "User must have the ORGANIZATION_USERS[READ] permission to use this service")
    @ApiResponse(
        responseCode = "200",
        description = "A user",
        content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = UserEntity.class))
    )
    @ApiResponse(responseCode = "404", description = "User not found")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions(@Permission(value = RolePermission.ORGANIZATION_USERS, acls = RolePermissionAction.READ))
    public UserEntity getUser() {
        UserEntity user = userService.findByIdWithRoles(GraviteeContext.getExecutionContext(), userId);

        // Delete password for security reason
        user.setPassword(null);
        user.setPicture(null);

        return user;
    }

    @DELETE
    @Operation(summary = "Delete a user", description = "User must have the ORGANIZATION_USERS[DELETE] permission to use this service")
    @ApiResponse(responseCode = "204", description = "User successfully deleted")
    @ApiResponse(responseCode = "404", description = "User not found")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions(@Permission(value = RolePermission.ORGANIZATION_USERS, acls = RolePermissionAction.DELETE))
    public Response deleteUser() {
        userService.delete(GraviteeContext.getExecutionContext(), userId);
        return Response.noContent().build();
    }

    @GET
    @Path("/groups")
    @Produces(APPLICATION_JSON)
    @Operation(
        summary = "List of groups the user belongs to",
        description = "User must have the ORGANIZATION_USERS[READ] permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "List of user groups",
        content = @Content(mediaType = APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = UserGroupEntity.class)))
    )
    @ApiResponse(responseCode = "404", description = "User not found")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions(@Permission(value = RolePermission.ORGANIZATION_USERS, acls = RolePermissionAction.READ))
    public List<UserGroupEntity> getUserGroups() {
        // Check that user belongs to current organization
        userService.findById(GraviteeContext.getExecutionContext(), userId);

        List<UserGroupEntity> groups = new ArrayList<>();
        groupService
            .findByUser(userId)
            .forEach(groupEntity -> {
                UserGroupEntity userGroupEntity = new UserGroupEntity();
                userGroupEntity.setId(groupEntity.getId());
                userGroupEntity.setName(groupEntity.getName());
                userGroupEntity.setRoles(new HashMap<>());
                Set<RoleEntity> roles = membershipService.getRoles(
                    MembershipReferenceType.GROUP,
                    groupEntity.getId(),
                    MembershipMemberType.USER,
                    userId
                );
                if (!roles.isEmpty()) {
                    roles.forEach(role -> userGroupEntity.getRoles().put(role.getScope().name(), role.getName()));
                }
                groups.add(userGroupEntity);
            });

        return groups;
    }

    @GET
    @Path("/memberships")
    @Produces(APPLICATION_JSON)
    @Operation(
        summary = "List of memberships the user belongs to",
        description = "User must have the ORGANIZATION_USERS[READ] permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "List of user memberships",
        content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = UserMembershipList.class))
    )
    @ApiResponse(responseCode = "404", description = "User not found")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions(@Permission(value = RolePermission.ORGANIZATION_USERS, acls = RolePermissionAction.READ))
    public UserMembershipList getUserMemberships(@QueryParam("type") String sType) {
        // Check that user belongs to current organization
        userService.findById(GraviteeContext.getExecutionContext(), userId);

        MembershipReferenceType type = null;
        if (sType != null) {
            type = MembershipReferenceType.valueOf(sType.toUpperCase());
        }
        List<UserMembership> userMemberships = membershipService.findUserMembership(GraviteeContext.getExecutionContext(), type, userId);
        Metadata metadata = membershipService.findUserMembershipMetadata(userMemberships, type);
        UserMembershipList userMembershipList = new UserMembershipList();
        userMembershipList.setMemberships(userMemberships);
        userMembershipList.setMetadata(metadata.toMap());
        return userMembershipList;
    }

    @POST
    @Operation(
        summary = "Reset the user's password",
        description = "User must have the ORGANIZATION_USERS[UPDATE] permission to use this service"
    )
    @ApiResponse(responseCode = "204", description = "User's password reset")
    @ApiResponse(responseCode = "400", description = "reset page URL must not be null")
    @ApiResponse(responseCode = "404", description = "User not found")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions(
        @Permission(value = RolePermission.ORGANIZATION_USERS, acls = RolePermissionAction.UPDATE)
        // if permission changes or a new one is added, please update io.gravitee.rest.api.service.impl.UserServiceImpl#canResetPassword
    )
    @Path("resetPassword")
    public Response resetUserPassword() {
        userService.resetPassword(GraviteeContext.getExecutionContext(), userId);
        return Response.noContent().build();
    }

    @GET
    @Path("/avatar")
    @Operation(summary = "Get the user's avatar")
    @ApiResponse(
        responseCode = "200",
        description = "User's avatar",
        content = @Content(mediaType = "*/*", schema = @Schema(type = "string", format = "binary"))
    )
    @ApiResponse(responseCode = "404", description = "User not found")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public Response getUserAvatar(@Context Request request) {
        PictureEntity picture = userService.getPicture(GraviteeContext.getExecutionContext(), userId);

        if (picture instanceof UrlPictureEntity) {
            return Response.temporaryRedirect(URI.create(((UrlPictureEntity) picture).getUrl())).build();
        }

        InlinePictureEntity image = (InlinePictureEntity) picture;
        if (image == null || image.getContent() == null) {
            return Response.ok().build();
        }

        CacheControl cc = new CacheControl();
        cc.setNoTransform(true);
        cc.setMustRevalidate(false);
        cc.setNoCache(false);
        cc.setMaxAge(86400);

        EntityTag etag = new EntityTag(Integer.toString(new String(image.getContent()).hashCode()));
        Response.ResponseBuilder builder = request.evaluatePreconditions(etag);

        if (builder != null) {
            // Preconditions are not met, returning HTTP 304 'not-modified'
            return builder.cacheControl(cc).build();
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(image.getContent(), 0, image.getContent().length);

        return Response.ok().entity(baos).cacheControl(cc).tag(etag).type(image.getType()).build();
    }

    @PUT
    @Consumes(io.gravitee.common.http.MediaType.APPLICATION_JSON)
    @Path("/roles")
    @Permissions(@Permission(value = RolePermission.ORGANIZATION_USERS, acls = RolePermissionAction.UPDATE))
    public Response updateUserRoles(@NotNull UserReferenceRoleEntity userReferenceRoles) {
        validateUserReferenceRoleEntity(userReferenceRoles);

        userService.updateUserRoles(
            GraviteeContext.getExecutionContext(),
            userId,
            userReferenceRoles.getReferenceType(),
            userReferenceRoles.getReferenceId(),
            userReferenceRoles.getRoles()
        );
        return Response.ok().build();
    }

    private void validateUserReferenceRoleEntity(UserReferenceRoleEntity userReferenceRoles) {
        var authenticatedUserRoles = membershipService.getRoles(
            userReferenceRoles.getReferenceType(),
            userReferenceRoles.getReferenceId(),
            MembershipMemberType.USER,
            getAuthenticatedUser()
        );

        var targetUserCurrentRoles = membershipService.getRoles(
            userReferenceRoles.getReferenceType(),
            userReferenceRoles.getReferenceId(),
            MembershipMemberType.USER,
            userReferenceRoles.getUser()
        );

        var rolesToSave = roleService.findAllById(new HashSet<>(userReferenceRoles.getRoles()));

        var targetUserNewRoles = rolesToSave
            .stream()
            .filter(role -> targetUserCurrentRoles.stream().noneMatch(r -> r.getId().equals(role.getId())))
            .collect(Collectors.toSet());

        if (
            targetUserNewRoles.stream().anyMatch(role -> SystemRole.ADMIN.name().equalsIgnoreCase(role.getName())) &&
            authenticatedUserRoles.stream().noneMatch(role -> SystemRole.ADMIN.name().equalsIgnoreCase(role.getName()))
        ) {
            throw new BadRequestException("User can not be assigned to ADMIN role. Please contact an ADMIN to assign this role.");
        }
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/changePassword")
    @Operation(summary = "Change user password after a reset", description = "User registration must be enabled")
    @ApiResponse(
        responseCode = "200",
        description = "User successfully updated",
        content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = UserEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public Response finalizeResetPassword(@Valid ResetPasswordUserEntity resetPwdEntity) {
        // Check that user belongs to current organization
        userService.findById(GraviteeContext.getExecutionContext(), userId);

        UserEntity newUser = userService.finalizeResetPassword(GraviteeContext.getExecutionContext(), resetPwdEntity);
        if (newUser != null) {
            return Response.ok().entity(newUser).build();
        }

        return Response.serverError().build();
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/_process")
    @Permissions(@Permission(value = RolePermission.ORGANIZATION_USERS, acls = RolePermissionAction.UPDATE))
    @Operation(summary = "Process a user registration by accepting or rejecting it")
    @ApiResponse(
        responseCode = "200",
        description = "Processed user",
        content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = UserEntity.class))
    )
    @ApiResponse(responseCode = "404", description = "User not found")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public Response validateRegistration(boolean accepted) {
        return Response.ok(userService.processRegistration(GraviteeContext.getExecutionContext(), userId, accepted)).build();
    }

    @Path("tokens")
    public UserTokensResource getUserTokensResource() {
        return resourceContext.getResource(UserTokensResource.class);
    }
}

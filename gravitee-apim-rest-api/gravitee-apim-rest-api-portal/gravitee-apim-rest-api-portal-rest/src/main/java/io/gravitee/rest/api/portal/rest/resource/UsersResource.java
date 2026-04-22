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
package io.gravitee.rest.api.portal.rest.resource;

import static io.gravitee.rest.api.model.permissions.RolePermissionAction.READ;
import static java.util.function.Predicate.not;

import io.gravitee.apim.core.user.model.UserSearchQuery;
import io.gravitee.apim.core.user.use_case.SearchUsersUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.InlinePictureEntity;
import io.gravitee.rest.api.model.PictureEntity;
import io.gravitee.rest.api.model.UrlPictureEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.portal.rest.mapper.UserMapper;
import io.gravitee.rest.api.portal.rest.mapper.UsersSearchQueryMapper;
import io.gravitee.rest.api.portal.rest.model.*;
import io.gravitee.rest.api.portal.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.portal.rest.utils.PortalApiLinkHelper;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.AbstractManagementException;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;
import io.gravitee.rest.api.service.exceptions.PasswordAlreadyResetException;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class UsersResource extends AbstractResource {

    @Inject
    private UserMapper userMapper;

    @Inject
    private UserService userService;

    @Inject
    private SearchUsersUseCase searchUsersUseCase;

    private final UsersSearchQueryMapper usersSearchQueryMapper = UsersSearchQueryMapper.INSTANCE;

    /**
     * Register a new user. Generate a token and send it in an email to allow a user
     * to create an account.
     */
    @POST
    @Path("/registration")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response registerUser(@Valid @NotNull(message = "Input must not be null.") RegisterUserInput registerUserInput) {
        UserEntity newUser = userService.register(
            GraviteeContext.getExecutionContext(),
            userMapper.convert(registerUserInput),
            registerUserInput.getConfirmationPageUrl()
        );
        if (newUser != null) {
            return Response.ok().entity(userMapper.convert(newUser)).build();
        }

        return Response.serverError().build();
    }

    @POST
    @Path("/registration/_finalize")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response finalizeRegistration(
        @Valid @NotNull(message = "Input must not be null.") FinalizeRegistrationInput finalizeRegistrationInput
    ) {
        UserEntity newUser = userService.finalizeRegistration(
            GraviteeContext.getExecutionContext(),
            userMapper.convert(finalizeRegistrationInput)
        );
        if (newUser != null) {
            return Response.ok().entity(userMapper.convert(newUser)).build();
        }

        return Response.serverError().build();
    }

    @POST
    @Path("_reset_password")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response resetUserPassword(@NotNull(message = "Input must not be null.") @Valid ResetUserPasswordInput resetUserPasswordInput) {
        try {
            userService.resetPasswordFromSourceId(
                GraviteeContext.getExecutionContext(),
                resetUserPasswordInput.getUsername(),
                resetUserPasswordInput.getResetPageUrl()
            );
        } catch (PasswordAlreadyResetException e) {
            throw e;
        } catch (AbstractManagementException e) {
            // do not fail to prevent from brute force attack
            log.warn("Problem while resetting a password : {}", e.getMessage());
        }
        return Response.noContent().build();
    }

    @POST
    @Path("_change_password")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response changeUserPassword(
        @NotNull(message = "Input must not be null.") @Valid ChangeUserPasswordInput changeUserPasswordInput
    ) {
        UserEntity newUser = userService.finalizeResetPassword(
            GraviteeContext.getExecutionContext(),
            userMapper.convert(changeUserPasswordInput)
        );
        if (newUser != null) {
            return Response.ok().entity(userMapper.convert(newUser)).build();
        }

        return Response.serverError().build();
    }

    /**
     * Keeps reading pagination parameters and letting createListResponse expose them for backward compatibility with the
     * legacy endpoint contract. IdentityManager-backed user search does not apply pagination at the source, so passing
     * pageable input to the use case would incorrectly suggest that backend pagination is supported.
     */
    @POST
    @Path("_search")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ORGANIZATION_USERS, acls = READ) })
    public Response getUsers(@QueryParam("q") String query, @BeanParam PaginationParam paginationParam, UsersSearchInput input) {
        var executionContext = GraviteeContext.getExecutionContext();
        var searchQuery = resolveSearchQuery(query, input);
        var applicationMembership = resolveApplicationMembership(input);
        ensureCanReadApplicationMembership(executionContext, applicationMembership);
        var output = searchUsersUseCase.execute(new SearchUsersUseCase.Input(executionContext, searchQuery, applicationMembership));

        List<User> users = output
            .data()
            .stream()
            .map(searchUser ->
                userMapper
                    .convert(searchUser)
                    .links(userMapper.computeUserLinks(PortalApiLinkHelper.usersURL(uriInfo.getBaseUriBuilder(), searchUser.id()), null))
            )
            .toList();

        var metadata = createUsersSearchMetadata(output);
        return createListResponse(executionContext, users, paginationParam, metadata, false);
    }

    private UserSearchQuery resolveSearchQuery(String query, UsersSearchInput input) {
        if (query != null && input != null) {
            throw new BadRequestException("Query parameter 'q' cannot be used together with request body.");
        }

        if (query != null) {
            return new UserSearchQuery(query);
        }

        if (input != null) {
            return usersSearchQueryMapper.toSearchQuery(input);
        }

        return new UserSearchQuery(null);
    }

    private Optional<String> resolveApplicationMembership(UsersSearchInput input) {
        if (input == null || input.getIncludes() == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(input.getIncludes().getApplicationMembership()).filter(not(String::isBlank));
    }

    @GET
    @Path("/{userId}/avatar")
    public Response getUserAvatar(@Context Request request, @PathParam("userId") String userId) {
        PictureEntity picture = userService.getPicture(GraviteeContext.getExecutionContext(), userId);

        if (picture == null) {
            return Response.ok().build();
        }

        if (picture instanceof UrlPictureEntity) {
            return Response.temporaryRedirect(URI.create(((UrlPictureEntity) picture).getUrl())).build();
        }

        return createPictureResponse(request, (InlinePictureEntity) picture);
    }

    private Map<String, Map<String, Object>> createUsersSearchMetadata(SearchUsersUseCase.Output output) {
        if (output.applicationMembership() == null) {
            return null;
        }

        Map<String, Map<String, Object>> metadata = new HashMap<>();
        metadata.put("applicationMembership", new HashMap<>(output.applicationMembership()));
        return metadata;
    }

    private void ensureCanReadApplicationMembership(
        io.gravitee.rest.api.service.common.ExecutionContext executionContext,
        Optional<String> applicationMembership
    ) {
        applicationMembership.ifPresent(applicationId -> {
            if (!hasPermission(executionContext, RolePermission.APPLICATION_MEMBER, applicationId, READ)) {
                throw new ForbiddenAccessException();
            }
        });
    }
}

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

import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.gravitee.apim.core.invitation.use_case.AcceptUserInvitationUseCase;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.apim.core.user.model.DecodedToken;
import io.gravitee.apim.core.user.model.UserSearchQuery;
import io.gravitee.apim.core.user.use_case.SearchUsersUseCase;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.NewExternalUserEntity;
import io.gravitee.rest.api.model.RegisterUserEntity;
import io.gravitee.rest.api.model.UrlPictureEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.portal.rest.model.*;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.JWTHelper;
import io.gravitee.rest.api.service.exceptions.UserNotFoundException;
import io.gravitee.rest.api.service.exceptions.UserStateConflictException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class UsersResourceTest extends AbstractResourceTest {

    @Autowired
    private SearchUsersUseCase searchUsersUseCase;

    @Override
    protected String contextPath() {
        return "users/";
    }

    private io.gravitee.apim.core.user.model.User searchUser;

    @BeforeEach
    public void init() {
        resetAllMocks();
        reset(searchUsersUseCase);
        reset(acceptUserInvitationUseCase);
        reset(registrationTokenService);

        searchUser = io.gravitee.apim.core.user.model.User.builder()
            .id("my-user-id")
            .reference("my-user-reference")
            .displayName("my-user-display-name")
            .firstName("my-user-firstname")
            .lastName("my-user-lastname")
            .email("my-user-email")
            .build();

        doCallRealMethod().when(userMapper).convert(searchUser);

        when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(true);
    }

    @Test
    public void should_get_users_from_legacy_query_param() {
        var anotherSearchUser = io.gravitee.apim.core.user.model.User.builder()
            .id("another-user-id")
            .displayName("another-user-display-name")
            .lastName("another-user-lastname")
            .build();
        doCallRealMethod().when(userMapper).convert(anotherSearchUser);
        when(searchUsersUseCase.execute(any())).thenReturn(new SearchUsersUseCase.Output(List.of(searchUser, anotherSearchUser), 2, null));

        final Response response = target("_search").queryParam("q", "tests").queryParam("size", 1).request().post(null);
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        UsersResponse usersResponse = response.readEntity(UsersResponse.class);
        assertEquals(2, usersResponse.getData().size());
        assertEquals("my-user-id", usersResponse.getData().get(0).getId());
        assertNull(usersResponse.getMetadata().getAdditionalProperty("paginateMetaData"));

        Links links = usersResponse.getLinks();
        assertNotNull(links);

        var captor = org.mockito.ArgumentCaptor.forClass(SearchUsersUseCase.Input.class);
        verify(searchUsersUseCase).execute(captor.capture());
        assertEquals("tests", captor.getValue().searchQuery().query());
        assertEquals(Optional.empty(), captor.getValue().applicationMembership());
    }

    @Test
    public void should_get_users_with_body_filters_and_application_membership_metadata() {
        when(searchUsersUseCase.execute(any())).thenReturn(
            new SearchUsersUseCase.Output(List.of(searchUser), 42, java.util.Map.of("my-user-id", true))
        );

        var input = new UsersSearchInput()
            .filters(new UsersSearchFilters().query("body-query"))
            .includes(new UsersSearchIncludes().applicationMembership("app-123"));

        final Response response = target("_search").request().post(Entity.json(input));
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        UsersResponse usersResponse = response.readEntity(UsersResponse.class);
        assertEquals(1, usersResponse.getData().size());
        assertEquals(Boolean.TRUE, usersResponse.getMetadata().getApplicationMembership().get("my-user-id"));
        assertNull(usersResponse.getMetadata().getAdditionalProperty("paginateMetaData"));

        var captor = org.mockito.ArgumentCaptor.forClass(SearchUsersUseCase.Input.class);
        verify(searchUsersUseCase).execute(captor.capture());
        assertEquals("body-query", captor.getValue().searchQuery().query());
        assertEquals(Optional.of("app-123"), captor.getValue().applicationMembership());
    }

    @Test
    public void should_return_bad_request_when_legacy_query_param_and_request_body_are_both_provided() {
        var input = new UsersSearchInput().filters(new UsersSearchFilters().query("body-query"));

        final Response response = target("_search").queryParam("q", "legacy-query").request().post(Entity.json(input));
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());

        verify(searchUsersUseCase, never()).execute(any());
    }

    @Test
    public void should_return_forbidden_when_application_membership_is_requested_without_application_member_permission() {
        reset(permissionService);

        doReturn(true)
            .when(permissionService)
            .hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(io.gravitee.rest.api.model.permissions.RolePermission.ORGANIZATION_USERS),
                eq(GraviteeContext.getExecutionContext().getOrganizationId()),
                eq(io.gravitee.rest.api.model.permissions.RolePermissionAction.READ)
            );
        doReturn(false)
            .when(permissionService)
            .hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(io.gravitee.rest.api.model.permissions.RolePermission.APPLICATION_MEMBER),
                eq("app-123"),
                eq(io.gravitee.rest.api.model.permissions.RolePermissionAction.READ)
            );

        var input = new UsersSearchInput().includes(new UsersSearchIncludes().applicationMembership("app-123"));

        final Response response = target("_search").request().post(Entity.json(input));
        assertEquals(HttpStatusCode.FORBIDDEN_403, response.getStatus());
        verify(searchUsersUseCase, never()).execute(any());
    }

    @Test
    public void should_use_default_criteria_when_no_query_and_no_body_are_provided() {
        when(searchUsersUseCase.execute(any())).thenReturn(new SearchUsersUseCase.Output(List.of(), 0, null));

        final Response response = target("_search").request().post(null);
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        var captor = org.mockito.ArgumentCaptor.forClass(SearchUsersUseCase.Input.class);
        verify(searchUsersUseCase).execute(captor.capture());
        assertEquals(UserSearchQuery.DEFAULT_QUERY, captor.getValue().searchQuery().query());
    }

    @Test
    public void should_get_no_user_and_no_link() {
        when(searchUsersUseCase.execute(any())).thenReturn(new SearchUsersUseCase.Output(List.of(), 0, null));

        final Response response = target("_search").queryParam("q", "q").request().post(null);
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        UsersResponse usersResponse = response.readEntity(UsersResponse.class);
        assertEquals(0, usersResponse.getData().size());
        assertNull(usersResponse.getMetadata().getAdditionalProperty("paginateMetaData"));
        assertNull(usersResponse.getLinks());

        final Response anotherResponse = target("_search")
            .queryParam("q", "q")
            .queryParam("page", 2)
            .queryParam("size", 1)
            .request()
            .post(null);
        assertEquals(HttpStatusCode.OK_200, anotherResponse.getStatus());

        usersResponse = anotherResponse.readEntity(UsersResponse.class);
        assertEquals(0, usersResponse.getData().size());
        assertNull(usersResponse.getLinks());
    }

    @Test
    public void shouldCreateRegistration() {
        // init
        RegisterUserInput input = new RegisterUserInput()
            .email("test@example.com")
            .firstname("Firstname")
            .lastname("LASTNAME")
            .confirmationPageUrl("HTTP://MY-CONFIRM-PAGE");

        NewExternalUserEntity newExternalUserEntity = new NewExternalUserEntity();
        doReturn(newExternalUserEntity).when(userMapper).convert(input);

        doReturn(new UserEntity())
            .when(userService)
            .register(GraviteeContext.getExecutionContext(), newExternalUserEntity, "HTTP://MY-CONFIRM-PAGE");

        // test
        final Response response = target("registration").request().post(Entity.json(input));
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        Mockito.verify(userMapper).convert(input);
        Mockito.verify(userService).register(GraviteeContext.getExecutionContext(), newExternalUserEntity, "HTTP://MY-CONFIRM-PAGE");
    }

    @Test
    public void shouldNotCreateRegistration() {
        // init
        RegisterUserInput input = new RegisterUserInput().email("test@example.com").firstname("Firstname").lastname("LASTNAME");

        NewExternalUserEntity newExternalUserEntity = new NewExternalUserEntity();
        doReturn(newExternalUserEntity).when(userMapper).convert(input);

        doReturn(null).when(userService).register(GraviteeContext.getExecutionContext(), newExternalUserEntity, null);

        // test
        final Response response = target("registration").request().post(Entity.json(input));
        assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR_500, response.getStatus());

        Mockito.verify(userMapper).convert(input);
        Mockito.verify(userService).register(GraviteeContext.getExecutionContext(), newExternalUserEntity, null);
    }

    @Test
    public void shouldHaveBadRequestWhileRegisteringAUserWithNull() {
        final Response response = target("registration").request().post(Entity.json(null));
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
    }

    @Test
    public void shouldHaveBadRequestWhileRegisteringAUserWithEmpty() {
        final Response response = target("registration").request().post(Entity.json(new RegisterUserInput()));
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
    }

    @Test
    public void shouldHaveBadRequestWhileFinalizingRegistrationWithNull() {
        final Response response = target("registration/_finalize").request().post(Entity.json(null));
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
    }

    @Test
    public void shouldHaveBadRequestWhileFinalizingRegistrationWithEmpty() {
        final Response response = target("registration/_finalize").request().post(Entity.json(new FinalizeRegistrationInput()));
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
    }

    @Test
    public void should_finalize_registration_for_user_registration_action() {
        var input = new FinalizeRegistrationInput().token("my-jwt").password("P4s5vv0Rd").firstname("John").lastname("Doe");
        var decoded = new DecodedToken(JWTHelper.ACTION.USER_REGISTRATION.name(), "user@example.com", Optional.empty());
        var user = BaseUserEntity.builder().id("user-id").email("user@example.com").build();

        doReturn(decoded).when(registrationTokenService).decode("my-jwt");
        doReturn(new AcceptUserInvitationUseCase.Output(user)).when(acceptUserInvitationUseCase).execute(any());
        doReturn(new io.gravitee.rest.api.portal.rest.model.User()).when(userMapper).convert(any(UserEntity.class));

        final Response response = target("registration/_finalize").request().post(Entity.json(input));

        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        verify(registrationTokenService).decode("my-jwt");
        verify(acceptUserInvitationUseCase).execute(any());
    }

    @Test
    public void should_finalize_registration_for_group_invitation_action() {
        var input = new FinalizeRegistrationInput().token("my-jwt").password("P4s5vv0Rd").firstname("John").lastname("Doe");
        var decoded = new DecodedToken(JWTHelper.ACTION.GROUP_INVITATION.name(), "user@example.com", Optional.of("user-id"));
        var user = BaseUserEntity.builder().id("user-id").email("user@example.com").build();

        doReturn(decoded).when(registrationTokenService).decode("my-jwt");
        doReturn(new AcceptUserInvitationUseCase.Output(user)).when(acceptUserInvitationUseCase).execute(any());
        doReturn(new io.gravitee.rest.api.portal.rest.model.User()).when(userMapper).convert(any(UserEntity.class));

        final Response response = target("registration/_finalize").request().post(Entity.json(input));

        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        verify(acceptUserInvitationUseCase).execute(any());
    }

    @Test
    public void should_return_conflict_when_reset_password_action() {
        var input = new FinalizeRegistrationInput().token("my-jwt").password("P4s5vv0Rd").firstname("John").lastname("Doe");
        var decoded = new DecodedToken(JWTHelper.ACTION.RESET_PASSWORD.name(), "user@example.com", Optional.empty());

        doReturn(decoded).when(registrationTokenService).decode("my-jwt");

        final Response response = target("registration/_finalize").request().post(Entity.json(input));

        assertEquals(HttpStatusCode.CONFLICT_409, response.getStatus());
        verify(acceptUserInvitationUseCase, never()).execute(any());
    }

    @Test
    public void should_propagate_token_decode_error() {
        var input = new FinalizeRegistrationInput().token("invalid-jwt").password("P4s5vv0Rd").firstname("John").lastname("Doe");
        doThrow(new com.auth0.jwt.exceptions.JWTVerificationException("invalid token"))
            .when(registrationTokenService)
            .decode("invalid-jwt");

        final Response response = target("registration/_finalize").request().post(Entity.json(input));

        assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR_500, response.getStatus());
        verify(acceptUserInvitationUseCase, never()).execute(any());
    }

    @Test
    public void shouldResetPassword() {
        ResetUserPasswordInput input = new ResetUserPasswordInput().username("my@email.com").resetPageUrl("HTTP://MY-RESET-PAGE");
        final Response response = target().path("_reset_password").request().post(Entity.json(input));
        assertEquals(HttpStatusCode.NO_CONTENT_204, response.getStatus());
    }

    @Test
    public void shouldHaveNoContentResponseWithUnexistingUser() {
        doThrow(new UserNotFoundException("my@email.com"))
            .when(userService)
            .resetPasswordFromSourceId(GraviteeContext.getExecutionContext(), "my@email.com", "HTTP://MY-RESET-PAGE");
        ResetUserPasswordInput input = new ResetUserPasswordInput().username("my@email.com").resetPageUrl("HTTP://MY-RESET-PAGE");
        final Response response = target().path("_reset_password").request().post(Entity.json(input));
        assertEquals(HttpStatusCode.NO_CONTENT_204, response.getStatus());
    }

    @Test
    public void shouldHaveBadRequestWhileResettingPasswordWithoutInput() {
        final Response response = target().path("_reset_password").request().post(Entity.json(null));
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
    }

    @Test
    public void shouldGetUserAvatar() throws IOException {
        final Response response = target().path("userId").path("avatar").request().get();
        assertEquals(OK_200, response.getStatus());
    }

    @Test
    public void shouldGetUserAvatarRedirectUrl() throws IOException {
        doReturn(new UrlPictureEntity(root().path("openapi").getUri().toURL().toString()))
            .when(userService)
            .getPicture(eq(GraviteeContext.getExecutionContext()), any());
        final Response response = target().path("userId").path("avatar").request().get();
        assertEquals(OK_200, response.getStatus());
    }
}

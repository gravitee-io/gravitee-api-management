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
package io.gravitee.rest.api.portal.rest.resource;

import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.idp.api.identity.SearchableUser;
import io.gravitee.rest.api.model.NewExternalUserEntity;
import io.gravitee.rest.api.model.RegisterUserEntity;
import io.gravitee.rest.api.model.UrlPictureEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.portal.rest.model.*;
import io.gravitee.rest.api.service.exceptions.UserNotFoundException;
import java.io.IOException;
import java.util.Collections;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class UsersResourceTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "users/";
    }

    private SearchableUser searchableUser;

    @Before
    public void init() {
        resetAllMocks();

        searchableUser = Mockito.mock(SearchableUser.class);
        doReturn("my-user-display-name").when(searchableUser).getDisplayName();
        doReturn("my-user-email").when(searchableUser).getEmail();
        doReturn("my-user-firstname").when(searchableUser).getFirstname();
        doReturn("my-user-id").when(searchableUser).getId();
        doReturn("my-user-lastname").when(searchableUser).getLastname();
        doReturn("my-user-picture").when(searchableUser).getPicture();
        doReturn("my-user-reference").when(searchableUser).getReference();
        doReturn(Collections.singletonList(searchableUser)).when(identityService).search(anyString());

        doCallRealMethod().when(userMapper).convert(searchableUser);
    }

    @Test
    public void shouldGetUsers() {
        final Response response = target("_search").queryParam("q", "tests").request().post(null);
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        UsersResponse usersResponse = response.readEntity(UsersResponse.class);
        assertEquals(1, usersResponse.getData().size());

        Links links = usersResponse.getLinks();
        assertNotNull(links);
        Mockito.verify(identityService).search("tests");
    }

    @Test
    public void shouldGetNoUserAndNoLink() {
        doReturn(Collections.emptyList()).when(identityService).search(anyString());

        // Test with default limit
        final Response response = target("_search").queryParam("q", "q").request().post(null);
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        UsersResponse usersResponse = response.readEntity(UsersResponse.class);
        assertEquals(0, usersResponse.getData().size());

        Links links = usersResponse.getLinks();
        assertNull(links);

        // Test with small limit
        final Response anotherResponse = target("_search")
            .queryParam("q", "q")
            .queryParam("page", 2)
            .queryParam("size", 1)
            .request()
            .post(null);
        assertEquals(HttpStatusCode.OK_200, anotherResponse.getStatus());

        usersResponse = anotherResponse.readEntity(UsersResponse.class);
        assertEquals(0, usersResponse.getData().size());

        links = usersResponse.getLinks();
        assertNull(links);
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

        doReturn(new UserEntity()).when(userService).register(newExternalUserEntity, "HTTP://MY-CONFIRM-PAGE");

        // test
        final Response response = target("registration").request().post(Entity.json(input));
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        Mockito.verify(userMapper).convert(input);
        Mockito.verify(userService).register(newExternalUserEntity, "HTTP://MY-CONFIRM-PAGE");
    }

    @Test
    public void shouldNotCreateRegistration() {
        // init
        RegisterUserInput input = new RegisterUserInput().email("test@example.com").firstname("Firstname").lastname("LASTNAME");

        NewExternalUserEntity newExternalUserEntity = new NewExternalUserEntity();
        doReturn(newExternalUserEntity).when(userMapper).convert(input);

        doReturn(null).when(userService).register(newExternalUserEntity, null);

        // test
        final Response response = target("registration").request().post(Entity.json(input));
        assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR_500, response.getStatus());

        Mockito.verify(userMapper).convert(input);
        Mockito.verify(userService).register(newExternalUserEntity, null);
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
    public void shouldFinalizeRegistration() {
        // init
        FinalizeRegistrationInput input = new FinalizeRegistrationInput()
            .token("token")
            .password("P4s5vv0Rd")
            .firstname("Firstname")
            .lastname("LASTNAME");

        RegisterUserEntity registerUserEntity = new RegisterUserEntity();
        doReturn(registerUserEntity).when(userMapper).convert(input);

        doReturn(new UserEntity()).when(userService).finalizeRegistration(registerUserEntity);

        // test
        final Response response = target("registration/_finalize").request().post(Entity.json(input));
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        Mockito.verify(userMapper).convert(input);
        Mockito.verify(userService).finalizeRegistration(registerUserEntity);
    }

    @Test
    public void shouldNotFinalizeRegistration() {
        // init
        FinalizeRegistrationInput input = new FinalizeRegistrationInput()
            .token("token")
            .password("P4s5vv0Rd")
            .firstname("Firstname")
            .lastname("LASTNAME");

        RegisterUserEntity registerUserEntity = new RegisterUserEntity();
        doReturn(registerUserEntity).when(userMapper).convert(input);

        doReturn(null).when(userService).finalizeRegistration(registerUserEntity);

        // test
        final Response response = target("registration/_finalize").request().post(Entity.json(input));
        assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR_500, response.getStatus());

        Mockito.verify(userMapper).convert(input);
        Mockito.verify(userService).finalizeRegistration(registerUserEntity);
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
            .resetPasswordFromSourceId("my@email.com", "HTTP://MY-RESET-PAGE");
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
        doReturn(new UrlPictureEntity(root().path("openapi").getUri().toURL().toString())).when(userService).getPicture(any());
        final Response response = target().path("userId").path("avatar").request().get();
        assertEquals(OK_200, response.getStatus());
    }
}

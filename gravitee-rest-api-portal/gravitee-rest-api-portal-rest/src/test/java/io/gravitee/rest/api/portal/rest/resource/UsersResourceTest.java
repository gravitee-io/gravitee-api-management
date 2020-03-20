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

import io.gravitee.common.data.domain.Page;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.repository.management.api.search.UserCriteria;
import io.gravitee.rest.api.model.NewExternalUserEntity;
import io.gravitee.rest.api.model.RegisterUserEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.portal.rest.model.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class UsersResourceTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "users/";
    }

    private static final String USER = "my-user";
    private static final String ANOTHER_USER = "my-another-user";

    private Page<UserEntity> userEntityPage;

    @Before
    public void init() {
        resetAllMocks();

        UserEntity userEntity1 = new UserEntity();
        userEntity1.setId(USER);
        UserEntity userEntity2 = new UserEntity();
        userEntity2.setId(ANOTHER_USER);
        userEntityPage = new Page<UserEntity>(Arrays.asList(userEntity1, userEntity2), 1, 2, 2);
        doReturn(userEntityPage).when(userService).search(any(UserCriteria.class), any());

        doReturn(new User().id(USER)).when(userMapper).convert(userEntity1);
        doReturn(new User().id(ANOTHER_USER)).when(userMapper).convert(userEntity2);

    }

    @Test
    public void shouldGetUsers() {
        final Response response = target().request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        UsersResponse usersResponse = response.readEntity(UsersResponse.class);
        assertEquals(2, usersResponse.getData().size());

        Links links = usersResponse.getLinks();
        assertNotNull(links);
    }

    @Test
    public void shouldGetNoUserAndNoLink() {

        doReturn(new Page<UserEntity>(Collections.emptyList(), 1, 0, 0)).when(userService)
                .search(any(UserCriteria.class), any());

        // Test with default limit
        final Response response = target().request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        UsersResponse usersResponse = response.readEntity(UsersResponse.class);
        assertEquals(0, usersResponse.getData().size());

        Links links = usersResponse.getLinks();
        assertNull(links);

        // Test with small limit
        final Response anotherResponse = target().queryParam("page", 2).queryParam("size", 1).request().get();
        assertEquals(HttpStatusCode.OK_200, anotherResponse.getStatus());

        usersResponse = anotherResponse.readEntity(UsersResponse.class);
        assertEquals(0, usersResponse.getData().size());

        links = usersResponse.getLinks();
        assertNull(links);

    }

    @Test
    public void shouldCreateRegistration() {
        // init
        RegisterUserInput input = new RegisterUserInput().email("test@example.com").firstname("Firstname")
                .lastname("LASTNAME").confirmationPageUrl("HTTP://MY-CONFIRM-PAGE");

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
        RegisterUserInput input = new RegisterUserInput().email("test@example.com").firstname("Firstname")
                .lastname("LASTNAME");

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
        FinalizeRegistrationInput input = new FinalizeRegistrationInput().token("token").password("P4s5vv0Rd")
                .firstname("Firstname").lastname("LASTNAME");

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
        FinalizeRegistrationInput input = new FinalizeRegistrationInput().token("token").password("P4s5vv0Rd")
                .firstname("Firstname").lastname("LASTNAME");

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
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
    }

    @Test
    public void shouldHaveBadRequestWhileResettingPasswordWithoutInput() {
        final Response response = target().path("_reset_password").request().post(Entity.json(null));
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
    }
}

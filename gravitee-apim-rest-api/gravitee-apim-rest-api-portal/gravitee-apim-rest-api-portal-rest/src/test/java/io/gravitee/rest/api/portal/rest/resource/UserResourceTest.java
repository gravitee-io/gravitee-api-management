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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.InlinePictureEntity;
import io.gravitee.rest.api.model.UpdateUserEntity;
import io.gravitee.rest.api.model.UrlPictureEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.settings.ConsoleSettingsEntity;
import io.gravitee.rest.api.model.settings.Management;
import io.gravitee.rest.api.portal.rest.model.User;
import io.gravitee.rest.api.portal.rest.model.UserConfig;
import io.gravitee.rest.api.portal.rest.model.UserInput;
import io.gravitee.rest.api.portal.rest.model.UserLinks;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class UserResourceTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "user";
    }

    @Before
    public void init() throws IOException, URISyntaxException {
        resetAllMocks();

        doReturn(new User()).when(userMapper).convert(nullable(UserEntity.class));
        doReturn(new UserLinks()).when(userMapper).computeUserLinks(any(), any());

        InlinePictureEntity mockImage = new InlinePictureEntity();
        byte[] apiLogoContent = Files.readAllBytes(Paths.get(this.getClass().getClassLoader().getResource("media/logo.svg").toURI()));
        mockImage.setContent(apiLogoContent);
        mockImage.setType("image/svg");
        doReturn(mockImage).when(userService).getPicture(eq(GraviteeContext.getExecutionContext()), any());
    }

    @Test
    public void shouldGetCurrentUserWithoutConfig() {
        when(userService.findByIdWithRoles(GraviteeContext.getExecutionContext(), USER_NAME)).thenReturn(new UserEntity());
        when(permissionService.hasManagementRights(GraviteeContext.getExecutionContext(), USER_NAME)).thenReturn(Boolean.FALSE);

        final Response response = target().request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        ArgumentCaptor<String> userId = ArgumentCaptor.forClass(String.class);
        Mockito.verify(userService).findByIdWithRoles(eq(GraviteeContext.getExecutionContext()), userId.capture());

        assertEquals(USER_NAME, userId.getValue());

        User user = response.readEntity(User.class);
        assertNotNull(user);
        assertNull(user.getConfig());
        assertNotNull(user.getLinks());
    }

    @Test
    public void shouldGetCurrentUserWithEmptyManagementConfig() {
        when(userService.findByIdWithRoles(GraviteeContext.getExecutionContext(), USER_NAME)).thenReturn(new UserEntity());
        when(permissionService.hasManagementRights(GraviteeContext.getExecutionContext(), USER_NAME)).thenReturn(Boolean.TRUE);
        when(configService.getConsoleSettings(GraviteeContext.getExecutionContext())).thenReturn(new ConsoleSettingsEntity());

        final Response response = target().request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        ArgumentCaptor<String> userId = ArgumentCaptor.forClass(String.class);
        Mockito.verify(userService).findByIdWithRoles(eq(GraviteeContext.getExecutionContext()), userId.capture());

        assertEquals(USER_NAME, userId.getValue());

        User user = response.readEntity(User.class);
        assertNotNull(user);
        assertNull(user.getConfig());
        assertNotNull(user.getLinks());
    }

    @Test
    public void shouldGetCurrentUserWithManagementConfigWithoutUrl() {
        when(userService.findByIdWithRoles(GraviteeContext.getExecutionContext(), USER_NAME)).thenReturn(new UserEntity());
        when(permissionService.hasManagementRights(GraviteeContext.getExecutionContext(), USER_NAME)).thenReturn(Boolean.TRUE);
        ConsoleSettingsEntity consoleConfigEntity = new ConsoleSettingsEntity();
        when(configService.getConsoleSettings(GraviteeContext.getExecutionContext())).thenReturn(consoleConfigEntity);

        final Response response = target().request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        ArgumentCaptor<String> userId = ArgumentCaptor.forClass(String.class);
        Mockito.verify(userService).findByIdWithRoles(eq(GraviteeContext.getExecutionContext()), userId.capture());

        assertEquals(USER_NAME, userId.getValue());

        User user = response.readEntity(User.class);
        assertNotNull(user);
        assertNull(user.getConfig());
        assertNotNull(user.getLinks());
    }

    @Test
    public void shouldGetCurrentUserWithManagementConfigWithUrl() {
        when(userService.findByIdWithRoles(GraviteeContext.getExecutionContext(), USER_NAME)).thenReturn(new UserEntity());
        when(permissionService.hasManagementRights(GraviteeContext.getExecutionContext(), USER_NAME)).thenReturn(Boolean.TRUE);
        ConsoleSettingsEntity consoleConfigEntity = new ConsoleSettingsEntity();
        Management managementConfig = new Management();
        managementConfig.setUrl("URL");
        consoleConfigEntity.setManagement(managementConfig);
        when(configService.getConsoleSettings(GraviteeContext.getExecutionContext())).thenReturn(consoleConfigEntity);

        final Response response = target().request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        ArgumentCaptor<String> userId = ArgumentCaptor.forClass(String.class);
        Mockito.verify(userService).findByIdWithRoles(eq(GraviteeContext.getExecutionContext()), userId.capture());

        assertEquals(USER_NAME, userId.getValue());

        User user = response.readEntity(User.class);
        assertNotNull(user);
        UserConfig config = user.getConfig();
        assertNotNull(config);
        assertEquals("URL", config.getManagementUrl());
        assertNotNull(user.getLinks());
    }

    @Test
    public void shouldHaveUnauthorizedAccessWhileUpdatingWithWrongId() {
        UserInput user = new UserInput();
        user.setId("anotherId");
        user.setAvatar("");

        final Response response = target().request().put(Entity.json(user));
        assertEquals(HttpStatusCode.UNAUTHORIZED_401, response.getStatus());
    }

    @Test
    public void shouldUpdateCurrentUser() {
        UserInput userInput = new UserInput();
        final String newAvatar = "data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7";

        final String expectedAvatar =
            "data:image/gif;base64,R0lGODlhyADIAPAAAAAAAP///ywAAAAAyADIAEAC/4SPqcvtD6OctNqL" +
            "s968+w+G4kiW5omm6sq27gvH8kzX9o3n+s73/g8MCofEovGITCqXzKbzCY1Kp9Sq9YrNarfcrvcLDovH5LL5jE6r1+y2+w2Py+" +
            "f0uv2Oz+v3/L7/DxgoOEhYaHiImKi4yNjo+AgZKTlJWWl5iZmpucnZ6fkJGio6SlpqeoqaqrrK2ur6ChsrO0tba3uLm6u7y9vr" +
            "+wscLDxMXGx8jJysvMzc7PwMHS09TV1tfY2drb3N3e39DR4uPk5ebn6Onq6+zt7u/g4fLz9PX29/j5+vv8/f7/8PMKDAgQQLGj" +
            "yIMKHChQwbOnwIMaLEiRQrWryIMaPGjQYcO3osUwAAOw==";
        final String userEmail = "example@gio.com";

        userInput.setAvatar(newAvatar);
        userInput.setId(USER_NAME);
        when(userService.update(eq(GraviteeContext.getExecutionContext()), eq(USER_NAME), any())).thenReturn(new UserEntity());

        UserEntity existingUser = new UserEntity();
        existingUser.setEmail(userEmail);
        when(userService.findById(eq(GraviteeContext.getExecutionContext()), any())).thenReturn(existingUser);

        final Response response = target().request().put(Entity.json(userInput));
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        ArgumentCaptor<UpdateUserEntity> user = ArgumentCaptor.forClass(UpdateUserEntity.class);
        Mockito.verify(userService).update(eq(GraviteeContext.getExecutionContext()), eq(USER_NAME), user.capture());

        final UpdateUserEntity updateUserEntity = user.getValue();
        assertNotNull(updateUserEntity);
        assertEquals(expectedAvatar, updateUserEntity.getPicture());
        assertNull(updateUserEntity.getStatus());
        assertEquals(userEmail, updateUserEntity.getEmail());

        User updateUser = response.readEntity(User.class);
        assertNotNull(updateUser);
    }

    @Test
    public void shouldGetUserAvatar() throws IOException {
        doReturn(new UserEntity()).when(userService).findById(eq(GraviteeContext.getExecutionContext()), any());
        final Response response = target().path("avatar").request().get();
        assertEquals(OK_200, response.getStatus());
    }

    @Test
    public void shouldGetUserAvatarRedirectUrl() throws IOException {
        doReturn(new UserEntity()).when(userService).findById(eq(GraviteeContext.getExecutionContext()), any());
        doReturn(new UserEntity()).when(userService).findByIdWithRoles(eq(GraviteeContext.getExecutionContext()), any());
        doReturn(new UrlPictureEntity(target().getUri().toURL().toString()))
            .when(userService)
            .getPicture(eq(GraviteeContext.getExecutionContext()), any());
        final Response response = target().path("avatar").request().get();
        assertEquals(OK_200, response.getStatus());
    }

    @Test
    public void shouldGetNoContent() throws IOException {
        doReturn(new UserEntity()).when(userService).findById(eq(GraviteeContext.getExecutionContext()), any());
        doReturn(null).when(userService).getPicture(eq(GraviteeContext.getExecutionContext()), any());
        final Response response = target().path("avatar").request().get();
        assertEquals(OK_200, response.getStatus());
    }
}

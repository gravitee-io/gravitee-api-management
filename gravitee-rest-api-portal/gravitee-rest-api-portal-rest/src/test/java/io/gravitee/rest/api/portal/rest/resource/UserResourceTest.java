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

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.PortalConfigEntity.Management;
import io.gravitee.rest.api.portal.rest.model.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import javax.validation.Valid;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static io.gravitee.common.http.HttpStatusCode.NOT_FOUND_404;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

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
        doReturn(mockImage).when(userService).getPicture(any());
    }

    @Test
    public void shouldGetCurrentUserWithoutConfig() {
        when(userService.findById(USER_NAME)).thenReturn(new UserEntity());
        when(permissionService.hasManagementRights(USER_NAME)).thenReturn(Boolean.FALSE);
        
        final Response response = target().request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        ArgumentCaptor<String> userId = ArgumentCaptor.forClass(String.class);
        Mockito.verify(userService).findById(userId.capture());

        assertEquals(USER_NAME, userId.getValue());

        User user = response.readEntity(User.class);
        assertNotNull(user);
        assertNull(user.getConfig());
        assertNotNull(user.getLinks());
    }

    @Test
    public void shouldGetCurrentUserWithEmptyManagementConfig() {
        when(userService.findById(USER_NAME)).thenReturn(new UserEntity());
        when(permissionService.hasManagementRights(USER_NAME)).thenReturn(Boolean.TRUE);
        when(configService.getPortalConfig()).thenReturn(new PortalConfigEntity());
        
        final Response response = target().request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        ArgumentCaptor<String> userId = ArgumentCaptor.forClass(String.class);
        Mockito.verify(userService).findById(userId.capture());

        assertEquals(USER_NAME, userId.getValue());

        User user = response.readEntity(User.class);
        assertNotNull(user);
        assertNull(user.getConfig());
        assertNotNull(user.getLinks());
    }

    @Test
    public void shouldGetCurrentUserWithManagementConfigWithoutUrl() {
        when(userService.findById(USER_NAME)).thenReturn(new UserEntity());
        when(permissionService.hasManagementRights(USER_NAME)).thenReturn(Boolean.TRUE);
        PortalConfigEntity portalConfigEntity = new PortalConfigEntity();
        portalConfigEntity.setManagement(portalConfigEntity.new Management());
        when(configService.getPortalConfig()).thenReturn(portalConfigEntity);
        
        final Response response = target().request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        ArgumentCaptor<String> userId = ArgumentCaptor.forClass(String.class);
        Mockito.verify(userService).findById(userId.capture());

        assertEquals(USER_NAME, userId.getValue());

        User user = response.readEntity(User.class);
        assertNotNull(user);
        assertNull(user.getConfig());
        assertNotNull(user.getLinks());
    }

    @Test
    public void shouldGetCurrentUserWithManagementConfigWithUrl() {
        when(userService.findById(USER_NAME)).thenReturn(new UserEntity());
        when(permissionService.hasManagementRights(USER_NAME)).thenReturn(Boolean.TRUE);
        PortalConfigEntity portalConfigEntity = new PortalConfigEntity();
        Management managementConfig = portalConfigEntity.new Management();
        managementConfig.setUrl("URL");
        portalConfigEntity.setManagement(managementConfig);
        when(configService.getPortalConfig()).thenReturn(portalConfigEntity);
        
        final Response response = target().request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        ArgumentCaptor<String> userId = ArgumentCaptor.forClass(String.class);
        Mockito.verify(userService).findById(userId.capture());

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

        final String expectedAvatar = "data:image/gif;base64,R0lGODlhyADIAPAAAAAAAP///ywAAAAAyADIAEAC/4SPqcvtD6OctNqL"
                + "s968+w+G4kiW5omm6sq27gvH8kzX9o3n+s73/g8MCofEovGITCqXzKbzCY1Kp9Sq9YrNarfcrvcLDovH5LL5jE6r1+y2+w2Py+"
                + "f0uv2Oz+v3/L7/DxgoOEhYaHiImKi4yNjo+AgZKTlJWWl5iZmpucnZ6fkJGio6SlpqeoqaqrrK2ur6ChsrO0tba3uLm6u7y9vr"
                + "+wscLDxMXGx8jJysvMzc7PwMHS09TV1tfY2drb3N3e39DR4uPk5ebn6Onq6+zt7u/g4fLz9PX29/j5+vv8/f7/8PMKDAgQQLGj"
                + "yIMKHChQwbOnwIMaLEiRQrWryIMaPGjQYcO3osUwAAOw==";

        userInput.setAvatar(newAvatar);
        userInput.setId(USER_NAME);
        when(userService.update(eq(USER_NAME), any())).thenReturn(new UserEntity());

        final Response response = target().request().put(Entity.json(userInput));
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        ArgumentCaptor<UpdateUserEntity> user = ArgumentCaptor.forClass(UpdateUserEntity.class);
        Mockito.verify(userService).update(eq(USER_NAME), user.capture());

        final UpdateUserEntity updateUserEntity = user.getValue();
        assertNotNull(updateUserEntity);
        assertEquals(expectedAvatar, updateUserEntity.getPicture());
        assertNull(updateUserEntity.getStatus());

        User updateUser = response.readEntity(User.class);
        assertNotNull(updateUser);
    }

    @Test
    public void shouldGetUserAvatar() throws IOException {
        doReturn(new UserEntity()).when(userService).findById(any());
        final Response response = target().path("avatar").request().get();
        assertEquals(OK_200, response.getStatus());
    }

    @Test
    public void shouldGetUserAvatarRedirectUrl() throws IOException {
        doReturn(new UserEntity()).when(userService).findById(any());
        doReturn(new UrlPictureEntity(target().getUri().toURL().toString())).when(userService).getPicture(any());
        final Response response = target().path("avatar").request().get();
        assertEquals(OK_200, response.getStatus());
    }

    @Test
    public void shouldGetNoContent() throws IOException {
        doReturn(new UserEntity()).when(userService).findById(any());
        doReturn(null).when(userService).getPicture(any());
        final Response response = target().path("avatar").request().get();
        assertEquals(OK_200, response.getStatus());
    }
}

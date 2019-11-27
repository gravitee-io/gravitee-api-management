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
import io.gravitee.rest.api.model.InlinePictureEntity;
import io.gravitee.rest.api.model.UpdateUserEntity;
import io.gravitee.rest.api.model.UrlPictureEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.portal.rest.model.User;
import io.gravitee.rest.api.portal.rest.model.UserLinks;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

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
    public void shouldGetCurrentUser() {
        final Response response = target().request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        
        ArgumentCaptor<String> userId = ArgumentCaptor.forClass(String.class);
        Mockito.verify(userService).findById(userId.capture());
        
        assertEquals(USER_NAME, userId.getValue());
        
        User user = response.readEntity(User.class);
        assertNotNull(user);
        
        assertNotNull(user.getLinks());
    }
    
    @Test
    public void shouldHaveUnauthorizedAccessWhileUpdatingWithWrongId() {
        User newUser = new User();
        newUser.setId("anotherId");
        
        final Response response = target().request().put(Entity.json(newUser));
        assertEquals(HttpStatusCode.UNAUTHORIZED_401, response.getStatus());
    }
    
    @Test
    public void shouldUpdateCurrentUser() {
        User newUser = new User();
        newUser.setEmail("new email");
        newUser.setFirstName("new firstname");
        newUser.setLastName("new lastname");
        final String newAvatar = "data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7";
        
        final String expectedAvatar = "data:image/gif;base64,R0lGODlhyADIAPAAAAAAAP///ywAAAAAyADIAEAC/4SPqcvtD6OctNqL"
                + "s968+w+G4kiW5omm6sq27gvH8kzX9o3n+s73/g8MCofEovGITCqXzKbzCY1Kp9Sq9YrNarfcrvcLDovH5LL5jE6r1+y2+w2Py+"
                + "f0uv2Oz+v3/L7/DxgoOEhYaHiImKi4yNjo+AgZKTlJWWl5iZmpucnZ6fkJGio6SlpqeoqaqrrK2ur6ChsrO0tba3uLm6u7y9vr"
                + "+wscLDxMXGx8jJysvMzc7PwMHS09TV1tfY2drb3N3e39DR4uPk5ebn6Onq6+zt7u/g4fLz9PX29/j5+vv8/f7/8PMKDAgQQLGj"
                + "yIMKHChQwbOnwIMaLEiRQrWryIMaPGjQYcO3osUwAAOw==";
        
        newUser.setAvatar(newAvatar);
        newUser.setId(USER_NAME);
        
        final Response response = target().request().put(Entity.json(newUser));
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        
        ArgumentCaptor<UpdateUserEntity> updateUser = ArgumentCaptor.forClass(UpdateUserEntity.class);
        Mockito.verify(userService).update(eq(USER_NAME), updateUser.capture());
        
        final UpdateUserEntity updateUserEntity = updateUser.getValue();
        assertNotNull(updateUserEntity);
        assertEquals("new email", updateUserEntity.getEmail());
        assertEquals("new firstname", updateUserEntity.getFirstname());
        assertEquals("new lastname", updateUserEntity.getLastname());
        assertEquals(expectedAvatar, updateUserEntity.getPicture());
        assertNull(updateUserEntity.getStatus());
        
        User user = response.readEntity(User.class);
        assertNotNull(user);
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
    public void shouldGetNotFound() throws IOException {
        doReturn(new UserEntity()).when(userService).findById(any());
        doReturn(null).when(userService).getPicture(any());
        final Response response = target().path("avatar").request().get();
        assertEquals(NOT_FOUND_404, response.getStatus());
    }
}

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.UpdateUserEntity;
import io.gravitee.rest.api.portal.rest.model.User;

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
    public void init() {
        resetAllMocks();
        
        doReturn(new User()).when(userMapper).convert(any());

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
        final String newAvatar = "data:image/jpeg;base64,"
        + "/9j/4AAQSkZJRgABAQAAAQABAAD/2wBDAAICAgICAQICAgIDAgIDAwYEAwMDAwcFBQQGCAcJCAgHCAgJCg0LCQoMCggICw8LDA0O"
        + "Dg8OCQsQERAOEQ0ODg7/2wBDAQIDAwMDAwcEBAcOCQgJDg4ODg4ODg4ODg4ODg4ODg4ODg4ODg4ODg4ODg4ODg4ODg4ODg4ODg4O"
        + "Dg4ODg4ODg7/wAARCACAAIADASIAAhEBAxEB/8QAHgABAAICAgMBAAAAAAAAAAAAAAYHBQgJCgIDBAH/xABTEAABAwMBAwIOCgwP"
        + "AAAAAAAAAgMEAQUGBxESEwgUISIjJSYnMTRBc4OSosIVMkJDUVNydLKzFlJUY3GChIWjw9LwFyQzREVVYmRllaSxtMHi/8QAHAEB"
        + "AAIDAQEBAAAAAAAAAAAAAAYHAQQIBQID/8QANxEAAQMDAQQHBgQHAAAAAAAAAQACAwQFEQYSIVHBE2FxgZGh0QcxQUJSsRQkQ2IV"
        + "IjJykrLC/9oADAMBAAIRAxEAPwDn8AARAAEQHjVWyhg5l6t8JNKSJbcf4auuJZT6ZgkD3os8CvXtR8UZRsVktnrX4fZVtf0Np8i9"
        + "UsL2bPskttPwVdV6hjKyrNBVH8LOF07mWWzynER6h9DOrGBvJ2UzbH6eMuzbP0z46QLOyVZ4InbMrs107xucS4/BWFLbkJ9AkyXK"
        + "Oo2tVofTXNd7isEEe9e4AH2sIAAiAAIgACKEZjVTyLDb6SXY8edd0x5NY691dU8J1e7ve56KEnsh4ni0NVH2LHB5x4XXY6XXfPc6"
        + "ch2r064W3CLDKtEpMCei8Iq08qMl7c6g77kyFuTeHMat/Pr3Nekc3b4vD4bW1VU7fe0Eenu9NDXGjIJcGB54YJcB/qVvMpXvg6XI"
        + "xkjr3DKnKG4zHe/Cj+Lb3D4pa5Gz330iIvW2r39J3L/MXCC5tjNJuNR2GLnd47/OU9Vbu0lCvd/2yO1+qqOggdK5jjs/Dd6regoj"
        + "JKGFwGeoq0V852e++kY2SiM9/L8KR4xvfNZncLvLVehkuQV/P8j9swkm1ZXC73zbIY/56eX65BW+0y0Zw+Jw8FIhYC7+mUeCvHIc"
        + "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"
        + "yck5XkAAiAAIv//Z";
        
        final String expectedAvatar = "data:image/jpeg;base64,"
        + "/9j/4AAQSkZJRgABAgAAAQABAAD/2wBDAAgGBgcGBQgHBwcJCQgKDBQNDAsLDBkSEw8UHRofHh0aHBwgJC4nICIsIxwcKDcpLDAx"
        + "NDQ0Hyc5PTgyPC4zNDL/2wBDAQkJCQwLDBgNDRgyIRwhMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIy"
        + "MjIyMjIyMjL/wAARCADIAMgDASIAAhEBAxEB/8QAHwAAAQUBAQEBAQEAAAAAAAAAAAECAwQFBgcICQoL/8QAtRAAAgEDAwIEAwUF"
        + "BAQAAAF9AQIDAAQRBRIhMUEGE1FhByJxFDKBkaEII0KxwRVS0fAkM2JyggkKFhcYGRolJicoKSo0NTY3ODk6Q0RFRkdISUpTVFVW"
        + "V1hZWmNkZWZnaGlqc3R1dnd4eXqDhIWGh4iJipKTlJWWl5iZmqKjpKWmp6ipqrKztLW2t7i5usLDxMXGx8jJytLT1NXW19jZ2uHi"
        + "4+Tl5ufo6erx8vP09fb3+Pn6/8QAHwEAAwEBAQEBAQEBAQAAAAAAAAECAwQFBgcICQoL/8QAtREAAgECBAQDBAcFBAQAAQJ3AAEC"
        + "AxEEBSExBhJBUQdhcRMiMoEIFEKRobHBCSMzUvAVYnLRChYkNOEl8RcYGRomJygpKjU2Nzg5OkNERUZHSElKU1RVVldYWVpjZGVm"
        + "Z2hpanN0dXZ3eHl6goOEhYaHiImKkpOUlZaXmJmaoqOkpaanqKmqsrO0tba3uLm6wsPExcbHyMnK0tPU1dbX2Nna4uPk5ebn6Onq"
        + "8vP09fb3+Pn6/9oADAMBAAIRAxEAPwD3+iiigAooooAKKKKACiiigAopM0ZoAWijNJmgBaKTNLmgAoopM0ALRSZpaACiiigAoooo"
        + "AKKKKACiiigAooooAKKKKACiiigAprHA5P5UprJmkubq9lghk8pI8Bm/AH+tAGg86AdvxqI3Ua9ZIR9WFVRpCsczXFxIfaQgflUh"
        + "0ixzlrdWPqyg0gFbUYB1uYh9OaadXtO92v8A3yaeNOsV6WVufrEv+FI1jZAf8edt/wB+l/woHYiOr2Pe7H/fJpP7Wsu14o/4CaVr"
        + "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";
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
}

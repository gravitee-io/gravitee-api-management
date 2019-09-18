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
import static org.mockito.Mockito.doReturn;

import java.util.Arrays;
import java.util.Collections;

import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;

import io.gravitee.common.data.domain.Page;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.repository.management.api.search.UserCriteria;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.portal.rest.model.DataResponse;
import io.gravitee.rest.api.portal.rest.model.Links;
import io.gravitee.rest.api.portal.rest.model.User;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class UsersResourceTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "users";
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
        
        DataResponse usersResponse = response.readEntity(DataResponse.class);
        assertEquals(2, usersResponse.getData().size());
        
        Links links = usersResponse.getLinks();
        assertNotNull(links);
    }
    
    @Test
    public void shouldGetNoUserAndNoLink() {

        doReturn(new Page<UserEntity>(Collections.EMPTY_LIST, 1, 0, 0)).when(userService).search(any(UserCriteria.class), any());

        //Test with default limit
        final Response response = target().request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        
        DataResponse usersResponse = response.readEntity(DataResponse.class);
        assertEquals(0, usersResponse.getData().size());
        
        Links links = usersResponse.getLinks();
        assertNull(links);
        
        //Test with small limit
        final Response anotherResponse = target().queryParam("page", 2).queryParam("size", 1).request().get();
        assertEquals(HttpStatusCode.OK_200, anotherResponse.getStatus());
        
        usersResponse = anotherResponse.readEntity(DataResponse.class);
        assertEquals(0, usersResponse.getData().size());
        
        links = usersResponse.getLinks();
        assertNull(links);

    }
    
}

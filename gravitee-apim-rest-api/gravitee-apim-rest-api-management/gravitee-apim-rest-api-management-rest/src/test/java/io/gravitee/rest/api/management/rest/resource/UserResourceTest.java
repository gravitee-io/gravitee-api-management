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
package io.gravitee.rest.api.management.rest.resource;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.*;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import java.util.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Test for UserResource.getUserGroups() method
 */
public class UserResourceTest extends AbstractResourceTest {

    private static final String USER_ID = "test-user";
    private static final String ENV_ID = "test-env";

    @Before
    public void resetMocks() {
        Mockito.reset(groupService, membershipService);
    }

    @Override
    protected String contextPath() {
        // Matches the REST path you’re testing, e.g. /users/{userId}/groups?environmentId=ENV_ID
        return "users/" + USER_ID + "/groups/";
    }

    @Test
    public void shouldReturnAllGroupsIncludingEnvGroups() {
        GroupEntity group1 = new GroupEntity();
        group1.setId("group1");
        group1.setName("Global Group");
        GroupEntity group2 = new GroupEntity();
        group2.setId("group2");
        group2.setName("Env Group");

        when(groupService.findByUserAndEnvironment(USER_ID, null)).thenReturn(Set.of(group1, group2));

        final Response response = orgTarget().request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        List<UserGroupEntity> result = response.readEntity(new GenericType<>() {});
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.stream().map(UserGroupEntity::getId).toList().containsAll(List.of("group1", "group2")));

        verify(groupService, atLeastOnce()).findByUserAndEnvironment(USER_ID, null);
    }

    @Test
    public void shouldReturnGroupsByEnvironment() {
        GroupEntity group = new GroupEntity();
        group.setId("env-group");
        group.setName("Env Group");
        when(groupService.findByUserAndEnvironment(USER_ID, ENV_ID)).thenReturn(Set.of(group));

        final Response response = orgTarget().queryParam("environmentId", ENV_ID).request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        List<UserGroupEntity> result = response.readEntity(new GenericType<>() {});
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("env-group", result.get(0).getId());
        assertEquals("Env Group", result.get(0).getName());

        verify(groupService, atLeastOnce()).findByUserAndEnvironment(USER_ID, ENV_ID);
    }

    @Test
    public void shouldReturnEmptyListWhenNoGroups() {
        when(groupService.findByUserAndEnvironment(USER_ID, null)).thenReturn(Collections.emptySet());

        final Response response = orgTarget().request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        List<UserGroupEntity> result = response.readEntity(new GenericType<>() {});
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(groupService, atLeastOnce()).findByUserAndEnvironment(USER_ID, null);
        verify(membershipService, never()).getRoles(any(), any(), any(), any());
    }
}

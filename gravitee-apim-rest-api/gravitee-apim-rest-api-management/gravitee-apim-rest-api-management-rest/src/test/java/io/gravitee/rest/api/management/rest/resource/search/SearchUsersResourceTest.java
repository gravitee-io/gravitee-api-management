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
package io.gravitee.rest.api.management.rest.resource.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.idp.api.identity.SearchableUser;
import io.gravitee.rest.api.management.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.service.IdentityService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class SearchUsersResourceTest extends AbstractResourceTest {

    @Autowired
    private IdentityService identityService;

    @Override
    protected String contextPath() {
        return "search/users";
    }

    @Before
    public void init() {
        reset(identityService);
        when(identityService.search(anyString())).thenReturn(List.of(user("Zoe", "Martin"), user("Adam", "Dupont")));
    }

    @Test
    public void should_forbid_search_when_caller_manages_no_member() {
        givenCallerCanManageMembers(false);

        Response response = orgTarget().queryParam("q", "du").request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.FORBIDDEN_403);
        verify(identityService, never()).search(anyString());
    }

    @Test
    public void should_forbid_search_on_environment_route_when_caller_manages_no_member() {
        givenCallerCanManageMembers(false);

        Response response = envTarget().queryParam("q", "du").request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.FORBIDDEN_403);
        verify(identityService, never()).search(anyString());
    }

    @Test
    public void should_search_users_sorted_by_lastname_when_caller_manages_members() {
        givenCallerCanManageMembers(true);

        Response response = orgTarget().queryParam("q", "du").request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        List<Map<String, Object>> users = response.readEntity(new GenericType<List<Map<String, Object>>>() {});
        assertThat(users).extracting("displayName").containsExactly("Adam Dupont", "Zoe Martin");
        verify(identityService).search("du");
    }

    @Test
    public void should_search_users_on_environment_route_when_caller_manages_members() {
        givenCallerCanManageMembers(true);

        Response response = envTarget().queryParam("q", "du").request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        verify(identityService).search("du");
    }

    private void givenCallerCanManageMembers(boolean canManageMembers) {
        when(permissionService.canManageMembers(any(ExecutionContext.class), eq(USER_NAME))).thenReturn(canManageMembers);
    }

    private static SearchableUser user(String firstname, String lastname) {
        return new SearchableUser() {
            @Override
            public String getDisplayName() {
                return firstname + " " + lastname;
            }

            @Override
            public String getFirstname() {
                return firstname;
            }

            @Override
            public String getLastname() {
                return lastname;
            }

            @Override
            public String getEmail() {
                return null;
            }

            @Override
            public String getPicture() {
                return null;
            }
        };
    }
}

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
import io.gravitee.rest.api.service.PermissionService.MemberSearchAccess;
import io.gravitee.rest.api.service.common.ExecutionContext;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class SearchUsersResourceTest extends AbstractResourceTest {

    @Autowired
    private IdentityService identityService;

    @Override
    protected String contextPath() {
        return "search/users";
    }

    @BeforeEach
    public void init() {
        reset(identityService);
        when(identityService.search(anyString())).thenReturn(
            List.of(user("uuid-zoe", "zoe.martin", "Zoe", "Martin"), user("uuid-adam", "adam.dupont", "Adam", "Dupont"))
        );
    }

    @Test
    public void should_forbid_search_when_caller_manages_no_member() {
        givenMemberSearchAccess(MemberSearchAccess.NONE);

        Response response = orgTarget().queryParam("q", "du").request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.FORBIDDEN_403);
        verify(identityService, never()).search(anyString());
    }

    @Test
    public void should_forbid_search_on_environment_route_when_caller_manages_no_member() {
        givenMemberSearchAccess(MemberSearchAccess.NONE);

        Response response = envTarget().queryParam("q", "du").request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.FORBIDDEN_403);
        verify(identityService, never()).search(anyString());
    }

    @Test
    public void should_search_users_sorted_by_lastname_when_caller_manages_members() {
        givenMemberSearchAccess(MemberSearchAccess.FULL);

        Response response = orgTarget().queryParam("q", "du").request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        List<Map<String, Object>> users = response.readEntity(new GenericType<List<Map<String, Object>>>() {});
        assertThat(users).extracting("displayName").containsExactly("Adam Dupont", "Zoe Martin");
        verify(identityService).search("du");
    }

    @Test
    public void should_search_users_on_environment_route_when_caller_manages_members() {
        givenMemberSearchAccess(MemberSearchAccess.FULL);

        Response response = envTarget().queryParam("q", "du").request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        verify(identityService).search("du");
    }

    @Test
    public void should_filter_to_exact_match_for_application_only_caller() {
        givenMemberSearchAccess(MemberSearchAccess.APPLICATION_ONLY);

        var response = orgTarget().queryParam("q", "du").request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        var users = response.readEntity(new GenericType<List<Map<String, Object>>>() {});
        assertThat(users).isEmpty();
        verify(identityService).search("du");
    }

    @Test
    public void should_allow_exact_display_name_for_application_only_caller() {
        givenMemberSearchAccess(MemberSearchAccess.APPLICATION_ONLY);

        var response = orgTarget().queryParam("q", "Adam Dupont").request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        var users = response.readEntity(new GenericType<List<Map<String, Object>>>() {});
        assertThat(users).extracting("displayName").containsExactly("Adam Dupont");
    }

    @Test
    public void should_allow_exact_source_id_for_application_only_caller() {
        givenMemberSearchAccess(MemberSearchAccess.APPLICATION_ONLY);

        var response = orgTarget().queryParam("q", "adam.dupont").request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        var users = response.readEntity(new GenericType<List<Map<String, Object>>>() {});
        assertThat(users).extracting("displayName").containsExactly("Adam Dupont");
    }

    @Test
    public void should_allow_exact_email_for_application_only_caller_when_email_is_available() {
        givenMemberSearchAccess(MemberSearchAccess.APPLICATION_ONLY);
        when(identityService.search(anyString())).thenReturn(
            List.of(
                user("uuid-zoe", "zoe.martin", "Zoe", "Martin", "zoe.martin@example.com"),
                user("uuid-adam", "adam.dupont", "Adam", "Dupont", "adam.dupont@example.com")
            )
        );

        var response = orgTarget().queryParam("q", "adam.dupont@example.com").request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        var users = response.readEntity(new GenericType<List<Map<String, Object>>>() {});
        assertThat(users).extracting("displayName").containsExactly("Adam Dupont");
    }

    @Test
    public void should_not_match_uuid_for_application_only_caller() {
        givenMemberSearchAccess(MemberSearchAccess.APPLICATION_ONLY);

        var response = orgTarget().queryParam("q", "uuid-adam").request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        var users = response.readEntity(new GenericType<List<Map<String, Object>>>() {});
        assertThat(users).isEmpty();
    }

    @Test
    public void should_reject_single_firstname_for_application_only_caller() {
        givenMemberSearchAccess(MemberSearchAccess.APPLICATION_ONLY);

        var response = orgTarget().queryParam("q", "Adam").request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        var users = response.readEntity(new GenericType<List<Map<String, Object>>>() {});
        assertThat(users).isEmpty();
    }

    @Test
    public void should_still_allow_prefix_search_for_non_application_caller() {
        givenMemberSearchAccess(MemberSearchAccess.FULL);

        var response = orgTarget().queryParam("q", "du").request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        var users = response.readEntity(new GenericType<List<Map<String, Object>>>() {});
        assertThat(users).extracting("displayName").containsExactly("Adam Dupont", "Zoe Martin");
    }

    @Test
    public void should_not_expose_sourceId_for_full_caller() {
        givenMemberSearchAccess(MemberSearchAccess.FULL);

        var response = orgTarget().queryParam("q", "du").request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        var users = response.readEntity(new GenericType<List<Map<String, Object>>>() {});
        assertThat(users).hasSize(2);
        assertThat(users.get(0)).doesNotContainKey("sourceId");
    }

    @Test
    public void should_not_expose_sourceId_for_application_only_caller() {
        givenMemberSearchAccess(MemberSearchAccess.APPLICATION_ONLY);

        var response = orgTarget().queryParam("q", "Adam Dupont").request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        var users = response.readEntity(new GenericType<List<Map<String, Object>>>() {});
        assertThat(users).hasSize(1);
        assertThat(users.get(0)).doesNotContainKey("sourceId");
    }

    private void givenMemberSearchAccess(MemberSearchAccess access) {
        when(permissionService.memberSearchAccess(any(ExecutionContext.class), eq(USER_NAME))).thenReturn(access);
    }

    private static SearchableUser user(String id, String sourceId, String firstname, String lastname) {
        return user(id, sourceId, firstname, lastname, null);
    }

    private static SearchableUser user(String id, String sourceId, String firstname, String lastname, String email) {
        return new SearchableUser() {
            @Override
            public String getId() {
                return id;
            }

            @Override
            public String getSourceId() {
                return sourceId;
            }

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
                return email;
            }

            @Override
            public String getPicture() {
                return null;
            }
        };
    }
}

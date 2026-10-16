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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.GroupEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class GroupResourceTest extends AbstractResourceTest {

    private static final String GROUP_ID = "group-id";

    @Override
    protected String contextPath() {
        return "configuration/groups/" + GROUP_ID;
    }

    @BeforeEach
    public void init() {
        Mockito.reset(groupService);

        ApplicationEntity application = new ApplicationEntity();
        application.setId("application-id");
        when(groupService.getApplications(GROUP_ID)).thenReturn(List.of(application));

        ApiEntity api = new ApiEntity();
        api.setId("api-id");
        when(groupService.getApis(GraviteeContext.getCurrentEnvironment(), GROUP_ID)).thenReturn(List.of(api));
    }

    @Test
    public void should_forbid_listing_applications_when_caller_does_not_manage_the_group() {
        givenGroup(false);

        Response response = envTarget().path("memberships").queryParam("type", "application").request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.FORBIDDEN_403);
        verify(groupService, never()).getApplications(anyString());
    }

    @Test
    public void should_forbid_listing_apis_when_caller_does_not_manage_the_group() {
        givenGroup(false);

        Response response = envTarget().path("memberships").queryParam("type", "api").request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.FORBIDDEN_403);
        verify(groupService, never()).getApis(any(), anyString());
    }

    @Test
    public void should_list_applications_when_caller_manages_the_group() {
        givenGroup(true);

        Response response = envTarget().path("memberships").queryParam("type", "application").request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        assertThat(idsOf(response)).containsExactly("application-id");
    }

    @Test
    public void should_list_apis_when_caller_manages_the_group() {
        givenGroup(true);

        Response response = envTarget().path("memberships").queryParam("type", "api").request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        assertThat(idsOf(response)).containsExactly("api-id");
    }

    private void givenGroup(boolean manageable) {
        when(groupService.findById(GraviteeContext.getExecutionContext(), GROUP_ID)).thenReturn(
            GroupEntity.builder().id(GROUP_ID).manageable(manageable).build()
        );
    }

    private static List<Object> idsOf(Response response) {
        return response
            .readEntity(new GenericType<List<Map<String, Object>>>() {})
            .stream()
            .map(entity -> entity.get("id"))
            .toList();
    }
}

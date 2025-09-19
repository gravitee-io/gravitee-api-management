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

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.GroupMemberEntity;
import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class ApiGroupsResourceTest extends AbstractResourceTest {

    private static final String API_ID = "8b8a03a1-429e-4cd2-a860-b258300c2b80";
    private static final String ENVIRONMENT = "DEFAULT";

    @Override
    protected String contextPath() {
        return "apis/";
    }

    @Override
    protected void decorate(ResourceConfig resourceConfig) {
        resourceConfig.register(NotAdminAuthenticationFilter.class);
    }

    @Before
    public void init() {
        Mockito.reset(apiService);
        Mockito.reset(apiGroupService);
        Mockito.reset(roleService);
    }

    @Test
    public void shouldReturn500IfTechnicalException() {
        when(apiGroupService.getGroupsWithMembers(GraviteeContext.getExecutionContext(), API_ID)).thenThrow(
            new TechnicalManagementException()
        );

        Response response = envTarget(API_ID).path("groups").request().get();

        assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR_500, response.getStatus());
    }

    @Test
    public void shouldReturn403IfNotGranted() {
        when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(false);
        Response response = envTarget(API_ID).path("groups").request().get();
        assertEquals(HttpStatusCode.FORBIDDEN_403, response.getStatus());
    }

    @Test
    public void shouldReturnGroupsIfGranted() {
        ApiEntity api = Mockito.mock(ApiEntity.class);

        when(apiService.findById(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(api);

        when(apiGroupService.getGroupsWithMembers(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(
            Map.of(UuidString.generateRandom(), List.of(new GroupMemberEntity()))
        );

        Response response = envTarget(API_ID).path("groups").request().get();

        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        Map<String, List<MemberEntity>> responseBody = response.readEntity(new GenericType<>() {});

        assertEquals(1, responseBody.size());
    }
}

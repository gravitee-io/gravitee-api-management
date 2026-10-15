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

import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import io.gravitee.common.data.domain.Page;
import io.gravitee.definition.model.Proxy;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.common.Sortable;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.core.Response;
import java.util.*;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.Test;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class ApisResourceNotAdminTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "apis";
    }

    @Override
    protected void decorate(ResourceConfig resourceConfig) {
        resourceConfig.register(NotAdminAuthenticationFilter.class);
    }

    @Test
    public void get_should_search_apis() throws TechnicalException {
        when(apiAuthorizationService.findIdsByUser(eq(GraviteeContext.getExecutionContext()), any(), eq(true))).thenReturn(
            Set.of("api1", "api2", "api15")
        );

        List<ApiEntity> resultApis = List.of(mockApi("api1"), mockApi("api2"), mockApi("api15"));
        Page<ApiEntity> apisPage = new Page<>(resultApis, 7, 3, 54);
        when(
            apiService.search(
                eq(GraviteeContext.getExecutionContext()),
                eq("*"),
                argThat(filters -> ((Set<String>) filters.get("api")).size() == 3),
                isNull(),
                isA(Pageable.class)
            )
        ).thenReturn(apisPage);

        final Response response = envTarget().path("_search/_paged").queryParam("q", "*").request().post(null);

        assertEquals(OK_200, response.getStatus());
    }

    @Test
    public void get_should_search_apis_with_order() throws TechnicalException {
        when(apiAuthorizationService.findIdsByUser(eq(GraviteeContext.getExecutionContext()), any(), eq(true))).thenReturn(
            Set.of("api1", "api2", "api15")
        );

        List<ApiEntity> resultApis = List.of(mockApi("api1"), mockApi("api2"), mockApi("api15"));
        Page<ApiEntity> apisPage = new Page<>(resultApis, 7, 3, 54);
        when(
            apiService.search(
                eq(GraviteeContext.getExecutionContext()),
                eq("*"),
                argThat(filters -> ((Set<String>) filters.get("api")).size() == 3),
                isA(Sortable.class),
                isA(Pageable.class)
            )
        ).thenReturn(apisPage);

        final Response response = envTarget().path("_search/_paged").queryParam("q", "*").queryParam("order", "name").request().post(null);

        assertEquals(OK_200, response.getStatus());
    }

    @Test
    public void get_should_search_apis_with_manageOnly_to_false() throws TechnicalException {
        when(apiAuthorizationService.findIdsByUser(eq(GraviteeContext.getExecutionContext()), any(), eq(false))).thenReturn(
            Set.of("api1", "api2", "api15")
        );

        List<ApiEntity> resultApis = List.of(mockApi("api1"), mockApi("api2"), mockApi("api15"));
        Page<ApiEntity> apisPage = new Page<>(resultApis, 7, 3, 54);

        when(
            apiService.search(
                eq(GraviteeContext.getExecutionContext()),
                eq("*"),
                argThat(filters -> ((Set<String>) filters.get("api")).size() == 3),
                isNull(),
                isA(Pageable.class)
            )
        ).thenReturn(apisPage);
        final Response response = envTarget()
            .path("_search/_paged")
            .queryParam("q", "*")
            .queryParam("manageOnly", "false")
            .request()
            .post(null);

        assertEquals(OK_200, response.getStatus());
    }

    @Test
    public void paged_search_should_return_no_api_when_the_user_can_access_none() {
        reset(apiService, apiAuthorizationService);
        when(apiAuthorizationService.findIdsByUser(eq(GraviteeContext.getExecutionContext()), any(), eq(true))).thenReturn(Set.of());

        final Response response = envTarget().path("_search/_paged").queryParam("q", "*").request().post(null);

        assertEquals(OK_200, response.getStatus());
        assertThat((Collection<?>) response.readEntity(Map.class).get("data")).isEmpty();
        verify(apiService, never()).search(any(), any(), any(), any(), any());
    }

    @Test
    public void search_should_return_no_api_when_the_user_can_access_none() {
        reset(apiService, apiAuthorizationService);
        when(apiAuthorizationService.findIdsByUser(eq(GraviteeContext.getExecutionContext()), any(), eq(true))).thenReturn(Set.of());

        final Response response = envTarget().path("_search").queryParam("q", "*").request().post(null);

        assertEquals(OK_200, response.getStatus());
        assertThat(response.readEntity(List.class)).isEmpty();
        verify(apiService, never()).search(any(), any(), any(), any(), any());
    }

    private ApiEntity mockApi(String apiId) {
        ApiEntity apiEntity = mock(ApiEntity.class);
        when(apiEntity.getId()).thenReturn(apiId);
        when(apiEntity.getUpdatedAt()).thenReturn(new Date());
        when(apiEntity.getProxy()).thenReturn(new Proxy());
        return apiEntity;
    }
}

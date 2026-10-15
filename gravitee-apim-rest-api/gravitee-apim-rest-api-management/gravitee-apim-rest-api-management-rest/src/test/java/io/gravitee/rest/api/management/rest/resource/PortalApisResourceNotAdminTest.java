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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PortalApisResourceNotAdminTest extends AbstractResourceTest {

    private static final String API_ID = "my-api";

    @Override
    protected String contextPath() {
        return "portal/apis";
    }

    @Override
    protected void decorate(ResourceConfig resourceConfig) {
        resourceConfig.register(NotAdminAuthenticationFilter.class);
    }

    @BeforeEach
    public void resetMocks() {
        reset(apiService);
    }

    @Test
    public void should_return_no_api_when_the_user_can_access_none() throws TechnicalException {
        when(apiService.findByUser(eq(GraviteeContext.getExecutionContext()), eq(USER_NAME), any(), eq(false))).thenReturn(Set.of());

        final Response response = envTarget().path("_search").queryParam("q", "*").request().post(null);

        assertThat(response.getStatus()).isEqualTo(OK_200);
        assertThat(response.readEntity(List.class)).isEmpty();
        verify(apiService, never()).search(any(), anyString(), anyMap());
    }

    @Test
    public void should_search_only_the_apis_the_user_can_access() throws TechnicalException {
        ApiEntity api = new ApiEntity();
        api.setId(API_ID);
        api.setName("My API");
        when(apiService.findByUser(eq(GraviteeContext.getExecutionContext()), eq(USER_NAME), any(), eq(false))).thenReturn(Set.of(api));
        when(
            apiService.search(
                eq(GraviteeContext.getExecutionContext()),
                eq("*"),
                argThat(filters -> Set.of(API_ID).equals(filters.get("api")))
            )
        ).thenReturn(List.of(api));

        final Response response = envTarget().path("_search").queryParam("q", "*").request().post(null);

        assertThat(response.getStatus()).isEqualTo(OK_200);
        List<Map<String, Object>> apis = response.readEntity(List.class);
        assertThat(apis)
            .extracting(item -> item.get("id"))
            .containsExactly(API_ID);
    }
}

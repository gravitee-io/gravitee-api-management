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
package io.gravitee.rest.api.portal.rest.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.Visibility;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.api.ApiLifecycleState;
import io.gravitee.rest.api.portal.rest.model.Api;
import io.gravitee.rest.api.portal.rest.model.ApisResponse;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 */
public class ApisResourceNotAuthenticatedTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "apis";
    }

    @Override
    protected void decorate(ResourceConfig resourceConfig) {
        resourceConfig.register(NotAuthenticatedAuthenticationFilter.class);
    }

    @Before
    public void init() {
        resetAllMocks();

        ApiEntity publishedApi = new ApiEntity();
        publishedApi.setLifecycleState(ApiLifecycleState.PUBLISHED);
        publishedApi.setName("A");
        publishedApi.setId("A");
        publishedApi.setVisibility(Visibility.PUBLIC);
        publishedApi.setCategories(Set.of("Category1", "Category2"));

        ApiEntity unpublishedApi = new ApiEntity();
        unpublishedApi.setLifecycleState(ApiLifecycleState.UNPUBLISHED);
        unpublishedApi.setName("B");
        unpublishedApi.setId("B");
        publishedApi.setVisibility(Visibility.PUBLIC);
        publishedApi.setCategories(Set.of("Category1", "Category2"));

        ApiEntity anotherPublishedApi = new ApiEntity();
        anotherPublishedApi.setLifecycleState(ApiLifecycleState.PUBLISHED);
        anotherPublishedApi.setName("C");
        anotherPublishedApi.setId("C");
        publishedApi.setVisibility(Visibility.PUBLIC);
        publishedApi.setCategories(Set.of("Category1", "Category2"));

        ApiEntity publishedApi1 = new ApiEntity();
        publishedApi1.setLifecycleState(ApiLifecycleState.PUBLISHED);
        publishedApi1.setName("D");
        publishedApi1.setId("D");
        publishedApi1.setVisibility(Visibility.PRIVATE);
        publishedApi1.setCategories(Set.of("Category1", "Category2"));

        doReturn(Arrays.asList("A", "C")).when(filteringService).filterApis(any(), any(), any(), any(), any());
        doReturn(List.of(publishedApi, anotherPublishedApi))
            .when(apiSearchService)
            .search(eq(GraviteeContext.getExecutionContext()), any());

        doReturn(false).when(ratingService).isEnabled(GraviteeContext.getExecutionContext());

        doReturn(new Api().name("A").id("A")).when(apiMapper).convert(GraviteeContext.getExecutionContext(), publishedApi);
        doReturn(new Api().name("B").id("B")).when(apiMapper).convert(GraviteeContext.getExecutionContext(), unpublishedApi);
        doReturn(new Api().name("C").id("C")).when(apiMapper).convert(GraviteeContext.getExecutionContext(), anotherPublishedApi);
        doReturn(new Api().name("D").id("D")).when(apiMapper).convert(GraviteeContext.getExecutionContext(), publishedApi1);
    }

    @Test
    public void shouldGetPublishedApi() {
        final Response response = target().request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        ApisResponse apiResponse = response.readEntity(ApisResponse.class);
        assertNotNull(apiResponse.getData());
        assertEquals(2, apiResponse.getData().size());
        assertEquals("A", apiResponse.getData().get(0).getName());
        assertEquals("C", apiResponse.getData().get(1).getName());
    }

    @Test
    public void shouldReturnPublishedPublicApiWhenQueryByCategory() {
        final Response response = target().queryParam("category", "Category1").request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        ApisResponse apiResponse = response.readEntity(ApisResponse.class);
        assertNotNull(apiResponse.getData());
        assertEquals(2, apiResponse.getData().size());
        assertEquals("A", apiResponse.getData().get(0).getName());
        assertEquals("C", apiResponse.getData().get(1).getName());
    }
}

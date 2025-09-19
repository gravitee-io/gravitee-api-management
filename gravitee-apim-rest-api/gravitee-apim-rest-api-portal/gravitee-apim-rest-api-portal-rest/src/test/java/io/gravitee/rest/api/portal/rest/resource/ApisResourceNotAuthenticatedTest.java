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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;

import inmemory.ApiAuthorizationDomainServiceInMemory;
import inmemory.ApiCategoryOrderQueryServiceInMemory;
import inmemory.ApiQueryServiceInMemory;
import inmemory.CategoryQueryServiceInMemory;
import io.gravitee.apim.core.category.model.ApiCategoryOrder;
import io.gravitee.apim.core.category.model.Category;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 */
public class ApisResourceNotAuthenticatedTest extends AbstractResourceTest {

    @Autowired
    private CategoryQueryServiceInMemory categoryQueryServiceInMemory;

    @Autowired
    private ApiCategoryOrderQueryServiceInMemory apiCategoryOrderQueryServiceInMemory;

    @Autowired
    private ApiAuthorizationDomainServiceInMemory apiAuthorizationDomainService;

    @Autowired
    private ApiQueryServiceInMemory apiQueryServiceInMemory;

    @Override
    protected String contextPath() {
        return "apis";
    }

    @Override
    protected void decorate(ResourceConfig resourceConfig) {
        resourceConfig.register(NotAuthenticatedAuthenticationFilter.class);
    }

    @BeforeEach
    public void init() {
        resetAllMocks();
        categoryQueryServiceInMemory.reset();
        apiCategoryOrderQueryServiceInMemory.reset();
        apiAuthorizationDomainService.reset();

        ApiEntity publishedApi = createApiEntity("A", "A", Visibility.PUBLIC, ApiLifecycleState.PUBLISHED);
        ApiEntity unpublishedApi = createApiEntity("B", "B", Visibility.PUBLIC, ApiLifecycleState.UNPUBLISHED);
        ApiEntity anotherPublishedApi = createApiEntity("C", "C", Visibility.PUBLIC, ApiLifecycleState.PUBLISHED);
        ApiEntity privateApi = createApiEntity("D", "D", Visibility.PRIVATE, ApiLifecycleState.PUBLISHED);

        doReturn(Arrays.asList("A", "C")).when(filteringService).filterApis(any(), any(), any(), any(), any());
        doReturn(List.of(publishedApi, anotherPublishedApi))
            .when(apiSearchService)
            .search(eq(GraviteeContext.getExecutionContext()), any());

        categoryQueryServiceInMemory.initWith(List.of(Category.builder().id("Category1").build()));

        apiCategoryOrderQueryServiceInMemory.initWith(
            List.of(
                ApiCategoryOrder.builder().apiId("1").categoryId("myCat").build(),
                ApiCategoryOrder.builder().apiId("A").categoryId("Category1").build(),
                ApiCategoryOrder.builder().apiId("B").categoryId("Category1").build(),
                ApiCategoryOrder.builder().apiId("C").categoryId("Category1").build(),
                ApiCategoryOrder.builder().apiId("D").categoryId("Category1").build()
            )
        );

        apiAuthorizationDomainService.initWith(List.of(createApi("A"), createApi("C")));

        apiQueryServiceInMemory.initWith(List.of(createApi("A"), createApi("C")));

        doReturn(false).when(ratingService).isEnabled(GraviteeContext.getExecutionContext());

        doReturn(new Api().name("A").id("A")).when(apiMapper).convert(GraviteeContext.getExecutionContext(), publishedApi);
        doReturn(new Api().name("B").id("B")).when(apiMapper).convert(GraviteeContext.getExecutionContext(), unpublishedApi);
        doReturn(new Api().name("C").id("C")).when(apiMapper).convert(GraviteeContext.getExecutionContext(), anotherPublishedApi);
        doReturn(new Api().name("D").id("D")).when(apiMapper).convert(GraviteeContext.getExecutionContext(), privateApi);
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

    private static ApiEntity createApiEntity(String id, String name, Visibility visibility, ApiLifecycleState apiLifecycleState) {
        ApiEntity anotherPublishedApi = new ApiEntity();
        anotherPublishedApi.setLifecycleState(apiLifecycleState);
        anotherPublishedApi.setVisibility(visibility);
        anotherPublishedApi.setName(name);
        anotherPublishedApi.setId(id);
        return anotherPublishedApi;
    }

    private static io.gravitee.apim.core.api.model.Api createApi(String A) {
        return io.gravitee.apim.core.api.model.Api.builder()
            .id(A)
            .name(A)
            .apiLifecycleState(io.gravitee.apim.core.api.model.Api.ApiLifecycleState.PUBLISHED)
            .categories(Set.of("Category1"))
            .visibility(io.gravitee.apim.core.api.model.Api.Visibility.PUBLIC)
            .build();
    }
}

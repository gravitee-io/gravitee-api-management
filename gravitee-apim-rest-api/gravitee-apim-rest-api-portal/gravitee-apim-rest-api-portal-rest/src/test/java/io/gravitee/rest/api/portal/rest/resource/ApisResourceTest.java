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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import inmemory.ApiAuthorizationDomainServiceInMemory;
import inmemory.ApiCategoryOrderQueryServiceInMemory;
import inmemory.ApiCategoryQueryServiceInMemory;
import inmemory.ApiKeyCrudServiceInMemory;
import inmemory.ApiQueryServiceInMemory;
import inmemory.CategoryQueryServiceInMemory;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.category.model.ApiCategoryOrder;
import io.gravitee.apim.core.category.model.Category;
import io.gravitee.apim.core.category.query_service.CategoryQueryService;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.rest.api.model.CategoryEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.api.ApiLifecycleState;
import io.gravitee.rest.api.model.api.ApiQuery;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.portal.rest.model.ApisResponse;
import io.gravitee.rest.api.portal.rest.model.Error;
import io.gravitee.rest.api.portal.rest.model.ErrorResponse;
import io.gravitee.rest.api.portal.rest.model.Links;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApisResourceTest extends AbstractResourceTest {

    @Autowired
    private CategoryQueryServiceInMemory categoryQueryServiceInMemory;

    @Autowired
    private ApiCategoryOrderQueryServiceInMemory apiCategoryOrderQueryServiceInMemory;

    @Autowired
    private ApiAuthorizationDomainServiceInMemory apiAuthorizationDomainServiceInMemory;

    @Autowired
    private ApiQueryServiceInMemory apiQueryServiceInMemory;

    @Override
    protected String contextPath() {
        return "apis";
    }

    @Before
    public void init() {
        resetAllMocks();
        prepareInMemoryServices();

        ApiEntity publishedApi1 = new ApiEntity();
        publishedApi1.setLifecycleState(ApiLifecycleState.PUBLISHED);
        publishedApi1.setName("1");
        publishedApi1.setId("1");
        publishedApi1.setLabels(List.of("label1", "label2"));

        ApiEntity unpublishedApi = new ApiEntity();
        unpublishedApi.setLifecycleState(ApiLifecycleState.UNPUBLISHED);
        unpublishedApi.setName("2");
        unpublishedApi.setId("2");
        unpublishedApi.setLabels(List.of("unpublished", "label2"));

        ApiEntity publishedApi2 = new ApiEntity();
        publishedApi2.setLifecycleState(ApiLifecycleState.PUBLISHED);
        publishedApi2.setName("3");
        publishedApi2.setId("3");
        publishedApi2.setLabels(List.of("label1", "label2"));

        ApiEntity publishedApi3 = new ApiEntity();
        publishedApi3.setLifecycleState(ApiLifecycleState.PUBLISHED);
        publishedApi3.setName("4");
        publishedApi3.setId("4");
        publishedApi3.setLabels(List.of("label1", "label2", "label3"));

        ApiEntity publishedApi4 = new ApiEntity();
        publishedApi4.setLifecycleState(ApiLifecycleState.PUBLISHED);
        publishedApi4.setName("5");
        publishedApi4.setId("5");
        publishedApi4.setLabels(List.of("label1"));

        ApiEntity publishedApi5 = new ApiEntity();
        publishedApi5.setLifecycleState(ApiLifecycleState.PUBLISHED);
        publishedApi5.setName("6");
        publishedApi5.setId("6");

        ApiQuery pageQuery = new ApiQuery();
        pageQuery.setIds(Arrays.asList("1", "3", "5"));
        doReturn(List.of(publishedApi1, publishedApi2, publishedApi4))
            .when(apiSearchService)
            .search(eq(GraviteeContext.getExecutionContext()), eq(pageQuery));

        pageQuery = new ApiQuery();
        pageQuery.setIds(Arrays.asList("3", "4", "5"));
        doReturn(List.of(publishedApi2, publishedApi3, publishedApi4))
            .when(apiSearchService)
            .search(eq(GraviteeContext.getExecutionContext()), eq(pageQuery));

        pageQuery = new ApiQuery();
        pageQuery.setIds(Arrays.asList("1", "3", "4", "5", "6"));
        doReturn(List.of(publishedApi1, publishedApi2, publishedApi3, publishedApi4, publishedApi5))
            .when(apiSearchService)
            .search(eq(GraviteeContext.getExecutionContext()), eq(pageQuery));

        pageQuery = new ApiQuery();
        pageQuery.setIds(Arrays.asList("1"));
        doReturn(List.of(publishedApi1)).when(apiSearchService).search(eq(GraviteeContext.getExecutionContext()), eq(pageQuery));

        pageQuery = new ApiQuery();
        pageQuery.setIds(Arrays.asList("3", "4"));
        doReturn(List.of(publishedApi2, publishedApi3))
            .when(apiSearchService)
            .search(eq(GraviteeContext.getExecutionContext()), eq(pageQuery));

        pageQuery = new ApiQuery();
        pageQuery.setIds(Arrays.asList("4"));
        doReturn(List.of(publishedApi3)).when(apiSearchService).search(eq(GraviteeContext.getExecutionContext()), eq(pageQuery));

        pageQuery = new ApiQuery();
        pageQuery.setIds(Arrays.asList("3"));
        doReturn(List.of(publishedApi2)).when(apiSearchService).search(eq(GraviteeContext.getExecutionContext()), eq(pageQuery));

        Set<ApiEntity> mockApis = new HashSet<>(Arrays.asList(publishedApi5, publishedApi2, publishedApi1, publishedApi3, publishedApi4));
        when(apiAuthorizationService.findAccessibleApiIdsForUser(eq(GraviteeContext.getExecutionContext()), any(), any(Set.class)))
            .thenReturn(mockApis.stream().map(ApiEntity::getId).collect(Collectors.toSet()));

        doReturn(false).when(ratingService).isEnabled(GraviteeContext.getExecutionContext());

        doReturn(mockApis.stream().map(api -> api.getId()).collect(Collectors.toSet()))
            .when(filteringService)
            .filterApis(any(), any(), any(), any(), any());

        when(apiMapper.convert(eq(GraviteeContext.getExecutionContext()), any())).thenCallRealMethod();

        when(
            parameterService.findAsBoolean(
                GraviteeContext.getExecutionContext(),
                Key.PORTAL_APIS_SHOW_TAGS_IN_APIHEADER,
                ParameterReferenceType.ENVIRONMENT
            )
        )
            .thenReturn(true);
    }

    private void prepareInMemoryServices() {
        categoryQueryServiceInMemory.reset();
        apiCategoryOrderQueryServiceInMemory.reset();
        apiAuthorizationDomainServiceInMemory.reset();
        apiQueryServiceInMemory.reset();

        final Category myCat = Category.builder().id("myCat").build();
        categoryQueryServiceInMemory.initWith(List.of(myCat));
        apiCategoryOrderQueryServiceInMemory.initWith(
            List.of(
                ApiCategoryOrder.builder().apiId("1").categoryId("myCat").build(),
                ApiCategoryOrder.builder().apiId("3").categoryId("myCat").build(),
                ApiCategoryOrder.builder().apiId("4").categoryId("myCat").build(),
                ApiCategoryOrder.builder().apiId("5").categoryId("myCat").build(),
                ApiCategoryOrder.builder().apiId("6").categoryId("myCat").build()
            )
        );
        apiAuthorizationDomainServiceInMemory.initWith(
            List.of(
                Api
                    .builder()
                    .id("1")
                    .name("1")
                    .labels(List.of("label1", "label2"))
                    .apiLifecycleState(Api.ApiLifecycleState.PUBLISHED)
                    .build(),
                Api
                    .builder()
                    .id("3")
                    .name("3")
                    .labels(List.of("label1", "label2"))
                    .apiLifecycleState(Api.ApiLifecycleState.PUBLISHED)
                    .build(),
                Api
                    .builder()
                    .id("4")
                    .name("4")
                    .labels(List.of("label1", "label2", "label3"))
                    .apiLifecycleState(Api.ApiLifecycleState.PUBLISHED)
                    .build(),
                Api.builder().id("5").name("5").labels(List.of("label1")).apiLifecycleState(Api.ApiLifecycleState.PUBLISHED).build(),
                Api.builder().id("6").name("6").apiLifecycleState(Api.ApiLifecycleState.PUBLISHED).build()
            )
        );
        apiQueryServiceInMemory.initWith(
            List.of(
                Api
                    .builder()
                    .id("1")
                    .name("1")
                    .labels(List.of("label1", "label2"))
                    .apiLifecycleState(Api.ApiLifecycleState.PUBLISHED)
                    .build(),
                Api
                    .builder()
                    .id("3")
                    .name("3")
                    .labels(List.of("label1", "label2"))
                    .apiLifecycleState(Api.ApiLifecycleState.PUBLISHED)
                    .build(),
                Api
                    .builder()
                    .id("4")
                    .name("4")
                    .labels(List.of("label1", "label2", "label3"))
                    .apiLifecycleState(Api.ApiLifecycleState.PUBLISHED)
                    .build(),
                Api.builder().id("5").name("5").labels(List.of("label1")).apiLifecycleState(Api.ApiLifecycleState.PUBLISHED).build(),
                Api.builder().id("6").name("6").apiLifecycleState(Api.ApiLifecycleState.PUBLISHED).build()
            )
        );
    }

    @Test
    public void shouldGetPublishedApi() {
        final Response response = target()
            .queryParam("context-path", "context-path")
            .queryParam("label", "label")
            .queryParam("version", "version")
            .queryParam("name", "name")
            .queryParam("tag", "tag")
            .request()
            .get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        ArgumentCaptor<String> basePathCaptor = ArgumentCaptor.forClass(String.class);
        Mockito
            .verify(apiMapper, Mockito.times(5))
            .computeApiLinks(basePathCaptor.capture(), ArgumentCaptor.forClass(Date.class).capture());
        final String expectedBasePath = target().getUri().toString();
        final List<String> bastPathList = basePathCaptor.getAllValues();
        assertTrue(bastPathList.contains(expectedBasePath + "/1"));
        assertTrue(bastPathList.contains(expectedBasePath + "/3"));
        assertTrue(bastPathList.contains(expectedBasePath + "/4"));
        assertTrue(bastPathList.contains(expectedBasePath + "/5"));
        assertTrue(bastPathList.contains(expectedBasePath + "/6"));

        ArgumentCaptor<ApiEntity> apiEntityCaptor = ArgumentCaptor.forClass(ApiEntity.class);
        Mockito.verify(apiMapper, Mockito.times(5)).convert(eq(GraviteeContext.getExecutionContext()), apiEntityCaptor.capture());
        final List<String> allNameValues = apiEntityCaptor.getAllValues().stream().map(a -> a.getName()).collect(Collectors.toList());
        assertEquals(5, allNameValues.size());
        assertTrue(allNameValues.containsAll(Arrays.asList("1", "3", "4", "5", "6")));

        ApisResponse apiResponse = response.readEntity(ApisResponse.class);
        assertEquals(5, apiResponse.getData().size());
        assertTrue(getmaxLabelsListSize(apiResponse) > 0);
    }

    @Test
    public void shouldGetPublishedApiWithPaginatedLink() {
        final Response response = target().queryParam("page", 3).queryParam("size", 1).request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        ArgumentCaptor<ApiEntity> apiEntityCaptor = ArgumentCaptor.forClass(ApiEntity.class);
        Mockito.verify(apiMapper, Mockito.times(1)).convert(eq(GraviteeContext.getExecutionContext()), apiEntityCaptor.capture());
        final List<String> allNameValues = apiEntityCaptor.getAllValues().stream().map(a -> a.getName()).collect(Collectors.toList());
        assertEquals(1, allNameValues.size());
        assertTrue(allNameValues.containsAll(Arrays.asList("4")));

        ApisResponse apiResponse = response.readEntity(ApisResponse.class);
        assertEquals(1, apiResponse.getData().size());
        assertTrue(getmaxLabelsListSize(apiResponse) > 0);

        Links links = apiResponse.getLinks();
        assertNotNull(links);
    }

    @Test
    public void shouldGetAllApis() {
        final Response response = target().queryParam("size", -1).request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        ArgumentCaptor<ApiEntity> apiEntityCaptor = ArgumentCaptor.forClass(ApiEntity.class);
        Mockito.verify(apiMapper, Mockito.times(5)).convert(eq(GraviteeContext.getExecutionContext()), apiEntityCaptor.capture());
        final List<String> allNameValues = apiEntityCaptor.getAllValues().stream().map(a -> a.getName()).collect(Collectors.toList());
        assertEquals(5, allNameValues.size());
        assertTrue(allNameValues.containsAll(Arrays.asList("1", "3", "4", "5", "6")));

        ApisResponse apiResponse = response.readEntity(ApisResponse.class);
        assertEquals(5, apiResponse.getData().size());
        assertTrue(getmaxLabelsListSize(apiResponse) > 0);
    }

    @Test
    public void shouldGetAllApisWithoutLabels() {
        when(
            parameterService.findAsBoolean(
                GraviteeContext.getExecutionContext(),
                Key.PORTAL_APIS_SHOW_TAGS_IN_APIHEADER,
                ParameterReferenceType.ENVIRONMENT
            )
        )
            .thenReturn(false);
        final Response response = target().queryParam("size", -1).request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        ArgumentCaptor<ApiEntity> apiEntityCaptor = ArgumentCaptor.forClass(ApiEntity.class);
        Mockito.verify(apiMapper, Mockito.times(5)).convert(eq(GraviteeContext.getExecutionContext()), apiEntityCaptor.capture());
        final List<String> allNameValues = apiEntityCaptor.getAllValues().stream().map(a -> a.getName()).collect(Collectors.toList());
        assertEquals(5, allNameValues.size());
        assertTrue(allNameValues.containsAll(Arrays.asList("1", "3", "4", "5", "6")));

        ApisResponse apiResponse = response.readEntity(ApisResponse.class);
        assertEquals(5, apiResponse.getData().size());
        assertEquals(0, getmaxLabelsListSize(apiResponse));
    }

    @Test
    public void shouldGetMetaDataForAllApis() {
        final Response response = target().queryParam("size", 0).request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        ArgumentCaptor<ApiEntity> apiEntityCaptor = ArgumentCaptor.forClass(ApiEntity.class);
        Mockito.verify(apiMapper, Mockito.times(0)).convert(eq(GraviteeContext.getExecutionContext()), apiEntityCaptor.capture());
        final List<String> allNameValues = apiEntityCaptor.getAllValues().stream().map(a -> a.getName()).collect(Collectors.toList());
        assertEquals(0, allNameValues.size());

        ApisResponse apiResponse = response.readEntity(ApisResponse.class);
        assertEquals(0, apiResponse.getData().size());
        Map<String, Object> metadata = apiResponse.getMetadata().get(AbstractResource.METADATA_DATA_KEY);
        assertEquals(5, metadata.get(AbstractResource.METADATA_DATA_TOTAL_KEY));
    }

    @Test
    public void shouldGetNoApi() {
        final Response response = target().queryParam("page", 10).queryParam("size", 1).request().get();
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());

        ErrorResponse errorResponse = response.readEntity(ErrorResponse.class);
        List<Error> errors = errorResponse.getErrors();
        assertNotNull(errors);
        assertEquals(1, errors.size());

        Error error = errors.get(0);
        assertEquals("errors.pagination.invalid", error.getCode());
        assertEquals("400", error.getStatus());
        assertEquals("Pagination is not valid", error.getMessage());
    }

    @Test
    public void shouldGetNoPublishedApiAndNoLink() {
        when(apiAuthorizationService.findAccessibleApiIdsForUser(eq(GraviteeContext.getExecutionContext()), any(), any(Set.class)))
            .thenReturn(Collections.emptySet());
        when(filteringService.filterApis(any(), any(), any(), any(), any())).thenReturn(Collections.emptySet());

        // Test with default limit
        final Response response = target().request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        ApisResponse apiResponse = response.readEntity(ApisResponse.class);
        assertEquals(0, apiResponse.getData().size());

        Links links = apiResponse.getLinks();
        assertNull(links);

        // Test with small limit
        final Response anotherResponse = target().queryParam("page", 2).queryParam("size", 1).request().get();
        assertEquals(HttpStatusCode.OK_200, anotherResponse.getStatus());

        apiResponse = anotherResponse.readEntity(ApisResponse.class);
        assertEquals(0, apiResponse.getData().size());

        links = apiResponse.getLinks();
        assertNull(links);
    }

    @Test
    public void shouldSearchApis() throws TechnicalException {
        doReturn(new HashSet<>(List.of("3")))
            .when(filteringService)
            .searchApis(eq(GraviteeContext.getExecutionContext()), any(), any(), any());

        final Response response = target("/_search").queryParam("q", "3").request().post(Entity.json(null));
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        ApisResponse apiResponse = response.readEntity(ApisResponse.class);
        assertEquals(1, apiResponse.getData().size());
        assertTrue(getmaxLabelsListSize(apiResponse) > 0);
    }

    @Test
    public void shouldSearchApisWithCategory() throws TechnicalException {
        doReturn(new HashSet<>(List.of("3")))
            .when(filteringService)
            .searchApis(eq(GraviteeContext.getExecutionContext()), any(), any(), any());

        final Response response = target("/_search").queryParam("q", "3").queryParam("category", "12345").request().post(Entity.json(null));
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        ApisResponse apiResponse = response.readEntity(ApisResponse.class);
        assertEquals(1, apiResponse.getData().size());
        assertTrue(getmaxLabelsListSize(apiResponse) > 0);
    }

    @Test
    public void shouldSearchApisWithEmptyQ() throws TechnicalException {
        doReturn(new HashSet<>(List.of("3")))
            .when(filteringService)
            .searchApis(eq(GraviteeContext.getExecutionContext()), any(), any(), any());

        final Response response = target("/_search").queryParam("q", "").request().post(Entity.json(null));
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        ApisResponse apiResponse = response.readEntity(ApisResponse.class);
        assertEquals(1, apiResponse.getData().size());
        assertTrue(getmaxLabelsListSize(apiResponse) > 0);
    }

    @Test
    public void shouldSearchApisWithNoQParameter() throws TechnicalException {
        doReturn(new HashSet<>(List.of("3")))
            .when(filteringService)
            .searchApis(eq(GraviteeContext.getExecutionContext()), any(), any(), any());

        final Response response = target("/_search").request().post(Entity.json(null));
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        ApisResponse apiResponse = response.readEntity(ApisResponse.class);
        assertEquals(1, apiResponse.getData().size());
        assertTrue(getmaxLabelsListSize(apiResponse) > 0);
    }

    @Test
    public void shouldSearchApisWithoutLabel() throws TechnicalException {
        when(
            parameterService.findAsBoolean(
                GraviteeContext.getExecutionContext(),
                Key.PORTAL_APIS_SHOW_TAGS_IN_APIHEADER,
                ParameterReferenceType.ENVIRONMENT
            )
        )
            .thenReturn(false);

        doReturn(new HashSet<>(List.of("3")))
            .when(filteringService)
            .searchApis(eq(GraviteeContext.getExecutionContext()), any(), any(), any());
        final Response response = target("/_search").queryParam("q", "3").request().post(Entity.json(null));
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        ArgumentCaptor<String> basePathCaptor = ArgumentCaptor.forClass(String.class);
        Mockito
            .verify(apiMapper, Mockito.times(1))
            .computeApiLinks(basePathCaptor.capture(), ArgumentCaptor.forClass(Date.class).capture());
        final String expectedBasePath = target().getUri().toString();
        final List<String> bastPathList = basePathCaptor.getAllValues();
        assertTrue(bastPathList.contains(expectedBasePath + "/3"));

        ArgumentCaptor<ApiEntity> apiEntityCaptor = ArgumentCaptor.forClass(ApiEntity.class);
        Mockito.verify(apiMapper, Mockito.times(1)).convert(eq(GraviteeContext.getExecutionContext()), apiEntityCaptor.capture());
        assertEquals("3", apiEntityCaptor.getValue().getName());

        ApisResponse apiResponse = response.readEntity(ApisResponse.class);
        assertEquals(1, apiResponse.getData().size());
        assertTrue(getmaxLabelsListSize(apiResponse) == 0);
    }

    @Test
    public void shouldPrioritizeApisContainingQueryInTheirNames() throws TechnicalException {
        doReturn(new HashSet<>(List.of("1", "3", "4")))
            .when(filteringService)
            .searchApis(eq(GraviteeContext.getExecutionContext()), any(), any(), any());

        ApiEntity api1 = new ApiEntity();
        api1.setContextPath("/get13");
        api1.setName("Hello");
        api1.setId("1");

        ApiEntity api3 = new ApiEntity();
        api3.setName("API 3");
        api3.setId("3");

        ApiEntity api4 = new ApiEntity();
        api4.setDescription("This also contains 3");
        api4.setName("API 4");
        api4.setId("4");

        ApiQuery pageQuery = new ApiQuery();
        pageQuery.setIds(List.of("1", "3", "4"));

        doReturn(List.of(api1, api3, api4)).when(apiSearchService).search(eq(GraviteeContext.getExecutionContext()), eq(pageQuery));

        final Response response = target("/_search").queryParam("q", "3").request().post(Entity.json(null));

        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        ApisResponse apiResponse = response.readEntity(ApisResponse.class);

        assertEquals(3, apiResponse.getData().size());
        assertEquals("API 3", apiResponse.getData().get(0).getName());
    }

    @Test
    public void shouldNotPrioritizeWhenApiNameDoesNotContainsQuery() throws TechnicalException {
        doReturn(new HashSet<>(List.of("3", "4")))
            .when(filteringService)
            .searchApis(eq(GraviteeContext.getExecutionContext()), any(), any(), any());

        final Response response = target("/_search").queryParam("q", "no-match").request().post(Entity.json(null));

        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        ApisResponse apiResponse = response.readEntity(ApisResponse.class);
        assertEquals(2, apiResponse.getData().size());

        List<String> ids = apiResponse.getData().stream().map(entity -> entity.getName()).collect(Collectors.toList());
        assertEquals("3", ids.get(0));
        assertEquals("4", ids.get(1));
    }

    @Test
    public void shouldReturnInternalServerErrorWhenExceptionThrown() throws TechnicalException {
        doThrow(new TechnicalException("Service failure"))
            .when(filteringService)
            .searchApis(eq(GraviteeContext.getExecutionContext()), any(), any(), any());

        final Response response = target("/_search").queryParam("q", "test").request().post(Entity.json(null));

        assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR_500, response.getStatus());

        String errorMessage = response.readEntity(String.class);
        assertTrue(errorMessage.contains("Service failure"));
    }

    @Test
    public void shouldHaveAllButPromotedApiIfNoCategory() throws TechnicalException {
        final Response response = target().queryParam("size", 2).queryParam("promoted", false).request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        ArgumentCaptor<ApiEntity> apiEntityCaptor = ArgumentCaptor.forClass(ApiEntity.class);
        Mockito.verify(apiMapper, Mockito.times(2)).convert(eq(GraviteeContext.getExecutionContext()), apiEntityCaptor.capture());
        final List<String> allNameValues = apiEntityCaptor.getAllValues().stream().map(a -> a.getName()).collect(Collectors.toList());
        assertEquals(2, allNameValues.size());
        assertTrue(allNameValues.containsAll(Arrays.asList("3", "4")));

        ApisResponse apiResponse = response.readEntity(ApisResponse.class);
        assertEquals(2, apiResponse.getData().size());
        assertTrue(getmaxLabelsListSize(apiResponse) > 0);
    }

    @Test
    public void shouldHaveAllButPromotedApiIfCategoryWithoutHighLightedApi() throws TechnicalException {
        doReturn(new CategoryEntity()).when(categoryService).findById("myCat", GraviteeContext.getCurrentEnvironment());

        final Response response = target()
            .queryParam("size", 3)
            .queryParam("promoted", false)
            .queryParam("category", "myCat")
            .request()
            .get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        ArgumentCaptor<ApiEntity> apiEntityCaptor = ArgumentCaptor.forClass(ApiEntity.class);
        Mockito.verify(apiMapper, Mockito.times(3)).convert(eq(GraviteeContext.getExecutionContext()), apiEntityCaptor.capture());
        final List<String> allNameValues = apiEntityCaptor.getAllValues().stream().map(a -> a.getName()).collect(Collectors.toList());
        assertEquals(3, allNameValues.size());
        assertTrue(allNameValues.containsAll(Arrays.asList("3", "4", "5")));

        ApisResponse apiResponse = response.readEntity(ApisResponse.class);
        assertEquals(3, apiResponse.getData().size());
        assertTrue(getmaxLabelsListSize(apiResponse) > 0);
    }

    @Test
    public void shouldHaveAllButPromotedApiIfCategoryWithHighLightedApi() throws TechnicalException {
        CategoryEntity myCatEntity = new CategoryEntity();
        myCatEntity.setHighlightApi("4");
        doReturn(myCatEntity).when(categoryService).findById("myCat", GraviteeContext.getCurrentEnvironment());

        final Response response = target()
            .queryParam("size", 3)
            .queryParam("promoted", false)
            .queryParam("category", "myCat")
            .request()
            .get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        ArgumentCaptor<ApiEntity> apiEntityCaptor = ArgumentCaptor.forClass(ApiEntity.class);
        Mockito.verify(apiMapper, Mockito.times(3)).convert(eq(GraviteeContext.getExecutionContext()), apiEntityCaptor.capture());
        final List<String> allNameValues = apiEntityCaptor.getAllValues().stream().map(a -> a.getName()).collect(Collectors.toList());
        assertEquals(3, allNameValues.size());
        assertTrue(allNameValues.containsAll(Arrays.asList("1", "3", "5")));

        ApisResponse apiResponse = response.readEntity(ApisResponse.class);
        assertEquals(3, apiResponse.getData().size());
        assertTrue(getmaxLabelsListSize(apiResponse) > 0);
    }

    @Test
    public void shouldHaveAllButPromotedApiIfCategoryWithHighLightedApiNotInFilteredList() throws TechnicalException {
        CategoryEntity myCatEntity = new CategoryEntity();
        myCatEntity.setHighlightApi("7");
        doReturn(myCatEntity).when(categoryService).findById("myCat", GraviteeContext.getCurrentEnvironment());

        final Response response = target()
            .queryParam("size", 3)
            .queryParam("promoted", false)
            .queryParam("category", "myCat")
            .request()
            .get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        ArgumentCaptor<ApiEntity> apiEntityCaptor = ArgumentCaptor.forClass(ApiEntity.class);
        Mockito.verify(apiMapper, Mockito.times(3)).convert(eq(GraviteeContext.getExecutionContext()), apiEntityCaptor.capture());
        final List<String> allNameValues = apiEntityCaptor.getAllValues().stream().map(a -> a.getName()).collect(Collectors.toList());
        assertEquals(3, allNameValues.size());
        assertTrue(allNameValues.containsAll(Arrays.asList("3", "4", "5")));

        ApisResponse apiResponse = response.readEntity(ApisResponse.class);
        assertEquals(3, apiResponse.getData().size());
        assertTrue(getmaxLabelsListSize(apiResponse) > 0);
    }

    @Test
    public void shouldHavePromotedApiIfNoCategory() throws TechnicalException {
        final Response response = target().queryParam("size", 2).queryParam("promoted", true).request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        ArgumentCaptor<ApiEntity> apiEntityCaptor = ArgumentCaptor.forClass(ApiEntity.class);
        Mockito.verify(apiMapper, Mockito.times(1)).convert(eq(GraviteeContext.getExecutionContext()), apiEntityCaptor.capture());
        final List<String> allNameValues = apiEntityCaptor.getAllValues().stream().map(a -> a.getName()).collect(Collectors.toList());
        assertEquals(1, allNameValues.size());
        assertTrue(allNameValues.containsAll(Arrays.asList("1")));

        ApisResponse apiResponse = response.readEntity(ApisResponse.class);
        assertEquals(1, apiResponse.getData().size());
        assertTrue(getmaxLabelsListSize(apiResponse) > 0);
    }

    @Test
    public void shouldHavePromotedApiIfCategoryWithoutHighLightedApi() throws TechnicalException {
        doReturn(new CategoryEntity()).when(categoryService).findById("myCat", GraviteeContext.getCurrentEnvironment());

        final Response response = target()
            .queryParam("size", 3)
            .queryParam("promoted", true)
            .queryParam("category", "myCat")
            .request()
            .get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        ArgumentCaptor<ApiEntity> apiEntityCaptor = ArgumentCaptor.forClass(ApiEntity.class);
        Mockito.verify(apiMapper, Mockito.times(1)).convert(eq(GraviteeContext.getExecutionContext()), apiEntityCaptor.capture());
        final List<String> allNameValues = apiEntityCaptor.getAllValues().stream().map(a -> a.getName()).collect(Collectors.toList());
        assertEquals(1, allNameValues.size());
        assertTrue(allNameValues.containsAll(Arrays.asList("1")));

        ApisResponse apiResponse = response.readEntity(ApisResponse.class);
        assertEquals(1, apiResponse.getData().size());
        assertTrue(getmaxLabelsListSize(apiResponse) > 0);
    }

    @Test
    public void shouldHavePromotedApiIfCategoryWithHighLightedApi() throws TechnicalException {
        CategoryEntity myCatEntity = new CategoryEntity();
        myCatEntity.setHighlightApi("4");
        doReturn(myCatEntity).when(categoryService).findById("myCat", GraviteeContext.getCurrentEnvironment());

        final Response response = target()
            .queryParam("size", 3)
            .queryParam("promoted", true)
            .queryParam("category", "myCat")
            .request()
            .get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        ArgumentCaptor<ApiEntity> apiEntityCaptor = ArgumentCaptor.forClass(ApiEntity.class);
        Mockito.verify(apiMapper, Mockito.times(1)).convert(eq(GraviteeContext.getExecutionContext()), apiEntityCaptor.capture());
        final List<String> allNameValues = apiEntityCaptor.getAllValues().stream().map(a -> a.getName()).collect(Collectors.toList());
        assertEquals(1, allNameValues.size());
        assertTrue(allNameValues.containsAll(Arrays.asList("4")));

        ApisResponse apiResponse = response.readEntity(ApisResponse.class);
        assertEquals(1, apiResponse.getData().size());
        assertTrue(getmaxLabelsListSize(apiResponse) > 0);
    }

    @Test
    public void shouldHavePromotedApiIfCategoryWithHighLightedApiNotInFilteredList() throws TechnicalException {
        CategoryEntity myCatEntity = new CategoryEntity();
        myCatEntity.setHighlightApi("7");
        doReturn(myCatEntity).when(categoryService).findById("myCat", GraviteeContext.getCurrentEnvironment());

        final Response response = target()
            .queryParam("size", 3)
            .queryParam("promoted", true)
            .queryParam("category", "myCat")
            .request()
            .get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        ArgumentCaptor<ApiEntity> apiEntityCaptor = ArgumentCaptor.forClass(ApiEntity.class);
        Mockito.verify(apiMapper, Mockito.times(1)).convert(eq(GraviteeContext.getExecutionContext()), apiEntityCaptor.capture());
        final List<String> allNameValues = apiEntityCaptor.getAllValues().stream().map(a -> a.getName()).collect(Collectors.toList());
        assertEquals(1, allNameValues.size());
        assertTrue(allNameValues.containsAll(Arrays.asList("1")));

        ApisResponse apiResponse = response.readEntity(ApisResponse.class);
        assertEquals(1, apiResponse.getData().size());
        assertTrue(getmaxLabelsListSize(apiResponse) > 0);
    }

    private int getmaxLabelsListSize(ApisResponse apiResponse) {
        return apiResponse
            .getData()
            .stream()
            .map(api -> {
                if (api.getLabels() != null) {
                    return api.getLabels().size();
                } else {
                    return 0;
                }
            })
            .max(Comparator.comparingInt(i -> i))
            .orElse(0);
    }
}

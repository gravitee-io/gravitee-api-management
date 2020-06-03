/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.portal.rest.resource;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.api.ApiLifecycleState;
import io.gravitee.rest.api.model.api.ApiQuery;
import io.gravitee.rest.api.model.filtering.FilteredEntities;
import io.gravitee.rest.api.portal.rest.model.Error;
import io.gravitee.rest.api.portal.rest.model.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApisResourceTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "apis";
    }

    @Before
    public void init() {
        resetAllMocks();

        ApiEntity publishedApi1 = new ApiEntity();
        publishedApi1.setLifecycleState(ApiLifecycleState.PUBLISHED);
        publishedApi1.setName("1");
        publishedApi1.setId("1");

        ApiEntity unpublishedApi = new ApiEntity();
        unpublishedApi.setLifecycleState(ApiLifecycleState.UNPUBLISHED);
        unpublishedApi.setName("2");
        unpublishedApi.setId("2");

        ApiEntity publishedApi2 = new ApiEntity();
        publishedApi2.setLifecycleState(ApiLifecycleState.PUBLISHED);
        publishedApi2.setName("3");
        publishedApi2.setId("3");

        ApiEntity publishedApi3 = new ApiEntity();
        publishedApi3.setLifecycleState(ApiLifecycleState.PUBLISHED);
        publishedApi3.setName("4");
        publishedApi3.setId("4");

        ApiEntity publishedApi4 = new ApiEntity();
        publishedApi4.setLifecycleState(ApiLifecycleState.PUBLISHED);
        publishedApi4.setName("5");
        publishedApi4.setId("5");

        ApiEntity publishedApi5 = new ApiEntity();
        publishedApi5.setLifecycleState(ApiLifecycleState.PUBLISHED);
        publishedApi5.setName("6");
        publishedApi5.setId("6");

        Set<ApiEntity> mockApis = new HashSet<>(
                Arrays.asList(publishedApi5, publishedApi2, publishedApi1, publishedApi3, publishedApi4));
        doReturn(mockApis).when(apiService).findPublishedByUser(any(), any());

        doReturn(false).when(ratingService).isEnabled();

        doReturn(new FilteredEntities<ApiEntity>(new ArrayList<>(mockApis), null)).when(filteringService).filterApis(any(), any(), any());

        doReturn(new Api().name("1").id("1")).when(apiMapper).convert(publishedApi1);
        doReturn(new Api().name("2").id("2")).when(apiMapper).convert(unpublishedApi);
        doReturn(new Api().name("3").id("3")).when(apiMapper).convert(publishedApi2);
        doReturn(new Api().name("4").id("4")).when(apiMapper).convert(publishedApi3);
        doReturn(new Api().name("5").id("5")).when(apiMapper).convert(publishedApi4);
        doReturn(new Api().name("6").id("6")).when(apiMapper).convert(publishedApi5);

    }

    @Test
    public void shouldGetPublishedApi() {
        final Response response = target().queryParam("context-path", "context-path").queryParam("label", "label")
                .queryParam("version", "version").queryParam("name", "name").queryParam("tag", "tag").request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        ArgumentCaptor<ApiQuery> queryCaptor = ArgumentCaptor.forClass(ApiQuery.class);
        Mockito.verify(apiService).findPublishedByUser(eq(USER_NAME), queryCaptor.capture());
        final ApiQuery query = queryCaptor.getValue();
        assertEquals("context-path", query.getContextPath());
        assertEquals("label", query.getLabel());
        assertEquals("version", query.getVersion());
        assertEquals("name", query.getName());
        assertEquals("tag", query.getTag());

        ArgumentCaptor<String> basePathCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(apiMapper, Mockito.times(5)).computeApiLinks(basePathCaptor.capture());
        final String expectedBasePath = target().getUri().toString();
        final List<String> bastPathList = basePathCaptor.getAllValues();
        assertTrue(bastPathList.contains(expectedBasePath + "/1"));
        assertTrue(bastPathList.contains(expectedBasePath + "/3"));
        assertTrue(bastPathList.contains(expectedBasePath + "/4"));
        assertTrue(bastPathList.contains(expectedBasePath + "/5"));
        assertTrue(bastPathList.contains(expectedBasePath + "/6"));

        ArgumentCaptor<ApiEntity> apiEntityCaptor = ArgumentCaptor.forClass(ApiEntity.class);
        Mockito.verify(apiMapper, Mockito.times(5)).convert(apiEntityCaptor.capture());
        final List<String> allNameValues = apiEntityCaptor.getAllValues().stream().map(a -> a.getName())
                .collect(Collectors.toList());
        assertEquals(5, allNameValues.size());
        assertTrue(allNameValues.containsAll(Arrays.asList("1", "3", "4", "5", "6")));

        ApisResponse apiResponse = response.readEntity(ApisResponse.class);
        assertEquals(5, apiResponse.getData().size());

    }


    @Test
    public void shouldGetPublishedApiWithPaginatedLink() {
        final Response response = target().queryParam("page", 3).queryParam("size", 1).request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        ArgumentCaptor<ApiEntity> apiEntityCaptor = ArgumentCaptor.forClass(ApiEntity.class);
        Mockito.verify(apiMapper, Mockito.times(5)).convert(apiEntityCaptor.capture());
        final List<String> allNameValues = apiEntityCaptor.getAllValues().stream().map(a -> a.getName())
                .collect(Collectors.toList());
        assertEquals(5, allNameValues.size());
        assertTrue(allNameValues.containsAll(Arrays.asList("1", "3", "4", "5", "6")));

        ApisResponse apiResponse = response.readEntity(ApisResponse.class);
        assertEquals(1, apiResponse.getData().size());

        Links links = apiResponse.getLinks();
        assertNotNull(links);
    }

    @Test
    public void shouldGetAllApis() {
        final Response response = target().queryParam("size", -1).request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        ArgumentCaptor<ApiEntity> apiEntityCaptor = ArgumentCaptor.forClass(ApiEntity.class);
        Mockito.verify(apiMapper, Mockito.times(5)).convert(apiEntityCaptor.capture());
        final List<String> allNameValues = apiEntityCaptor.getAllValues().stream().map(a -> a.getName())
                .collect(Collectors.toList());
        assertEquals(5, allNameValues.size());
        assertTrue(allNameValues.containsAll(Arrays.asList("1", "3", "4", "5", "6")));

        ApisResponse apiResponse = response.readEntity(ApisResponse.class);
        assertEquals(5, apiResponse.getData().size());
    }

    @Test
    public void shouldGetMetaDataForAllApis() {
        final Response response = target().queryParam("size", 0).request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        ArgumentCaptor<ApiEntity> apiEntityCaptor = ArgumentCaptor.forClass(ApiEntity.class);
        Mockito.verify(apiMapper, Mockito.times(5)).convert(apiEntityCaptor.capture());
        final List<String> allNameValues = apiEntityCaptor.getAllValues().stream().map(a -> a.getName())
                .collect(Collectors.toList());
        assertEquals(5, allNameValues.size());
        assertTrue(allNameValues.containsAll(Arrays.asList("1", "3", "4", "5", "6")));

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

        doReturn(Collections.emptySet()).when(apiService).findPublishedByUser(any(), any());
        doReturn(new FilteredEntities<ApiEntity>(Collections.emptyList(), null)).when(filteringService).filterApis(any(), any(), any());

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
        ApiEntity searchedApi = new ApiEntity();
        searchedApi.setLifecycleState(ApiLifecycleState.PUBLISHED);
        searchedApi.setName("3");
        searchedApi.setId("3");

        doReturn(new HashSet<>(Arrays.asList(searchedApi))).when(apiService).search(any(), any());
        final Response response = target("/_search").queryParam("q", "3").request().post(Entity.json(null));
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        ArgumentCaptor<ApiQuery> queryCaptor = ArgumentCaptor.forClass(ApiQuery.class);
        Mockito.verify(apiService).findPublishedByUser(eq(USER_NAME), queryCaptor.capture());
        final ApiQuery query = queryCaptor.getValue();
        assertNull(query.getContextPath());
        assertNull(query.getLabel());
        assertNull(query.getVersion());
        assertNull(query.getName());
        assertNull(query.getTag());

        ArgumentCaptor<String> basePathCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(apiMapper, Mockito.times(1)).computeApiLinks(basePathCaptor.capture());
        final String expectedBasePath = target().getUri().toString();
        final List<String> bastPathList = basePathCaptor.getAllValues();
        assertTrue(bastPathList.contains(expectedBasePath + "/3"));

        ArgumentCaptor<ApiEntity> apiEntityCaptor = ArgumentCaptor.forClass(ApiEntity.class);
        Mockito.verify(apiMapper, Mockito.times(1)).convert(apiEntityCaptor.capture());
        assertEquals("3", apiEntityCaptor.getValue().getName());

        ApisResponse apiResponse = response.readEntity(ApisResponse.class);
        assertEquals(1, apiResponse.getData().size());
    }
}

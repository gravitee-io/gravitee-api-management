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
package io.gravitee.rest.api.management.v2.rest.resource.api;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.gravitee.common.component.Lifecycle;
import io.gravitee.common.data.domain.Page;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.rest.api.management.v2.rest.model.ApiSearchQuery;
import io.gravitee.rest.api.management.v2.rest.model.ApisResponse;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.model.common.SortableImpl;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.search.query.QueryBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.mockito.ArgumentCaptor;

public class ApisResource_SearchApisTest extends AbstractResourceTest {

    private static final String ENVIRONMENT = "fake-env";

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis/_search";
    }

    @Before
    public void init() {
        GraviteeContext.cleanContext();
        GraviteeContext.setCurrentOrganization(ORGANIZATION);
        GraviteeContext.setCurrentEnvironment(ENVIRONMENT);

        EnvironmentEntity environment = new EnvironmentEntity();
        environment.setId(ENVIRONMENT);
        environment.setOrganizationId(ORGANIZATION);

        doReturn(environment).when(environmentService).findById(ENVIRONMENT);
        doReturn(environment).when(environmentService).findByOrgAndIdOrHrid(ORGANIZATION, ENVIRONMENT);
    }

    @Test
    public void should_not_search_if_no_params() {
        final Response response = rootTarget().request().post(null);
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
    }

    @Test
    public void should_search_with_empty_query() {
        var apiSearchQuery = new ApiSearchQuery();
        apiSearchQuery.setQuery("");

        var apiEntity = new ApiEntity();
        apiEntity.setId("api-id");
        apiEntity.setState(Lifecycle.State.INITIALIZED);
        apiEntity.setDefinitionVersion(DefinitionVersion.V4);

        ArgumentCaptor<QueryBuilder<ApiEntity>> apiQueryBuilderCaptor = ArgumentCaptor.forClass(QueryBuilder.class);

        when(
            apiSearchServiceV4.search(
                eq(GraviteeContext.getExecutionContext()),
                eq("UnitTests"),
                eq(true),
                apiQueryBuilderCaptor.capture(),
                eq(new PageableImpl(1, 10))
            )
        )
            .thenReturn(new Page<>(List.of(apiEntity), 1, 1, 1));

        final Response response = rootTarget().request().post(Entity.json(apiSearchQuery));
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        var page = response.readEntity(ApisResponse.class);
        var data = page.getData();
        assertNotNull(data);
        assertEquals(1, data.size());
        assertEquals("api-id", data.get(0).getApiV4().getId());

        var apiQueryBuilder = apiQueryBuilderCaptor.getValue();
        var apiQuery = apiQueryBuilder.build();
        assertNull(apiQuery.getQuery());
        assertEquals(0, apiQuery.getFilters().size());
    }

    @Test
    public void should_search_with_valid_query() {
        var apiSearchQuery = new ApiSearchQuery();
        apiSearchQuery.setQuery("api-name");

        var apiEntity = new ApiEntity();
        apiEntity.setId("api-id");
        apiEntity.setState(Lifecycle.State.INITIALIZED);
        apiEntity.setDefinitionVersion(DefinitionVersion.V4);

        ArgumentCaptor<QueryBuilder<ApiEntity>> apiQueryBuilderCaptor = ArgumentCaptor.forClass(QueryBuilder.class);

        when(
            apiSearchServiceV4.search(
                eq(GraviteeContext.getExecutionContext()),
                eq("UnitTests"),
                eq(true),
                apiQueryBuilderCaptor.capture(),
                eq(new PageableImpl(1, 10))
            )
        )
            .thenReturn(new Page<>(List.of(apiEntity), 1, 1, 1));

        final Response response = rootTarget().request().post(Entity.json(apiSearchQuery));
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        var page = response.readEntity(ApisResponse.class);
        var data = page.getData();
        assertNotNull(data);
        assertEquals(1, data.size());
        assertEquals("api-id", data.get(0).getApiV4().getId());

        var apiQueryBuilder = apiQueryBuilderCaptor.getValue();
        var apiQuery = apiQueryBuilder.build();
        assertEquals("api-name", apiQuery.getQuery());
        assertEquals(0, apiQuery.getFilters().size());
    }

    @Test
    public void should_not_search_with_empty_ids() {
        var apiSearchQuery = new ApiSearchQuery();
        apiSearchQuery.setIds(List.of());

        var apiEntity = new ApiEntity();
        apiEntity.setId("api-id");
        apiEntity.setState(Lifecycle.State.INITIALIZED);
        apiEntity.setDefinitionVersion(DefinitionVersion.V4);

        ArgumentCaptor<QueryBuilder<ApiEntity>> apiQueryBuilderCaptor = ArgumentCaptor.forClass(QueryBuilder.class);

        when(
            apiSearchServiceV4.search(
                eq(GraviteeContext.getExecutionContext()),
                eq("UnitTests"),
                eq(true),
                apiQueryBuilderCaptor.capture(),
                eq(new PageableImpl(1, 10))
            )
        )
            .thenReturn(new Page<>(List.of(apiEntity), 1, 1, 1));

        final Response response = rootTarget().request().post(Entity.json(apiSearchQuery));
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        var page = response.readEntity(ApisResponse.class);
        var data = page.getData();
        assertNotNull(data);
        assertEquals(1, data.size());
        assertEquals("api-id", data.get(0).getApiV4().getId());

        var apiQueryBuilder = apiQueryBuilderCaptor.getValue();
        var apiQuery = apiQueryBuilder.build();
        assertEquals(0, apiQuery.getFilters().size());
    }

    @Test
    public void should_search_with_ids() {
        var apiSearchQuery = new ApiSearchQuery();
        apiSearchQuery.setIds(List.of("id-1", "id-2"));

        var apiEntity = new ApiEntity();
        apiEntity.setId("id-1");
        apiEntity.setState(Lifecycle.State.INITIALIZED);
        apiEntity.setDefinitionVersion(DefinitionVersion.V4);

        ArgumentCaptor<QueryBuilder<ApiEntity>> apiQueryBuilderCaptor = ArgumentCaptor.forClass(QueryBuilder.class);

        when(
            apiSearchServiceV4.search(
                eq(GraviteeContext.getExecutionContext()),
                eq("UnitTests"),
                eq(true),
                apiQueryBuilderCaptor.capture(),
                eq(new PageableImpl(1, 10))
            )
        )
            .thenReturn(new Page<>(List.of(apiEntity), 1, 1, 1));

        final Response response = rootTarget().request().post(Entity.json(apiSearchQuery));
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        var page = response.readEntity(ApisResponse.class);
        var data = page.getData();
        assertNotNull(data);
        assertEquals(1, data.size());
        assertEquals("id-1", data.get(0).getApiV4().getId());

        var apiQueryBuilder = apiQueryBuilderCaptor.getValue();
        var apiQuery = apiQueryBuilder.build();
        assertNotNull(apiQuery.getFilters());
        assertNotNull(apiQuery.getFilters().get("api"));
        List<String> ids = (List<String>) apiQuery.getFilters().get("api");
        assertEquals(2, ids.size());
        assertEquals("id-2", ids.get(1));

        assert (!apiQuery.getFilters().containsKey("definition_version"));
    }

    @Test
    public void should_return_error_with_wrong_value_for_sort_by_param() {
        var apiSearchQuery = new ApiSearchQuery();
        apiSearchQuery.setIds(List.of("id-1", "id-2"));

        final Response response = rootTarget().queryParam("sortBy", "wrong_value").request().post(Entity.json(apiSearchQuery));
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());

        Map<String, Object> body = response.readEntity(Map.class);
        assert (String.valueOf(body.get("message")).endsWith("wrong_value"));
    }

    @Test
    public void should_sort_results_with_sort_by_param_asc() {
        var apiSearchQuery = new ApiSearchQuery();
        apiSearchQuery.setQuery("api-name");

        var apiEntity = new ApiEntity();
        apiEntity.setId("api-id");
        apiEntity.setState(Lifecycle.State.INITIALIZED);
        apiEntity.setDefinitionVersion(DefinitionVersion.V4);

        ArgumentCaptor<QueryBuilder<ApiEntity>> apiQueryBuilderCaptor = ArgumentCaptor.forClass(QueryBuilder.class);

        when(
            apiSearchServiceV4.search(
                eq(GraviteeContext.getExecutionContext()),
                eq("UnitTests"),
                eq(true),
                apiQueryBuilderCaptor.capture(),
                eq(new PageableImpl(1, 10))
            )
        )
            .thenReturn(new Page<>(List.of(apiEntity), 1, 1, 1));

        final Response response = rootTarget().queryParam("sortBy", "name").request().post(Entity.json(apiSearchQuery));
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        var page = response.readEntity(ApisResponse.class);
        var data = page.getData();
        assertNotNull(data);
        assertEquals(1, data.size());
        assertEquals("api-id", data.get(0).getApiV4().getId());

        var apiQueryBuilder = apiQueryBuilderCaptor.getValue();
        var apiQuery = apiQueryBuilder.build();
        assertEquals("api-name", apiQuery.getQuery());
        assertEquals(0, apiQuery.getFilters().size());
    }

    @Test
    public void should_sort_results_with_sort_by_param_desc() {
        var apiSearchQuery = new ApiSearchQuery();
        apiSearchQuery.setQuery("api-name");

        var apiEntity = new ApiEntity();
        apiEntity.setId("api-id");
        apiEntity.setState(Lifecycle.State.INITIALIZED);
        apiEntity.setDefinitionVersion(DefinitionVersion.V4);

        ArgumentCaptor<QueryBuilder<ApiEntity>> apiQueryBuilderCaptor = ArgumentCaptor.forClass(QueryBuilder.class);

        when(
            apiSearchServiceV4.search(
                eq(GraviteeContext.getExecutionContext()),
                eq("UnitTests"),
                eq(true),
                apiQueryBuilderCaptor.capture(),
                eq(new PageableImpl(1, 10))
            )
        )
            .thenReturn(new Page<>(List.of(apiEntity), 1, 1, 1));

        final Response response = rootTarget().queryParam("sortBy", "-paths").request().post(Entity.json(apiSearchQuery));
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        var page = response.readEntity(ApisResponse.class);
        var data = page.getData();
        assertNotNull(data);
        assertEquals(1, data.size());
        assertEquals("api-id", data.get(0).getApiV4().getId());

        var apiQueryBuilder = apiQueryBuilderCaptor.getValue();
        var apiQuery = apiQueryBuilder.build();
        assertEquals("api-name", apiQuery.getQuery());
        assertEquals(0, apiQuery.getFilters().size());
        assertNotNull(apiQuery.getSort());
        assertEquals("paths", apiQuery.getSort().getField());
        assertEquals(false, apiQuery.getSort().isAscOrder());
    }

    @Test
    public void should_search_with_definition_version_param() {
        var apiSearchQuery = new ApiSearchQuery();
        apiSearchQuery.setDefinitionVersion(io.gravitee.rest.api.management.v2.rest.model.DefinitionVersion.V4);

        var apiEntity = new ApiEntity();
        apiEntity.setId("id-1");
        apiEntity.setState(Lifecycle.State.INITIALIZED);
        apiEntity.setDefinitionVersion(DefinitionVersion.V4);

        ArgumentCaptor<QueryBuilder<ApiEntity>> apiQueryBuilderCaptor = ArgumentCaptor.forClass(QueryBuilder.class);

        when(
            apiSearchServiceV4.search(
                eq(GraviteeContext.getExecutionContext()),
                eq("UnitTests"),
                eq(true),
                apiQueryBuilderCaptor.capture(),
                eq(new PageableImpl(1, 10))
            )
        )
            .thenReturn(new Page<>(List.of(apiEntity), 1, 1, 1));

        final Response response = rootTarget().request().post(Entity.json(apiSearchQuery));
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        var page = response.readEntity(ApisResponse.class);
        var data = page.getData();
        assertNotNull(data);
        assertEquals(1, data.size());
        assertEquals("id-1", data.get(0).getApiV4().getId());

        var apiQueryBuilder = apiQueryBuilderCaptor.getValue();
        var apiQuery = apiQueryBuilder.build();
        assertNotNull(apiQuery.getFilters());
        assertNotNull(apiQuery.getFilters().get("definition_version"));
        String definitionVersion = (String) apiQuery.getFilters().get("definition_version");
        assertEquals("4.0.0", definitionVersion);
        assertNull(apiQuery.getSort());
    }

    @Test
    public void should_sort_by_paths() {
        var apiSearchQuery = new ApiSearchQuery();
        apiSearchQuery.setDefinitionVersion(io.gravitee.rest.api.management.v2.rest.model.DefinitionVersion.V4);

        var apiEntity = new ApiEntity();
        apiEntity.setId("id-1");
        apiEntity.setState(Lifecycle.State.INITIALIZED);
        apiEntity.setDefinitionVersion(DefinitionVersion.V4);

        ArgumentCaptor<QueryBuilder<ApiEntity>> apiQueryBuilderCaptor = ArgumentCaptor.forClass(QueryBuilder.class);

        when(
            apiSearchServiceV4.search(
                eq(GraviteeContext.getExecutionContext()),
                eq("UnitTests"),
                eq(true),
                apiQueryBuilderCaptor.capture(),
                eq(new PageableImpl(1, 10))
            )
        )
            .thenReturn(new Page<>(List.of(apiEntity), 1, 1, 1));

        final Response response = rootTarget().queryParam("sortBy", "paths").request().post(Entity.json(apiSearchQuery));
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        var page = response.readEntity(ApisResponse.class);
        var data = page.getData();
        assertNotNull(data);
        assertEquals(1, data.size());
        assertEquals("id-1", data.get(0).getApiV4().getId());

        var apiQueryBuilder = apiQueryBuilderCaptor.getValue();
        var apiQuery = apiQueryBuilder.build();
        assertNotNull(apiQuery.getFilters());
        assertNotNull(apiQuery.getFilters().get("definition_version"));
        String definitionVersion = (String) apiQuery.getFilters().get("definition_version");
        assertEquals("4.0.0", definitionVersion);
        assertNotNull(apiQuery.getSort());
        assertEquals("paths", apiQuery.getSort().getField());
        assertEquals(true, apiQuery.getSort().isAscOrder());
    }
}

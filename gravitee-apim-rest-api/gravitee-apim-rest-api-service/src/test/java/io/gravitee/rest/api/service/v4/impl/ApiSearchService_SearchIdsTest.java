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
package io.gravitee.rest.api.service.v4.impl;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.data.domain.Page;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApiFieldFilter;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.LifecycleState;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.service.CategoryService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.WorkflowService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.converter.ApiConverter;
import io.gravitee.rest.api.service.converter.CategoryMapper;
import io.gravitee.rest.api.service.impl.search.SearchResult;
import io.gravitee.rest.api.service.search.SearchEngineService;
import io.gravitee.rest.api.service.search.query.QueryBuilder;
import io.gravitee.rest.api.service.v4.ApiAuthorizationService;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import io.gravitee.rest.api.service.v4.FlowService;
import io.gravitee.rest.api.service.v4.PlanService;
import io.gravitee.rest.api.service.v4.PrimaryOwnerService;
import io.gravitee.rest.api.service.v4.mapper.ApiMapper;
import io.gravitee.rest.api.service.v4.mapper.GenericApiMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@RunWith(MockitoJUnitRunner.class)
public class ApiSearchService_SearchIdsTest {

    private final String USER_ID = "user-1";

    @Mock
    private ApiRepository apiRepository;

    @Mock
    private SearchEngineService searchEngineService;

    @Mock
    private CategoryService categoryService;

    @Mock
    private PrimaryOwnerService primaryOwnerService;

    // ApiMapper injections
    @Mock
    private PlanService planService;

    @Mock
    private FlowService flowService;

    @Mock
    private ParameterService parameterService;

    @Mock
    private WorkflowService workflowService;

    @Mock
    private ApiAuthorizationService apiAuthorizationService;

    private ApiSearchService apiSearchService;

    @AfterClass
    public static void cleanSecurityContextHolder() {
        // reset authentication to avoid side effect during test executions.
        SecurityContextHolder.setContext(
            new SecurityContext() {
                @Override
                public Authentication getAuthentication() {
                    return null;
                }

                @Override
                public void setAuthentication(Authentication authentication) {}
            }
        );
    }

    @Before
    public void setUp() {
        ApiMapper apiMapper = new ApiMapper(
            new ObjectMapper(),
            planService,
            flowService,
            parameterService,
            workflowService,
            new CategoryMapper(categoryService)
        );
        ApiConverter apiConverter = new ApiConverter(
            new GraviteeMapper(),
            Mockito.mock(io.gravitee.rest.api.service.PlanService.class),
            Mockito.mock(io.gravitee.rest.api.service.configuration.flow.FlowService.class),
            new CategoryMapper(categoryService),
            parameterService,
            workflowService
        );
        apiSearchService =
            new ApiSearchServiceImpl(
                apiRepository,
                apiMapper,
                new GenericApiMapper(apiMapper, apiConverter),
                primaryOwnerService,
                categoryService,
                searchEngineService,
                apiAuthorizationService
            );
    }

    @Test
    public void should_return_empty_page_if_no_results() {
        QueryBuilder<GenericApiEntity> apiEntityQueryBuilder = QueryBuilder.create(GenericApiEntity.class).setQuery("*").setSort(null);
        var ids = List.of("id-1", "id-2");

        var filters = new HashMap<String, Object>();
        filters.put("api", ids);
        apiEntityQueryBuilder.setFilters(filters);

        when(searchEngineService.search(eq(GraviteeContext.getExecutionContext()), eq(apiEntityQueryBuilder.build())))
            .thenReturn(new SearchResult(List.of()));

        final var apis = apiSearchService.searchIds(GraviteeContext.getExecutionContext(), "*", filters, null, false);

        assertThat(apis).isNotNull();
        assertThat(apis.size()).isEqualTo(0);
    }

    @Test
    public void should_return_api_ids() {
        QueryBuilder<GenericApiEntity> apiEntityQueryBuilder = QueryBuilder.create(GenericApiEntity.class).setQuery("*").setSort(null);
        var ids = List.of("id-1", "id-2");

        var filters = new HashMap<String, Object>();
        filters.put("api", ids);
        apiEntityQueryBuilder.setFilters(filters);

        when(searchEngineService.search(eq(GraviteeContext.getExecutionContext()), eq(apiEntityQueryBuilder.build())))
            .thenReturn(new SearchResult(List.of("api-id")));

        final var apis = apiSearchService.searchIds(GraviteeContext.getExecutionContext(), "*", filters, null, false);

        assertThat(apis).isNotNull();
        assertThat(apis.size()).isEqualTo(1);
        assertThat(apis).isEqualTo(List.of("api-id"));
    }

    @Test
    public void should_not_add_empty_query() {
        QueryBuilder<GenericApiEntity> apiEntityQueryBuilder = QueryBuilder.create(GenericApiEntity.class).setSort(null);
        var ids = List.of("id-1", "id-2");

        var filters = new HashMap<String, Object>();
        filters.put("api", ids);
        apiEntityQueryBuilder.setFilters(filters);

        when(searchEngineService.search(eq(GraviteeContext.getExecutionContext()), eq(apiEntityQueryBuilder.build())))
            .thenReturn(new SearchResult(List.of("api-id")));

        final var apis = apiSearchService.searchIds(GraviteeContext.getExecutionContext(), "", filters, null, false);

        assertThat(apis).isNotNull();
        assertThat(apis.size()).isEqualTo(1);
        assertThat(apis).isEqualTo(List.of("api-id"));
    }

    @Test
    public void should_allow_null_query() {
        QueryBuilder<GenericApiEntity> apiEntityQueryBuilder = QueryBuilder.create(GenericApiEntity.class).setQuery(null).setSort(null);
        var ids = List.of("id-1", "id-2");

        var filters = new HashMap<String, Object>();
        filters.put("api", ids);
        apiEntityQueryBuilder.setFilters(filters);

        when(searchEngineService.search(eq(GraviteeContext.getExecutionContext()), eq(apiEntityQueryBuilder.build())))
            .thenReturn(new SearchResult(List.of("api-id")));

        final var apis = apiSearchService.searchIds(GraviteeContext.getExecutionContext(), null, filters, null, false);

        assertThat(apis).isNotNull();
        assertThat(apis.size()).isEqualTo(1);
        assertThat(apis).isEqualTo(List.of("api-id"));
    }
}

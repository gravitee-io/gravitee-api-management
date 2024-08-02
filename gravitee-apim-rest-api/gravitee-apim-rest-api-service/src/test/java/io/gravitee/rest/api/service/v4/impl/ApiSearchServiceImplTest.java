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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.IntegrationRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApiFieldFilter;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.ApiLifecycleState;
import io.gravitee.repository.management.model.LifecycleState;
import io.gravitee.repository.management.model.Visibility;
import io.gravitee.repository.management.model.flow.FlowReferenceType;
import io.gravitee.rest.api.model.CategoryEntity;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.api.ApiQuery;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.service.CategoryService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.WorkflowService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.converter.ApiConverter;
import io.gravitee.rest.api.service.converter.CategoryMapper;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.search.SearchEngineService;
import io.gravitee.rest.api.service.v4.ApiAuthorizationService;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import io.gravitee.rest.api.service.v4.FlowService;
import io.gravitee.rest.api.service.v4.PlanService;
import io.gravitee.rest.api.service.v4.PrimaryOwnerService;
import io.gravitee.rest.api.service.v4.mapper.ApiMapper;
import io.gravitee.rest.api.service.v4.mapper.GenericApiMapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiSearchServiceImplTest {

    private static final String API_ID = "id-api";

    @Mock
    private ApiRepository apiRepository;

    @Mock
    private SearchEngineService searchEngineService;

    @Mock
    private ParameterService parameterService;

    @Mock
    private MembershipService membershipService;

    @Mock
    private CategoryService categoryService;

    @Mock
    private PlanService planServiceV4;

    @Mock
    private io.gravitee.rest.api.service.PlanService planService;

    @Mock
    private FlowService flowServiceV4;

    @Mock
    private io.gravitee.rest.api.service.configuration.flow.FlowService flowService;

    @Mock
    private WorkflowService workflowService;

    @Mock
    private PrimaryOwnerService primaryOwnerService;

    @Mock
    private ApiAuthorizationService apiAuthorizationService;

    @Mock
    private IntegrationRepository integrationRepository;

    private ApiSearchService apiSearchService;
    private Api api;

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
        var categoryMapper = new CategoryMapper(categoryService);
        var apiConverter = new ApiConverter(
            new ObjectMapper(),
            planService,
            flowService,
            categoryMapper,
            parameterService,
            workflowService
        );

        var apiMapper = new ApiMapper(new ObjectMapper(), planServiceV4, flowServiceV4, parameterService, workflowService, categoryMapper);
        apiSearchService =
            new ApiSearchServiceImpl(
                apiRepository,
                apiMapper,
                new GenericApiMapper(apiMapper, apiConverter),
                primaryOwnerService,
                categoryService,
                searchEngineService,
                apiAuthorizationService,
                integrationRepository
            );

        reset(searchEngineService);

        api = new Api();
        api.setId(API_ID);
        api.setEnvironmentId(GraviteeContext.getExecutionContext().getEnvironmentId());
        api.setDefinitionVersion(DefinitionVersion.V4);
    }

    @Test
    public void shouldFindById() throws TechnicalException {
        Api api = new Api();
        api.setId(API_ID);
        api.setDefinitionVersion(DefinitionVersion.V4);
        api.setType(ApiType.MESSAGE);
        api.setDefinition(
            "{\"definitionVersion\" : \"4.0.0\", " +
            "\"type\": \"message\", " +
            "\"listeners\" : " +
            "   [{ \"type\" : \"http\", \"paths\" : [{ \"path\": \"/context\"}]" +
            "}] }"
        );
        api.setEnvironmentId("DEFAULT");

        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));
        UserEntity userEntity = new UserEntity();
        userEntity.setId("user");
        when(primaryOwnerService.getPrimaryOwner(any(), eq(API_ID))).thenReturn(new PrimaryOwnerEntity(userEntity));

        final ApiEntity apiEntity = apiSearchService.findById(GraviteeContext.getExecutionContext(), API_ID);

        assertThat(apiEntity).isNotNull();
        assertThat(apiEntity.getId()).isEqualTo(API_ID);
        assertThat(apiEntity.getDefinitionVersion()).isEqualTo(DefinitionVersion.V4);
        assertThat(apiEntity.getType()).isEqualTo(ApiType.MESSAGE);
        assertThat(apiEntity.getListeners()).isNotNull();
        assertThat(apiEntity.getListeners().size()).isEqualTo(1);
        assertThat(apiEntity.getListeners().get(0)).isInstanceOf(HttpListener.class);
        HttpListener httpListenerCreated = (HttpListener) apiEntity.getListeners().get(0);
        assertThat(httpListenerCreated.getPaths().size()).isEqualTo(1);
        assertThat(httpListenerCreated.getPaths().get(0).getHost()).isNull();
        assertThat(httpListenerCreated.getPaths().get(0).getPath()).isEqualTo("/context");
    }

    @Test
    public void shouldFindByIdWithFlows() throws TechnicalException {
        Api api = new Api();
        api.setId(API_ID);
        api.setDefinitionVersion(DefinitionVersion.V4);
        api.setType(ApiType.MESSAGE);
        api.setDefinition(
            "{\"definitionVersion\" : \"4.0.0\", " +
            "\"type\": \"message\", " +
            "\"listeners\" : " +
            "   [{ \"type\" : \"http\", \"paths\" : [{ \"path\": \"/context\"}]" +
            "}] }"
        );
        api.setEnvironmentId("DEFAULT");

        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));

        Flow flow1 = new Flow();
        flow1.setName("flow1");
        Flow flow2 = new Flow();
        flow1.setName("flow2");
        List<Flow> apiFlows = List.of(flow1, flow2);
        when(flowServiceV4.findByReference(FlowReferenceType.API, API_ID)).thenReturn(apiFlows);

        UserEntity userEntity = new UserEntity();
        userEntity.setId("user");
        when(primaryOwnerService.getPrimaryOwner(any(), eq(API_ID))).thenReturn(new PrimaryOwnerEntity(userEntity));

        final ApiEntity apiEntity = apiSearchService.findById(GraviteeContext.getExecutionContext(), API_ID);

        assertThat(apiEntity).isNotNull();
        assertThat(apiEntity.getId()).isEqualTo(API_ID);
        assertThat(apiEntity.getDefinitionVersion()).isEqualTo(DefinitionVersion.V4);
        assertThat(apiEntity.getType()).isEqualTo(ApiType.MESSAGE);
        assertThat(apiEntity.getListeners()).isNotNull();
        assertThat(apiEntity.getListeners().size()).isEqualTo(1);
        assertThat(apiEntity.getListeners().get(0)).isInstanceOf(HttpListener.class);
        HttpListener httpListenerCreated = (HttpListener) apiEntity.getListeners().get(0);
        assertThat(httpListenerCreated.getPaths().size()).isEqualTo(1);
        assertThat(httpListenerCreated.getPaths().get(0).getHost()).isNull();
        assertThat(httpListenerCreated.getPaths().get(0).getPath()).isEqualTo("/context");
        assertSame(apiFlows, apiEntity.getFlows());
        verify(flowServiceV4, times(1)).findByReference(FlowReferenceType.API, API_ID);
        verifyNoMoreInteractions(flowServiceV4);
    }

    @Test
    public void shouldFindV4GenericApiWithDefinitionVersionV4() throws TechnicalException {
        Api api = new Api();
        api.setId(API_ID);
        api.setDefinitionVersion(DefinitionVersion.V4);
        api.setEnvironmentId("DEFAULT");

        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));
        UserEntity userEntity = new UserEntity();
        userEntity.setId("user");
        when(primaryOwnerService.getPrimaryOwner(any(), eq(API_ID))).thenReturn(new PrimaryOwnerEntity(userEntity));

        final GenericApiEntity indexableApi = apiSearchService.findGenericById(GraviteeContext.getExecutionContext(), API_ID);

        assertThat(indexableApi).isNotNull();
        assertThat(indexableApi).isInstanceOf(ApiEntity.class);
    }

    @Test
    public void shouldFindV2GenericApiWithNoDefinitionVersion() throws TechnicalException {
        Api api = new Api();
        api.setId(API_ID);
        api.setEnvironmentId("DEFAULT");
        api.setDefinitionVersion(null);

        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));
        UserEntity userEntity = new UserEntity();
        userEntity.setId("user");
        when(primaryOwnerService.getPrimaryOwner(any(), eq(API_ID))).thenReturn(new PrimaryOwnerEntity(userEntity));

        final GenericApiEntity indexableApi = apiSearchService.findGenericById(GraviteeContext.getExecutionContext(), API_ID);

        assertThat(indexableApi).isNotNull();
        assertThat(indexableApi).isInstanceOf(io.gravitee.rest.api.model.api.ApiEntity.class);
    }

    @Test
    public void shouldFindV2GenericApiWithV2DefinitionVersion() throws TechnicalException {
        String categoryId1 = "categoryId1";
        String categoryId2 = "categoryId2";
        String categoryKey1 = "categoryKey1";
        String categoryKey2 = "categoryKey2";
        Api api = new Api();
        api.setId(API_ID);
        api.setEnvironmentId("DEFAULT");
        api.setDefinitionVersion(DefinitionVersion.V2);
        api.setCategories(Set.of(categoryKey1, categoryKey2));

        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));
        UserEntity userEntity = new UserEntity();
        userEntity.setId("user");
        when(primaryOwnerService.getPrimaryOwner(any(), eq(API_ID))).thenReturn(new PrimaryOwnerEntity(userEntity));
        CategoryEntity category1 = new CategoryEntity();
        category1.setId(categoryId1);
        category1.setKey(categoryKey1);
        CategoryEntity category2 = new CategoryEntity();
        category2.setId(categoryId2);
        category2.setKey(categoryKey2);
        when(categoryService.findAll("DEFAULT")).thenReturn(List.of(category1, category2));
        final GenericApiEntity indexableApi = apiSearchService.findGenericById(GraviteeContext.getExecutionContext(), API_ID);

        assertThat(indexableApi).isNotNull();
        assertThat(indexableApi).isInstanceOf(io.gravitee.rest.api.model.api.ApiEntity.class);
        Assertions.assertThat(indexableApi.getCategories()).containsExactlyInAnyOrder(categoryKey1, categoryKey2);
    }

    @Test(expected = ApiNotFoundException.class)
    public void shouldNotFindBecauseNotExists() throws TechnicalException {
        when(apiRepository.findById(API_ID)).thenReturn(Optional.empty());

        apiSearchService.findById(GraviteeContext.getExecutionContext(), API_ID);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotFindBecauseTechnicalException() throws TechnicalException {
        when(apiRepository.findById(API_ID)).thenThrow(TechnicalException.class);

        apiSearchService.findById(GraviteeContext.getExecutionContext(), API_ID);
    }

    @Test
    public void shouldExists() throws TechnicalException {
        Api api = new Api();
        api.setId(API_ID);

        when(apiRepository.existById(API_ID)).thenReturn(true);

        boolean exists = apiSearchService.exists(API_ID);
        assertThat(exists).isTrue();
    }

    @Test
    public void shouldFindByEnvironmentAndEmptyIdIn() {
        final Set<GenericApiEntity> apiEntities = apiSearchService.findGenericByEnvironmentAndIdIn(
            GraviteeContext.getExecutionContext(),
            Set.of()
        );

        assertNotNull(apiEntities);
        assertEquals(0, apiEntities.size());
        verify(apiRepository, times(0)).search(any(), eq(ApiFieldFilter.allFields()));
    }

    @Test
    public void shouldSearch() {
        final Api api1 = new Api();
        api1.setId("api1");
        api1.setName("api1");
        final Api api2 = new Api();
        api2.setId("api2");
        api2.setName("api2");

        when(apiRepository.search(eq(new ApiCriteria.Builder().environmentId("DEFAULT").build()), isNull(), eq(ApiFieldFilter.allFields())))
            .thenReturn(Stream.of(api1));

        UserEntity admin = new UserEntity();
        admin.setId("admin");
        when(primaryOwnerService.getPrimaryOwners(any(), any())).thenReturn(Map.of(api1.getId(), new PrimaryOwnerEntity(admin)));

        final ApiQuery apiQuery = new ApiQuery();
        final Collection<GenericApiEntity> entities = apiSearchService.search(GraviteeContext.getExecutionContext(), apiQuery);

        assertNotNull(entities);
        assertEquals(1, entities.size());
        assertEquals(api1.getId(), entities.iterator().next().getId());
    }

    @Test
    public void should_search_and_exclude_v4() {
        final Api api1 = new Api();
        api1.setId("api1");
        api1.setName("api1");
        final Api api2 = new Api();
        api2.setId("api2");
        api2.setName("api2");

        var definitionList = new ArrayList<DefinitionVersion>();
        definitionList.add(null);
        definitionList.add(DefinitionVersion.V1);
        definitionList.add(DefinitionVersion.V2);

        when(
            apiRepository.search(
                eq(new ApiCriteria.Builder().environmentId("DEFAULT").definitionVersion(definitionList).build()),
                isNull(),
                eq(ApiFieldFilter.allFields())
            )
        )
            .thenReturn(Stream.of(api1));

        UserEntity admin = new UserEntity();
        admin.setId("admin");
        when(primaryOwnerService.getPrimaryOwners(any(), any())).thenReturn(Map.of(api1.getId(), new PrimaryOwnerEntity(admin)));

        final ApiQuery apiQuery = new ApiQuery();

        final Collection<GenericApiEntity> entities = apiSearchService.search(GraviteeContext.getExecutionContext(), apiQuery, true);

        assertNotNull(entities);
        assertEquals(1, entities.size());
    }

    @Test
    public void should_search_with_query() {
        final Api api1 = new Api();
        api1.setId("api1");
        api1.setCrossId("api1");
        api1.setName("api1");
        api1.setCategories(Set.of("cat1"));
        api1.setGroups(Set.of("group1"));
        api1.setLifecycleState(LifecycleState.STARTED);
        api1.setVisibility(Visibility.PUBLIC);
        api1.setApiLifecycleState(ApiLifecycleState.CREATED);
        final Api api2 = new Api();
        api2.setId("api2");
        api2.setName("api2");

        var definitionList = new ArrayList<DefinitionVersion>();
        definitionList.add(null);
        definitionList.add(DefinitionVersion.V1);
        definitionList.add(DefinitionVersion.V2);

        when(
            apiRepository.search(
                eq(
                    new ApiCriteria.Builder()
                        .environmentId("DEFAULT")
                        .category("cat1")
                        .groups(List.of("group1"))
                        .state(LifecycleState.STARTED)
                        .visibility(Visibility.PUBLIC)
                        .lifecycleStates(new ArrayList<>(List.of(ApiLifecycleState.CREATED)))
                        .ids(Set.of("api1"))
                        .crossId("api1")
                        .definitionVersion(definitionList)
                        .build()
                ),
                isNull(),
                eq(ApiFieldFilter.allFields())
            )
        )
            .thenReturn(Stream.of(api1));

        CategoryEntity category = new CategoryEntity();
        category.setId("cat1");
        category.setKey("category1");
        var categoryList = List.of(category);
        when(categoryService.findById("cat1", GraviteeContext.getCurrentEnvironment())).thenReturn(category);
        when(categoryService.findAll(GraviteeContext.getCurrentEnvironment())).thenReturn(categoryList);

        UserEntity admin = new UserEntity();
        admin.setId("admin");
        when(primaryOwnerService.getPrimaryOwners(any(), any())).thenReturn(Map.of(api1.getId(), new PrimaryOwnerEntity(admin)));

        final ApiQuery apiQuery = new ApiQuery();

        apiQuery.setCategory("cat1");
        apiQuery.setGroups(List.of("group1"));
        apiQuery.setState("STARTED");
        apiQuery.setVisibility(io.gravitee.rest.api.model.Visibility.PUBLIC);
        apiQuery.setLifecycleStates(List.of(io.gravitee.rest.api.model.api.ApiLifecycleState.CREATED));
        apiQuery.setIds(List.of("api1"));
        apiQuery.setCrossId("api1");

        final Collection<GenericApiEntity> entities = apiSearchService.search(GraviteeContext.getExecutionContext(), apiQuery, true);

        assertNotNull(entities);
        assertEquals(1, entities.size());
    }
}

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
package io.gravitee.rest.api.service.impl;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.PropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import io.gravitee.common.data.domain.Page;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApiFieldFilter;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.api.ApiQuery;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.model.common.SortableImpl;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.CategoryService;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.PlanService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.SubscriptionService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.WorkflowService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.configuration.flow.FlowService;
import io.gravitee.rest.api.service.converter.ApiConverter;
import io.gravitee.rest.api.service.converter.CategoryMapper;
import io.gravitee.rest.api.service.impl.search.SearchResult;
import io.gravitee.rest.api.service.jackson.filter.ApiPermissionFilter;
import io.gravitee.rest.api.service.search.SearchEngineService;
import io.gravitee.rest.api.service.search.query.Query;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import io.gravitee.rest.api.service.v4.PrimaryOwnerService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Azize Elamrani (azize dot elamrani at gmail dot com)
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiService_SearchTest {

    private static final String USER_NAME = "myUser";

    @InjectMocks
    private ApiServiceImpl apiService = new ApiServiceImpl();

    @Mock
    private ApiRepository apiRepository;

    @Mock
    private MembershipService membershipService;

    @Mock
    private GroupService groupService;

    @Mock
    private RoleService roleService;

    @Mock
    private SubscriptionService subscriptionService;

    @Spy
    private ObjectMapper objectMapper = new GraviteeMapper();

    @Mock
    private Api api;

    @Mock
    private Api privateApi;

    @Mock
    private SubscriptionEntity subscription;

    @Mock
    private UserService userService;

    @Mock
    private ParameterService parameterService;

    @Mock
    private ApplicationService applicationService;

    @Mock
    private CategoryService categoryService;

    @Spy
    private CategoryMapper categoryMapper = new CategoryMapper(mock(CategoryService.class));

    @InjectMocks
    private ApiConverter apiConverter = Mockito.spy(
        new ApiConverter(
            objectMapper,
            mock(PlanService.class),
            mock(FlowService.class),
            categoryMapper,
            parameterService,
            mock(WorkflowService.class)
        )
    );

    @Mock
    private SearchEngineService searchEngineService;

    @Mock
    private PlanService planService;

    @Mock
    private FlowService flowService;

    @Mock
    private PrimaryOwnerService primaryOwnerService;

    @Mock
    private ApiSearchService apiSearchService;

    @Before
    public void setUp() {
        PropertyFilter apiMembershipTypeFilter = new ApiPermissionFilter();
        objectMapper.setFilterProvider(
            new SimpleFilterProvider(Collections.singletonMap("apiMembershipTypeFilter", apiMembershipTypeFilter))
        );
        GraviteeContext.cleanContext();
    }

    @Test
    public void shouldSearchPaginated() {
        final Api api1 = new Api();
        api1.setId("api1");
        api1.setName("api1");
        final Api api2 = new Api();
        api2.setId("api2");
        api2.setName("api2");

        Page<Api> page = new Page<>(Arrays.asList(api1), 2, 1, 2);
        when(
            apiRepository.search(
                eq(getDefaultApiCriteriaBuilder().environmentId("DEFAULT").build()),
                any(),
                any(),
                eq(new ApiFieldFilter.Builder().excludePicture().build())
            )
        )
            .thenReturn(page);

        UserEntity admin = new UserEntity();
        admin.setId("admin");
        when(primaryOwnerService.getPrimaryOwners(any(), any())).thenReturn(Map.of(api1.getId(), new PrimaryOwnerEntity(admin)));

        final ApiQuery apiQuery = new ApiQuery();
        final Page<ApiEntity> apiPage = apiService.search(
            GraviteeContext.getExecutionContext(),
            apiQuery,
            new SortableImpl("name", false),
            new PageableImpl(2, 1)
        );

        assertNotNull(apiPage);
        assertEquals(1, apiPage.getContent().size());
        assertEquals(api1.getId(), apiPage.getContent().get(0).getId());
        assertEquals(2, apiPage.getPageNumber());
        assertEquals(1, apiPage.getPageElements());
        assertEquals(2, apiPage.getTotalElements());
    }

    @Test
    public void shouldSearchPaginatedAndKeepOrder() {
        final Api api1 = new Api();
        api1.setId("api1");
        api1.setName("APITest");

        final Api api2 = new Api();
        api2.setId("api2");
        api2.setName("API Test with long name");

        final Api api3 = new Api();
        api3.setId("api3");
        api3.setName("API Test");

        when(apiSearchService.searchIds(eq(GraviteeContext.getExecutionContext()), any(), any(), any(), eq(true)))
            .thenReturn(List.of(api3.getId(), api1.getId(), api2.getId()));

        when(
            apiRepository.search(
                new ApiCriteria.Builder().environmentId("DEFAULT").ids(api3.getId(), api1.getId(), api2.getId()).build(),
                null,
                new PageableBuilder().pageNumber(0).pageSize(3).build(),
                ApiFieldFilter.allFields()
            )
        )
            .thenReturn(new Page<>(Arrays.asList(api3, api1, api2), 0, 3, 3));

        UserEntity admin = new UserEntity();
        admin.setId("admin");
        UserEntity user2 = new UserEntity();
        admin.setId("user 2");
        UserEntity user3 = new UserEntity();
        admin.setId("user 3");
        when(primaryOwnerService.getPrimaryOwners(any(), any()))
            .thenReturn(
                Map.of(
                    api1.getId(),
                    new PrimaryOwnerEntity(admin),
                    api2.getId(),
                    new PrimaryOwnerEntity(user2),
                    api3.getId(),
                    new PrimaryOwnerEntity(user3)
                )
            );

        final Page<ApiEntity> apiPage = apiService.search(
            GraviteeContext.getExecutionContext(),
            "API Test",
            emptyMap(),
            null,
            new PageableImpl(1, 10)
        );

        assertThat(apiPage.getPageNumber()).isEqualTo(1);
        assertThat(apiPage.getPageElements()).isEqualTo(3);
        assertThat(apiPage.getTotalElements()).isEqualTo(3);

        assertThat(apiPage.getContent()).extracting(ApiEntity::getId).containsExactly(api3.getId(), api1.getId(), api2.getId());
    }

    private ApiCriteria.Builder getDefaultApiCriteriaBuilder() {
        // By default in this service, we do not care for V4 APIs.
        List<DefinitionVersion> allowedDefinitionVersion = new ArrayList<>();
        allowedDefinitionVersion.add(null);
        allowedDefinitionVersion.add(DefinitionVersion.V1);
        allowedDefinitionVersion.add(DefinitionVersion.V2);

        return new ApiCriteria.Builder().definitionVersion(allowedDefinitionVersion);
    }
}

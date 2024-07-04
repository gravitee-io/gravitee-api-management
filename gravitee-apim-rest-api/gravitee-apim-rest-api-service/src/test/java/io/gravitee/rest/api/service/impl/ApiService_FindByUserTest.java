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

import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.PropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.google.common.collect.ImmutableMap;
import io.gravitee.common.data.domain.Page;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApiFieldFilter;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.model.MembershipEntity;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.application.ApplicationListItem;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.model.common.SortableImpl;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.permissions.SystemRole;
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
import io.gravitee.rest.api.service.jackson.filter.ApiPermissionFilter;
import io.gravitee.rest.api.service.v4.ApiAuthorizationService;
import io.gravitee.rest.api.service.v4.ApiEntrypointService;
import io.gravitee.rest.api.service.v4.PrimaryOwnerService;
import io.gravitee.rest.api.service.v4.impl.PrimaryOwnerServiceImpl;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
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
public class ApiService_FindByUserTest {

    private static final String USER_NAME = "myUser";

    @InjectMocks
    private ApiServiceImpl apiService = new ApiServiceImpl();

    @Mock
    private ApiAuthorizationService apiAuthorizationService;

    @Mock
    private ApiRepository apiRepository;

    @Mock
    private MembershipService membershipService;

    @Mock
    private RoleService roleService;

    @Spy
    private ObjectMapper objectMapper = new GraviteeMapper();

    @Mock
    private Api api;

    @Mock
    private ParameterService parameterService;

    @Mock
    private ApplicationService applicationService;

    @Mock
    private CategoryService categoryService;

    @Mock
    private PlanService planService;

    @Mock
    private FlowService flowService;

    @Mock
    private WorkflowService workflowService;

    @Spy
    private CategoryMapper categoryMapper = new CategoryMapper(mock(CategoryService.class));

    @InjectMocks
    private ApiConverter apiConverter = Mockito.spy(
        new ApiConverter(objectMapper, planService, flowService, categoryMapper, parameterService, workflowService)
    );

    @Mock
    private PrimaryOwnerService primaryOwnerService;

    @Before
    public void setUp() {
        PropertyFilter apiMembershipTypeFilter = new ApiPermissionFilter();
        objectMapper.setFilterProvider(
            new SimpleFilterProvider(Collections.singletonMap("apiMembershipTypeFilter", apiMembershipTypeFilter))
        );
        GraviteeContext.cleanContext();
    }

    @Test
    public void shouldFindByUser() {
        when(api.getId()).thenReturn("api-1");
        when(
            apiRepository.search(
                new ApiCriteria.Builder().environmentId("DEFAULT").ids(api.getId()).build(),
                null,
                new PageableBuilder().pageNumber(0).pageSize(1).build(),
                ApiFieldFilter.allFields()
            )
        )
            .thenReturn(new Page<>(singletonList(api), 0, 1, 1));
        when(apiAuthorizationService.findIdsByUser(any(), any(), any(), any(), anyBoolean())).thenReturn(Set.of("api-1"));

        UserEntity admin = new UserEntity();
        admin.setId("admin");
        when(primaryOwnerService.getPrimaryOwners(any(), any())).thenReturn(Map.of("api-1", new PrimaryOwnerEntity(admin)));

        final Set<ApiEntity> apiEntities = apiService.findByUser(GraviteeContext.getExecutionContext(), USER_NAME, null, true);

        assertNotNull(apiEntities);
        assertEquals(1, apiEntities.size());
    }

    @Test
    public void shouldFindByUserPaginated() {
        final Api api1 = new Api();
        api1.setId("api1");
        api1.setName("api1");
        final Api api2 = new Api();
        api2.setId("api2");
        api2.setName("api2");

        Set<String> sets = new LinkedHashSet<>();
        sets.add(api2.getId());
        sets.add(api1.getId());
        when(apiAuthorizationService.findIdsByUser(any(), any(), any(), any(), anyBoolean())).thenReturn(sets);

        UserEntity admin = new UserEntity();
        admin.setId("admin");
        when(primaryOwnerService.getPrimaryOwners(any(), any())).thenReturn(Map.of(api1.getId(), new PrimaryOwnerEntity(admin)));
        when(
            apiRepository.search(
                new ApiCriteria.Builder().environmentId("DEFAULT").ids(api1.getId()).build(),
                null,
                new PageableBuilder().pageNumber(0).pageSize(1).build(),
                ApiFieldFilter.allFields()
            )
        )
            .thenReturn(new Page<>(singletonList(api1), 0, 1, 1));

        final Page<ApiEntity> apiPage = apiService.findByUser(
            GraviteeContext.getExecutionContext(),
            USER_NAME,
            null,
            new SortableImpl("name", false),
            new PageableImpl(2, 1),
            true
        );

        assertNotNull(apiPage);
        assertEquals(1, apiPage.getContent().size());
        assertEquals(api1.getId(), apiPage.getContent().get(0).getId());
        assertEquals(2, apiPage.getPageNumber());
        assertEquals(1, apiPage.getPageElements());
        assertEquals(2, apiPage.getTotalElements());
    }

    @Test
    public void shouldNotFindByUserBecauseNotExists() throws TechnicalException {
        when(apiAuthorizationService.findIdsByUser(any(), eq(USER_NAME), any(), any(), anyBoolean())).thenReturn(Set.of());
        final String poRoleId = "API_PRIMARY_OWNER";

        RoleEntity poRole = new RoleEntity();
        poRole.setId(poRoleId);
        poRole.setScope(RoleScope.API);
        poRole.setName(SystemRole.PRIMARY_OWNER.name());

        final Set<ApiEntity> apiEntities = apiService.findByUser(GraviteeContext.getExecutionContext(), USER_NAME, null, true);

        assertNotNull(apiEntities);
        assertTrue(apiEntities.isEmpty());
    }

    @Test
    public void shouldFindPublicApisOnlyWithAnonymousUser() throws TechnicalException {
        when(apiAuthorizationService.findIdsByUser(any(), eq(null), any(), any(), anyBoolean())).thenReturn(Set.of());
        final Set<ApiEntity> apiEntities = apiService.findByUser(GraviteeContext.getExecutionContext(), null, null, true);

        assertNotNull(apiEntities);
        assertEquals(0, apiEntities.size());

        verify(membershipService, times(0))
            .getMembershipsByMemberAndReference(MembershipMemberType.USER, null, MembershipReferenceType.API);
        verify(membershipService, times(0))
            .getMembershipsByMemberAndReference(MembershipMemberType.USER, null, MembershipReferenceType.GROUP);
        verify(applicationService, times(0)).findByUser(GraviteeContext.getExecutionContext(), null);
    }
}

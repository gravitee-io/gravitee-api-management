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
package io.gravitee.rest.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.PropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import io.gravitee.common.data.domain.Page;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.api.ApiQuery;
import io.gravitee.rest.api.model.application.ApplicationListItem;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.model.common.SortableImpl;
import io.gravitee.rest.api.service.impl.ApiServiceImpl;
import io.gravitee.rest.api.service.jackson.filter.ApiPermissionFilter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static java.util.Collections.singletonList;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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

    @Before
    public void setUp() {
        PropertyFilter apiMembershipTypeFilter = new ApiPermissionFilter();
        objectMapper.setFilterProvider(new SimpleFilterProvider(Collections.singletonMap("apiMembershipTypeFilter", apiMembershipTypeFilter)));
    }

    @Test
    public void shouldSearchPaginated() {
        final Api api1 = new Api();
        api1.setId("api1");
        api1.setName("api1");
        final Api api2 = new Api();
        api2.setId("api2");
        api2.setName("api2");

        MembershipEntity membership1 = new MembershipEntity();
        membership1.setId("id1");
        membership1.setMemberId(USER_NAME);
        membership1.setMemberType(MembershipMemberType.USER);
        membership1.setReferenceId(api1.getId());
        membership1.setReferenceType(MembershipReferenceType.API);
        membership1.setRoleId("API_USER");

        MembershipEntity membership2 = new MembershipEntity();
        membership2.setId(api2.getId());
        membership2.setMemberId(USER_NAME);
        membership2.setMemberType(MembershipMemberType.USER);
        membership2.setReferenceId("api2");
        membership2.setReferenceType(MembershipReferenceType.API);
        membership2.setRoleId("API_USER");

        when(apiRepository.search(new ApiCriteria.Builder().environmentId("DEFAULT").build()))
                .thenReturn(Arrays.asList(api1, api2));

        RoleEntity poRole = new RoleEntity();
        poRole.setId("API_PRIMARY_OWNER");
        when(roleService.findPrimaryOwnerRoleByOrganization(any(), any())).thenReturn(poRole);

        MemberEntity poMember = new MemberEntity();
        poMember.setId("admin");
        poMember.setRoles(Collections.singletonList(poRole));
        when(membershipService.getMembersByReferencesAndRole(MembershipReferenceType.API, Collections.singletonList(api1.getId()), "API_PRIMARY_OWNER")).thenReturn(new HashSet<>(singletonList(poMember)));


        final ApiQuery apiQuery = new ApiQuery();
        final Page<ApiEntity> apiPage = apiService.search(apiQuery, new SortableImpl("name", false), new PageableImpl(2, 1));

        assertNotNull(apiPage);
        assertEquals(1, apiPage.getContent().size());
        assertEquals(api1.getId(), apiPage.getContent().get(0).getId());
        assertEquals(2, apiPage.getPageNumber());
        assertEquals(1, apiPage.getPageElements());
        assertEquals(2, apiPage.getTotalElements());
    }
}

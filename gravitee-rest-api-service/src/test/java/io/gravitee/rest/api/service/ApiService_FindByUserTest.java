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
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.application.ApplicationListItem;
import io.gravitee.rest.api.service.impl.ApiServiceImpl;
import io.gravitee.rest.api.service.jackson.filter.ApiPermissionFilter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.*;

import static io.gravitee.repository.management.model.Visibility.PUBLIC;
import static java.util.Collections.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * @author Azize Elamrani (azize dot elamrani at gmail dot com)
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiService_FindByUserTest {

    private static final String API_ID = "id-api";
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
    public void shouldFindByUser() throws TechnicalException {
        when(privateApi.getId()).thenReturn("private-api");
        when(apiRepository.search(new ApiCriteria.Builder().environmentId("DEFAULT").visibility(PUBLIC).build())).thenReturn(singletonList(api));
        when(apiRepository.search(new ApiCriteria.Builder().environmentId("DEFAULT").ids(api.getId()).build())).thenReturn(singletonList(api));
        when(apiRepository.search(new ApiCriteria.Builder().environmentId("DEFAULT").ids(privateApi.getId()).build())).thenReturn(singletonList(privateApi));

        MembershipEntity membership = new MembershipEntity();
        membership.setId("id");
        membership.setMemberId(USER_NAME);
        membership.setMemberType(MembershipMemberType.USER);
        membership.setReferenceId(api.getId());
        membership.setReferenceType(MembershipReferenceType.API);
        membership.setRoleId("API_USER");

        when(membershipService.getMembershipsByMemberAndReference(MembershipMemberType.USER, USER_NAME, MembershipReferenceType.API)).thenReturn(Collections.singleton(membership));
        
        RoleEntity poRole = new RoleEntity();
        poRole.setId("API_PRIMARY_OWNER");
        when(roleService.findByScopeAndName(any(), any())).thenReturn(Optional.of(poRole));

        MemberEntity poMember = new MemberEntity();
        poMember.setId("admin");
        poMember.setRoles(Collections.singletonList(poRole));
        when(membershipService.getMembersByReferencesAndRole(MembershipReferenceType.API, Collections.singletonList(api.getId()), "API_PRIMARY_OWNER")).thenReturn(new HashSet(Arrays.asList(poMember)));
        when(membershipService.getMembersByReferencesAndRole(MembershipReferenceType.API, Collections.singletonList(privateApi.getId()), "API_PRIMARY_OWNER")).thenReturn(new HashSet(Arrays.asList(poMember)));

        when(subscription.getApi()).thenReturn("private-api");
        when(subscriptionService.search(any())).thenReturn(singletonList(subscription));
        final ApplicationListItem application = new ApplicationListItem();
        application.setId("appId");
        when(applicationService.findByUser(any())).thenReturn(singleton(application));

        final Set<ApiEntity> apiEntities = apiService.findByUser(USER_NAME, null);

        assertNotNull(apiEntities);
        assertEquals(2, apiEntities.size());
    }

    @Test
    public void shouldNotFindByUserBecauseNotExists() throws TechnicalException {
        when(apiRepository.search(new ApiCriteria.Builder().environmentId("DEFAULT").visibility(PUBLIC).build())).thenReturn(emptyList());
        when(membershipService.getMembershipsByMemberAndReference(MembershipMemberType.USER, USER_NAME, MembershipReferenceType.API))
                .thenReturn(Collections.emptySet());

        final Set<ApiEntity> apiEntities = apiService.findByUser(USER_NAME, null);

        assertNotNull(apiEntities);
        assertTrue(apiEntities.isEmpty());
    }
}

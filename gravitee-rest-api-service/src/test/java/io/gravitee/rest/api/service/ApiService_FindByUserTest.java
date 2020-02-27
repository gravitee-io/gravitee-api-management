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

import static io.gravitee.repository.management.model.Visibility.PUBLIC;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Set;

import io.gravitee.repository.management.api.SubscriptionRepository;
import io.gravitee.repository.management.model.*;
import io.gravitee.rest.api.model.SubscriptionEntity;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.PropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.impl.ApiServiceImpl;
import io.gravitee.rest.api.service.jackson.filter.ApiPermissionFilter;

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
    private MembershipRepository membershipRepository;
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

    @Before
    public void setUp() {
        PropertyFilter apiMembershipTypeFilter = new ApiPermissionFilter();
        objectMapper.setFilterProvider(new SimpleFilterProvider(Collections.singletonMap("apiMembershipTypeFilter", apiMembershipTypeFilter)));
    }

    @Test
    public void shouldFindByUser() throws TechnicalException {
        when(privateApi.getId()).thenReturn("private-api");
        when(apiRepository.search(new ApiCriteria.Builder().environment("DEFAULT").visibility(PUBLIC).build())).thenReturn(singletonList(api));
        when(apiRepository.search(new ApiCriteria.Builder().environment("DEFAULT").ids(api.getId()).build())).thenReturn(singletonList(api));
        when(apiRepository.search(new ApiCriteria.Builder().environment("DEFAULT").ids(privateApi.getId()).build())).thenReturn(singletonList(privateApi));

        Membership membership = new Membership(USER_NAME, api.getId(), MembershipReferenceType.API);
        membership.setRoles(Collections.singletonMap(RoleScope.API.getId(), "USER"));
        Set<Membership> memberships = Collections.singleton(membership);
        when(membershipRepository.findByUserAndReferenceType(anyString(), any(MembershipReferenceType.class)))
                .thenReturn(memberships);
        Membership po = new Membership(USER_NAME, API_ID, MembershipReferenceType.API);
        po.setRoles(Collections.singletonMap(RoleScope.API.getId(), SystemRole.PRIMARY_OWNER.name()));
        when(membershipRepository.findByReferencesAndRole(any(), any(), any(), any()))
                .thenReturn(Collections.singleton(po));

        when(subscription.getApi()).thenReturn("private-api");
        when(subscriptionService.search(any())).thenReturn(singletonList(subscription));

        final Set<ApiEntity> apiEntities = apiService.findByUser(USER_NAME, null);

        assertNotNull(apiEntities);
        assertEquals(2, apiEntities.size());
    }

    @Test
    public void shouldNotFindByUserBecauseNotExists() throws TechnicalException {
        when(apiRepository.search(new ApiCriteria.Builder().environment("DEFAULT").visibility(PUBLIC).build())).thenReturn(emptyList());
        when(membershipRepository.findByUserAndReferenceType(anyString(), any(MembershipReferenceType.class)))
                .thenReturn(Collections.emptySet());

        final Set<ApiEntity> apiEntities = apiService.findByUser(USER_NAME, null);

        assertNotNull(apiEntities);
        assertTrue(apiEntities.isEmpty());
    }
}

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
package io.gravitee.management.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.PropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.management.model.ApiEntity;
import io.gravitee.management.model.MembershipType;
import io.gravitee.management.service.exceptions.TechnicalManagementException;
import io.gravitee.management.service.impl.ApiServiceImpl;
import io.gravitee.management.service.jackson.filter.ApiMembershipTypeFilter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Membership;
import io.gravitee.repository.management.model.MembershipReferenceType;
import io.gravitee.repository.management.model.Visibility;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.anyString;
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
    private MembershipRepository membershipRepository;

    @Spy
    private ObjectMapper objectMapper = new GraviteeMapper();

    @Mock
    private Api api;

    @Mock
    private UserService userService;

    @Before
    public void setUp() {
        PropertyFilter apiMembershipTypeFilter = new ApiMembershipTypeFilter();
        objectMapper.setFilterProvider(new SimpleFilterProvider(Collections.singletonMap("apiMembershipTypeFilter", apiMembershipTypeFilter)));
    }

    @Test
    public void shouldFindByUser() throws TechnicalException {
        when(apiRepository.findByVisibility(any(Visibility.class)))
                .thenReturn(new HashSet<>(Arrays.asList(api)));
        Set<Membership> memberships = Collections.singleton(new Membership(USER_NAME, api.getId(), MembershipReferenceType.API));
        when(membershipRepository.findByUserAndReferenceType(anyString(), any(MembershipReferenceType.class)))
                .thenReturn(memberships);
        when(apiRepository.findByIds(Arrays.asList(USER_NAME))).thenReturn(new HashSet<>(Arrays.asList(api)));
        Membership po = new Membership(USER_NAME, API_ID, MembershipReferenceType.API);
        po.setType(MembershipType.PRIMARY_OWNER.name());
        when(membershipRepository.findByReferencesAndMembershipType(any(), any(), any()))
                .thenReturn(Collections.singleton(po));

        final Set<ApiEntity> apiEntities = apiService.findByUser(USER_NAME);

        assertNotNull(apiEntities);
        assertEquals(1, apiEntities.size());
    }

    @Test
    public void shouldNotFindByUserBecauseNotExists() throws TechnicalException {
        when(apiRepository.findByVisibility(any(Visibility.class)))
                .thenReturn(Collections.emptySet());
        when(membershipRepository.findByUserAndReferenceType(anyString(), any(MembershipReferenceType.class)))
                .thenReturn(Collections.emptySet());
        when(apiRepository.findByIds(any())).thenReturn(Collections.emptySet());

        final Set<ApiEntity> apiEntities = apiService.findByUser(USER_NAME);

        assertNotNull(apiEntities);
        assertTrue(apiEntities.isEmpty());
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotFindByUserBecauseTechnicalException() throws TechnicalException {
        when(apiRepository.findByVisibility(any(Visibility.class))).thenThrow(TechnicalException.class);

        apiService.findByUser(USER_NAME);
    }

}

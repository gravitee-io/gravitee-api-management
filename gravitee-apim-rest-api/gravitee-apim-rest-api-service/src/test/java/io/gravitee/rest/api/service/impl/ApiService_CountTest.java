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
package io.gravitee.rest.api.service.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.PropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import io.gravitee.common.data.domain.Page;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApiFieldFilter;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Category;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.converter.ApiConverter;
import io.gravitee.rest.api.service.jackson.filter.ApiPermissionFilter;
import java.util.*;
import java.util.stream.Stream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ApiService_CountTest {

    private static final String USER_ID = "user-1";

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
    private CategoryServiceImpl categoryService;

    @Spy
    private ApiConverter apiConverter;

    @Before
    public void setUp() {
        PropertyFilter apiMembershipTypeFilter = new ApiPermissionFilter();
        objectMapper.setFilterProvider(
            new SimpleFilterProvider(Collections.singletonMap("apiMembershipTypeFilter", apiMembershipTypeFilter))
        );
    }

    @Test
    public void shouldRetrieveUserApiCountsGroupedByCategory() {
        final Category category1 = new Category();
        category1.setId("category-1");

        final Category category2 = new Category();
        category2.setId("category-2");

        final Category category3 = new Category();
        category3.setId("category-3");

        final Api api1 = new Api();
        api1.setId("api-1");
        api1.setCategories(Set.of(category1.getId()));

        final Api api2 = new Api();
        api2.setId("api-2");
        api2.setCategories(Set.of(category1.getId(), category2.getId()));

        final Api api3 = new Api();
        api3.setId("api-3");
        api3.setCategories(Set.of(category1.getId(), category2.getId(), category3.getId()));

        final Api api4 = new Api();
        api4.setId("api-4");

        when(apiRepository.searchIds(anyList(), isA(Pageable.class), eq(null)))
            .thenReturn(new Page<>(Arrays.asList(api1.getId(), api2.getId(), api3.getId(), api4.getId()), 0, 4, 4));
        when(apiRepository.search(any(ApiCriteria.class), isNull(), any(ApiFieldFilter.class)))
            .thenReturn(Stream.of(api1, api2, api3, api4));

        Map<String, Long> expectedCounts = Map.of(category1.getId(), 3L, category2.getId(), 2L, category3.getId(), 1L);
        Map<String, Long> counts = apiService.countPublishedByUserGroupedByCategories(USER_ID);

        assertEquals(expectedCounts, counts);
    }
}

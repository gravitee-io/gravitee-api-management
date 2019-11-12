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
import io.gravitee.management.model.PlanEntity;
import io.gravitee.management.model.PlanStatus;
import io.gravitee.management.service.exceptions.ApiNotDeletableException;
import io.gravitee.management.service.exceptions.ApiRunningStateException;
import io.gravitee.management.service.impl.ApiServiceImpl;
import io.gravitee.management.service.jackson.filter.ApiPermissionFilter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.LifecycleState;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.Optional;

import static org.mockito.Mockito.*;

/**
 * @author Azize Elamrani (azize dot elamrani at gmail dot com)
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiService_DeleteTest {

    private static final String API_ID = "id-api";
    private static final String PLAN_ID = "my-plan";

    @InjectMocks
    private ApiServiceImpl apiService = new ApiServiceImpl();

    @Spy
    private ObjectMapper objectMapper = new GraviteeMapper();

    @Mock
    private ApiRepository apiRepository;
    @Mock
    private Api api;
    @Mock
    private PlanService planService;
    @Mock
    private PlanEntity planEntity;
    @Mock
    private EventService eventService;
    @Mock
    private SubscriptionService subscriptionService;
    @Mock
    private AuditService auditService;
    @Mock
    private TopApiService topApiService;
    @Mock
    private PageService pageService;

    @Before
    public void setUp() {
        PropertyFilter apiMembershipTypeFilter = new ApiPermissionFilter();
        objectMapper.setFilterProvider(new SimpleFilterProvider(Collections.singletonMap("apiMembershipTypeFilter", apiMembershipTypeFilter)));
    }

    @Test(expected = ApiRunningStateException.class)
    public void shouldNotDeleteBecauseRunningState() throws TechnicalException {
        when(api.getLifecycleState()).thenReturn(LifecycleState.STARTED);
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));

        apiService.delete(API_ID);
    }

    @Test
    public void shouldDeleteBecauseNoPlan() throws TechnicalException {
        when(api.getLifecycleState()).thenReturn(LifecycleState.STOPPED);
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));
        when(planService.findByApi(API_ID)).thenReturn(Collections.emptySet());

        apiService.delete(API_ID);
    }

    @Test(expected = ApiNotDeletableException.class)
    public void shouldNotDeleteBecausePlanNotClosed() throws TechnicalException {
        when(api.getLifecycleState()).thenReturn(LifecycleState.STOPPED);
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));
        when(planEntity.getStatus()).thenReturn(PlanStatus.PUBLISHED);
        when(planService.findByApi(API_ID)).thenReturn(Collections.singleton(planEntity));

        apiService.delete(API_ID);
    }

    @Test
    public void shouldNotDeleteBecausePlanClosed() throws TechnicalException {
        when(api.getLifecycleState()).thenReturn(LifecycleState.STOPPED);
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));
        when(planEntity.getId()).thenReturn(PLAN_ID);
        when(planEntity.getStatus()).thenReturn(PlanStatus.CLOSED);
        when(planService.findByApi(API_ID)).thenReturn(Collections.singleton(planEntity));

        apiService.delete(API_ID);

        verify(planService, times(1)).delete(PLAN_ID);
    }

    @Test
    public void shouldNotDeleteBecausePlanStaging() throws TechnicalException {
        when(api.getLifecycleState()).thenReturn(LifecycleState.STOPPED);
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));
        when(planEntity.getId()).thenReturn(PLAN_ID);
        when(planEntity.getStatus()).thenReturn(PlanStatus.STAGING);
        when(planService.findByApi(API_ID)).thenReturn(Collections.singleton(planEntity));

        apiService.delete(API_ID);

        verify(planService, times(1)).delete(PLAN_ID);
    }
}

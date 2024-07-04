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

import static io.gravitee.definition.model.DefinitionContext.ORIGIN_MANAGEMENT;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.PropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiQualityRuleRepository;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.model.*;
import io.gravitee.repository.management.model.flow.FlowReferenceType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.PlanEntity;
import io.gravitee.rest.api.model.PlanStatus;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.configuration.flow.FlowService;
import io.gravitee.rest.api.service.converter.ApiConverter;
import io.gravitee.rest.api.service.converter.CategoryMapper;
import io.gravitee.rest.api.service.exceptions.ApiNotDeletableException;
import io.gravitee.rest.api.service.exceptions.ApiRunningStateException;
import io.gravitee.rest.api.service.jackson.filter.ApiPermissionFilter;
import io.gravitee.rest.api.service.search.SearchEngineService;
import java.util.Collections;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiService_DeleteTest {

    private static final String API_ID = "id-api";
    private static final String PLAN_ID = "my-plan";
    private static final String SUBSCRIPTION_ID = "my-subscription";

    @InjectMocks
    private ApiServiceImpl apiService = new ApiServiceImpl();

    @Spy
    private ObjectMapper objectMapper = new GraviteeMapper();

    @Mock
    private ApiRepository apiRepository;

    @Mock
    private PlanService planService;

    @Mock
    private FlowService flowService;

    @Mock
    private MembershipService membershipService;

    @Mock
    private EventService eventService;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private AuditService auditService;

    @Mock
    private TopApiService topApiService;

    @Mock
    private AlertService alertService;

    @Mock
    private PageService pageService;

    @Mock
    private GenericNotificationConfigService genericNotificationConfigService;

    @Mock
    private PortalNotificationConfigService portalNotificationConfigService;

    @Mock
    private ApiQualityRuleRepository apiQualityRuleRepository;

    @Mock
    private SearchEngineService searchEngineService;

    @Mock
    private CategoryService categoryService;

    @Mock
    private ParameterService parameterService;

    @Mock
    private MediaService mediaService;

    @Mock
    private ApiMetadataService apiMetadataService;

    @Spy
    private CategoryMapper categoryMapper = new CategoryMapper(mock(CategoryService.class));

    @InjectMocks
    private ApiConverter apiConverter = Mockito.spy(
        new ApiConverter(objectMapper, planService, flowService, categoryMapper, parameterService, mock(WorkflowService.class))
    );

    private Api api;
    private PlanEntity planEntity;

    @Before
    public void setUp() throws TechnicalException {
        PropertyFilter apiMembershipTypeFilter = new ApiPermissionFilter();
        objectMapper.setFilterProvider(
            new SimpleFilterProvider(Collections.singletonMap("apiMembershipTypeFilter", apiMembershipTypeFilter))
        );
        api = new Api();
        api.setId(API_ID);
        planEntity = new PlanEntity();
        planEntity.setId(PLAN_ID);
        planEntity.setApi(API_ID);

        lenient().when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));
        lenient().when(planService.findByApi(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(Collections.singleton(planEntity));
    }

    @Test(expected = ApiRunningStateException.class)
    public void shouldNotDeleteBecauseRunningState() throws TechnicalException {
        api.setLifecycleState(LifecycleState.STARTED);
        api.setOrigin(ORIGIN_MANAGEMENT);
        apiService.delete(GraviteeContext.getExecutionContext(), API_ID, false);
    }

    @Test
    public void shouldDeleteBecauseNoPlan() throws TechnicalException {
        api.setLifecycleState(LifecycleState.STOPPED);
        when(planService.findByApi(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(Collections.emptySet());

        apiService.delete(GraviteeContext.getExecutionContext(), API_ID, false);
        verify(membershipService, times(1)).deleteReference(GraviteeContext.getExecutionContext(), MembershipReferenceType.API, API_ID);
        verify(mediaService, times(1)).deleteAllByApi(API_ID);
        verify(apiMetadataService, times(1)).deleteAllByApi(eq(GraviteeContext.getExecutionContext()), eq(API_ID));
        verify(flowService, times(1)).save(FlowReferenceType.API, API_ID, null);
    }

    @Test(expected = ApiNotDeletableException.class)
    public void shouldNotDeleteBecausePlanNotClosed() throws TechnicalException {
        api.setLifecycleState(LifecycleState.STOPPED);
        planEntity.setStatus(PlanStatus.PUBLISHED);

        apiService.delete(GraviteeContext.getExecutionContext(), API_ID, false);
        verify(membershipService, times(1)).deleteReference(GraviteeContext.getExecutionContext(), MembershipReferenceType.API, API_ID);
    }

    @Test
    public void shouldDeleteBecausePlanClosed() throws TechnicalException {
        api.setLifecycleState(LifecycleState.STOPPED);
        planEntity.setStatus(PlanStatus.CLOSED);

        apiService.delete(GraviteeContext.getExecutionContext(), API_ID, false);

        verify(planService, times(1)).delete(GraviteeContext.getExecutionContext(), PLAN_ID);
        verify(membershipService, times(1)).deleteReference(GraviteeContext.getExecutionContext(), MembershipReferenceType.API, API_ID);
        verify(flowService, times(1)).save(FlowReferenceType.API, API_ID, null);
    }

    @Test
    public void shouldDeleteBecausePlanStaging() throws TechnicalException {
        api.setLifecycleState(LifecycleState.STOPPED);
        planEntity.setStatus(PlanStatus.STAGING);

        apiService.delete(GraviteeContext.getExecutionContext(), API_ID, false);

        verify(planService, times(1)).delete(GraviteeContext.getExecutionContext(), PLAN_ID);
        verify(apiQualityRuleRepository, times(1)).deleteByApi(API_ID);
        verify(membershipService, times(1)).deleteReference(GraviteeContext.getExecutionContext(), MembershipReferenceType.API, API_ID);
        verify(mediaService, times(1)).deleteAllByApi(API_ID);
        verify(apiMetadataService, times(1)).deleteAllByApi(eq(GraviteeContext.getExecutionContext()), eq(API_ID));
        verify(flowService, times(1)).save(FlowReferenceType.API, API_ID, null);
    }

    @Test
    public void shouldDeleteStoppedApiWithKubernetesOrigin() throws Exception {
        api.setOrigin(Api.ORIGIN_KUBERNETES);

        apiService.delete(GraviteeContext.getExecutionContext(), API_ID, false);

        verify(apiRepository).findById(eq(API_ID));
        verify(apiRepository).delete(eq(API_ID));
        verify(pageService).deleteAllByApi(eq(GraviteeContext.getExecutionContext()), eq(API_ID));
        verify(topApiService).delete(eq(GraviteeContext.getExecutionContext()), eq(API_ID));
        verify(searchEngineService).delete(eq(GraviteeContext.getExecutionContext()), argThat(_api -> _api.getId().equals(API_ID)));
        verify(membershipService).deleteReference(eq(GraviteeContext.getExecutionContext()), eq(MembershipReferenceType.API), eq(API_ID));
        verify(genericNotificationConfigService).deleteReference(eq(NotificationReferenceType.API), eq(API_ID));
        verify(portalNotificationConfigService).deleteReference(eq(NotificationReferenceType.API), eq(API_ID));
        verify(apiQualityRuleRepository).deleteByApi(eq(API_ID));
        verify(mediaService).deleteAllByApi(eq(API_ID));
        verify(apiMetadataService).deleteAllByApi(eq(GraviteeContext.getExecutionContext()), eq(API_ID));
    }

    @Test
    public void shouldCloseAndDeletePlansForApiWithKubernetesOrigin() throws Exception {
        api.setOrigin(Api.ORIGIN_KUBERNETES);

        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));

        final PlanEntity planEntity = new PlanEntity();
        planEntity.setId(PLAN_ID);
        planEntity.setStatus(PlanStatus.PUBLISHED);
        final PlanEntity closedPlan = new PlanEntity();
        closedPlan.setId(PLAN_ID);
        closedPlan.setStatus(PlanStatus.CLOSED);
        when(planService.findByApi(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(Collections.singleton(planEntity));
        when(planService.close(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(closedPlan);

        apiService.delete(GraviteeContext.getExecutionContext(), API_ID, true);

        verify(planService).close(eq(GraviteeContext.getExecutionContext()), eq(PLAN_ID));
        verify(planService).delete(eq(GraviteeContext.getExecutionContext()), eq(PLAN_ID));
        verify(apiRepository).delete(eq(API_ID));
    }

    @Test
    public void shouldDeleteSubscriptionsAndApiWithKubernetesOrigin() throws Exception {
        api.setOrigin(Api.ORIGIN_KUBERNETES);
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));

        SubscriptionEntity subscription = new SubscriptionEntity();
        subscription.setId(SUBSCRIPTION_ID);
        when(subscriptionService.findByApi(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(Collections.singleton(subscription));

        apiService.delete(GraviteeContext.getExecutionContext(), API_ID, false);

        verify(subscriptionService).findByApi(eq(GraviteeContext.getExecutionContext()), eq(API_ID));
        verify(subscriptionService).delete(eq(GraviteeContext.getExecutionContext()), eq(SUBSCRIPTION_ID));
        verify(apiRepository).delete(eq(API_ID));
    }
}

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

import static io.gravitee.repository.management.model.Plan.Status.PUBLISHED;
import static java.util.Arrays.asList;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.model.Plan;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.PlanValidationType;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.UpdatePlanEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.api.UpdateApiEntity;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.service.converter.ApiConverter;
import io.gravitee.rest.api.service.converter.PlanConverter;
import io.gravitee.rest.api.service.exceptions.PlanGeneralConditionStatusException;
import io.gravitee.rest.api.service.exceptions.PlanInvalidException;
import io.gravitee.rest.api.service.impl.PlanServiceImpl;
import io.gravitee.rest.api.service.processor.PlanSynchronizationProcessor;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class PlanService_UpdateTest {

    private static final String PLAN_ID = "my-plan";
    private static final String API_ID = "my-api";

    @InjectMocks
    private PlanService planService = new PlanServiceImpl();

    @Mock
    private PlanRepository planRepository;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private Plan plan;

    @Mock
    private SubscriptionEntity subscription;

    @Mock
    private AuditService auditService;

    @Mock
    private PageService pageService;

    @Mock
    private ApiService apiService;

    @Mock
    private ApiEntity apiEntity;

    @Mock
    private ParameterService parameterService;

    @Mock
    private PlanSynchronizationProcessor synchronizationProcessor;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private PlanConverter planConverter;

    @Mock
    private ApiConverter apiConverter;

    @Test
    public void shouldUpdate() throws TechnicalException {
        when(plan.getStatus()).thenReturn(Plan.Status.STAGING);
        when(plan.getType()).thenReturn(Plan.PlanType.API);
        when(plan.getSecurity()).thenReturn(Plan.PlanSecurityType.API_KEY);
        when(plan.getApi()).thenReturn(API_ID);
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(parameterService.findAsBoolean(any(), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn(true);
        when(apiService.findById(API_ID)).thenReturn(apiEntity);

        UpdatePlanEntity updatePlan = mock(UpdatePlanEntity.class);
        when(updatePlan.getId()).thenReturn(PLAN_ID);
        when(updatePlan.getValidation()).thenReturn(PlanValidationType.AUTO);
        when(updatePlan.getName()).thenReturn("NameUpdated");
        when(planRepository.update(any())).thenAnswer(returnsFirstArg());

        planService.update(updatePlan);

        verify(planRepository).update(any());
        verify(parameterService).findAsBoolean(any(), eq(ParameterReferenceType.ENVIRONMENT));
    }

    @Test
    public void shouldUpdateAndUpdateApiDefinition() throws TechnicalException {
        when(plan.getStatus()).thenReturn(Plan.Status.STAGING);
        when(plan.getType()).thenReturn(Plan.PlanType.API);
        when(plan.getSecurity()).thenReturn(Plan.PlanSecurityType.API_KEY);
        when(plan.getApi()).thenReturn(API_ID);
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(parameterService.findAsBoolean(any(), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn(true);
        when(apiEntity.getGraviteeDefinitionVersion()).thenReturn(DefinitionVersion.V2.getLabel());
        when(apiService.findById(API_ID)).thenReturn(apiEntity);

        UpdatePlanEntity updatePlan = mock(UpdatePlanEntity.class);
        when(updatePlan.getId()).thenReturn(PLAN_ID);
        when(updatePlan.getValidation()).thenReturn(PlanValidationType.AUTO);
        when(updatePlan.getName()).thenReturn("NameUpdated");
        when(planRepository.update(any())).thenAnswer(returnsFirstArg());

        planService.update(updatePlan);

        verify(planRepository).update(any());
        verify(parameterService).findAsBoolean(any(), eq(ParameterReferenceType.ENVIRONMENT));
        verify(apiService).update(anyString(), any());
    }

    @Test
    public void shouldUpdate_WithNewPublished_GeneralConditionPage() throws TechnicalException {
        final String PAGE_ID = "PAGE_ID_TEST";
        when(plan.getStatus()).thenReturn(PUBLISHED);
        when(plan.getType()).thenReturn(Plan.PlanType.API);
        when(plan.getSecurity()).thenReturn(Plan.PlanSecurityType.API_KEY);
        when(plan.getApi()).thenReturn(API_ID);
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(parameterService.findAsBoolean(any(), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn(true);

        UpdatePlanEntity updatePlan = mock(UpdatePlanEntity.class);
        when(updatePlan.getId()).thenReturn(PLAN_ID);
        when(updatePlan.getValidation()).thenReturn(PlanValidationType.AUTO);
        when(updatePlan.getName()).thenReturn("NameUpdated");
        when(updatePlan.getGeneralConditions()).thenReturn(PAGE_ID);
        when(planRepository.update(any())).thenAnswer(returnsFirstArg());
        when(apiService.findById(API_ID)).thenReturn(apiEntity);

        PageEntity unpublishedPage = new PageEntity();
        unpublishedPage.setId(PAGE_ID);
        unpublishedPage.setOrder(1);
        unpublishedPage.setType("MARKDOWN");
        unpublishedPage.setPublished(true);
        doReturn(unpublishedPage).when(pageService).findById(PAGE_ID);

        planService.update(updatePlan);

        verify(planRepository).update(any());
        verify(parameterService).findAsBoolean(any(), eq(ParameterReferenceType.ENVIRONMENT));
    }

    @Test(expected = PlanGeneralConditionStatusException.class)
    public void shouldNotUpdate_WithNotPublished_GeneralConditionPage_PublishPlan() throws TechnicalException {
        shouldNotUpdate_withNotPublished_GCUPage(PUBLISHED, DefinitionVersion.V1);
    }

    @Test(expected = PlanGeneralConditionStatusException.class)
    public void shouldNotUpdate_WithNotPublished_GeneralConditionPage_DeprecatedPlan() throws TechnicalException {
        shouldNotUpdate_withNotPublished_GCUPage(Plan.Status.DEPRECATED, DefinitionVersion.V1);
    }

    @Test(expected = PlanGeneralConditionStatusException.class)
    public void shouldNotUpdate_WithNotPublished_GeneralConditionPage_PublishPlan_v2() throws TechnicalException {
        shouldNotUpdate_withNotPublished_GCUPage(PUBLISHED, DefinitionVersion.V2);
    }

    @Test(expected = PlanGeneralConditionStatusException.class)
    public void shouldNotUpdate_WithNotPublished_GeneralConditionPage_DeprecatedPlan_v2() throws TechnicalException {
        shouldNotUpdate_withNotPublished_GCUPage(Plan.Status.DEPRECATED, DefinitionVersion.V2);
    }

    private void shouldNotUpdate_withNotPublished_GCUPage(Plan.Status planStatus, DefinitionVersion apiVersion) throws TechnicalException {
        final String PAGE_ID = "PAGE_ID_TEST";
        when(plan.getStatus()).thenReturn(planStatus);
        when(plan.getType()).thenReturn(Plan.PlanType.API);
        when(plan.getSecurity()).thenReturn(Plan.PlanSecurityType.API_KEY);
        when(plan.getApi()).thenReturn(API_ID);
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(parameterService.findAsBoolean(any(), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn(true);

        final ApiEntity apiV2 = new ApiEntity();
        apiV2.setGraviteeDefinitionVersion(apiVersion.getLabel());
        when(apiService.findById(any())).thenReturn(apiV2);

        UpdatePlanEntity updatePlan = mock(UpdatePlanEntity.class);
        when(updatePlan.getId()).thenReturn(PLAN_ID);
        when(updatePlan.getName()).thenReturn("NameUpdated");
        when(updatePlan.getGeneralConditions()).thenReturn(PAGE_ID);

        PageEntity unpublishedPage = new PageEntity();
        unpublishedPage.setId(PAGE_ID);
        unpublishedPage.setOrder(1);
        unpublishedPage.setType("MARKDOWN");
        unpublishedPage.setPublished(false);
        doReturn(unpublishedPage).when(pageService).findById(PAGE_ID);

        planService.update(updatePlan);
        verify(apiService, never()).update(any(), any());
    }

    @Test
    public void shouldUpdateApi_PlanWithFlow() throws TechnicalException {
        final String PAGE_ID = "PAGE_ID_TEST";
        when(plan.getStatus()).thenReturn(PUBLISHED);
        when(plan.getType()).thenReturn(Plan.PlanType.API);
        when(plan.getSecurity()).thenReturn(Plan.PlanSecurityType.KEY_LESS);
        when(plan.getApi()).thenReturn(API_ID);
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(planRepository.update(any())).thenReturn(plan);
        when(parameterService.findAsBoolean(any(), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn(true);
        final ApiEntity apiV2 = new ApiEntity();
        apiV2.setGraviteeDefinitionVersion(DefinitionVersion.V2.getLabel());
        when(apiService.findById(any())).thenReturn(apiV2);

        UpdatePlanEntity updatePlan = mock(UpdatePlanEntity.class);
        when(updatePlan.getId()).thenReturn(PLAN_ID);
        when(updatePlan.getName()).thenReturn("NameUpdated");
        when(updatePlan.getFlows()).thenReturn(asList(new Flow()));

        when(apiConverter.toUpdateApiEntity(apiV2)).thenReturn(new UpdateApiEntity());

        planService.update(updatePlan, true);

        verify(apiService)
            .update(any(), argThat(api -> api.getPlans().get(0).getFlows() != null && api.getPlans().get(0).getFlows().size() == 1));
    }

    @Test(expected = PlanInvalidException.class)
    public void shouldUpdateApi_PlanWithoutFlow() throws TechnicalException {
        final String PAGE_ID = "PAGE_ID_TEST";
        when(plan.getSecurity()).thenReturn(Plan.PlanSecurityType.KEY_LESS);
        when(plan.getApi()).thenReturn(API_ID);
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(parameterService.findAsBoolean(any(), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn(true);
        final ApiEntity apiV2 = new ApiEntity();
        apiV2.setGraviteeDefinitionVersion(DefinitionVersion.V2.getLabel());
        when(apiService.findById(any())).thenReturn(apiV2);

        UpdatePlanEntity updatePlan = mock(UpdatePlanEntity.class);
        when(updatePlan.getId()).thenReturn(PLAN_ID);
        when(updatePlan.getName()).thenReturn("NameUpdated");
        when(updatePlan.getFlows()).thenReturn(null);

        PageEntity unpublishedPage = new PageEntity();
        unpublishedPage.setId(PAGE_ID);
        unpublishedPage.setOrder(1);
        unpublishedPage.setType("MARKDOWN");
        unpublishedPage.setPublished(false);

        planService.update(updatePlan);
    }

    @Test
    public void shouldUpdateApi_PlanWithFlowEmptyList() throws TechnicalException {
        final String PAGE_ID = "PAGE_ID_TEST";
        when(plan.getStatus()).thenReturn(PUBLISHED);
        when(plan.getType()).thenReturn(Plan.PlanType.API);
        when(plan.getSecurity()).thenReturn(Plan.PlanSecurityType.KEY_LESS);
        when(plan.getApi()).thenReturn(API_ID);
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(planRepository.update(any())).thenReturn(plan);
        when(parameterService.findAsBoolean(any(), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn(true);
        final ApiEntity apiV2 = new ApiEntity();
        apiV2.setGraviteeDefinitionVersion(DefinitionVersion.V2.getLabel());
        when(apiService.findById(any())).thenReturn(apiV2);

        UpdatePlanEntity updatePlan = mock(UpdatePlanEntity.class);
        when(updatePlan.getId()).thenReturn(PLAN_ID);
        when(updatePlan.getName()).thenReturn("NameUpdated");
        when(updatePlan.getFlows()).thenReturn(asList());

        PageEntity unpublishedPage = new PageEntity();
        unpublishedPage.setId(PAGE_ID);
        unpublishedPage.setOrder(1);
        unpublishedPage.setType("MARKDOWN");
        unpublishedPage.setPublished(false);

        when(apiConverter.toUpdateApiEntity(apiV2)).thenReturn(new UpdateApiEntity());

        planService.update(updatePlan, true);

        verify(apiService)
            .update(any(), argThat(api -> api.getPlans().get(0).getFlows() == null || api.getPlans().get(0).getFlows().isEmpty()));
    }

    @Test
    public void shouldUpdate_withNotPublished_GCUPage_StagingPlan() throws TechnicalException {
        final String PAGE_ID = "PAGE_ID_TEST";
        when(plan.getStatus()).thenReturn(Plan.Status.STAGING);
        when(plan.getType()).thenReturn(Plan.PlanType.API);
        when(plan.getSecurity()).thenReturn(Plan.PlanSecurityType.API_KEY);
        when(plan.getApi()).thenReturn(API_ID);
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(parameterService.findAsBoolean(any(), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn(true);
        when(apiService.findById(API_ID)).thenReturn(apiEntity);

        UpdatePlanEntity updatePlan = mock(UpdatePlanEntity.class);
        when(updatePlan.getId()).thenReturn(PLAN_ID);
        when(updatePlan.getValidation()).thenReturn(PlanValidationType.AUTO);
        when(updatePlan.getName()).thenReturn("NameUpdated");
        when(updatePlan.getGeneralConditions()).thenReturn(PAGE_ID);
        when(planRepository.update(any())).thenAnswer(returnsFirstArg());

        planService.update(updatePlan);

        verify(planRepository).update(any());
        verify(parameterService).findAsBoolean(any(), eq(ParameterReferenceType.ENVIRONMENT));
    }
}

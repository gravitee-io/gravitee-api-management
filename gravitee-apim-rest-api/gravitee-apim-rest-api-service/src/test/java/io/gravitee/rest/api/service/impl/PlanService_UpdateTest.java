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

import static io.gravitee.definition.model.DefinitionVersion.V1;
import static io.gravitee.definition.model.DefinitionVersion.V2;
import static io.gravitee.repository.management.model.Plan.Status.PUBLISHED;
import static java.util.Arrays.asList;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Plan;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.PlanValidationType;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.UpdatePlanEntity;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.converter.ApiConverter;
import io.gravitee.rest.api.service.converter.PlanConverter;
import io.gravitee.rest.api.service.exceptions.PlanGeneralConditionStatusException;
import io.gravitee.rest.api.service.exceptions.PlanInvalidException;
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
    private ApiRepository apiRepository;

    @Mock
    private Api api;

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
        when(parameterService.findAsBoolean(eq(GraviteeContext.getExecutionContext()), any(), eq(ParameterReferenceType.ENVIRONMENT)))
            .thenReturn(true);
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));

        UpdatePlanEntity updatePlan = mock(UpdatePlanEntity.class);
        when(updatePlan.getId()).thenReturn(PLAN_ID);
        when(updatePlan.getValidation()).thenReturn(PlanValidationType.AUTO);
        when(updatePlan.getName()).thenReturn("NameUpdated");
        when(planRepository.update(any())).thenAnswer(returnsFirstArg());

        planService.update(GraviteeContext.getExecutionContext(), updatePlan);

        verify(planRepository).update(any());
        verify(parameterService).findAsBoolean(eq(GraviteeContext.getExecutionContext()), any(), eq(ParameterReferenceType.ENVIRONMENT));
    }

    @Test
    public void shouldUpdateAndUpdateApiDefinition() throws Exception {
        when(plan.getStatus()).thenReturn(Plan.Status.STAGING);
        when(plan.getType()).thenReturn(Plan.PlanType.API);
        when(plan.getSecurity()).thenReturn(Plan.PlanSecurityType.API_KEY);
        when(plan.getApi()).thenReturn(API_ID);
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(parameterService.findAsBoolean(eq(GraviteeContext.getExecutionContext()), any(), eq(ParameterReferenceType.ENVIRONMENT)))
            .thenReturn(true);
        mockApiDefinitionVersion(V2);
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));

        UpdatePlanEntity updatePlan = mock(UpdatePlanEntity.class);
        when(updatePlan.getId()).thenReturn(PLAN_ID);
        when(updatePlan.getValidation()).thenReturn(PlanValidationType.AUTO);
        when(updatePlan.getName()).thenReturn("NameUpdated");
        when(planRepository.update(any())).thenAnswer(returnsFirstArg());

        planService.update(GraviteeContext.getExecutionContext(), updatePlan);

        verify(planRepository).update(any());
        verify(parameterService).findAsBoolean(eq(GraviteeContext.getExecutionContext()), any(), eq(ParameterReferenceType.ENVIRONMENT));
    }

    @Test
    public void shouldUpdate_WithNewPublished_GeneralConditionPage() throws Exception {
        final String PAGE_ID = "PAGE_ID_TEST";
        when(plan.getStatus()).thenReturn(PUBLISHED);
        when(plan.getType()).thenReturn(Plan.PlanType.API);
        when(plan.getSecurity()).thenReturn(Plan.PlanSecurityType.API_KEY);
        when(plan.getApi()).thenReturn(API_ID);
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(parameterService.findAsBoolean(eq(GraviteeContext.getExecutionContext()), any(), eq(ParameterReferenceType.ENVIRONMENT)))
            .thenReturn(true);

        UpdatePlanEntity updatePlan = mock(UpdatePlanEntity.class);
        when(updatePlan.getId()).thenReturn(PLAN_ID);
        when(updatePlan.getValidation()).thenReturn(PlanValidationType.AUTO);
        when(updatePlan.getName()).thenReturn("NameUpdated");
        when(updatePlan.getGeneralConditions()).thenReturn(PAGE_ID);
        when(planRepository.update(any())).thenAnswer(returnsFirstArg());
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));

        PageEntity unpublishedPage = new PageEntity();
        unpublishedPage.setId(PAGE_ID);
        unpublishedPage.setOrder(1);
        unpublishedPage.setType("MARKDOWN");
        unpublishedPage.setPublished(true);
        doReturn(unpublishedPage).when(pageService).findById(PAGE_ID);

        planService.update(GraviteeContext.getExecutionContext(), updatePlan);

        verify(planRepository).update(any());
        verify(parameterService).findAsBoolean(eq(GraviteeContext.getExecutionContext()), any(), eq(ParameterReferenceType.ENVIRONMENT));
    }

    @Test(expected = PlanGeneralConditionStatusException.class)
    public void shouldNotUpdate_WithNotPublished_GeneralConditionPage_PublishPlan() throws Exception {
        shouldNotUpdate_withNotPublished_GCUPage(PUBLISHED, V1);
    }

    @Test(expected = PlanGeneralConditionStatusException.class)
    public void shouldNotUpdate_WithNotPublished_GeneralConditionPage_DeprecatedPlan() throws Exception {
        shouldNotUpdate_withNotPublished_GCUPage(Plan.Status.DEPRECATED, V1);
    }

    @Test(expected = PlanGeneralConditionStatusException.class)
    public void shouldNotUpdate_WithNotPublished_GeneralConditionPage_PublishPlan_v2() throws Exception {
        shouldNotUpdate_withNotPublished_GCUPage(PUBLISHED, V2);
    }

    @Test(expected = PlanGeneralConditionStatusException.class)
    public void shouldNotUpdate_WithNotPublished_GeneralConditionPage_DeprecatedPlan_v2() throws Exception {
        shouldNotUpdate_withNotPublished_GCUPage(Plan.Status.DEPRECATED, V2);
    }

    private void shouldNotUpdate_withNotPublished_GCUPage(Plan.Status planStatus, DefinitionVersion apiVersion) throws Exception {
        final String PAGE_ID = "PAGE_ID_TEST";
        when(plan.getStatus()).thenReturn(planStatus);
        when(plan.getType()).thenReturn(Plan.PlanType.API);
        when(plan.getSecurity()).thenReturn(Plan.PlanSecurityType.API_KEY);
        when(plan.getApi()).thenReturn(API_ID);
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(parameterService.findAsBoolean(eq(GraviteeContext.getExecutionContext()), any(), eq(ParameterReferenceType.ENVIRONMENT)))
            .thenReturn(true);

        mockApiDefinitionVersion(apiVersion);
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));

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

        planService.update(GraviteeContext.getExecutionContext(), updatePlan);

        verify(planRepository, never()).update(any());
    }

    @Test
    public void shouldUpdateApi_PlanWithFlow() throws Exception {
        final String PAGE_ID = "PAGE_ID_TEST";
        when(plan.getStatus()).thenReturn(PUBLISHED);
        when(plan.getType()).thenReturn(Plan.PlanType.API);
        when(plan.getSecurity()).thenReturn(Plan.PlanSecurityType.KEY_LESS);
        when(plan.getApi()).thenReturn(API_ID);
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(planRepository.update(any())).thenReturn(plan);
        when(parameterService.findAsBoolean(eq(GraviteeContext.getExecutionContext()), any(), eq(ParameterReferenceType.ENVIRONMENT)))
            .thenReturn(true);

        mockApiDefinitionVersion(V2);
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));

        UpdatePlanEntity updatePlan = mock(UpdatePlanEntity.class);
        when(updatePlan.getId()).thenReturn(PLAN_ID);
        when(updatePlan.getName()).thenReturn("NameUpdated");
        when(updatePlan.getFlows()).thenReturn(asList(new Flow()));

        planService.update(GraviteeContext.getExecutionContext(), updatePlan, true);

        verify(planRepository, times(1)).update(argThat(plan1 -> !plan1.getFlows().isEmpty()));
    }

    @Test(expected = PlanInvalidException.class)
    public void shouldUpdateApi_PlanWithoutFlow() throws Exception {
        final String PAGE_ID = "PAGE_ID_TEST";
        when(plan.getSecurity()).thenReturn(Plan.PlanSecurityType.KEY_LESS);
        when(plan.getApi()).thenReturn(API_ID);
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(parameterService.findAsBoolean(eq(GraviteeContext.getExecutionContext()), any(), eq(ParameterReferenceType.ENVIRONMENT)))
            .thenReturn(true);

        mockApiDefinitionVersion(V2);
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));

        UpdatePlanEntity updatePlan = mock(UpdatePlanEntity.class);
        when(updatePlan.getId()).thenReturn(PLAN_ID);
        when(updatePlan.getName()).thenReturn("NameUpdated");
        when(updatePlan.getFlows()).thenReturn(null);

        PageEntity unpublishedPage = new PageEntity();
        unpublishedPage.setId(PAGE_ID);
        unpublishedPage.setOrder(1);
        unpublishedPage.setType("MARKDOWN");
        unpublishedPage.setPublished(false);

        planService.update(GraviteeContext.getExecutionContext(), updatePlan);
    }

    @Test
    public void shouldUpdateApi_PlanWithFlowEmptyList() throws Exception {
        final String PAGE_ID = "PAGE_ID_TEST";
        when(plan.getStatus()).thenReturn(PUBLISHED);
        when(plan.getType()).thenReturn(Plan.PlanType.API);
        when(plan.getSecurity()).thenReturn(Plan.PlanSecurityType.KEY_LESS);
        when(plan.getApi()).thenReturn(API_ID);
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(planRepository.update(any())).thenReturn(plan);
        when(parameterService.findAsBoolean(eq(GraviteeContext.getExecutionContext()), any(), eq(ParameterReferenceType.ENVIRONMENT)))
            .thenReturn(true);

        mockApiDefinitionVersion(V2);
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));

        UpdatePlanEntity updatePlan = mock(UpdatePlanEntity.class);
        when(updatePlan.getId()).thenReturn(PLAN_ID);
        when(updatePlan.getName()).thenReturn("NameUpdated");
        when(updatePlan.getFlows()).thenReturn(asList());

        PageEntity unpublishedPage = new PageEntity();
        unpublishedPage.setId(PAGE_ID);
        unpublishedPage.setOrder(1);
        unpublishedPage.setType("MARKDOWN");
        unpublishedPage.setPublished(false);

        planService.update(GraviteeContext.getExecutionContext(), updatePlan, true);

        verify(planRepository, times(1)).update(argThat(plan1 -> !plan1.getFlows().isEmpty()));
    }

    @Test
    public void shouldUpdate_withNotPublished_GCUPage_StagingPlan() throws TechnicalException {
        final String PAGE_ID = "PAGE_ID_TEST";
        when(plan.getStatus()).thenReturn(Plan.Status.STAGING);
        when(plan.getType()).thenReturn(Plan.PlanType.API);
        when(plan.getSecurity()).thenReturn(Plan.PlanSecurityType.API_KEY);
        when(plan.getApi()).thenReturn(API_ID);
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(parameterService.findAsBoolean(eq(GraviteeContext.getExecutionContext()), any(), eq(ParameterReferenceType.ENVIRONMENT)))
            .thenReturn(true);
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));

        UpdatePlanEntity updatePlan = mock(UpdatePlanEntity.class);
        when(updatePlan.getId()).thenReturn(PLAN_ID);
        when(updatePlan.getValidation()).thenReturn(PlanValidationType.AUTO);
        when(updatePlan.getName()).thenReturn("NameUpdated");
        when(updatePlan.getGeneralConditions()).thenReturn(PAGE_ID);
        when(planRepository.update(any())).thenAnswer(returnsFirstArg());

        planService.update(GraviteeContext.getExecutionContext(), updatePlan);

        verify(planRepository).update(any());
        verify(parameterService).findAsBoolean(eq(GraviteeContext.getExecutionContext()), any(), eq(ParameterReferenceType.ENVIRONMENT));
    }

    private void mockApiDefinitionVersion(DefinitionVersion version) throws Exception {
        var apiDefinition = new io.gravitee.definition.model.Api();
        apiDefinition.setDefinitionVersion(version);
        when(api.getDefinition()).thenReturn(new ObjectMapper().writeValueAsString(apiDefinition));
    }
}

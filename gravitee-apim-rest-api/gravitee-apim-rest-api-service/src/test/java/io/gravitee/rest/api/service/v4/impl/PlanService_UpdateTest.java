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
package io.gravitee.rest.api.service.v4.impl;

import static io.gravitee.definition.model.DefinitionVersion.V1;
import static io.gravitee.definition.model.DefinitionVersion.V2;
import static io.gravitee.definition.model.DefinitionVersion.V4;
import static io.gravitee.repository.management.model.Plan.Status.PUBLISHED;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.flow.crud_service.FlowCrudService;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Group;
import io.gravitee.repository.management.model.Plan;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.model.v4.plan.PlanValidationType;
import io.gravitee.rest.api.model.v4.plan.UpdatePlanEntity;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.GroupNotFoundException;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import io.gravitee.rest.api.service.exceptions.PlanFlowRequiredException;
import io.gravitee.rest.api.service.exceptions.PlanGeneralConditionStatusException;
import io.gravitee.rest.api.service.exceptions.TagNotAllowedException;
import io.gravitee.rest.api.service.processor.SynchronizationService;
import io.gravitee.rest.api.service.v4.FlowService;
import io.gravitee.rest.api.service.v4.PlanService;
import io.gravitee.rest.api.service.v4.mapper.ApiMapper;
import io.gravitee.rest.api.service.v4.mapper.PlanMapper;
import io.gravitee.rest.api.service.v4.validation.FlowValidationService;
import io.gravitee.rest.api.service.v4.validation.PathParametersValidationService;
import io.gravitee.rest.api.service.v4.validation.TagsValidationService;
import io.reactivex.rxjava3.annotations.NonNull;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
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
    public static final String MOCK_API_DEFINITION = "api-definition";

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
    private SynchronizationService synchronizationService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private PlanMapper planMapper;

    @Mock
    private ApiMapper apiMapper;

    @Mock
    private FlowService flowService;

    @Mock
    private FlowCrudService flowCrudService;

    @Mock
    private TagsValidationService tagsValidationService;

    @Mock
    private PolicyService policyService;

    @Mock
    private FlowValidationService flowValidationService;

    @Mock
    private PathParametersValidationService pathParametersValidationService;

    @Mock
    private GroupService groupService;

    @Before
    public void setup() throws Exception {
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));
    }

    @Test
    public void shouldUpdate() throws TechnicalException {
        when(plan.getStatus()).thenReturn(Plan.Status.STAGING);
        when(plan.getType()).thenReturn(Plan.PlanType.API);
        when(plan.getSecurity()).thenReturn(Plan.PlanSecurityType.API_KEY);
        when(plan.getApi()).thenReturn(API_ID);
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(
            parameterService.findAsBoolean(eq(GraviteeContext.getExecutionContext()), any(), eq(ParameterReferenceType.ENVIRONMENT))
        ).thenReturn(true);

        final UpdatePlanEntity updatePlan = initUpdatePlanEntity();
        when(planRepository.update(any())).thenAnswer(returnsFirstArg());

        planService.update(GraviteeContext.getExecutionContext(), updatePlan);

        verify(planRepository).update(any());
        verify(parameterService).findAsBoolean(eq(GraviteeContext.getExecutionContext()), any(), eq(ParameterReferenceType.ENVIRONMENT));
        verify(flowValidationService, times(1)).validateAndSanitize(any(), anyList());
        verify(pathParametersValidationService, times(1)).validate(any(), any(), any());
    }

    @Test
    public void shouldUpdateAndUpdateApiDefinition() throws Exception {
        when(plan.getStatus()).thenReturn(Plan.Status.STAGING);
        when(plan.getType()).thenReturn(Plan.PlanType.API);
        when(plan.getSecurity()).thenReturn(Plan.PlanSecurityType.API_KEY);
        when(plan.getApi()).thenReturn(API_ID);
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(
            parameterService.findAsBoolean(eq(GraviteeContext.getExecutionContext()), any(), eq(ParameterReferenceType.ENVIRONMENT))
        ).thenReturn(true);

        final UpdatePlanEntity updatePlan = initUpdatePlanEntity();
        when(planRepository.update(any())).thenAnswer(returnsFirstArg());

        planService.update(GraviteeContext.getExecutionContext(), updatePlan);

        verify(planRepository).update(any());
        verify(parameterService).findAsBoolean(eq(GraviteeContext.getExecutionContext()), any(), eq(ParameterReferenceType.ENVIRONMENT));
        verify(pathParametersValidationService, times(1)).validate(any(), any(), any());
    }

    @Test
    public void shouldUpdate_WithNewPublished_GeneralConditionPage() throws Exception {
        final String PAGE_ID = "PAGE_ID_TEST";
        when(plan.getStatus()).thenReturn(PUBLISHED);
        when(plan.getType()).thenReturn(Plan.PlanType.API);
        when(plan.getSecurity()).thenReturn(Plan.PlanSecurityType.API_KEY);
        when(plan.getApi()).thenReturn(API_ID);
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(
            parameterService.findAsBoolean(eq(GraviteeContext.getExecutionContext()), any(), eq(ParameterReferenceType.ENVIRONMENT))
        ).thenReturn(true);

        final UpdatePlanEntity updatePlan = initUpdatePlanEntity();
        updatePlan.setGeneralConditions(PAGE_ID);
        when(planRepository.update(any())).thenAnswer(returnsFirstArg());

        PageEntity unpublishedPage = new PageEntity();
        unpublishedPage.setId(PAGE_ID);
        unpublishedPage.setOrder(1);
        unpublishedPage.setType("MARKDOWN");
        unpublishedPage.setPublished(true);
        doReturn(unpublishedPage).when(pageService).findById(PAGE_ID);

        planService.update(GraviteeContext.getExecutionContext(), updatePlan);

        verify(planRepository).update(any());
        verify(parameterService).findAsBoolean(eq(GraviteeContext.getExecutionContext()), any(), eq(ParameterReferenceType.ENVIRONMENT));
        verify(pathParametersValidationService, times(1)).validate(any(), any(), any());
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
        when(
            parameterService.findAsBoolean(eq(GraviteeContext.getExecutionContext()), any(), eq(ParameterReferenceType.ENVIRONMENT))
        ).thenReturn(true);

        final UpdatePlanEntity updatePlan = initUpdatePlanEntity();
        updatePlan.setGeneralConditions(PAGE_ID);

        PageEntity unpublishedPage = new PageEntity();
        unpublishedPage.setId(PAGE_ID);
        unpublishedPage.setOrder(1);
        unpublishedPage.setType("MARKDOWN");
        unpublishedPage.setPublished(false);
        doReturn(unpublishedPage).when(pageService).findById(PAGE_ID);

        planService.update(GraviteeContext.getExecutionContext(), updatePlan);

        verify(planRepository, never()).update(any());
        verify(pathParametersValidationService, times(1)).validate(any(), any(), any());
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
        when(
            parameterService.findAsBoolean(eq(GraviteeContext.getExecutionContext()), any(), eq(ParameterReferenceType.ENVIRONMENT))
        ).thenReturn(true);

        final UpdatePlanEntity updatePlan = initUpdatePlanEntity();

        planService.update(GraviteeContext.getExecutionContext(), updatePlan);

        verify(flowCrudService, times(1)).savePlanFlows(updatePlan.getId(), updatePlan.getFlows());
        verify(pathParametersValidationService, times(1)).validate(any(), any(), any());
    }

    @Test(expected = PlanFlowRequiredException.class)
    public void shouldUpdateApi_PlanWithoutFlow() throws Exception {
        final String PAGE_ID = "PAGE_ID_TEST";
        when(plan.getSecurity()).thenReturn(Plan.PlanSecurityType.KEY_LESS);
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(
            parameterService.findAsBoolean(eq(GraviteeContext.getExecutionContext()), any(), eq(ParameterReferenceType.ENVIRONMENT))
        ).thenReturn(true);

        final UpdatePlanEntity updatePlan = initUpdatePlanEntity();
        updatePlan.setFlows(null);

        PageEntity unpublishedPage = new PageEntity();
        unpublishedPage.setId(PAGE_ID);
        unpublishedPage.setOrder(1);
        unpublishedPage.setType("MARKDOWN");
        unpublishedPage.setPublished(false);

        planService.update(GraviteeContext.getExecutionContext(), updatePlan);
        verify(pathParametersValidationService, times(1)).validate(any(), any(), any());
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
        when(
            parameterService.findAsBoolean(eq(GraviteeContext.getExecutionContext()), any(), eq(ParameterReferenceType.ENVIRONMENT))
        ).thenReturn(true);

        final UpdatePlanEntity updatePlan = initUpdatePlanEntity();
        updatePlan.setFlows(List.of());

        PageEntity unpublishedPage = new PageEntity();
        unpublishedPage.setId(PAGE_ID);
        unpublishedPage.setOrder(1);
        unpublishedPage.setType("MARKDOWN");
        unpublishedPage.setPublished(false);

        planService.update(GraviteeContext.getExecutionContext(), updatePlan);

        verify(flowCrudService, times(1)).savePlanFlows(updatePlan.getId(), updatePlan.getFlows());
        verify(pathParametersValidationService, times(1)).validate(any(), any(), any());
    }

    @Test
    public void shouldUpdate_withNotPublished_GCUPage_StagingPlan() throws TechnicalException {
        final String PAGE_ID = "PAGE_ID_TEST";
        when(plan.getDefinitionVersion()).thenReturn(V4);
        when(plan.getStatus()).thenReturn(Plan.Status.STAGING);
        when(plan.getType()).thenReturn(Plan.PlanType.API);
        when(plan.getSecurity()).thenReturn(Plan.PlanSecurityType.API_KEY);
        when(plan.getApi()).thenReturn(API_ID);
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(
            parameterService.findAsBoolean(eq(GraviteeContext.getExecutionContext()), any(), eq(ParameterReferenceType.ENVIRONMENT))
        ).thenReturn(true);

        final UpdatePlanEntity updatePlan = initUpdatePlanEntity();
        updatePlan.setGeneralConditions(PAGE_ID);

        when(planRepository.update(any())).thenAnswer(returnsFirstArg());

        planService.update(GraviteeContext.getExecutionContext(), updatePlan);

        verify(planRepository).update(any());
        verify(parameterService).findAsBoolean(eq(GraviteeContext.getExecutionContext()), any(), eq(ParameterReferenceType.ENVIRONMENT));
        verify(pathParametersValidationService, times(1)).validate(any(), any(), any());
    }

    @Test(expected = TagNotAllowedException.class)
    public void should_throw_tagNotAllowedException_when_tag_validation_fails() throws Exception {
        when(plan.getApi()).thenReturn(API_ID);
        when(plan.getSecurity()).thenReturn(Plan.PlanSecurityType.KEY_LESS);
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(
            parameterService.findAsBoolean(eq(GraviteeContext.getExecutionContext()), any(), eq(ParameterReferenceType.ENVIRONMENT))
        ).thenReturn(true);

        final UpdatePlanEntity updatePlan = initUpdatePlanEntity();

        doThrow(new TagNotAllowedException(new String[0]))
            .when(tagsValidationService)
            .validatePlanTagsAgainstApiTags(any(), any(Api.class));

        planService.update(GraviteeContext.getExecutionContext(), updatePlan);
    }

    @Test(expected = InvalidDataException.class)
    public void should_throw_invalid_data_exception_when_security_configuration_is_invalid() throws Exception {
        final UpdatePlanEntity updatePlan = initUpdatePlanEntity();

        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(
            parameterService.findAsBoolean(eq(GraviteeContext.getExecutionContext()), any(), eq(ParameterReferenceType.ENVIRONMENT))
        ).thenReturn(true);

        when(plan.getSecurity()).thenReturn(Plan.PlanSecurityType.OAUTH2);
        when(
            policyService.validatePolicyConfiguration(updatePlan.getSecurity().getType(), updatePlan.getSecurity().getConfiguration())
        ).thenThrow(new InvalidDataException("Mock exception"));

        planService.update(GraviteeContext.getExecutionContext(), updatePlan);
    }

    @NonNull
    private UpdatePlanEntity initUpdatePlanEntity() {
        final PlanSecurity planSecurity = new PlanSecurity("oauth2", "{ \"foo\": \"bar\"}");
        final UpdatePlanEntity updatePlan = new UpdatePlanEntity();
        updatePlan.setId(PLAN_ID);
        updatePlan.setValidation(PlanValidationType.AUTO);
        updatePlan.setName("NameUpdated");
        updatePlan.setTags(Set.of("tag1"));
        updatePlan.setSecurity(planSecurity);
        updatePlan.setFlows(List.of(new Flow()));
        return updatePlan;
    }

    @Test(expected = GroupNotFoundException.class)
    public void should_throw_GroupNotFoundException_if_updated_excluded_group_not_found() throws TechnicalException {
        when(plan.getStatus()).thenReturn(Plan.Status.STAGING);
        when(plan.getType()).thenReturn(Plan.PlanType.API);
        when(plan.getSecurity()).thenReturn(Plan.PlanSecurityType.KEY_LESS);
        when(plan.getApi()).thenReturn(API_ID);
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(
            parameterService.findAsBoolean(eq(GraviteeContext.getExecutionContext()), any(), eq(ParameterReferenceType.ENVIRONMENT))
        ).thenReturn(true);

        final UpdatePlanEntity updatePlan = initUpdatePlanEntity();
        updatePlan.setExcludedGroups(List.of("not-existing-group"));
        planService.update(GraviteeContext.getExecutionContext(), updatePlan);
    }
}

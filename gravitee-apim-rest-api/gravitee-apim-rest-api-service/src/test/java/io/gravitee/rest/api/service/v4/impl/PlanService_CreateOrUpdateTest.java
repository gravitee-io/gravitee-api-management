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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.flow.crud_service.FlowCrudService;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Plan;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.model.v4.plan.PlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanValidationType;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.PolicyService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.PlanNotFoundException;
import io.gravitee.rest.api.service.processor.SynchronizationService;
import io.gravitee.rest.api.service.v4.FlowService;
import io.gravitee.rest.api.service.v4.PlanSearchService;
import io.gravitee.rest.api.service.v4.PlanService;
import io.gravitee.rest.api.service.v4.mapper.PlanMapper;
import io.gravitee.rest.api.service.v4.validation.FlowValidationService;
import io.gravitee.rest.api.service.v4.validation.PathParametersValidationService;
import io.gravitee.rest.api.service.v4.validation.TagsValidationService;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class PlanService_CreateOrUpdateTest {

    private static final String PLAN_ID = UUID.randomUUID().toString();
    private static final String ENVIRONMENT_ID = "my-environment";
    public static final String API_ID = "api-id";

    @Spy
    @InjectMocks
    private PlanService planService = new PlanServiceImpl();

    @Mock
    private PlanSearchService planSearchService;

    @Mock
    private PlanRepository planRepository;

    @Spy
    private PlanMapper planMapper;

    @Mock
    private ParameterService parameterService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private AuditService auditService;

    @Mock
    private PlanEntity planEntity;

    @Mock
    private FlowValidationService flowValidationService;

    @Mock
    private Api api;

    @Mock
    private PolicyService policyService;

    @Mock
    private ApiRepository apiRepository;

    @Mock
    private PathParametersValidationService pathParametersValidationService;

    @Mock
    private TagsValidationService tagsValidationService;

    @Mock
    private SynchronizationService synchronizationService;

    @Mock
    private FlowService flowService;

    @Mock
    private FlowCrudService flowCrudService;

    @Mock
    private GroupService groupService;

    @Before
    public void setup() throws Exception {
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));
        when(planEntity.getMode()).thenReturn(PlanMode.STANDARD);
    }

    @Test
    public void shouldUpdateAndHaveId() throws TechnicalException {
        final PlanEntity expected = initPlanEntity("updated");

        when(planEntity.getId()).thenReturn(PLAN_ID);
        when(planEntity.getSecurity()).thenReturn(new PlanSecurity("oauth2", "{ \"foo\": \"bar\"}"));
        when(planEntity.getValidation()).thenReturn(PlanValidationType.AUTO);
        doReturn(new PlanEntity()).when(planSearchService).findById(GraviteeContext.getExecutionContext(), PLAN_ID);
        mockPrivateUpdate(expected);

        final PlanEntity actual = planService.createOrUpdatePlan(GraviteeContext.getExecutionContext(), planEntity);

        assertThat(actual.getId()).isEqualTo(expected.getId());
        verify(planSearchService, times(1)).findById(GraviteeContext.getExecutionContext(), PLAN_ID);
        verify(pathParametersValidationService, never()).validate(any(), any(), any());
    }

    @Test
    public void shouldUpdateAndHaveNoId() throws TechnicalException {
        final PlanEntity expected = initPlanEntity("updated");

        when(planEntity.getId()).thenReturn(null);
        when(planEntity.getSecurity()).thenReturn(new PlanSecurity("oauth2", "{ \"foo\": \"bar\"}"));
        when(planEntity.getValidation()).thenReturn(PlanValidationType.AUTO);
        when(planEntity.getApiId()).thenReturn(API_ID);
        mockPrivateCreate(expected);

        final PlanEntity actual = planService.createOrUpdatePlan(GraviteeContext.getExecutionContext(), planEntity);

        assertThat(actual.getId()).isEqualTo(expected.getId());
        verify(pathParametersValidationService, never()).validate(any(), any(), any());
    }

    @Test
    public void shouldCreateAndHaveId() throws TechnicalException {
        final PlanEntity expected = initPlanEntity("created");

        when(planEntity.getId()).thenReturn(PLAN_ID);
        when(planEntity.getSecurity()).thenReturn(new PlanSecurity("oauth2", "{ \"foo\": \"bar\"}"));
        when(planEntity.getValidation()).thenReturn(PlanValidationType.AUTO);
        when(planEntity.getApiId()).thenReturn(API_ID);
        mockPrivateCreate(expected);

        doThrow(PlanNotFoundException.class).when(planSearchService).findById(GraviteeContext.getExecutionContext(), PLAN_ID);

        final PlanEntity actual = planService.createOrUpdatePlan(GraviteeContext.getExecutionContext(), planEntity);

        assertThat(actual.getId()).isEqualTo(expected.getId());
        verify(planSearchService, times(1)).findById(GraviteeContext.getExecutionContext(), PLAN_ID);
        verify(pathParametersValidationService, never()).validate(any(), any(), any());
    }

    @Test
    public void shouldCreateAndHaveNoId() throws TechnicalException {
        final PlanEntity expected = initPlanEntity("created");

        when(planEntity.getId()).thenReturn(null);
        when(planEntity.getSecurity()).thenReturn(new PlanSecurity("oauth2", "{ \"foo\": \"bar\"}"));
        when(planEntity.getValidation()).thenReturn(PlanValidationType.AUTO);
        when(planEntity.getApiId()).thenReturn(API_ID);
        mockPrivateCreate(expected);

        final PlanEntity actual = planService.createOrUpdatePlan(GraviteeContext.getExecutionContext(), planEntity);

        assertThat(actual.getId()).isEqualTo(expected.getId());
        verify(pathParametersValidationService, never()).validate(any(), any(), any());
    }

    private void mockPrivateUpdate(PlanEntity expected) throws TechnicalException {
        Plan plan = mock(Plan.class);
        when(plan.getStatus()).thenReturn(Plan.Status.STAGING);
        when(plan.getType()).thenReturn(Plan.PlanType.API);
        when(plan.getSecurity()).thenReturn(Plan.PlanSecurityType.API_KEY);
        when(plan.getApi()).thenReturn(API_ID);
        when(plan.getId()).thenReturn(expected.getId());
        when(plan.getValidation()).thenReturn(Plan.PlanValidationType.AUTO);
        when(plan.getMode()).thenReturn(Plan.PlanMode.STANDARD);
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(
            parameterService.findAsBoolean(eq(GraviteeContext.getExecutionContext()), any(), eq(ParameterReferenceType.ENVIRONMENT))
        ).thenReturn(true);

        when(planRepository.update(any())).thenAnswer(returnsFirstArg());
    }

    private void mockPrivateCreate(PlanEntity expected) throws TechnicalException {
        Plan plan = mock(Plan.class);
        when(plan.getStatus()).thenReturn(Plan.Status.STAGING);
        when(plan.getType()).thenReturn(Plan.PlanType.API);
        when(plan.getSecurity()).thenReturn(Plan.PlanSecurityType.API_KEY);
        when(plan.getApi()).thenReturn(API_ID);
        when(plan.getId()).thenReturn(expected.getId() == null ? "created" : expected.getId());
        when(plan.getValidation()).thenReturn(Plan.PlanValidationType.AUTO);
        when(plan.getMode()).thenReturn(Plan.PlanMode.STANDARD);
        lenient().when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(planRepository.create(any())).thenReturn(plan);
        when(
            parameterService.findAsBoolean(eq(GraviteeContext.getExecutionContext()), any(), eq(ParameterReferenceType.ENVIRONMENT))
        ).thenReturn(true);
    }

    private PlanEntity initPlanEntity(String planId) {
        final PlanEntity plan = new PlanEntity();
        plan.setId(planId);
        plan.setSecurity(new PlanSecurity("oauth2", "{ \"foo\": \"bar\"}"));
        plan.setValidation(PlanValidationType.AUTO);
        plan.setName("NameUpdated");
        plan.setTags(Set.of("tag1"));
        plan.setFlows(List.of(new Flow()));
        plan.setMode(PlanMode.STANDARD);
        return plan;
    }
}

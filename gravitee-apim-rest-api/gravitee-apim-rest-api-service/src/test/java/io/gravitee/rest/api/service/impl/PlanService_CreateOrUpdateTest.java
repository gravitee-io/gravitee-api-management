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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.rest.api.model.PlanEntity;
import io.gravitee.rest.api.model.UpdatePlanEntity;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.PlanService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.converter.PlanConverter;
import io.gravitee.rest.api.service.exceptions.PlanNotFoundException;
import java.util.Optional;
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

    private static final String PLAN_ID = "my-plan";
    private static final String ENVIRONMENT_ID = "my-environment";

    @Spy
    @InjectMocks
    private PlanService planService = new PlanServiceImpl();

    @Mock
    private PlanRepository planRepository;

    @Mock
    private PlanConverter planConverter;

    @Mock
    private ParameterService parameterService;

    @Mock
    private ApiService apiService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private AuditService auditService;

    @Mock
    private PlanEntity planEntity;

    @Test
    public void shouldUpdateAndHaveId() throws TechnicalException {
        final PlanEntity expected = new PlanEntity();
        expected.setId("updated");

        when(this.planEntity.getId()).thenReturn(PLAN_ID);
        when(planConverter.toUpdatePlanEntity(planEntity)).thenCallRealMethod();
        doReturn(new PlanEntity()).when(planService).findById(GraviteeContext.getExecutionContext(), PLAN_ID);
        doReturn(expected).when(planService).update(eq(GraviteeContext.getExecutionContext()), any(UpdatePlanEntity.class));

        final PlanEntity actual = planService.createOrUpdatePlan(GraviteeContext.getExecutionContext(), this.planEntity);

        assertThat(actual.getId()).isEqualTo(expected.getId());
        verify(planService, times(1)).findById(GraviteeContext.getExecutionContext(), PLAN_ID);
        verify(planService, times(1)).update(eq(GraviteeContext.getExecutionContext()), any(UpdatePlanEntity.class));
    }

    @Test
    public void shouldCreateAndHaveId() throws TechnicalException {
        final PlanEntity expected = new PlanEntity();
        expected.setId("created");

        when(planEntity.getId()).thenReturn(PLAN_ID);
        doThrow(PlanNotFoundException.class).when(planService).findById(GraviteeContext.getExecutionContext(), PLAN_ID);
        doReturn(expected).when(planService).create(eq(GraviteeContext.getExecutionContext()), any());

        final PlanEntity actual = planService.createOrUpdatePlan(GraviteeContext.getExecutionContext(), planEntity);

        assertThat(actual.getId()).isEqualTo(expected.getId());
        verify(planService, times(1)).findById(GraviteeContext.getExecutionContext(), PLAN_ID);
        verify(planService, times(1)).create(eq(GraviteeContext.getExecutionContext()), any());
    }

    @Test
    public void shouldCreateAndHaveNoId() throws TechnicalException {
        final PlanEntity expected = new PlanEntity();
        expected.setId("created");

        when(planEntity.getId()).thenReturn(null);
        when(planRepository.findById(any())).thenReturn(Optional.empty());

        doReturn(expected).when(planService).create(eq(GraviteeContext.getExecutionContext()), any());

        final PlanEntity actual = planService.createOrUpdatePlan(GraviteeContext.getExecutionContext(), planEntity);

        assertThat(actual.getId()).isEqualTo(expected.getId());
        verify(planService, times(1)).findById(eq(GraviteeContext.getExecutionContext()), any());
        verify(planService, times(1)).create(eq(GraviteeContext.getExecutionContext()), any());
    }

    @Test
    public void shouldUpdateAndHaveNoId() throws TechnicalException {
        final PlanEntity expected = new PlanEntity();
        expected.setId("updated");

        when(planEntity.getId()).thenReturn(null);
        when(planConverter.toUpdatePlanEntity(planEntity)).thenCallRealMethod();
        doReturn(expected).when(planService).findById(eq(GraviteeContext.getExecutionContext()), any());
        doReturn(expected).when(planService).update(eq(GraviteeContext.getExecutionContext()), any(UpdatePlanEntity.class));

        final PlanEntity actual = planService.createOrUpdatePlan(GraviteeContext.getExecutionContext(), planEntity);

        assertThat(actual.getId()).isEqualTo(expected.getId());
        verify(planService, times(1)).findById(eq(GraviteeContext.getExecutionContext()), any());
        verify(planService, times(1)).update(eq(GraviteeContext.getExecutionContext()), any(UpdatePlanEntity.class));
    }
}

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

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.model.Plan;
import io.gravitee.rest.api.model.NewPlanEntity;
import io.gravitee.rest.api.model.PlanEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.service.converter.PlanConverter;
import io.gravitee.rest.api.service.exceptions.PlanNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.PlanServiceImpl;
import java.util.ArrayList;
import java.util.Optional;
import org.checkerframework.checker.units.qual.A;
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
        doReturn(new PlanEntity()).when(planService).findById(PLAN_ID);
        doReturn(expected).when(planService).update(any());

        final PlanEntity actual = planService.createOrUpdatePlan(this.planEntity, ENVIRONMENT_ID);

        assertThat(actual.getId()).isEqualTo(expected.getId());
        verify(planService, times(1)).findById(PLAN_ID);
        verify(planService, times(1)).update(any());
    }

    @Test
    public void shouldCreateAndHaveId() throws TechnicalException {
        final PlanEntity expected = new PlanEntity();
        expected.setId("created");

        when(planEntity.getId()).thenReturn(PLAN_ID);
        doThrow(PlanNotFoundException.class).when(planService).findById(PLAN_ID);
        doReturn(expected).when(planService).create(any());

        final PlanEntity actual = planService.createOrUpdatePlan(planEntity, ENVIRONMENT_ID);

        assertThat(actual.getId()).isEqualTo(expected.getId());
        verify(planService, times(1)).findById(PLAN_ID);
        verify(planService, times(1)).create(any());
    }

    @Test
    public void shouldCreateAndHaveNoId() throws TechnicalException {
        final PlanEntity expected = new PlanEntity();
        expected.setId("created");

        when(planEntity.getId()).thenReturn(null);
        when(planRepository.findById(any())).thenReturn(Optional.empty());

        doReturn(expected).when(planService).create(any());

        final PlanEntity actual = planService.createOrUpdatePlan(planEntity, ENVIRONMENT_ID);

        assertThat(actual.getId()).isEqualTo(expected.getId());
        verify(planService, times(1)).findById(any());
        verify(planService, times(1)).create(any());
    }

    @Test
    public void shouldUpdateAndHaveNoId() throws TechnicalException {
        final PlanEntity expected = new PlanEntity();
        expected.setId("updated");

        when(planEntity.getId()).thenReturn(null);
        doReturn(expected).when(planService).findById(any());
        doReturn(expected).when(planService).update(any());

        final PlanEntity actual = planService.createOrUpdatePlan(planEntity, ENVIRONMENT_ID);

        assertThat(actual.getId()).isEqualTo(expected.getId());
        verify(planService, times(1)).findById(any());
        verify(planService, times(1)).update(any());
    }
}

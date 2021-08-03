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
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.rest.api.model.PlanEntity;
import io.gravitee.rest.api.service.exceptions.PlanNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.PlanServiceImpl;
import java.util.ArrayList;
import org.junit.Test;
import org.junit.runner.RunWith;
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
    private PlanService planService = new PlanServiceImpl();

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
        doReturn(emptyList()).when(planService).search(any());
        doReturn(expected).when(planService).create(any());

        final PlanEntity actual = planService.createOrUpdatePlan(planEntity, ENVIRONMENT_ID);

        assertThat(actual.getId()).isEqualTo(expected.getId());
        verify(planService, times(1)).search(any());
        verify(planService, times(1)).create(any());
    }

    @Test
    public void shouldUpdateAndHaveNoId() throws TechnicalException {
        final PlanEntity expected = new PlanEntity();
        expected.setId("updated");

        when(planEntity.getId()).thenReturn(null);
        doReturn(singletonList(expected)).when(planService).search(any());
        doReturn(expected).when(planService).update(any());

        final PlanEntity actual = planService.createOrUpdatePlan(planEntity, ENVIRONMENT_ID);

        assertThat(actual.getId()).isEqualTo(expected.getId());
        verify(planService, times(1)).search(any());
        verify(planService, times(1)).update(any());
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldThrowExceptionWhenTooManyResults() throws TechnicalException {
        final PlanEntity expected = new PlanEntity();
        expected.setId("updated");

        final ArrayList<PlanEntity> searchResult = new ArrayList<>();
        searchResult.add(new PlanEntity());
        searchResult.add(new PlanEntity());

        when(planEntity.getId()).thenReturn(null);
        doReturn(searchResult).when(planService).search(any());

        final PlanEntity actual = planService.createOrUpdatePlan(planEntity, ENVIRONMENT_ID);

        assertThat(actual.getId()).isEqualTo(expected.getId());
        verify(planService, times(1)).search(any());
    }
}

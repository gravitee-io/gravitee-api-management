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

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.model.Plan;
import io.gravitee.rest.api.model.PlanEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.PlanService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.converter.PlanConverter;
import io.gravitee.rest.api.service.exceptions.PlanNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class PlanService_FindByIdTest {

    private static final String PLAN_ID = "my-plan";

    @InjectMocks
    private PlanService planService = new PlanServiceImpl();

    @Mock
    private PlanRepository planRepository;

    @Mock
    private Plan plan;

    @Mock
    private PlanConverter planConverter;

    @Test
    public void shouldFindById() throws TechnicalException {
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));

        PlanEntity planEntityFromConverter = mock(PlanEntity.class);
        when(planConverter.toPlanEntity(plan)).thenReturn(planEntityFromConverter);

        final PlanEntity resultPlanEntity = planService.findById(GraviteeContext.getExecutionContext(), PLAN_ID);

        assertSame(resultPlanEntity, planEntityFromConverter);
    }

    @Test(expected = PlanNotFoundException.class)
    public void shouldNotFindByIdBecauseNotExists() throws TechnicalException {
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.empty());

        planService.findById(GraviteeContext.getExecutionContext(), PLAN_ID);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotFindByIdBecauseTechnicalException() throws TechnicalException {
        when(planRepository.findById(PLAN_ID)).thenThrow(TechnicalException.class);

        planService.findById(GraviteeContext.getExecutionContext(), PLAN_ID);
    }
}

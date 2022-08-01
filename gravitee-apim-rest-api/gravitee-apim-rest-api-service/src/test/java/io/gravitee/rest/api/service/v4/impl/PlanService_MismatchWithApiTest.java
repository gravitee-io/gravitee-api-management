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
package io.gravitee.rest.api.service.v4.impl;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.model.Plan;
import io.gravitee.rest.api.service.v4.PlanService;
import io.gravitee.rest.api.service.v4.impl.PlanServiceImpl;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class PlanService_MismatchWithApiTest {

    private static final String PLAN_ID = "my-plan";
    private static final String API_ID = "my-api";

    @InjectMocks
    private PlanService planService = new PlanServiceImpl();

    @Mock
    private PlanRepository planRepository;

    @Test
    public void shouldNotMismatchWithApi() throws TechnicalException {
        Plan plan = mock(Plan.class);
        when(plan.getApi()).thenReturn(API_ID);
        when(planRepository.findByIdIn(List.of(PLAN_ID))).thenReturn(Set.of(plan));

        assertFalse(planService.anyPlanMismatchWithApi(List.of(PLAN_ID), API_ID));
    }

    @Test
    public void shouldMismatchWithApi() throws TechnicalException {
        Plan plan = mock(Plan.class);
        when(plan.getApi()).thenReturn("OTHER_API");
        when(planRepository.findByIdIn(List.of(PLAN_ID))).thenReturn(Set.of(plan));

        assertTrue(planService.anyPlanMismatchWithApi(List.of(PLAN_ID), API_ID));
    }
}

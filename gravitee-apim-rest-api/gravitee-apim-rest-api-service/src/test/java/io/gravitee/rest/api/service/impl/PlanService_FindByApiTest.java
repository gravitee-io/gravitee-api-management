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

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.flow.Flow;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.model.Plan;
import io.gravitee.repository.management.model.flow.FlowReferenceType;
import io.gravitee.rest.api.model.PlanEntity;
import io.gravitee.rest.api.service.PlanService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.configuration.flow.FlowService;
import io.gravitee.rest.api.service.converter.PlanConverter;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
public class PlanService_FindByApiTest {

    private static final String API_ID = "my-api";

    @InjectMocks
    private PlanService planService = new PlanServiceImpl();

    @Mock
    private PlanRepository planRepository;

    @Mock
    private List<Flow> plan1Flows;

    @Mock
    private List<Flow> plan2Flows;

    @Mock
    private PlanConverter planConverter;

    @Mock
    private FlowService flowService;

    @Test
    public void shouldFindByApi() throws TechnicalException {
        when(planConverter.toPlanEntity(any(), any())).thenCallRealMethod();

        Plan plan1 = mockPlan("plan1");
        Plan plan2 = mockPlan("plan2");

        when(planRepository.findByApi(API_ID)).thenReturn(Set.of(plan1, plan2));
        when(flowService.findByReference(FlowReferenceType.PLAN, plan1.getId())).thenReturn(plan1Flows);
        when(flowService.findByReference(FlowReferenceType.PLAN, plan2.getId())).thenReturn(plan2Flows);

        List<PlanEntity> plans = new ArrayList<>(planService.findByApi(GraviteeContext.getExecutionContext(), API_ID));

        assertNotNull(plans);
        assertEquals(2, plans.size());
        assertSame(plan1Flows, plans.get(0).getFlows());
        assertSame(plan2Flows, plans.get(1).getFlows());
    }

    private Plan mockPlan(String id) {
        Plan plan = mock(Plan.class);
        when(plan.getApi()).thenReturn(API_ID);
        when(plan.getId()).thenReturn(id);
        when(plan.getType()).thenReturn(Plan.PlanType.API);
        when(plan.getValidation()).thenReturn(Plan.PlanValidationType.AUTO);
        return plan;
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotFindByApiBecauseTechnicalException() throws TechnicalException {
        when(planRepository.findByApi(API_ID)).thenThrow(TechnicalException.class);

        planService.findByApi(GraviteeContext.getExecutionContext(), API_ID);
    }
}

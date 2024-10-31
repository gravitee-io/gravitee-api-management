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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import io.gravitee.rest.api.model.v4.nativeapi.NativePlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanType;
import io.gravitee.rest.api.model.v4.plan.PlanValidationType;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.v4.PlanSearchService;
import io.gravitee.rest.api.service.v4.PlanService;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Jourdi WALLER (jourdi.waller at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class PlanService_FindNativePlansByApiTest {

    private static final String API_ID = "my-api";

    @InjectMocks
    private PlanService planService = new PlanServiceImpl();

    @Mock
    private PlanSearchService planSearchService;

    @Test
    public void shouldFindNativePlansByApi() {
        var plan1 = createNativePlanEntity("plan1");
        var plan2 = createNativePlanEntity("plan2");

        when(planSearchService.findByApi(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(Set.of(plan1, plan2));

        var plans = planService.findNativePlansByApi(GraviteeContext.getExecutionContext(), API_ID);

        assertNotNull(plans);
        assertEquals(2, plans.size());
    }

    private NativePlanEntity createNativePlanEntity(String id) {
        NativePlanEntity planEntity = new NativePlanEntity();
        planEntity.setId(id);
        planEntity.setApiId(API_ID);
        planEntity.setType(PlanType.API);
        planEntity.setValidation(PlanValidationType.AUTO);
        return planEntity;
    }
}

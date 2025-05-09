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
package io.gravitee.rest.api.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.rest.api.model.PlanEntity;
import io.gravitee.rest.api.model.PlanType;
import io.gravitee.rest.api.model.PlanValidationType;
import io.gravitee.rest.api.service.PlanService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.v4.EntityConversionService;
import io.gravitee.rest.api.service.v4.PlanSearchService;
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
    private PlanSearchService planSearchService;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private EntityConversionService entityConversionService;

    @Test
    public void shouldFindByApi() {
        PlanEntity plan1 = createPlanEntity("plan1");
        PlanEntity plan2 = createPlanEntity("plan2");

        when(planSearchService.findByApi(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(Set.of(plan1, plan2));
        when(entityConversionService.convertV4ToPlanEntity(plan1)).thenReturn(plan1);
        when(entityConversionService.convertV4ToPlanEntity(plan2)).thenReturn(plan2);

        List<PlanEntity> plans = new ArrayList<>(planService.findByApi(GraviteeContext.getExecutionContext(), API_ID));

        assertNotNull(plans);
        assertEquals(2, plans.size());
    }

    private PlanEntity createPlanEntity(String id) {
        PlanEntity planEntity = new PlanEntity();
        planEntity.setId(id);
        planEntity.setApi(API_ID);
        planEntity.setType(PlanType.API);
        planEntity.setValidation(PlanValidationType.AUTO);
        return planEntity;
    }
}

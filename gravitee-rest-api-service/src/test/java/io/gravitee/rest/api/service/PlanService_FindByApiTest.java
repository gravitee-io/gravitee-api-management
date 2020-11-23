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

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.model.Plan;
import io.gravitee.rest.api.model.PlanEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.service.PlanService;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.PlanServiceImpl;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.Set;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class PlanService_FindByApiTest {

    private static final String PLAN_ID = "my-plan";
    private static final String API_ID = "my-api";

    @InjectMocks
    private PlanService planService = new PlanServiceImpl();

    @Mock
    private PlanRepository planRepository;

    @Mock
    private ApiService apiService;

    @Mock
    private Plan plan;

    @Mock
    private ApiEntity api;

    @Test
    public void shouldFindByApi() throws TechnicalException {
        when(plan.getType()).thenReturn(Plan.PlanType.API);
        when(plan.getId()).thenReturn(PLAN_ID);
        when(plan.getValidation()).thenReturn(Plan.PlanValidationType.AUTO);
        when(plan.getApi()).thenReturn(API_ID);
        when(apiService.findById(API_ID)).thenReturn(api);
        when(planRepository.findByApi(API_ID)).thenReturn(Collections.singleton(plan));

        final Set<PlanEntity> plans = planService.findByApi(API_ID);

        assertNotNull(plans);
        assertTrue(! plans.isEmpty());
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotFindByApiBecauseTechnicalException() throws TechnicalException {
        when(planRepository.findByApi(API_ID)).thenThrow(TechnicalException.class);

        planService.findByApi(API_ID);
    }
}

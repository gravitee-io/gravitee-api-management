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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.model.Plan;
import java.util.Map;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class PlanService_FindByIdAsMapTest {

    @InjectMocks
    private PlanServiceImpl planService;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Test
    public void shouldFindByIdAsMap() throws TechnicalException {
        Plan plan = new Plan();
        plan.setSecurity(Plan.PlanSecurityType.API_KEY);
        plan.setId("my-id");
        plan.setStatus(Plan.Status.STAGING);
        plan.setCrossId("cross-id");
        plan.setOrder(12);
        plan.setName("my-plan-name");
        plan.setDescription("my-plan-description");

        when(objectMapper.convertValue(plan, Map.class)).thenAnswer(i -> new ObjectMapper().convertValue(i.getArgument(0), Map.class));
        when(planRepository.findById("my-id")).thenReturn(Optional.of(plan));

        Map resultMap = planService.findByIdAsMap("my-id");

        assertEquals("my-id", resultMap.get("id"));
        assertEquals("cross-id", resultMap.get("crossId"));
        assertEquals("my-plan-name", resultMap.get("name"));
        assertEquals("my-plan-description", resultMap.get("description"));
        assertEquals("API_KEY", resultMap.get("security"));
    }
}

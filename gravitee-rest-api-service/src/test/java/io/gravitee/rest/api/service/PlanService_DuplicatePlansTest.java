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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.model.Plan;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.api.ApiLifecycleState;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.service.common.RandomString;
import io.gravitee.rest.api.service.impl.PlanServiceImpl;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PlanService_DuplicatePlansTest {

    private static final String API_ID = "myAPI";
    private static final String ENVIRONMENT_ID = "ba01aef0-e3da-4499-81ae-f0e3daa4995a";

    @InjectMocks
    private final PlanServiceImpl planService = new PlanServiceImpl();

    @Mock
    private PlanRepository planRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private ParameterService parameterService;

    @Mock
    private ApiService apiService;

    @Captor
    ArgumentCaptor<Plan> pageCaptor;

    @Test
    public void shouldDuplicatePlans() throws TechnicalException {
        PlanEntity plan1 = getAPlanEntity("Plan 1", PlanSecurityType.KEY_LESS);
        PlanEntity plan2 = getAPlanEntity("Plan 2", PlanSecurityType.KEY_LESS);
        PlanEntity plan3 = getAPlanEntity("Plan 3", PlanSecurityType.OAUTH2);

        ApiEntity apiEntity = new ApiEntity();
        apiEntity.setLifecycleState(ApiLifecycleState.PUBLISHED);
        apiEntity.setGraviteeDefinitionVersion(DefinitionVersion.V2.getLabel());
        when(apiService.findById(anyString())).thenReturn(apiEntity);

        when(planRepository.create(any(Plan.class))).thenAnswer(returnsFirstArg());
        when(parameterService.findAsBoolean(any(Key.class), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn(true);

        planService.duplicatePlans(Set.of(plan1, plan2, plan3), ENVIRONMENT_ID, API_ID);

        verify(planRepository, times(3)).create(pageCaptor.capture());

        List<Plan> createdPages = pageCaptor.getAllValues();
        assertThat(createdPages.size()).isEqualTo(3);
        assertThat(createdPages).extracting(Plan::getName).containsExactlyInAnyOrder("Plan 1", "Plan 2", "Plan 3");

        assertThat(createdPages).extracting(Plan::getId).doesNotContain(plan1.getId(), plan2.getId(), plan3.getId());
    }

    @NotNull
    private PlanEntity getAPlanEntity(String name, PlanSecurityType oauth2) {
        PlanEntity plan = new PlanEntity();
        plan.setId(RandomString.generate());
        plan.setSecurity(oauth2);
        plan.setType(PlanType.API);
        plan.setStatus(PlanStatus.PUBLISHED);
        plan.setPaths(Collections.emptyMap());
        plan.setName(name);
        plan.setValidation(PlanValidationType.AUTO);
        return plan;
    }
}

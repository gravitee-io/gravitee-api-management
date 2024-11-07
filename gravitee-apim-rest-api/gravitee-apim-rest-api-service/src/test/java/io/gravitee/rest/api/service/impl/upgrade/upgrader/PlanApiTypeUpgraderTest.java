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
package io.gravitee.rest.api.service.impl.upgrade.upgrader;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Plan;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Sergii ILLICHEVSKYI (sergii.illichevskyi at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class PlanApiTypeUpgraderTest {

    @InjectMocks
    private PlanApiTypeUpgrader planApiTypeUpgrader;

    @Mock
    private ApiRepository apiRepository;

    @Mock
    private PlanRepository planRepository;

    @Test
    public void shouldUpdatePlansWithCorrectApiType() throws Exception {
        Api api = new Api();
        api.setId("api1");
        api.setType(ApiType.MESSAGE);
        api.setDefinitionVersion(DefinitionVersion.V4);

        Plan plan = new Plan();
        plan.setId("plan1");

        when(apiRepository.search(any(), any(), any())).thenReturn(Stream.of(api));
        when(planRepository.findByApi("api1")).thenReturn(Set.of(plan));

        planApiTypeUpgrader.upgrade();

        verify(planRepository, times(1)).update(argThat(updatedPlan -> updatedPlan.getApiType() == ApiType.MESSAGE));
    }

    @Test
    public void shouldSkipUpgradeWhenNoV4ApisExist() throws Exception {
        when(apiRepository.search(any(), any(), any())).thenReturn(Stream.empty());

        planApiTypeUpgrader.upgrade();

        verify(planRepository, never()).update(any());
    }

    @Test
    public void shouldSkipUpgradeWhenNoPlansExistForApi() throws Exception {
        Api api = new Api();
        api.setId("api1");
        api.setType(ApiType.MESSAGE);
        api.setDefinitionVersion(DefinitionVersion.V4);

        when(apiRepository.search(any(), any(), any())).thenReturn(Stream.of(api));
        when(planRepository.findByApi("api1")).thenReturn(Set.of());

        planApiTypeUpgrader.upgrade();

        verify(planRepository, never()).update(any());
    }

    @Test
    public void shouldHandleExceptionDuringUpgrade() throws Exception {
        when(apiRepository.search(any(), any(), any())).thenThrow(new RuntimeException());

        boolean result = planApiTypeUpgrader.upgrade();

        assertFalse(result);
    }

    @Test
    public void shouldSkipUpgradeIfApiIsNotV4() throws Exception {
        Api api = new Api();
        api.setId("api1");
        api.setType(ApiType.MESSAGE);
        api.setDefinitionVersion(DefinitionVersion.V2);

        when(apiRepository.search(any(), any(), any())).thenReturn(Stream.of(api));

        planApiTypeUpgrader.upgrade();

        verify(planRepository, never()).update(any());
    }

    @Test
    public void shouldLogErrorWhenUpdatingPlanFails() throws Exception {
        Api api = new Api();
        api.setId("api1");
        api.setType(ApiType.MESSAGE);
        api.setDefinitionVersion(DefinitionVersion.V4);

        Plan plan = new Plan();
        plan.setId("plan1");

        when(apiRepository.search(any(), any(), any())).thenReturn(Stream.of(api));
        when(planRepository.findByApi("api1")).thenReturn(Set.of(plan));
        doThrow(new RuntimeException()).when(planRepository).update(plan);

        planApiTypeUpgrader.upgrade();

        verify(planRepository, times(1)).update(any());
    }
}

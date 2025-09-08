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
package io.gravitee.rest.api.services.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.event.use_case.CleanupEventsUseCase;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.OrganizationEntity;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.OrganizationService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;

@ExtendWith(MockitoExtension.class)
class ScheduledEventsCleaningServiceTest {

    @Mock
    CleanupEventsUseCase cleanupEventsUseCase;

    @Mock
    OrganizationService organizationService;

    @Mock
    EnvironmentService environmentService;

    @Mock
    TaskScheduler scheduler;

    ScheduledEventsCleaningService sut;

    @Captor
    ArgumentCaptor<CleanupEventsUseCase.Input> inputArgumentCaptor;

    @BeforeEach
    void setup() {
        sut = new ScheduledEventsCleaningService(cleanupEventsUseCase, organizationService, environmentService, scheduler, "", 5, true, 1);
    }

    @Test
    void run_cleaning_on_all_environments() {
        // given
        OrganizationEntity org = new OrganizationEntity();
        org.setId("oId");
        when(organizationService.findAll()).thenReturn(List.of(org));
        EnvironmentEntity env1 = new EnvironmentEntity();
        env1.setId("env1");
        EnvironmentEntity env2 = new EnvironmentEntity();
        env2.setId("env2");
        when(environmentService.findByOrganization(any())).thenReturn(List.of(env1, env2));

        // when
        sut.run();

        // then
        verify(cleanupEventsUseCase, times(2)).execute(inputArgumentCaptor.capture());
        assertThat(inputArgumentCaptor.getAllValues()).map(CleanupEventsUseCase.Input::environmentId).containsOnly("env1", "env2");
        assertThat(inputArgumentCaptor.getAllValues()).map(CleanupEventsUseCase.Input::nbEventsToKeep).containsOnly(5, 5);
    }
}

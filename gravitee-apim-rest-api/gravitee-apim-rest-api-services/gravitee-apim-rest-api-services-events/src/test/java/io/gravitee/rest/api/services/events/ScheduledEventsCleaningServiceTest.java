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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.event.use_case.CleanupEventsUseCase;
import io.gravitee.node.api.cluster.ClusterManager;
import io.gravitee.node.api.cluster.Member;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.OrganizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

@ExtendWith(MockitoExtension.class)
class ScheduledEventsCleaningServiceTest {

    @Mock
    private CleanupEventsUseCase cleanupEventsUseCase;

    @Mock
    private OrganizationService organizationService;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private TaskScheduler scheduler;

    @Mock
    private ClusterManager clusterManager;

    @BeforeEach
    void setUp() {
        Member mockMember = org.mockito.Mockito.mock(Member.class);
        when(mockMember.primary()).thenReturn(true);
        when(clusterManager.self()).thenReturn(mockMember);
    }

    @Test
    void should_schedule_task_when_enabled() throws Exception {
        // Given
        var service = new ScheduledEventsCleaningService(
            cleanupEventsUseCase,
            organizationService,
            environmentService,
            scheduler,
            clusterManager,
            "@daily",
            5,
            true,
            30
        );

        // When
        service.doStart();

        // Then
        verify(scheduler).schedule(eq(service), any(CronTrigger.class));
    }

    @Test
    void should_not_schedule_task_when_disabled() throws Exception {
        // Given
        var service = new ScheduledEventsCleaningService(
            cleanupEventsUseCase,
            organizationService,
            environmentService,
            scheduler,
            clusterManager,
            "@daily",
            5,
            false,
            30
        );

        // When
        service.doStart();

        // Then
        verifyNoInteractions(scheduler);
    }

    @Test
    void should_use_correct_cron_expression() throws Exception {
        // Given
        String cronExpression = "0 0 2 * * ?";
        var service = new ScheduledEventsCleaningService(
            cleanupEventsUseCase,
            organizationService,
            environmentService,
            scheduler,
            clusterManager,
            cronExpression,
            5,
            true,
            30
        );

        // When
        service.doStart();

        // Then
        verify(scheduler).schedule(eq(service), any(CronTrigger.class));
    }
}

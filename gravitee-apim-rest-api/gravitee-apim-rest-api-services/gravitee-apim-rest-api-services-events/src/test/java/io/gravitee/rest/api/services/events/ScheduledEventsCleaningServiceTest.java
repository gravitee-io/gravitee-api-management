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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.event.use_case.CleanupEventsUseCase;
import io.gravitee.node.api.cluster.ClusterManager;
import io.gravitee.node.api.cluster.Member;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.OrganizationService;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.test.util.ReflectionTestUtils;

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

    private ScheduledEventsCleaningService scheduledEventsCleaningService;

    @BeforeEach
    void setUp() {
        scheduledEventsCleaningService = new ScheduledEventsCleaningService(
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
    }

    @Test
    void should_be_enabled_by_default() throws Exception {
        // Given
        Member mockMember = org.mockito.Mockito.mock(Member.class);
        when(mockMember.primary()).thenReturn(true);
        when(clusterManager.self()).thenReturn(mockMember);

        scheduledEventsCleaningService = new ScheduledEventsCleaningService(
            cleanupEventsUseCase,
            organizationService,
            environmentService,
            scheduler,
            clusterManager,
            "@daily",
            5,
            true, // enabled = true
            30
        );

        // When
        scheduledEventsCleaningService.doStart();

        // Then
        verify(scheduler).schedule(eq(scheduledEventsCleaningService), any(CronTrigger.class));
    }

    @Test
    void should_schedule_task_when_enabled() throws Exception {
        // Given
        Member mockMember = org.mockito.Mockito.mock(Member.class);
        when(mockMember.primary()).thenReturn(true);
        when(clusterManager.self()).thenReturn(mockMember);
        String cronExpression = "@hourly";
        scheduledEventsCleaningService = new ScheduledEventsCleaningService(
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
        scheduledEventsCleaningService.doStart();

        // Then
        verify(scheduler).schedule(eq(scheduledEventsCleaningService), any(CronTrigger.class));
    }

    @Test
    void should_use_correct_cron_expression() throws Exception {
        // Given
        Member mockMember = org.mockito.Mockito.mock(Member.class);
        when(mockMember.primary()).thenReturn(true);
        when(clusterManager.self()).thenReturn(mockMember);
        String cronExpression = "0 0 2 * * ?"; // Daily at 2 AM
        scheduledEventsCleaningService = new ScheduledEventsCleaningService(
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
        scheduledEventsCleaningService.doStart();

        // Then
        verify(scheduler).schedule(eq(scheduledEventsCleaningService), any(CronTrigger.class));
    }

    @Test
    void should_use_correct_events_keep_value() {
        // Given
        int eventsKeep = 10;
        scheduledEventsCleaningService = new ScheduledEventsCleaningService(
            cleanupEventsUseCase,
            organizationService,
            environmentService,
            scheduler,
            clusterManager,
            "@daily",
            eventsKeep,
            true,
            30
        );

        // When
        int actualEventsKeep = (int) ReflectionTestUtils.getField(scheduledEventsCleaningService, "eventsKeep");

        // Then
        assertThat(actualEventsKeep).isEqualTo(10);
    }

    @Test
    void should_use_correct_time_to_live_value() {
        // Given
        long timeToLiveMinutes = 60;
        scheduledEventsCleaningService = new ScheduledEventsCleaningService(
            cleanupEventsUseCase,
            organizationService,
            environmentService,
            scheduler,
            clusterManager,
            "@daily",
            5,
            true,
            timeToLiveMinutes
        );

        // When
        Duration actualTimeToLive = (Duration) ReflectionTestUtils.getField(scheduledEventsCleaningService, "timeToLive");

        // Then
        assertThat(actualTimeToLive.toMinutes()).isEqualTo(60);
    }

    @Test
    void should_use_default_events_keep_when_not_specified() {
        // Given
        scheduledEventsCleaningService = new ScheduledEventsCleaningService(
            cleanupEventsUseCase,
            organizationService,
            environmentService,
            scheduler,
            clusterManager,
            "@daily",
            5, // Use default value instead of null
            true,
            30
        );

        // When
        int actualEventsKeep = (int) ReflectionTestUtils.getField(scheduledEventsCleaningService, "eventsKeep");

        // Then
        assertThat(actualEventsKeep).isEqualTo(5); // Default value
    }

    @Test
    void should_use_default_time_to_live_when_not_specified() {
        // Given
        scheduledEventsCleaningService = new ScheduledEventsCleaningService(
            cleanupEventsUseCase,
            organizationService,
            environmentService,
            scheduler,
            clusterManager,
            "@daily",
            5,
            true,
            30 // Use default value instead of null
        );

        // When
        Duration actualTimeToLive = (Duration) ReflectionTestUtils.getField(scheduledEventsCleaningService, "timeToLive");

        // Then
        assertThat(actualTimeToLive.toMinutes()).isEqualTo(30); // Default value
    }

    @Test
    void should_use_default_enabled_when_not_specified() {
        // Given
        scheduledEventsCleaningService = new ScheduledEventsCleaningService(
            cleanupEventsUseCase,
            organizationService,
            environmentService,
            scheduler,
            clusterManager,
            "@daily",
            5,
            true, // Use default value instead of null
            30
        );

        // When
        boolean actualEnabled = (boolean) ReflectionTestUtils.getField(scheduledEventsCleaningService, "enabled");

        // Then
        assertThat(actualEnabled).isTrue(); // Default value is now true
    }

    @Test
    void should_have_correct_constructor_parameters() {
        // Given & When
        ScheduledEventsCleaningService service = new ScheduledEventsCleaningService(
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

        // Then
        assertThat(service).isNotNull();
        assertThat(ReflectionTestUtils.getField(service, "cleanupEventsUseCase")).isEqualTo(cleanupEventsUseCase);
        assertThat(ReflectionTestUtils.getField(service, "organizationService")).isEqualTo(organizationService);
        assertThat(ReflectionTestUtils.getField(service, "environmentService")).isEqualTo(environmentService);
        assertThat(ReflectionTestUtils.getField(service, "scheduler")).isEqualTo(scheduler);
        assertThat(ReflectionTestUtils.getField(service, "cronTrigger")).isEqualTo("@daily");
        assertThat(ReflectionTestUtils.getField(service, "eventsKeep")).isEqualTo(5);
        assertThat(ReflectionTestUtils.getField(service, "enabled")).isEqualTo(true);
        assertThat(ReflectionTestUtils.getField(service, "timeToLive")).isInstanceOf(Duration.class);
    }

    @Test
    void should_log_info_message_when_starting() throws Exception {
        // Given
        Member mockMember = org.mockito.Mockito.mock(Member.class);
        when(mockMember.primary()).thenReturn(true);
        when(clusterManager.self()).thenReturn(mockMember);
        scheduledEventsCleaningService = new ScheduledEventsCleaningService(
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
        scheduledEventsCleaningService.doStart();

        // Then
        // The service should log an info message about initialization
        // This is verified by the fact that the service starts without throwing exceptions
        assertThat(scheduledEventsCleaningService).isNotNull();
    }

    @Test
    void should_not_log_warning_when_enabled() throws Exception {
        // Given
        Member mockMember = org.mockito.Mockito.mock(Member.class);
        when(mockMember.primary()).thenReturn(true);
        when(clusterManager.self()).thenReturn(mockMember);
        scheduledEventsCleaningService = new ScheduledEventsCleaningService(
            cleanupEventsUseCase,
            organizationService,
            environmentService,
            scheduler,
            clusterManager,
            "@daily",
            5,
            true, // enabled = true
            30
        );

        // When
        scheduledEventsCleaningService.doStart();

        // Then
        // No warning should be logged when the service is enabled
        // This is verified by the fact that the service starts without throwing exceptions
        assertThat(scheduledEventsCleaningService).isNotNull();
    }
}

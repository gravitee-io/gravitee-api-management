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
package io.gravitee.rest.api.services.performancetargets;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.performance_target.model.PerformanceTargetSchedule;
import io.gravitee.apim.core.performance_target.use_case.EvaluateDuePerformanceTargetsUseCase;
import io.gravitee.node.api.cluster.ClusterManager;
import io.gravitee.node.api.cluster.Member;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ScheduledPerformanceTargetEvaluationServiceTest {

    private static final String CRON = "0 * * * * *";

    @Mock
    private TaskScheduler scheduler;

    @Mock
    private EvaluateDuePerformanceTargetsUseCase useCase;

    @Mock
    private ClusterManager clusterManager;

    private ScheduledPerformanceTargetEvaluationService service;

    @BeforeEach
    void setUp() {
        service = newService(CRON, true);
    }

    private ScheduledPerformanceTargetEvaluationService newService(String cron, boolean enabled) {
        return new ScheduledPerformanceTargetEvaluationService(scheduler, cron, enabled, 3, 60, 288, useCase, clusterManager);
    }

    private void givenPrimaryNode(boolean primary) {
        Member member = mock(Member.class);
        when(member.primary()).thenReturn(primary);
        when(clusterManager.self()).thenReturn(member);
    }

    @Test
    void should_schedule_the_evaluation_when_enabled() throws Exception {
        service.doStart();

        verify(scheduler).schedule(eq(service), any(CronTrigger.class));
    }

    @Test
    void should_not_schedule_anything_when_disabled() throws Exception {
        service = newService(CRON, false);

        service.doStart();

        verify(scheduler, never()).schedule(any(Runnable.class), any(CronTrigger.class));
        verifyNoInteractions(useCase);
    }

    @Test
    void should_fail_to_start_when_the_configured_cron_is_invalid() {
        service = newService("not-a-cron", true);

        assertThatThrownBy(() -> service.doStart()).isInstanceOf(IllegalArgumentException.class);

        verify(scheduler, never()).schedule(any(Runnable.class), any(CronTrigger.class));
    }

    @Test
    void should_refuse_a_meaningless_backoff_configuration() {
        assertThatThrownBy(() ->
            new ScheduledPerformanceTargetEvaluationService(scheduler, CRON, true, 0, 60, 288, useCase, clusterManager)
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_evaluate_the_due_targets_with_the_configured_schedule() {
        givenPrimaryNode(true);
        when(useCase.execute(any())).thenReturn(new EvaluateDuePerformanceTargetsUseCase.Output(3, List.of()));

        service.run();

        verify(useCase).execute(
            new EvaluateDuePerformanceTargetsUseCase.Input(new PerformanceTargetSchedule(3, Duration.ofMinutes(60), 288))
        );
    }

    @Test
    void should_not_propagate_a_failure_of_the_whole_run() {
        givenPrimaryNode(true);
        when(useCase.execute(any())).thenThrow(new RuntimeException("analytics backend unreachable"));

        assertThatCode(() -> service.run()).doesNotThrowAnyException();
    }

    @Test
    void should_skip_the_run_when_the_node_is_not_primary() {
        givenPrimaryNode(false);

        service.run();

        verifyNoInteractions(useCase);
    }
}

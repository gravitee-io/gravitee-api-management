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
package io.gravitee.rest.api.services.portal.autofetch;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.portal_page.use_case.AutoFetchPortalNavigationItemsUseCase;
import io.gravitee.node.api.cluster.ClusterManager;
import io.gravitee.node.api.cluster.Member;
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
class ScheduledPortalPageAutoFetchServiceTest {

    private static final String CRON = "0 */5 * * * *";

    @Mock
    private TaskScheduler scheduler;

    @Mock
    private AutoFetchPortalNavigationItemsUseCase useCase;

    @Mock
    private ClusterManager clusterManager;

    private ScheduledPortalPageAutoFetchService service;

    @BeforeEach
    void setUp() {
        service = new ScheduledPortalPageAutoFetchService(scheduler, CRON, true, useCase, clusterManager);
    }

    private void givenPrimaryNode(boolean primary) {
        Member member = mock(Member.class);
        when(member.primary()).thenReturn(primary);
        when(clusterManager.self()).thenReturn(member);
    }

    @Test
    void should_schedule_the_auto_fetch_when_enabled() throws Exception {
        service.doStart();

        verify(scheduler).schedule(eq(service), any(CronTrigger.class));
    }

    @Test
    void should_not_schedule_anything_when_disabled() throws Exception {
        service = new ScheduledPortalPageAutoFetchService(scheduler, CRON, false, useCase, clusterManager);

        service.doStart();

        verify(scheduler, never()).schedule(any(Runnable.class), any(CronTrigger.class));
    }

    @Test
    void should_fail_to_start_when_the_configured_cron_is_invalid() {
        service = new ScheduledPortalPageAutoFetchService(scheduler, "not-a-cron", true, useCase, clusterManager);

        assertThatThrownBy(() -> service.doStart()).isInstanceOf(IllegalArgumentException.class);

        verify(scheduler, never()).schedule(any(Runnable.class), any(CronTrigger.class));
    }

    @Test
    void should_run_the_auto_fetch_of_portal_navigation_items() {
        givenPrimaryNode(true);
        when(useCase.execute()).thenReturn(new AutoFetchPortalNavigationItemsUseCase.Output(2, 1));

        service.run();

        verify(useCase, times(1)).execute();
    }

    @Test
    void should_not_propagate_a_failure_of_the_whole_run() {
        givenPrimaryNode(true);
        when(useCase.execute()).thenThrow(new RuntimeException("DB connection failed"));

        assertThatCode(() -> service.run()).doesNotThrowAnyException();

        verify(useCase, times(1)).execute();
    }

    @Test
    void should_skip_the_run_when_the_node_is_not_primary() {
        givenPrimaryNode(false);

        service.run();

        verify(useCase, never()).execute();
    }
}

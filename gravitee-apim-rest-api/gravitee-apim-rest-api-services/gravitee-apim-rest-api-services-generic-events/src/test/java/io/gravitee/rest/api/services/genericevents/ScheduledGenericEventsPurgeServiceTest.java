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
package io.gravitee.rest.api.services.genericevents;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.generic_event.use_case.PurgeGenericEventsUseCase;
import io.gravitee.node.api.cluster.ClusterManager;
import io.gravitee.node.api.cluster.Member;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

@ExtendWith(MockitoExtension.class)
class ScheduledGenericEventsPurgeServiceTest {

    @Mock
    private PurgeGenericEventsUseCase purgeGenericEventsUseCase;

    @Mock
    private TaskScheduler scheduler;

    @Mock
    private ClusterManager clusterManager;

    private ScheduledGenericEventsPurgeService service(boolean enabled) {
        return new ScheduledGenericEventsPurgeService(purgeGenericEventsUseCase, scheduler, clusterManager, "0 * * * * *", enabled, 10080);
    }

    @Test
    void should_schedule_the_purge_when_enabled() throws Exception {
        var service = service(true);

        service.doStart();

        verify(scheduler).schedule(eq(service), any(CronTrigger.class));
    }

    @Test
    void should_not_schedule_the_purge_when_disabled() throws Exception {
        service(false).doStart();

        verifyNoInteractions(scheduler);
    }

    @Test
    void should_purge_with_the_configured_retention_on_the_primary_node() {
        primary(true);
        when(purgeGenericEventsUseCase.execute(any())).thenReturn(new PurgeGenericEventsUseCase.Output(0, 0));

        service(true).run();

        verify(purgeGenericEventsUseCase).execute(argThat(input -> input.deletedRetention().equals(Duration.ofDays(7))));
    }

    @Test
    void should_not_purge_on_a_secondary_node() {
        primary(false);

        service(true).run();

        verifyNoInteractions(purgeGenericEventsUseCase);
    }

    private void primary(boolean primary) {
        var member = mock(Member.class);
        when(member.primary()).thenReturn(primary);
        when(clusterManager.self()).thenReturn(member);
    }
}

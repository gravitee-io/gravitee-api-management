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
package io.gravitee.rest.api.service.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.application_certificate.use_case.ProcessPendingCertificateTransitionsUseCase;
import io.gravitee.apim.core.audit.model.AuditActor;
import java.util.concurrent.ScheduledFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

/**
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ScheduledCertificateRevocationServiceImplTest {

    @Mock
    private TaskScheduler scheduler;

    @Mock
    private ScheduledFuture<?> scheduledFuture;

    @Mock
    private ProcessPendingCertificateTransitionsUseCase processPendingCertificateTransitionsUseCase;

    private ScheduledCertificateRevocationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ScheduledCertificateRevocationServiceImpl(
            scheduler,
            "0 0 0 * * *",
            true,
            processPendingCertificateTransitionsUseCase
        );
    }

    @Test
    void shouldScheduleTaskWhenEnabledAndStarted() throws Exception {
        ArgumentCaptor<CronTrigger> triggerCaptor = ArgumentCaptor.forClass(CronTrigger.class);

        service.doStart();

        verify(scheduler).schedule(any(Runnable.class), triggerCaptor.capture());
        assertEquals("0 0 0 * * *", triggerCaptor.getValue().getExpression());
    }

    @Test
    void shouldThrowWhenCronExpressionIsInvalid() {
        service = new ScheduledCertificateRevocationServiceImpl(scheduler, "not-a-cron", true, processPendingCertificateTransitionsUseCase);

        assertThrows(IllegalArgumentException.class, () -> service.doStart());
    }

    @Test
    void shouldNotScheduleTaskWhenDisabled() throws Exception {
        service = new ScheduledCertificateRevocationServiceImpl(
            scheduler,
            "0 0 0 * * *",
            false,
            processPendingCertificateTransitionsUseCase
        );

        service.doStart();

        verify(scheduler, never()).schedule(any(Runnable.class), any(CronTrigger.class));
    }

    @Test
    void shouldScheduleTaskOnlyOnceWhenStartedTwice() throws Exception {
        service.doStart();
        service.doStart();

        verify(scheduler, times(1)).schedule(any(Runnable.class), any(CronTrigger.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldCancelScheduledFutureWhenStopped() throws Exception {
        when(scheduler.schedule(any(Runnable.class), any(CronTrigger.class))).thenReturn((ScheduledFuture) scheduledFuture);

        service.doStart();
        service.doStop();

        verify(scheduledFuture).cancel(false);
    }

    @Test
    void shouldProcessPendingCertificateTransitions() {
        service.executeRevocation();

        verify(processPendingCertificateTransitionsUseCase).execute(
            new ProcessPendingCertificateTransitionsUseCase.Input(AuditActor.builder().userId("system").build())
        );
    }

    @Test
    void shouldNotThrowWhenUseCaseFails() {
        doThrow(new RuntimeException("DB connection failed")).when(processPendingCertificateTransitionsUseCase).execute(any());

        assertDoesNotThrow(() -> service.executeRevocation());

        verify(processPendingCertificateTransitionsUseCase).execute(any());
    }
}

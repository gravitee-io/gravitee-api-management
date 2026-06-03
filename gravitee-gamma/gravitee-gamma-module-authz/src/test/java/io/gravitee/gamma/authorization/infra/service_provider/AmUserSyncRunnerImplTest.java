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
package io.gravitee.gamma.authorization.infra.service_provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.async_job.crud_service.AsyncJobCrudService;
import io.gravitee.apim.core.async_job.model.AsyncJob;
import io.gravitee.apim.plugin.gamma.api.identity.AmConnection;
import io.gravitee.gamma.authorization.api.AuthzCallerContext;
import io.gravitee.gamma.authorization.core.am.use_case.SyncAmUsersUseCase;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AmUserSyncRunnerImplTest {

    private static final AuthzCallerContext CALLER = AuthzCallerContext.ofUser("org-1", "env-1", "user-1");
    private static final AmConnection CONNECTION = new AmConnection("http://am:8093", "token", "domain-1", "domain-hrid", null);
    private static final AsyncJob JOB = AsyncJob
        .builder()
        .id("job-1")
        .sourceId("org-1")
        .type(AsyncJob.Type.AM_USER_SYNC)
        .status(AsyncJob.Status.PENDING)
        .build();

    private SyncAmUsersUseCase syncAmUsersUseCase;
    private AsyncJobCrudService asyncJobCrudService;
    private ExecutorService executor;
    private AmUserSyncRunnerImpl runner;

    @BeforeEach
    void setUp() {
        syncAmUsersUseCase = mock(SyncAmUsersUseCase.class);
        asyncJobCrudService = mock(AsyncJobCrudService.class);
        executor = Executors.newSingleThreadExecutor();
        runner = new AmUserSyncRunnerImpl(syncAmUsersUseCase, asyncJobCrudService, executor);
    }

    private void awaitCompletion() throws InterruptedException {
        executor.shutdown();
        assertThat(executor.awaitTermination(2, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void completes_the_job_with_the_synced_count() throws InterruptedException {
        when(syncAmUsersUseCase.execute(any())).thenReturn(new SyncAmUsersUseCase.Output(5, 3, 7));

        runner.runAsync(JOB, CALLER, CONNECTION);
        awaitCompletion();

        ArgumentCaptor<AsyncJob> captor = ArgumentCaptor.forClass(AsyncJob.class);
        verify(asyncJobCrudService).update(captor.capture());
        AsyncJob updated = captor.getValue();
        assertThat(updated.getStatus()).isEqualTo(AsyncJob.Status.SUCCESS);
        assertThat(updated.getUpperLimit()).isEqualTo(7L);
        assertThat(updated.getErrorMessage()).isNull();
    }

    @Test
    void records_an_error_when_the_sync_use_case_throws() throws InterruptedException {
        when(syncAmUsersUseCase.execute(any())).thenThrow(new RuntimeException("upstream down"));

        runner.runAsync(JOB, CALLER, CONNECTION);
        awaitCompletion();

        ArgumentCaptor<AsyncJob> captor = ArgumentCaptor.forClass(AsyncJob.class);
        verify(asyncJobCrudService).update(captor.capture());
        AsyncJob updated = captor.getValue();
        assertThat(updated.getStatus()).isEqualTo(AsyncJob.Status.ERROR);
        assertThat(updated.getErrorMessage()).isEqualTo("upstream down");
    }

    @Test
    void shuts_the_worker_pool_down_on_destroy() {
        runner.destroy();

        assertThat(executor.isShutdown()).isTrue();
    }
}

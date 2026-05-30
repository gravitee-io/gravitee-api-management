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
package io.gravitee.gamma.authorization.core.am.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.async_job.crud_service.AsyncJobCrudService;
import io.gravitee.apim.core.async_job.model.AsyncJob;
import io.gravitee.apim.core.async_job.query_service.AsyncJobQueryService;
import io.gravitee.apim.plugin.gamma.api.identity.AmConnection;
import io.gravitee.apim.plugin.gamma.api.identity.AmConnectionRepository;
import io.gravitee.apim.plugin.gamma.api.identity.AmNotConfiguredException;
import io.gravitee.gamma.authorization.api.AuthzCallerContext;
import io.gravitee.gamma.authorization.core.am.exception.AmSyncConflictException;
import io.gravitee.gamma.authorization.core.am.service_provider.AmUserSyncRunner;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class StartAmUserSyncUseCaseTest {

    private static final AuthzCallerContext CALLER = AuthzCallerContext.ofUser("org-1", "env-1", "user-1");
    private static final AmConnection CONNECTION = new AmConnection("http://am:8093", "token", "domain-1", "domain-hrid", null);

    private AsyncJobQueryService asyncJobQueryService;
    private AsyncJobCrudService asyncJobCrudService;
    private AmConnectionRepository amConnectionRepository;
    private AmUserSyncRunner runner;
    private StartAmUserSyncUseCase useCase;

    @BeforeEach
    void setUp() {
        asyncJobQueryService = mock(AsyncJobQueryService.class);
        asyncJobCrudService = mock(AsyncJobCrudService.class);
        amConnectionRepository = mock(AmConnectionRepository.class);
        runner = mock(AmUserSyncRunner.class);
        useCase = new StartAmUserSyncUseCase(asyncJobQueryService, asyncJobCrudService, amConnectionRepository, runner);
    }

    @Test
    void rejects_when_a_sync_is_already_pending_for_the_organization() {
        when(asyncJobQueryService.findPendingJobFor("org-1")).thenReturn(Optional.of(AsyncJob.builder().id("running").build()));

        assertThatThrownBy(() -> useCase.execute(new StartAmUserSyncUseCase.Input(CALLER))).isInstanceOf(AmSyncConflictException.class);

        verify(asyncJobCrudService, never()).create(any());
        verify(runner, never()).runAsync(any(), any(), any());
    }

    @Test
    void propagates_when_am_is_not_configured() {
        when(asyncJobQueryService.findPendingJobFor("org-1")).thenReturn(Optional.empty());
        when(amConnectionRepository.requireByOrg("org-1")).thenThrow(new AmNotConfiguredException());

        assertThatThrownBy(() -> useCase.execute(new StartAmUserSyncUseCase.Input(CALLER))).isInstanceOf(AmNotConfiguredException.class);

        verify(asyncJobCrudService, never()).create(any());
        verify(runner, never()).runAsync(any(), any(), any());
    }

    @Test
    void creates_a_pending_job_and_hands_it_to_the_runner() {
        when(asyncJobQueryService.findPendingJobFor("org-1")).thenReturn(Optional.empty());
        when(amConnectionRepository.requireByOrg("org-1")).thenReturn(CONNECTION);
        when(asyncJobCrudService.create(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AsyncJob job = useCase.execute(new StartAmUserSyncUseCase.Input(CALLER)).job();

        assertThat(job.getStatus()).isEqualTo(AsyncJob.Status.PENDING);
        assertThat(job.getType()).isEqualTo(AsyncJob.Type.AM_USER_SYNC);
        assertThat(job.getSourceId()).isEqualTo("org-1");
        assertThat(job.getEnvironmentId()).isEqualTo("env-1");
        assertThat(job.getInitiatorId()).isEqualTo("user-1");
        assertThat(job.getDeadLine()).isNotNull();
        verify(runner).runAsync(eq(job), eq(CALLER), eq(CONNECTION));
    }
}

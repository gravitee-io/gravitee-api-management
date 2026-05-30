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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.async_job.model.AsyncJob;
import io.gravitee.apim.core.async_job.query_service.AsyncJobQueryService;
import io.gravitee.common.data.domain.Page;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GetAmUserSyncStatusUseCaseTest {

    private AsyncJobQueryService asyncJobQueryService;
    private GetAmUserSyncStatusUseCase useCase;

    @BeforeEach
    void setUp() {
        asyncJobQueryService = mock(AsyncJobQueryService.class);
        useCase = new GetAmUserSyncStatusUseCase(asyncJobQueryService);
    }

    @Test
    void returns_the_latest_am_user_sync_job_for_the_organization() {
        AsyncJob job = AsyncJob.builder().id("job-1").sourceId("org-1").status(AsyncJob.Status.SUCCESS).build();
        when(asyncJobQueryService.listAsyncJobs(any(), any())).thenReturn(new Page<>(List.of(job), 1, 1, 1));

        Optional<AsyncJob> result = useCase.execute(new GetAmUserSyncStatusUseCase.Input("org-1", "env-1")).job();

        assertThat(result).contains(job);
        ArgumentCaptor<AsyncJobQueryService.ListQuery> captor = ArgumentCaptor.forClass(AsyncJobQueryService.ListQuery.class);
        verify(asyncJobQueryService).listAsyncJobs(captor.capture(), any());
        assertThat(captor.getValue().sourceId()).contains("org-1");
        assertThat(captor.getValue().type()).contains(AsyncJob.Type.AM_USER_SYNC);
        assertThat(captor.getValue().environmentId()).isEqualTo("env-1");
    }

    @Test
    void returns_empty_when_no_sync_has_run() {
        when(asyncJobQueryService.listAsyncJobs(any(), any())).thenReturn(new Page<>(List.of(), 1, 0, 0));

        assertThat(useCase.execute(new GetAmUserSyncStatusUseCase.Input("org-1", "env-1")).job()).isEmpty();
    }
}

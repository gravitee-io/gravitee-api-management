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
package io.gravitee.apim.core.async_job.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import fixtures.core.model.AsyncJobFixture;
import inmemory.AsyncJobQueryServiceInMemory;
import inmemory.InMemoryAlternative;
import io.gravitee.apim.core.async_job.model.AsyncJob;
import io.gravitee.apim.core.async_job.use_case.ListUserAsyncJobsUseCase.Output;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.common.PageableImpl;
import java.util.List;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ListUserAsyncJobsUseCaseTest {

    private static final String ENV_ID = "my-env";
    private static final String ORGANIZATION_ID = "my-org";
    private static final String INTEGRATION_ID = "integration-id";
    private static final String USER_ID = "user-id";
    private static final int PAGE_NUMBER = 1;
    private static final int PAGE_SIZE = 5;
    private static final Pageable pageable = new PageableImpl(PAGE_NUMBER, PAGE_SIZE);

    AsyncJobQueryServiceInMemory asyncJobQueryService = new AsyncJobQueryServiceInMemory();

    ListUserAsyncJobsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ListUserAsyncJobsUseCase(asyncJobQueryService);
    }

    @AfterEach
    void tearDown() {
        Stream.of(asyncJobQueryService).forEach(InMemoryAlternative::reset);
    }

    @Test
    void should_return_all_async_jobs_of_the_caller() {
        // Given
        var job1 = AsyncJobFixture.anAsyncJob("job1").toBuilder().initiatorId(USER_ID).build();
        var job2 = AsyncJobFixture.anAsyncJob("job2").toBuilder().initiatorId(USER_ID).build();
        var job3 = AsyncJobFixture.anAsyncJob("job3").toBuilder().initiatorId("other").build();
        givenAsyncJobs(job1, job2, job3);

        // When
        var result = useCase.execute(new ListUserAsyncJobsUseCase.Input(ENV_ID, USER_ID));

        // Then
        assertThat(result).extracting(Output::jobs).extracting(Page::getContent).isEqualTo(List.of(job1, job2));
    }

    @Test
    void should_filter_user_async_jobs_by_type() {
        // Given
        var job1 = AsyncJobFixture.anAsyncJob("job1").toBuilder().initiatorId(USER_ID).build();
        var job2 = AsyncJobFixture.anAsyncJob("job2").toBuilder().type(AsyncJob.Type.SCORING_REQUEST).initiatorId(USER_ID).build();
        var job3 = AsyncJobFixture.anAsyncJob("job3").toBuilder().initiatorId(USER_ID).build();
        var other = AsyncJobFixture.anAsyncJob("other").toBuilder().type(AsyncJob.Type.SCORING_REQUEST).initiatorId("other").build();
        givenAsyncJobs(job1, job2, job3, other);

        // When
        var result = useCase.execute(new ListUserAsyncJobsUseCase.Input(ENV_ID, USER_ID, AsyncJob.Type.SCORING_REQUEST));

        // Then
        assertThat(result).extracting(Output::jobs).extracting(Page::getContent).isEqualTo(List.of(job2));
    }

    @Test
    void should_filter_user_async_jobs_by_status() {
        // Given
        var job1 = AsyncJobFixture.anAsyncJob("job1").toBuilder().initiatorId(USER_ID).build();
        var job2 = AsyncJobFixture.anAsyncJob("job2").toBuilder().initiatorId(USER_ID).build();
        var job3 = AsyncJobFixture.anAsyncJob("job3").toBuilder().status(AsyncJob.Status.SUCCESS).initiatorId(USER_ID).build();
        var other = AsyncJobFixture.anAsyncJob("other").toBuilder().status(AsyncJob.Status.SUCCESS).initiatorId("other").build();
        givenAsyncJobs(job1, job2, job3, other);

        // When
        var result = useCase.execute(new ListUserAsyncJobsUseCase.Input(ENV_ID, USER_ID, AsyncJob.Status.SUCCESS));

        // Then
        assertThat(result).extracting(Output::jobs).extracting(Page::getContent).isEqualTo(List.of(job3));
    }

    @Test
    void should_return_default_pagination() {
        // Given
        var jobNumber = 20L;
        var jobs = LongStream
            .range(0, jobNumber)
            .mapToObj(i -> AsyncJobFixture.anAsyncJob("job" + i).toBuilder().initiatorId(USER_ID).build())
            .toList();
        givenAsyncJobs(jobs);

        // When
        var result = useCase.execute(new ListUserAsyncJobsUseCase.Input(ENV_ID, USER_ID));

        // Then
        assertThat(result)
            .extracting(Output::jobs)
            .extracting(Page::getPageNumber, Page::getPageElements, Page::getTotalElements)
            .containsExactly(PAGE_NUMBER, 10L, jobNumber);
    }

    @Test
    void should_throw_when_environmentId_is_not_defined() {
        // Given
        // When
        var throwable = catchThrowable(() -> useCase.execute(new ListUserAsyncJobsUseCase.Input(null, USER_ID)));

        // Then
        assertThat(throwable).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_throw_when_initiatorId_is_not_defined() {
        // Given
        // When
        var throwable = catchThrowable(() -> useCase.execute(new ListUserAsyncJobsUseCase.Input(ENV_ID, null)));

        // Then
        assertThat(throwable).isInstanceOf(IllegalArgumentException.class);
    }

    private void givenAsyncJobs(AsyncJob... jobs) {
        givenAsyncJobs(List.of(jobs));
    }

    private void givenAsyncJobs(List<AsyncJob> jobs) {
        asyncJobQueryService.initWith(jobs);
    }
}

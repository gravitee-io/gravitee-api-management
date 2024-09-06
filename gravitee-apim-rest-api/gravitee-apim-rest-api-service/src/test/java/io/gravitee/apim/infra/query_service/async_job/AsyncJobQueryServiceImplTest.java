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
package io.gravitee.apim.infra.query_service.async_job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import fixtures.repository.AsyncJobFixture;
import io.gravitee.apim.core.async_job.model.AsyncJob;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.AsyncJobRepository;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AsyncJobQueryServiceImplTest {

    AsyncJobRepository integrationRepository;

    AsyncJobQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        integrationRepository = mock(AsyncJobRepository.class);
        service = new AsyncJobQueryServiceImpl(integrationRepository);
    }

    @Test
    @SneakyThrows
    void should_return_pending_job_from_source_id() {
        // Given
        when(integrationRepository.findPendingJobFor(any()))
            .thenAnswer(invocation -> Optional.of(AsyncJobFixture.anAsyncJob().toBuilder().sourceId(invocation.getArgument(0)).build()));

        // When
        var result = service.findPendingJobFor("integration-id");

        //Then
        assertThat(result)
            .contains(
                AsyncJob
                    .builder()
                    .id("job-id")
                    .sourceId("integration-id")
                    .environmentId("environment-id")
                    .initiatorId("initiator-id")
                    .type(AsyncJob.Type.FEDERATED_APIS_INGESTION)
                    .upperLimit(10L)
                    .status(AsyncJob.Status.PENDING)
                    .createdAt(Instant.parse("2020-02-03T20:22:02.00Z").atZone(ZoneId.systemDefault()))
                    .updatedAt(Instant.parse("2020-02-03T20:22:02.00Z").atZone(ZoneId.systemDefault()))
                    .build()
            );
    }

    @Test
    @SneakyThrows
    void should_throw_when_technical_exception_occurs() {
        // Given
        when(integrationRepository.findPendingJobFor(any())).thenThrow(TechnicalException.class);

        // When
        var throwable = catchThrowable(() -> service.findPendingJobFor("integration-id"));

        // Then
        assertThat(throwable)
            .isInstanceOf(TechnicalManagementException.class)
            .hasMessage("An error occurred while finding pending AsyncJob for: integration-id");
    }
}

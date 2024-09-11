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
package io.gravitee.apim.infra.crud_service.async_job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.core.model.AsyncJobFixture;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.AsyncJobRepository;
import io.gravitee.repository.management.model.AsyncJob;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class AsyncJobCrudServiceImplTest {

    AsyncJobRepository repository;

    AsyncJobCrudServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(AsyncJobRepository.class);
        service = new AsyncJobCrudServiceImpl(repository);
    }

    @Nested
    class Create {

        @Test
        @SneakyThrows
        void should_create_job() {
            //Given
            var job = AsyncJobFixture.aPendingFederatedApiIngestionJob();
            when(repository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));

            //When
            var created = service.create(job);

            //Then
            assertThat(created).isEqualTo(job);
        }

        @Test
        void should_throw_when_technical_exception_occurs() throws TechnicalException {
            // Given
            var job = AsyncJobFixture.aPendingFederatedApiIngestionJob();
            when(repository.create(any())).thenThrow(TechnicalException.class);

            // When
            Throwable throwable = catchThrowable(() -> service.create(job));

            // Then
            assertThat(throwable)
                .isInstanceOf(TechnicalManagementException.class)
                .hasMessage("Error when creating AsyncJob for integration: integration-id");
        }
    }

    @Nested
    class FindById {

        @Test
        @SneakyThrows
        void should_return_the_found_job() {
            //Given
            when(repository.findById(any()))
                .thenAnswer(invocation ->
                    Optional.of(fixtures.repository.AsyncJobFixture.anAsyncJob().toBuilder().id(invocation.getArgument(0)).build())
                );

            //When
            var result = service.findById("my-id");

            //Then
            assertThat(result)
                .contains(
                    AsyncJobFixture.aPendingFederatedApiIngestionJob().toBuilder().id("my-id").environmentId("environment-id").build()
                );
        }

        @Test
        @SneakyThrows
        void should_return_empty_when_not_found() {
            //Given
            when(repository.findById(any())).thenAnswer(invocation -> Optional.empty());

            //When
            var result = service.findById("my-id");

            //Then
            assertThat(result).isEmpty();
        }

        @Test
        void should_throw_when_technical_exception_occurs() throws TechnicalException {
            // Given
            when(repository.findById(any())).thenThrow(TechnicalException.class);

            // When
            Throwable throwable = catchThrowable(() -> service.findById("my-id"));

            // Then
            assertThat(throwable)
                .isInstanceOf(TechnicalManagementException.class)
                .hasMessage("An error occurs while trying to find the AsyncJob: my-id");
        }
    }

    @Nested
    class Update {

        @Test
        @SneakyThrows
        void should_update_integration() {
            var job = AsyncJobFixture
                .aSuccessFederatedApiIngestionJob()
                .toBuilder()
                .updatedAt(Instant.parse("2020-02-04T20:22:02.00Z").atZone(ZoneId.systemDefault()))
                .build();
            when(repository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));

            service.update(job);

            var captor = ArgumentCaptor.forClass(AsyncJob.class);
            verify(repository).update(captor.capture());

            assertThat(captor.getValue())
                .usingRecursiveComparison()
                .isEqualTo(
                    AsyncJob
                        .builder()
                        .id("job-id")
                        .sourceId("integration-id")
                        .initiatorId("initiator-id")
                        .environmentId("my-env")
                        .status("SUCCESS")
                        .type("FEDERATED_APIS_INGESTION")
                        .upperLimit(10L)
                        .createdAt(Date.from(Instant.parse("2020-02-03T20:22:02.00Z")))
                        .updatedAt(Date.from(Instant.parse("2020-02-04T20:22:02.00Z")))
                        .build()
                );
        }

        @Test
        @SneakyThrows
        void should_return_the_updated_integration() {
            //Given
            when(repository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));

            //When
            var toUpdate = AsyncJobFixture.aPendingFederatedApiIngestionJob();
            var updated = service.update(toUpdate);

            //Then
            assertThat(updated).isEqualTo(toUpdate);
        }

        @Test
        void should_throw_when_technical_exception_occurs() throws TechnicalException {
            // Given
            var job = AsyncJobFixture.aSuccessFederatedApiIngestionJob();
            when(repository.update(any())).thenThrow(TechnicalException.class);

            // When
            Throwable throwable = catchThrowable(() -> service.update(job));

            // Then
            assertThat(throwable)
                .isInstanceOf(TechnicalManagementException.class)
                .hasMessage("An error occurred when updating AsyncJob: job-id");
        }
    }

    @Nested
    class Delete {

        @Test
        void should_delete_a_job() throws TechnicalException {
            service.delete("job-id");
            verify(repository).delete("job-id");
        }

        @Test
        void should_throw_if_deletion_problem_occurs() throws TechnicalException {
            doThrow(new TechnicalException()).when(repository).delete("job-id");
            assertThatThrownBy(() -> service.delete("job-id"))
                .isInstanceOf(TechnicalManagementException.class)
                .hasMessage("Error when deleting AsyncJob: job-id");
            verify(repository).delete("job-id");
        }
    }
}

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
package io.gravitee.apim.infra.crud_service.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.core.model.IntegrationFixture;
import io.gravitee.apim.core.integration.model.Integration;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.IntegrationRepository;
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

public class IntegrationCrudServiceImplTest {

    IntegrationRepository integrationRepository;

    IntegrationCrudServiceImpl service;

    @BeforeEach
    void setUp() {
        integrationRepository = mock(IntegrationRepository.class);
        service = new IntegrationCrudServiceImpl(integrationRepository);
    }

    @Nested
    class Create {

        @Test
        @SneakyThrows
        void should_create_integration() {
            //Given
            Integration integration = IntegrationFixture.anIntegration();
            when(integrationRepository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));

            //When
            Integration createdIntegration = service.create(integration);

            //Then
            assertThat(createdIntegration).isEqualTo(integration);
        }

        @Test
        void should_throw_when_technical_exception_occurs() throws TechnicalException {
            // Given
            var integration = IntegrationFixture.anIntegration();
            when(integrationRepository.create(any())).thenThrow(TechnicalException.class);

            // When
            Throwable throwable = catchThrowable(() -> service.create(integration));

            // Then
            assertThat(throwable).isInstanceOf(TechnicalManagementException.class).hasMessage("Error when creating Integration: test-name");
        }
    }

    @Nested
    class FindById {

        @Test
        @SneakyThrows
        void should_return_the_found_integration() {
            //Given
            when(integrationRepository.findById(any())).thenAnswer(invocation ->
                Optional.of(fixtures.repository.IntegrationFixture.anIntegration().toBuilder().id(invocation.getArgument(0)).build())
            );

            //When
            var result = service.findById("my-id");

            //Then
            assertThat(result).contains(
                IntegrationFixture.anIntegration()
                    .toBuilder()
                    .id("my-id")
                    .name("An integration")
                    .description("A description")
                    .provider("amazon")
                    .environmentId("environment-id")
                    .createdAt(Instant.parse("2020-02-03T20:22:02.00Z").atZone(ZoneId.systemDefault()))
                    .updatedAt(Instant.parse("2020-02-04T20:22:02.00Z").atZone(ZoneId.systemDefault()))
                    .build()
            );
        }

        @Test
        @SneakyThrows
        void should_return_empty_when_not_found() {
            //Given
            when(integrationRepository.findById(any())).thenAnswer(invocation -> Optional.empty());

            //When
            var result = service.findById("my-id");

            //Then
            assertThat(result).isEmpty();
        }

        @Test
        void should_throw_when_technical_exception_occurs() throws TechnicalException {
            // Given
            when(integrationRepository.findById(any())).thenThrow(TechnicalException.class);

            // When
            Throwable throwable = catchThrowable(() -> service.findById("my-id"));

            // Then
            assertThat(throwable)
                .isInstanceOf(TechnicalManagementException.class)
                .hasMessage("An error occurs while trying to find the integration: my-id");
        }
    }

    @Nested
    class Update {

        @Test
        @SneakyThrows
        void should_update_integration() {
            Integration integration = IntegrationFixture.anIntegration()
                .toBuilder()
                .updatedAt(Instant.parse("2020-02-04T20:22:02.00Z").atZone(ZoneId.systemDefault()))
                .build();
            when(integrationRepository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));

            service.update(integration);

            var captor = ArgumentCaptor.forClass(io.gravitee.repository.management.model.Integration.class);
            verify(integrationRepository).update(captor.capture());

            assertThat(captor.getValue())
                .usingRecursiveComparison()
                .isEqualTo(
                    io.gravitee.repository.management.model.Integration.builder()
                        .id("integration-id")
                        .name("test-name")
                        .description("integration-description")
                        .provider("test-provider")
                        .environmentId("my-env")
                        .createdAt(Date.from(Instant.parse("2020-02-03T20:22:02.00Z")))
                        .updatedAt(Date.from(Instant.parse("2020-02-04T20:22:02.00Z")))
                        .build()
                );
        }

        @Test
        @SneakyThrows
        void should_return_the_updated_integration() {
            //Given
            when(integrationRepository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));

            //When
            Integration toUpdate = IntegrationFixture.anIntegration();
            Integration updated = service.update(toUpdate);

            //Then
            assertThat(updated).isEqualTo(toUpdate);
        }

        @Test
        void should_throw_when_technical_exception_occurs() throws TechnicalException {
            // Given
            var integration = IntegrationFixture.anIntegration();
            when(integrationRepository.update(any())).thenThrow(TechnicalException.class);

            // When
            Throwable throwable = catchThrowable(() -> service.update(integration));

            // Then
            assertThat(throwable)
                .isInstanceOf(TechnicalManagementException.class)
                .hasMessage("An error occurred when updating integration: integration-id");
        }
    }

    @Nested
    class Delete {

        @Test
        void should_delete_an_integration() throws TechnicalException {
            service.delete("integration-id");
            verify(integrationRepository).delete("integration-id");
        }

        @Test
        void should_throw_if_deletion_problem_occurs() throws TechnicalException {
            doThrow(new TechnicalException()).when(integrationRepository).delete("integration-id");
            assertThatThrownBy(() -> service.delete("integration-id"))
                .isInstanceOf(TechnicalManagementException.class)
                .hasMessage("Error when deleting Integration: integration-id");
            verify(integrationRepository).delete("integration-id");
        }
    }
}

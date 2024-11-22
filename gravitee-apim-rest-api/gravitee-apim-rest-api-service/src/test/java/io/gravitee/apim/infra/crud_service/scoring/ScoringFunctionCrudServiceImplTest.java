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
package io.gravitee.apim.infra.crud_service.scoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.core.model.ScoringFunctionFixture;
import io.gravitee.apim.core.scoring.model.ScoringFunction;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ScoringFunctionRepository;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class ScoringFunctionCrudServiceImplTest {

    ScoringFunctionRepository scoringFunctionRepository;

    ScoringFunctionCrudServiceImpl service;

    @BeforeEach
    void setUp() {
        scoringFunctionRepository = mock(ScoringFunctionRepository.class);
        service = new ScoringFunctionCrudServiceImpl(scoringFunctionRepository);
    }

    @Nested
    class Create {

        @Test
        @SneakyThrows
        void should_create_scoring_function() {
            // Given
            var function = ScoringFunctionFixture.aFunction();
            when(scoringFunctionRepository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            var created = service.create(function);

            // Then
            assertThat(created).isEqualTo(function);
        }

        @Test
        void should_throw_when_technical_exception_occurs() throws TechnicalException {
            // Given
            var function = ScoringFunctionFixture.aFunction();
            when(scoringFunctionRepository.create(any())).thenThrow(TechnicalException.class);

            // When
            Throwable throwable = catchThrowable(() -> service.create(function));

            // Then
            assertThat(throwable)
                .isInstanceOf(TechnicalManagementException.class)
                .hasMessage("Error when creating Scoring Function: function-id");
        }
    }

    @Nested
    class FindById {

        @Test
        @SneakyThrows
        void should_find_a_function_by_id() {
            // Given
            var function = ScoringFunctionFixture.aFunction();
            when(scoringFunctionRepository.findById("function-id"))
                .thenReturn(Optional.of(fixtures.repository.ScoringFunctionFixture.aFunction()));

            // When
            var found = service.findById("function-id");

            // Then
            assertThat(found)
                .contains(
                    ScoringFunction
                        .builder()
                        .id("function-id")
                        .name("function-name")
                        .payload("function-payload")
                        .createdAt(Instant.parse("2020-02-03T20:22:02.00Z").atZone(ZoneId.systemDefault()))
                        .referenceType(ScoringFunction.ReferenceType.ENVIRONMENT)
                        .referenceId("reference-id")
                        .build()
                );
        }

        @Test
        void should_throw_when_technical_exception_occurs() throws TechnicalException {
            // Given
            when(scoringFunctionRepository.findById(any())).thenThrow(TechnicalException.class);

            // When
            Throwable throwable = catchThrowable(() -> service.findById("function-id"));

            // Then
            assertThat(throwable)
                .isInstanceOf(TechnicalManagementException.class)
                .hasMessage("Error when searching for Scoring Function: function-id");
        }
    }

    @Nested
    class Delete {

        @Test
        @SneakyThrows
        void should_delete_a_function() {
            // Given

            // When
            service.delete("function-id");

            // Then
            verify(scoringFunctionRepository).delete("function-id");
        }

        @Test
        void should_throw_when_technical_exception_occurs() throws TechnicalException {
            // Given
            doThrow(TechnicalException.class).when(scoringFunctionRepository).delete(any());

            // When
            Throwable throwable = catchThrowable(() -> service.delete("function-id"));

            // Then
            assertThat(throwable)
                .isInstanceOf(TechnicalManagementException.class)
                .hasMessage("Error when deleting Scoring Function: function-id");
        }
    }

    @Nested
    class DeleteByReference {

        @Test
        @SneakyThrows
        void should_delete_all_functions_of_an_environment() {
            // Given

            // When
            service.deleteByReference("env-id", ScoringFunction.ReferenceType.ENVIRONMENT);

            // Then
            verify(scoringFunctionRepository).deleteByReferenceId("env-id", "ENVIRONMENT");
        }

        @Test
        void should_throw_when_technical_exception_occurs() throws TechnicalException {
            // Given
            doThrow(TechnicalException.class).when(scoringFunctionRepository).deleteByReferenceId(any(), any());

            // When
            Throwable throwable = catchThrowable(() -> service.deleteByReference("env-id", ScoringFunction.ReferenceType.ENVIRONMENT));

            // Then
            assertThat(throwable)
                .isInstanceOf(TechnicalManagementException.class)
                .hasMessage("Error when deleting Scoring Function for [ENVIRONMENT:env-id]");
        }
    }
}

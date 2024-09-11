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

import fixtures.core.model.IntegrationFixture;
import fixtures.core.model.ScoringReportFixture;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ScoringReportRepository;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class ScoringReportCrudServiceImplTest {

    ScoringReportRepository scoringReportRepository;

    ScoringReportCrudServiceImpl service;

    @BeforeEach
    void setUp() {
        scoringReportRepository = mock(ScoringReportRepository.class);
        service = new ScoringReportCrudServiceImpl(scoringReportRepository);
    }

    @Nested
    class Create {

        @Test
        @SneakyThrows
        void should_create_scoring_report() {
            // Given
            var report = ScoringReportFixture.aScoringReport();
            when(scoringReportRepository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            var created = service.create(report);

            // Then
            assertThat(created).isEqualTo(report);
        }

        @Test
        void should_throw_when_technical_exception_occurs() throws TechnicalException {
            // Given
            var report = ScoringReportFixture.aScoringReport();
            when(scoringReportRepository.create(any())).thenThrow(TechnicalException.class);

            // When
            Throwable throwable = catchThrowable(() -> service.create(report));

            // Then
            assertThat(throwable)
                .isInstanceOf(TechnicalManagementException.class)
                .hasMessage("Error when creating Scoring Report for API: api-id");
        }
    }

    @Nested
    class DeleteByApi {

        @Test
        @SneakyThrows
        void should_delete_all_reports_of_an_api() {
            // Given

            // When
            service.deleteByApi("api-id");

            // Then
            verify(scoringReportRepository).deleteByApi("api-id");
        }

        @Test
        void should_throw_when_technical_exception_occurs() throws TechnicalException {
            // Given
            doThrow(TechnicalException.class).when(scoringReportRepository).deleteByApi(any());

            // When
            Throwable throwable = catchThrowable(() -> service.deleteByApi("api-id"));

            // Then
            assertThat(throwable)
                .isInstanceOf(TechnicalManagementException.class)
                .hasMessage("Error when deleting Scoring Report for API: api-id");
        }
    }
}

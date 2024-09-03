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
package io.gravitee.apim.infra.query_service.scoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.scoring.model.ScoringAssetType;
import io.gravitee.apim.core.scoring.model.ScoringReport;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ScoringReportRepository;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class ScoringReportQueryServiceImplTest {

    ScoringReportRepository scoringReportRepository;

    ScoringReportQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        scoringReportRepository = mock(ScoringReportRepository.class);
        service = new ScoringReportQueryServiceImpl(scoringReportRepository);
    }

    @Nested
    class FindLatestByApiId {

        @Test
        @SneakyThrows
        void should_find_scoring_report() {
            when(scoringReportRepository.findLatestFor(any())).thenAnswer(invocation -> Optional.of(aReport()));

            // When
            var result = service.findLatestByApiId("api-id");

            // Then
            assertThat(result)
                .contains(
                    new ScoringReport(
                        "report-id",
                        "api-id",
                        Instant.parse("2020-02-01T20:22:02.00Z").atZone(ZoneId.systemDefault()),
                        List.of(
                            new ScoringReport.Asset(
                                "asset1",
                                ScoringAssetType.SWAGGER,
                                List.of(
                                    new ScoringReport.Diagnostic(
                                        ScoringReport.Severity.WARN,
                                        new ScoringReport.Range(new ScoringReport.Position(17, 12), new ScoringReport.Position(38, 25)),
                                        "operation-operationId",
                                        "Operation must have \"operationId\".",
                                        "paths./echo.options"
                                    )
                                )
                            ),
                            new ScoringReport.Asset("asset2", ScoringAssetType.GRAVITEE_DEFINITION, List.of())
                        )
                    )
                );
        }

        @Test
        @SneakyThrows
        void should_return_empty_when_no_report() {
            when(scoringReportRepository.findLatestFor(any())).thenAnswer(invocation -> Optional.empty());

            // When
            var result = service.findLatestByApiId("api-id");

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        void should_throw_when_technical_exception_occurs() throws TechnicalException {
            // Given
            when(scoringReportRepository.findLatestFor(any())).thenThrow(TechnicalException.class);

            // When
            Throwable throwable = catchThrowable(() -> service.findLatestByApiId("api-id"));

            // Then
            assertThat(throwable)
                .isInstanceOf(TechnicalManagementException.class)
                .hasMessage("An error occurred while finding Scoring Report of API: api-id");
        }
    }

    io.gravitee.repository.management.model.ScoringReport aReport() {
        return io.gravitee.repository.management.model.ScoringReport
            .builder()
            .id("report-id")
            .apiId("api-id")
            .asset(
                new io.gravitee.repository.management.model.ScoringReport.Asset(
                    "asset1",
                    "SWAGGER",
                    List.of(
                        new io.gravitee.repository.management.model.ScoringReport.Diagnostic(
                            "WARN",
                            new io.gravitee.repository.management.model.ScoringReport.Range(
                                new io.gravitee.repository.management.model.ScoringReport.Position(17, 12),
                                new io.gravitee.repository.management.model.ScoringReport.Position(38, 25)
                            ),
                            "operation-operationId",
                            "Operation must have \"operationId\".",
                            "paths./echo.options"
                        )
                    )
                )
            )
            .asset(new io.gravitee.repository.management.model.ScoringReport.Asset("asset2", "GRAVITEE_DEFINITION", List.of()))
            .createdAt(Date.from(Instant.parse("2020-02-01T20:22:02.00Z")))
            .build();
    }
}

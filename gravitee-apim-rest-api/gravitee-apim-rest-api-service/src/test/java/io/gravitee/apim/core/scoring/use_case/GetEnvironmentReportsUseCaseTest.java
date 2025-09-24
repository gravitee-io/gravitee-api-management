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
package io.gravitee.apim.core.scoring.use_case;

import fixtures.core.model.ApiFixtures;
import fixtures.core.model.ScoringReportFixture;
import inmemory.InMemoryAlternative;
import inmemory.ScoringReportQueryServiceInMemory;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.scoring.model.EnvironmentApiScoringReport;
import io.gravitee.apim.core.scoring.model.ScoringAssetType;
import io.gravitee.apim.core.scoring.model.ScoringReport;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.common.PageableImpl;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetEnvironmentReportsUseCaseTest {

    private static final ZonedDateTime UPDATED_AT = Instant.parse("2023-10-22T10:15:30Z").atZone(ZoneId.systemDefault());
    private static final String REPORT_ID = "report-id";
    private static final String ENVIRONMENT_1 = "environment-1";
    private static final String ENVIRONMENT_2 = "environment-2";
    private static final String API_ID_1 = "api-1";
    private static final String API_ID_2 = "api-2";
    private static final String API_ID_3 = "api-3";

    private static final ZonedDateTime CREATED_AT = Instant.parse("2023-10-22T10:15:30Z").atZone(ZoneId.systemDefault());
    private static final String PAGE_ID = "page-id";
    private static final ScoringReport.Asset ASSET = new ScoringReport.Asset(
        PAGE_ID,
        ScoringAssetType.SWAGGER,
        List.of(
            new ScoringReport.Diagnostic(
                ScoringReport.Severity.WARN,
                new ScoringReport.Range(new ScoringReport.Position(17, 12), new ScoringReport.Position(38, 25)),
                "operation-operationId",
                "Operation must have \"operationId\".",
                "paths./echo.options"
            ),
            new ScoringReport.Diagnostic(
                ScoringReport.Severity.ERROR,
                new ScoringReport.Range(new ScoringReport.Position(10, 4), new ScoringReport.Position(10, 10)),
                "error-rule",
                "Error rule message",
                "paths./error"
            ),
            new ScoringReport.Diagnostic(
                ScoringReport.Severity.INFO,
                new ScoringReport.Range(new ScoringReport.Position(11, 4), new ScoringReport.Position(11, 10)),
                "info-rule",
                "Info rule message",
                "paths./info"
            ),
            new ScoringReport.Diagnostic(
                ScoringReport.Severity.HINT,
                new ScoringReport.Range(new ScoringReport.Position(12, 4), new ScoringReport.Position(12, 10)),
                "hint-rule",
                "Hint rule message",
                "paths./hint"
            )
        ),
        List.of()
    );

    private static final ScoringReport.Asset VALIDATION_ERROR_ASSET = new ScoringReport.Asset(
        PAGE_ID,
        ScoringAssetType.ASYNCAPI,
        List.of(),
        List.of(new ScoringReport.ScoringError("ruleset-validation-error", List.of("path", "to", "invalid", "element")))
    );

    ScoringReportQueryServiceInMemory scoringReportQueryService = new ScoringReportQueryServiceInMemory();

    GetEnvironmentReportsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetEnvironmentReportsUseCase(scoringReportQueryService);
    }

    @AfterEach
    void tearDown() {
        Stream.of(scoringReportQueryService).forEach(InMemoryAlternative::reset);
    }

    @Test
    void should_return_scoring_reports_of_environment_apis() {
        // Given
        givenExistingScoringReports(aReport().withApiId(API_ID_1), aReport().withApiId(API_ID_2), aReport().withApiId(API_ID_3));

        // When
        var report = useCase.execute(new GetEnvironmentReportsUseCase.Input(ENVIRONMENT_1));

        // Then
        Assertions.assertThat(report)
            .extracting(GetEnvironmentReportsUseCase.Output::reports)
            .extracting(Page::getContent)
            .asInstanceOf(InstanceOfAssertFactories.LIST)
            .contains(
                new EnvironmentApiScoringReport(
                    new EnvironmentApiScoringReport.Api(API_ID_1, "api-name", UPDATED_AT),
                    new EnvironmentApiScoringReport.Summary(REPORT_ID, CREATED_AT, 0.84D, 1L, 1L, 1L, 1L)
                ),
                new EnvironmentApiScoringReport(
                    new EnvironmentApiScoringReport.Api(API_ID_2, "api-name", UPDATED_AT),
                    new EnvironmentApiScoringReport.Summary(REPORT_ID, CREATED_AT, 0.84D, 1L, 1L, 1L, 1L)
                )
            );
    }

    @Test
    void should_ignore_assets_with_validation_errors() {
        //Given
        givenExistingScoringReports(aReport().withApiId(API_ID_1).toBuilder().assets(List.of(ASSET, VALIDATION_ERROR_ASSET)).build());

        // When
        var report = useCase.execute(new GetEnvironmentReportsUseCase.Input(ENVIRONMENT_1));

        // Then
        Assertions.assertThat(report)
            .extracting(GetEnvironmentReportsUseCase.Output::reports)
            .extracting(Page::getContent)
            .asInstanceOf(InstanceOfAssertFactories.LIST)
            .contains(
                new EnvironmentApiScoringReport(
                    new EnvironmentApiScoringReport.Api(API_ID_1, "api-name", UPDATED_AT),
                    new EnvironmentApiScoringReport.Summary(REPORT_ID, CREATED_AT, 0.84D, 1L, 1L, 1L, 1L)
                )
            );
    }

    @Test
    void should_return_the_page_requested() {
        // Given
        var expectedTotal = 15;
        var pageNumber = 2;
        var pageSize = 5;

        givenExistingScoringReports(
            IntStream.range(0, expectedTotal)
                .mapToObj(i -> aReport().withApiId(String.valueOf(i)))
                .toList()
        );

        // When
        var report = useCase.execute(new GetEnvironmentReportsUseCase.Input(ENVIRONMENT_1, new PageableImpl(pageNumber, pageSize)));

        // Then
        Assertions.assertThat(report)
            .extracting(GetEnvironmentReportsUseCase.Output::reports)
            .extracting(Page::getPageNumber, Page::getPageElements, Page::getTotalElements)
            .contains(pageNumber, (long) pageSize, (long) expectedTotal);
    }

    private static ScoringReport aReport() {
        return ScoringReportFixture.aScoringReport()
            .toBuilder()
            .id(REPORT_ID)
            .environmentId(ENVIRONMENT_1)
            .createdAt(CREATED_AT)
            .summary(new ScoringReport.Summary(0.84D, 1L, 1L, 1L, 1L))
            .assets(List.of(ASSET))
            .build();
    }

    private void givenExistingScoringReports(ScoringReport... reports) {
        givenExistingScoringReports(List.of(reports));
    }

    private void givenExistingScoringReports(List<ScoringReport> reports) {
        scoringReportQueryService.initWith(reports);
    }
}

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

import fixtures.core.model.PageFixtures;
import fixtures.core.model.ScoringReportFixture;
import inmemory.InMemoryAlternative;
import inmemory.PageCrudServiceInMemory;
import inmemory.ScoringReportQueryServiceInMemory;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.scoring.model.ScoringAssetType;
import io.gravitee.apim.core.scoring.model.ScoringReport;
import io.gravitee.apim.core.scoring.model.ScoringReportView;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetLatestReportUseCaseTest {

    private static final String REPORT_ID = "report-id";
    private static final String API_ID = "api-id";
    private static final ZonedDateTime CREATED_AT = Instant.parse("2023-10-22T10:15:30Z").atZone(ZoneId.systemDefault());
    private static final String PAGE_ID = "page-id";
    private static final String PAGE_NAME = "echo-oas.json";
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
        )
    );

    ScoringReportQueryServiceInMemory scoringReportQueryService = new ScoringReportQueryServiceInMemory();
    PageCrudServiceInMemory pageCrudService = new PageCrudServiceInMemory();

    GetLatestReportUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetLatestReportUseCase(scoringReportQueryService, pageCrudService);
    }

    @AfterEach
    void tearDown() {
        Stream.of(pageCrudService, scoringReportQueryService).forEach(InMemoryAlternative::reset);
    }

    @Test
    void should_return_the_latest_report_of_an_api() {
        // Given
        givenExistingScoringReports(aReport());
        givenExistingPage(PageFixtures.aPage().toBuilder().id(PAGE_ID).name(PAGE_NAME).build());

        // When
        var report = useCase.execute(new GetLatestReportUseCase.Input(API_ID));

        // Then
        Assertions
            .assertThat(report)
            .extracting(GetLatestReportUseCase.Output::report)
            .isEqualTo(
                new ScoringReportView(
                    REPORT_ID,
                    API_ID,
                    CREATED_AT,
                    List.of(new ScoringReportView.AssetView(PAGE_NAME, ASSET.type(), ASSET.diagnostics())),
                    new ScoringReportView.Summary(4L, 1L, 1L, 1L, 1L)
                )
            );
    }

    @Test
    void should_return_null_when_no_report() {
        // Given

        // When
        var report = useCase.execute(new GetLatestReportUseCase.Input(API_ID));

        // Then
        Assertions.assertThat(report).extracting(GetLatestReportUseCase.Output::report).isNull();
    }

    @Test
    void should_return_the_latest_report_without_page_name_when_page_has_been_deleted() {
        // Given
        givenExistingScoringReports(aReport());

        // When
        var report = useCase.execute(new GetLatestReportUseCase.Input(API_ID));

        // Then
        Assertions
            .assertThat(report)
            .extracting(GetLatestReportUseCase.Output::report)
            .isEqualTo(
                new ScoringReportView(
                    REPORT_ID,
                    API_ID,
                    CREATED_AT,
                    List.of(new ScoringReportView.AssetView(null, ASSET.type(), ASSET.diagnostics())),
                    new ScoringReportView.Summary(4L, 1L, 1L, 1L, 1L)
                )
            );
    }

    private static ScoringReport aReport() {
        return ScoringReportFixture
            .aScoringReport()
            .toBuilder()
            .id(REPORT_ID)
            .apiId(API_ID)
            .createdAt(CREATED_AT)
            .assets(List.of(ASSET))
            .build();
    }

    private void givenExistingScoringReports(ScoringReport... reports) {
        scoringReportQueryService.initWith(List.of(reports));
    }

    private void givenExistingPage(Page page) {
        pageCrudService.initWith(List.of(page));
    }
}

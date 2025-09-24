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
package io.gravitee.repository.management;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.common.utils.UUID;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.model.ScoringEnvironmentApi;
import io.gravitee.repository.management.model.ScoringEnvironmentSummary;
import io.gravitee.repository.management.model.ScoringReport;
import java.util.Date;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

public class ScoringReportRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/scoring-tests/";
    }

    // Create
    @Test
    public void create_should_create_with_assets() throws TechnicalException {
        var toCreate = aDiagnosticScoring();

        var created = scoringReportRepository.create(toCreate);

        assertThat(created).usingRecursiveComparison().isEqualTo(toCreate);
    }

    @Test
    public void create_should_create_with_errors() throws TechnicalException {
        var toCreate = anErrorScoring();

        var created = scoringReportRepository.create(toCreate);

        assertThat(created).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(toCreate);
    }

    @Test
    public void create_should_create_a_report_without_assets() throws TechnicalException {
        var toCreate = aDiagnosticScoring().toBuilder().assets(List.of()).build();

        var created = scoringReportRepository.create(toCreate);

        assertThat(created).usingRecursiveComparison().isEqualTo(toCreate);
    }

    @Test
    public void create_should_create_a_report_with_Gravitee_Definition_asset() throws TechnicalException {
        var toCreate = aDiagnosticScoring()
            .toBuilder()
            .assets(List.of(new ScoringReport.Asset(null, "GRAVITEE_DEFINITION", List.of(), List.of())))
            .build();

        var created = scoringReportRepository.create(toCreate);

        assertThat(created).usingRecursiveComparison().isEqualTo(toCreate);
    }

    // findLatestFor
    @Test
    public void findLatestFor_should_return_diagnostic_report_if_exists() throws Exception {
        var result = scoringReportRepository.findLatestFor("api2");

        Assertions.assertThat(result).satisfies(report -> {
            assertThat(report).isPresent();
            assertThat(report.get().getApiId()).isEqualTo("api2");
            assertThat(report.get().getEnvironmentId()).isEqualTo("env1");
            assertThat(report.get().getCreatedAt()).isEqualTo(new Date(1470157767000L));
            assertThat(report.get().getSummary()).usingRecursiveComparison().isEqualTo(new ScoringReport.Summary(0.74D, 1L, 2L, 3L, 4L));
            assertThat(report.get().getAssets()).contains(
                new ScoringReport.Asset(null, "GRAVITEE_DEFINITION", List.of(), List.of()),
                new ScoringReport.Asset("7f2ad639-3ac4-4517-8dbe-55f377e9118a", "ASYNCAPI", List.of(), List.of()),
                new ScoringReport.Asset(
                    "f931618f-3207-412a-adad-5bffdce746f7",
                    "SWAGGER",
                    List.of(
                        new ScoringReport.Diagnostic(
                            "WARN",
                            new ScoringReport.Range(new ScoringReport.Position(1, 1), new ScoringReport.Position(1, 1)),
                            "operation-operationId",
                            "Operation must have \"operationId\".",
                            "paths./echo.options"
                        )
                    ),
                    List.of()
                )
            );
        });
    }

    @Test
    public void findLatestFor_should_return_error_report_if_exists() throws Exception {
        var result = scoringReportRepository.findLatestFor("api6");

        Assertions.assertThat(result).satisfies(report -> {
            assertThat(report).isPresent();
            assertThat(report.get().getApiId()).isEqualTo("api6");
            assertThat(report.get().getEnvironmentId()).isEqualTo("env2");
            assertThat(report.get().getCreatedAt()).isEqualTo(new Date(1470157767000L));
            assertThat(report.get().getSummary()).usingRecursiveComparison().isEqualTo(new ScoringReport.Summary(0.0D, 0L, 0L, 0L, 0L));
            assertThat(report.get().getAssets()).contains(
                new ScoringReport.Asset(
                    null,
                    "GRAVITEE_DEFINITION",
                    List.of(),
                    List.of(
                        new ScoringReport.ScoringError(
                            "undefined-function",
                            List.of("rules", "api-key-security-scheme", "then", "function")
                        )
                    )
                )
            );
        });
    }

    @Test
    public void findLatestFor_should_return_empty_report() throws Exception {
        var result = scoringReportRepository.findLatestFor("api3");

        Assertions.assertThat(result)
            .isPresent()
            .get()
            .usingRecursiveComparison()
            .isEqualTo(
                new ScoringReport(
                    "b1419ea8-75c6-4fd9-a8c8-b43a6bda6ee9",
                    "api3",
                    "env1",
                    new Date(1470157767000L),
                    new ScoringReport.Summary(1D, 0L, 0L, 0L, 0L),
                    List.of()
                )
            );
    }

    // findLatestReports
    @Test
    public void findLatestReports_should_return_reports() throws Exception {
        var result = scoringReportRepository.findLatestReports(List.of("api1", "api2", "api3"));

        Assertions.assertThat(result).contains(
            new ScoringReport(
                "1e9013a0-fc13-4bd0-b2e2-cdf2b1895b46",
                "api1",
                "env1",
                new Date(1470157767000L),
                new ScoringReport.Summary(1D, 0L, 0L, 0L, 0L),
                List.of(new ScoringReport.Asset("a8e754af-a593-4dc5-bf38-2d0b83a7edc1", "SWAGGER", List.of(), List.of()))
            ),
            new ScoringReport(
                "cad107c9-27f2-40b2-9107-c927f2e0b2fc",
                "api2",
                "env1",
                new Date(1470157767000L),
                new ScoringReport.Summary(0.74D, 1L, 2L, 3L, 4L),
                List.of(
                    new ScoringReport.Asset(
                        "f931618f-3207-412a-adad-5bffdce746f7",
                        "SWAGGER",
                        List.of(
                            new ScoringReport.Diagnostic(
                                "WARN",
                                new ScoringReport.Range(new ScoringReport.Position(1, 1), new ScoringReport.Position(1, 1)),
                                "operation-operationId",
                                "Operation must have \"operationId\".",
                                "paths./echo.options"
                            )
                        ),
                        List.of()
                    ),
                    new ScoringReport.Asset("7f2ad639-3ac4-4517-8dbe-55f377e9118a", "ASYNCAPI", List.of(), List.of()),
                    new ScoringReport.Asset(null, "GRAVITEE_DEFINITION", List.of(), List.of())
                )
            ),
            new ScoringReport(
                "b1419ea8-75c6-4fd9-a8c8-b43a6bda6ee9",
                "api3",
                "env1",
                new Date(1470157767000L),
                new ScoringReport.Summary(1D, 0L, 0L, 0L, 0L),
                List.of()
            )
        );
    }

    // findEnvironmentLatestReports
    @Test
    public void findEnvironmentLatestReports_should_return_reports() throws Exception {
        var result = scoringReportRepository.findEnvironmentLatestReports("env1", new PageableBuilder().pageNumber(0).pageSize(3).build());

        Assertions.assertThat(result.getContent()).contains(
            new ScoringEnvironmentApi(
                "api1",
                "api 1",
                new Date(1439022010883L),
                "1e9013a0-fc13-4bd0-b2e2-cdf2b1895b46",
                new Date(1470157767000L),
                1D,
                0L,
                0L,
                0L,
                0L
            ),
            new ScoringEnvironmentApi(
                "api3",
                "api 3",
                new Date(1439022010883L),
                "b1419ea8-75c6-4fd9-a8c8-b43a6bda6ee9",
                new Date(1470157767000L),
                1D,
                0L,
                0L,
                0L,
                0L
            ),
            new ScoringEnvironmentApi(
                "api5",
                "api 5",
                new Date(1439022010883L),
                "26a590f2-05fa-433e-a982-d64c0a274ee9",
                new Date(1470157767000L),
                0.95D,
                0L,
                1L,
                0L,
                0L
            )
        );
    }

    @Test
    public void findEnvironmentLatestReports_should_return_apis_without_reports() throws Exception {
        var result = scoringReportRepository.findEnvironmentLatestReports("env2", new PageableBuilder().pageNumber(0).pageSize(3).build());

        Assertions.assertThat(result.getContent()).contains(new ScoringEnvironmentApi("api7", "api 7", new Date(1439022010883L)));
    }

    @Test
    public void findEnvironmentLatestReports_should_return_paginated_reports() throws Exception {
        var result = scoringReportRepository.findEnvironmentLatestReports("env1", new PageableBuilder().pageNumber(1).pageSize(3).build());

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result.getContent()).hasSize(3);
            softly.assertThat(result.getTotalElements()).isEqualTo(6);
            softly.assertThat(result.getPageElements()).isEqualTo(3);
            softly.assertThat(result.getPageNumber()).isOne();
        });
    }

    // DeleteByApi
    @Test
    public void should_deleteByApi() throws Exception {
        var reportBefore = scoringReportRepository.findLatestFor("api1");
        assertThat(reportBefore).isPresent();

        scoringReportRepository.deleteByApi("api1");

        var reportAfter = scoringReportRepository.findLatestFor("api1");
        assertThat(reportAfter).isEmpty();
    }

    // getScoringEnvironmentSummary
    @Test
    public void getScoringEnvironmentSummary_should_return_environment_summary() throws Exception {
        var result = scoringReportRepository.getScoringEnvironmentSummary("env1");

        Assertions.assertThat(result).isEqualTo(new ScoringEnvironmentSummary("env1", 0.82D, 5L, 5L, 5L, 5L));
    }

    @Test
    public void getScoringEnvironmentSummary_should_return_empty_summary_when_no_data() throws Exception {
        var result = scoringReportRepository.getScoringEnvironmentSummary("env4");

        Assertions.assertThat(result).isEqualTo(new ScoringEnvironmentSummary("env4"));
    }

    private static ScoringReport aDiagnosticScoring() {
        return ScoringReport.builder()
            .id(UUID.random().toString())
            .apiId("apiId")
            .environmentId("envId")
            .createdAt(new Date())
            .summary(new ScoringReport.Summary(0.74D, 1L, 2L, 3L, 4L))
            .assets(
                List.of(
                    new ScoringReport.Asset(
                        "pageId",
                        "SWAGGER",
                        List.of(
                            new ScoringReport.Diagnostic(
                                "WARN",
                                new ScoringReport.Range(new ScoringReport.Position(1, 1), new ScoringReport.Position(1, 2)),
                                "rule",
                                "message",
                                "path"
                            )
                        ),
                        List.of()
                    )
                )
            )
            .build();
    }

    private static ScoringReport anErrorScoring() {
        return ScoringReport.builder()
            .id(UUID.random().toString())
            .apiId("error-apiId")
            .environmentId("envId")
            .createdAt(new Date())
            .summary(new ScoringReport.Summary(0.0D, 0L, 0L, 0L, 0L))
            .assets(
                List.of(
                    new ScoringReport.Asset(
                        "pageId",
                        "SWAGGER",
                        List.of(),
                        List.of(
                            new ScoringReport.ScoringError(
                                "undefined-function",
                                List.of("rules", "api-key-security-scheme", "then", "function")
                            ),
                            new ScoringReport.ScoringError("invalid-severity", List.of("rules", "no-http-basic", "severity"))
                        )
                    )
                )
            )
            .build();
    }
}

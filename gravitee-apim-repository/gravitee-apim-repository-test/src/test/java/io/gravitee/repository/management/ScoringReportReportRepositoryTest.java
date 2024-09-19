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
import static org.assertj.core.groups.Tuple.tuple;

import io.gravitee.common.utils.UUID;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.ScoringEnvironmentSummary;
import io.gravitee.repository.management.model.ScoringReport;
import java.util.Date;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class ScoringReportReportRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/scoring-tests/";
    }

    // Create
    @Test
    public void create_should_create() throws TechnicalException {
        var toCreate = aScoring();

        var created = scoringReportRepository.create(toCreate);

        assertThat(created).usingRecursiveComparison().isEqualTo(toCreate);
    }

    @Test
    public void create_should_create_a_report_without_assets() throws TechnicalException {
        var toCreate = aScoring().toBuilder().assets(List.of()).build();

        var created = scoringReportRepository.create(toCreate);

        assertThat(created).usingRecursiveComparison().isEqualTo(toCreate);
    }

    @Test
    public void create_should_create_a_report_with_Gravitee_Definition_asset() throws TechnicalException {
        var toCreate = aScoring().toBuilder().assets(List.of(new ScoringReport.Asset(null, "GRAVITEE_DEFINITION", List.of()))).build();

        var created = scoringReportRepository.create(toCreate);

        assertThat(created).usingRecursiveComparison().isEqualTo(toCreate);
    }

    // findLatestFor
    @Test
    public void findLatestFor_should_return_report_if_exists() throws Exception {
        var result = scoringReportRepository.findLatestFor("api2");

        Assertions
            .assertThat(result)
            .satisfies(report -> {
                assertThat(report).isPresent();
                assertThat(report.get().getApiId()).isEqualTo("api2");
                assertThat(report.get().getEnvironmentId()).isEqualTo("env1");
                assertThat(report.get().getCreatedAt()).isEqualTo(new Date(1470157767000L));
                assertThat(report.get().getSummary()).usingRecursiveComparison().isEqualTo(new ScoringReport.Summary(1L, 2L, 3L, 4L));
                assertThat(report.get().getAssets())
                    .contains(
                        new ScoringReport.Asset(null, "GRAVITEE_DEFINITION", List.of()),
                        new ScoringReport.Asset("7f2ad639-3ac4-4517-8dbe-55f377e9118a", "ASYNCAPI", List.of()),
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
                            )
                        )
                    );
            });
    }

    @Test
    public void findLatestFor_should_return_empty_report() throws Exception {
        var result = scoringReportRepository.findLatestFor("api3");

        Assertions
            .assertThat(result)
            .isPresent()
            .get()
            .usingRecursiveComparison()
            .isEqualTo(
                new ScoringReport(
                    "b1419ea8-75c6-4fd9-a8c8-b43a6bda6ee9",
                    "api3",
                    "env1",
                    new Date(1470157767000L),
                    new ScoringReport.Summary(0L, 0L, 0L, 0L),
                    List.of()
                )
            );
    }

    // findLatestReports
    @Test
    public void findLatestReports_should_return_reports() throws Exception {
        var result = scoringReportRepository.findLatestReports(List.of("api1", "api2", "api3"));

        Assertions
            .assertThat(result)
            .contains(
                new ScoringReport(
                    "1e9013a0-fc13-4bd0-b2e2-cdf2b1895b46",
                    "api1",
                    "env1",
                    new Date(1470157767000L),
                    new ScoringReport.Summary(0L, 0L, 0L, 0L),
                    List.of(new ScoringReport.Asset("a8e754af-a593-4dc5-bf38-2d0b83a7edc1", "SWAGGER", List.of()))
                ),
                new ScoringReport(
                    "cad107c9-27f2-40b2-9107-c927f2e0b2fc",
                    "api2",
                    "env1",
                    new Date(1470157767000L),
                    new ScoringReport.Summary(1L, 2L, 3L, 4L),
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
                            )
                        ),
                        new ScoringReport.Asset("7f2ad639-3ac4-4517-8dbe-55f377e9118a", "ASYNCAPI", List.of()),
                        new ScoringReport.Asset(null, "GRAVITEE_DEFINITION", List.of())
                    )
                ),
                new ScoringReport(
                    "b1419ea8-75c6-4fd9-a8c8-b43a6bda6ee9",
                    "api3",
                    "env1",
                    new Date(1470157767000L),
                    new ScoringReport.Summary(0L, 0L, 0L, 0L),
                    List.of()
                )
            );
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

        Assertions.assertThat(result).isEqualTo(new ScoringEnvironmentSummary("env1", 5L, 5L, 5L, 5L));
    }

    @Test
    public void getScoringEnvironmentSummary_should_return_empty_summary_when_no_data() throws Exception {
        var result = scoringReportRepository.getScoringEnvironmentSummary("env4");

        Assertions.assertThat(result).isEqualTo(new ScoringEnvironmentSummary("env4", 0L, 0L, 0L, 0L));
    }

    private static ScoringReport aScoring() {
        return ScoringReport
            .builder()
            .id(UUID.random().toString())
            .apiId("apiId")
            .environmentId("envId")
            .createdAt(new Date())
            .summary(new ScoringReport.Summary(1L, 2L, 3L, 4L))
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
                        )
                    )
                )
            )
            .build();
    }
}

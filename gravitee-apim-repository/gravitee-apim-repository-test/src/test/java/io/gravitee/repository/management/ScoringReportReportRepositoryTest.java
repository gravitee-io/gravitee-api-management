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

import io.gravitee.repository.exceptions.TechnicalException;
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

        assertThat(created).isEqualTo(toCreate);
    }

    @Test
    public void create_should_create_a_report_without_assets() throws TechnicalException {
        var toCreate = aScoring().toBuilder().assets(List.of()).build();

        var created = scoringReportRepository.create(toCreate);

        assertThat(created).isEqualTo(toCreate);
    }

    @Test
    public void create_should_create_a_report_without_Gravitee_Definition_asset() throws TechnicalException {
        var toCreate = aScoring().toBuilder().assets(List.of(new ScoringReport.Asset(null, "GRAVITEE_DEFINITION", List.of()))).build();

        var created = scoringReportRepository.create(toCreate);

        assertThat(created).isEqualTo(toCreate);
    }

    // findLatestFor
    @Test
    public void findLatestFor_should_return_report_if_exists() throws Exception {
        var result = scoringReportRepository.findLatestFor("api2");

        Assertions
            .assertThat(result)
            .contains(
                new ScoringReport(
                    "cad107c9-27f2-40b2-9107-c927f2e0b2fc",
                    "api2",
                    new Date(1470157767000L),
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

    private static ScoringReport aScoring() {
        return ScoringReport
            .builder()
            .id("scoring-id")
            .apiId("apiId")
            .createdAt(new Date())
            .asset(
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
            .build();
    }
}

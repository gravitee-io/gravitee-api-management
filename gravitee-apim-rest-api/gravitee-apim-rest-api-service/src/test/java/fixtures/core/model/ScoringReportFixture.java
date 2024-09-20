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
package fixtures.core.model;

import io.gravitee.apim.core.scoring.model.ScoringAssetType;
import io.gravitee.apim.core.scoring.model.ScoringReport;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.function.Supplier;

public class ScoringReportFixture {

    private ScoringReportFixture() {}

    public static final Supplier<ScoringReport.ScoringReportBuilder> BASE = () ->
        ScoringReport
            .builder()
            .id("report-id")
            .apiId("api-id")
            .environmentId("environment-id")
            .summary(new ScoringReport.Summary(0.9D, 0L, 1L, 0L, 0L))
            .assets(
                List.of(
                    new ScoringReport.Asset(
                        "page-id",
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
                    new ScoringReport.Asset(null, ScoringAssetType.GRAVITEE_DEFINITION, List.of())
                )
            )
            .createdAt(Instant.parse("2020-02-03T20:22:02.00Z").atZone(ZoneId.systemDefault()));

    public static ScoringReport aScoringReport() {
        return BASE.get().build();
    }
}

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

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.documentation.crud_service.PageCrudService;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.scoring.model.ScoringReport;
import io.gravitee.apim.core.scoring.model.ScoringReportView;
import io.gravitee.apim.core.scoring.query_service.ScoringReportQueryService;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@UseCase
public class GetLatestReportUseCase {

    private final ScoringReportQueryService scoringReportQueryService;
    private final PageCrudService pageCrudService;

    public Output execute(Input input) {
        var report = scoringReportQueryService.findLatestByApiId(input.apiId).orElse(null);

        if (report == null) {
            return new Output(null);
        }

        var assets = report
            .assets()
            .stream()
            .map(asset -> {
                var pageName = pageCrudService.findById(asset.pageId()).map(Page::getName).orElse(null);
                return new ScoringReportView.AssetView(pageName, asset.type(), asset.diagnostics());
            })
            .toList();

        return new Output(
            new ScoringReportView(
                report.id(),
                report.apiId(),
                report.createdAt(),
                assets,
                new ScoringReportView.Summary(
                    report.summary().errors(),
                    report.summary().warnings(),
                    report.summary().infos(),
                    report.summary().hints()
                )
            )
        );
    }

    public record Input(String apiId) {}

    public record Output(ScoringReportView report) {}
}

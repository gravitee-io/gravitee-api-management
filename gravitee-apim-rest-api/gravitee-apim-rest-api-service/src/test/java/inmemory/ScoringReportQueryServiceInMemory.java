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
package inmemory;

import io.gravitee.apim.core.scoring.model.EnvironmentApiScoringReport;
import io.gravitee.apim.core.scoring.model.EnvironmentOverview;
import io.gravitee.apim.core.scoring.model.ScoringReport;
import io.gravitee.apim.core.scoring.query_service.ScoringReportQueryService;
import io.gravitee.common.data.domain.Page;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.rest.api.model.common.Pageable;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ScoringReportQueryServiceInMemory implements ScoringReportQueryService, InMemoryAlternative<ScoringReport> {

    private final List<ScoringReport> storage;

    /**
     * API type per API id. The storage holds {@link ScoringReport}s, which carry no API type — the real
     * query reads it from the API row it joins. Tests that exercise the `apiTypes` filter declare the
     * types here; an API absent from this map has no type, exactly like a pre-v4 API.
     */
    private final Map<String, ApiType> apiTypeByApiId = new HashMap<>();

    public ScoringReportQueryServiceInMemory() {
        storage = new ArrayList<>();
    }

    public ScoringReportQueryServiceInMemory(ScoringReportCrudServiceInMemory integrationCrudServiceInMemory) {
        storage = integrationCrudServiceInMemory.storage;
    }

    /** Declares the type of an API, so `findEnvironmentLatestReports` can filter on it. */
    public void giveApiType(String apiId, ApiType apiType) {
        apiTypeByApiId.put(apiId, apiType);
    }

    @Override
    public Optional<ScoringReport> findLatestByApiId(String apiId) {
        return storage
            .stream()
            .filter(report -> report.apiId().equals(apiId))
            .max(Comparator.comparing(ScoringReport::createdAt));
    }

    @Override
    public Page<EnvironmentApiScoringReport> findEnvironmentLatestReports(
        String environmentId,
        Collection<ApiType> apiTypes,
        Pageable pageable
    ) {
        var pageNumber = pageable.getPageNumber();
        var pageSize = pageable.getPageSize();
        var filtered = apiTypes != null && !apiTypes.isEmpty();

        var matches = storage
            .stream()
            .filter(report -> environmentId.equals(report.environmentId()))
            .filter(report -> !filtered || apiTypes.contains(apiTypeByApiId.get(report.apiId())))
            .sorted(Comparator.comparing((ScoringReport r) -> r.summary().score()).reversed())
            .map(report ->
                new EnvironmentApiScoringReport(
                    new EnvironmentApiScoringReport.Api(
                        report.apiId(),
                        "api-name",
                        apiTypeByApiId.get(report.apiId()),
                        Instant.parse("2023-10-22T10:15:30Z").atZone(ZoneId.systemDefault())
                    ),
                    new EnvironmentApiScoringReport.Summary(
                        report.id(),
                        report.createdAt(),
                        report.summary().score(),
                        report.summary().errors(),
                        report.summary().warnings(),
                        report.summary().infos(),
                        report.summary().hints()
                    )
                )
            )
            .toList();

        var page = matches.size() <= pageSize
            ? matches
            : matches.subList((pageNumber - 1) * pageSize, Math.min(pageNumber * pageSize, matches.size()));

        return new Page<>(page, pageNumber, pageSize, matches.size());
    }

    @Override
    public EnvironmentOverview getEnvironmentScoringSummary(String environmentId) {
        return storage
            .stream()
            .filter(report -> report.environmentId().equals(environmentId))
            .reduce(
                new EnvironmentOverview(environmentId, 0.84, 0L, 0L, 0L, 0L),
                (summary, report) ->
                    new EnvironmentOverview(
                        environmentId,
                        0.84,
                        summary.errors() + report.summary().errors(),
                        summary.warnings() + report.summary().warnings(),
                        summary.infos() + report.summary().infos(),
                        summary.hints() + report.summary().hints()
                    ),
                (summary1, summary2) ->
                    new EnvironmentOverview(
                        environmentId,
                        0.84,
                        summary1.errors() + summary2.errors(),
                        summary1.warnings() + summary2.warnings(),
                        summary1.infos() + summary2.infos(),
                        summary1.hints() + summary2.hints()
                    )
            );
    }

    @Override
    public void initWith(List<ScoringReport> items) {
        storage.clear();
        storage.addAll(items);
    }

    @Override
    public void reset() {
        storage.clear();
        apiTypeByApiId.clear();
    }

    @Override
    public List<ScoringReport> storage() {
        return Collections.unmodifiableList(storage);
    }
}

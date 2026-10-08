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
package io.gravitee.apim.infra.query_service.analytics_engine;

import io.gravitee.apim.core.analytics_engine.model.FacetsResponse;
import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import io.gravitee.apim.core.analytics_engine.model.MeasuresResponse;
import io.gravitee.apim.core.analytics_engine.model.TimeSeriesResponse;
import io.gravitee.apim.infra.adapter.AnalyticsMeasuresAdapter;
import io.gravitee.repository.analytics.engine.api.query.AnalyticsSearchPath;
import io.gravitee.repository.analytics.engine.api.query.Filter;
import io.gravitee.repository.analytics.engine.api.query.FilterOutcome;
import io.gravitee.repository.analytics.engine.api.query.Query;
import io.gravitee.repository.log.v4.api.AnalyticsRepository;
import java.util.List;

/**
 * Marks a response with the top-level filters its search path skipped, as the analytics repository
 * reports them. Done by the query service that ran the search, so the path reported is the one taken.
 */
final class IgnoredFilters {

    private IgnoredFilters() {}

    static List<FilterSpec.Name> of(AnalyticsRepository repository, AnalyticsSearchPath path, Query query) {
        if (query == null || query.filters() == null) {
            return List.of();
        }
        var filters = query.filters();
        var outcomes = filters
            .stream()
            .map(filter -> new Outcome(filter.name(), repository.filterOutcome(path, filter.name())))
            .toList();
        // A condition that empties the result makes the others moot: nothing is counted either way.
        if (outcomes.stream().anyMatch(outcome -> outcome.outcome() == FilterOutcome.EMPTIES)) {
            return List.of();
        }
        return outcomes
            .stream()
            .filter(outcome -> outcome.outcome() == FilterOutcome.IGNORED)
            .map(outcome -> AnalyticsMeasuresAdapter.INSTANCE.fromFilterName(outcome.name()))
            .distinct()
            .toList();
    }

    static MeasuresResponse mark(MeasuresResponse response, List<FilterSpec.Name> ignored) {
        return response == null ? null : response.withIgnoredFilters(ignored);
    }

    static FacetsResponse mark(FacetsResponse response, List<FilterSpec.Name> ignored) {
        return response == null ? null : response.withIgnoredFilters(ignored);
    }

    static TimeSeriesResponse mark(TimeSeriesResponse response, List<FilterSpec.Name> ignored) {
        return response == null ? null : response.withIgnoredFilters(ignored);
    }

    private record Outcome(Filter.Name name, FilterOutcome outcome) {}
}

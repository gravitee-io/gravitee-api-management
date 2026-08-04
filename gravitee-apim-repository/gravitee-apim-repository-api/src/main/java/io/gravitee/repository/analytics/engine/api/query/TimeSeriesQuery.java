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
package io.gravitee.repository.analytics.engine.api.query;

import java.util.List;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public record TimeSeriesQuery(
    TimeRange timeRange,
    List<Filter> filters,
    Long interval,
    List<MetricMeasuresQuery> metrics,
    List<Facet> facets,
    Integer limit,
    List<NumberRange> ranges
) implements Query {
    public TimeSeriesQuery {
        // The convenience constructors below default the limit to 0 to mean "no explicit cap", but a
        // terms `size` of 0 is rejected by Elasticsearch. Normalise it to null once here (FacetsQuery
        // already defaults to null) so the terms adapters simply omit `size` — closing the size:0 hazard
        // for both the HTTP and native time-series paths at the source rather than in each adapter.
        if (limit != null && limit <= 0) {
            limit = null;
        }
    }

    public TimeSeriesQuery(TimeRange timeRange, List<Filter> filters, Long interval, List<MetricMeasuresQuery> metrics) {
        this(timeRange, filters, interval, metrics, List.of(), 0, List.of());
    }

    public TimeSeriesQuery(
        TimeRange timeRange,
        List<Filter> filters,
        Long interval,
        List<MetricMeasuresQuery> metrics,
        List<Facet> facets
    ) {
        this(timeRange, filters, interval, metrics, facets, 0, List.of());
    }

    public FacetsQuery toFacetQuery() {
        return new FacetsQuery(timeRange, filters, metrics, facets, limit, ranges);
    }
}

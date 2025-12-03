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
package io.gravitee.apim.core.analytics_engine.model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public record TimeSeriesRequest(
    TimeRange timeRange,
    Long interval,
    List<Filter> filters,
    List<FacetMetricMeasuresRequest> metrics,
    List<FacetSpec.Name> facets,
    Integer limit,
    List<NumberRange> ranges
) {
    public TimeSeriesRequest(TimeRange timeRange, Long interval, List<Filter> filters) {
        this(timeRange, interval, filters, new ArrayList<>(), List.of(), null, List.of());
    }

    public TimeSeriesRequest emptyMetrics() {
        return new TimeSeriesRequest(timeRange, interval, filters, new ArrayList<>(), facets, limit, ranges);
    }

    public TimeSeriesRequest withFilters(List<Filter> filters) {
        return new TimeSeriesRequest(
            this.timeRange(),
            this.interval(),
            filters,
            this.metrics(),
            this.facets(),
            this.limit(),
            this.ranges()
        );
    }
}

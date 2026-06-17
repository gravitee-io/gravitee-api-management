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
package io.gravitee.gamma.rest.core.observability.analytics.use_case;

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.apim.core.UseCase;
import io.gravitee.gamma.rest.core.observability.analytics.model.AnalyticsFacetMetricQuery;
import io.gravitee.gamma.rest.core.observability.analytics.model.AnalyticsNumberRange;
import io.gravitee.gamma.rest.core.observability.analytics.port.service_provider.ObservabilityAnalyticsDataPort;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterCondition;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;

/**
 * Computes analytics time-series via the shared engine, applying the unified Gamma filter vocabulary
 * and RBAC scoping.
 *
 * @author GraviteeSource Team
 */
@UseCase
@AllArgsConstructor
public class ComputeObservabilityTimeSeriesUseCase {

    private final ObservabilityAnalyticsDataPort analyticsDataPort;
    private final AnalyticsRequestPipeline pipeline;

    public record Input(
        String organizationId,
        String environmentId,
        List<FilterCondition> filters,
        Instant from,
        Instant to,
        Long interval,
        List<AnalyticsFacetMetricQuery> metrics,
        List<String> facets,
        Integer facetSize,
        List<AnalyticsNumberRange> ranges
    ) {}

    public record Output(JsonNode response) {}

    public Output execute(Input input) {
        var scope = pipeline.prepare(input.organizationId, input.environmentId, input.filters, input.from, input.to, analyticsDataPort);

        if (scope.isEmpty()) {
            return new Output(analyticsDataPort.emptyTimeSeriesResponse());
        }

        var query = new ObservabilityAnalyticsDataPort.TimeSeriesQuery(
            input.organizationId,
            input.environmentId,
            scope,
            input.interval,
            input.facets,
            input.facetSize,
            input.metrics,
            input.ranges
        );
        return new Output(analyticsDataPort.computeTimeSeries(query));
    }
}

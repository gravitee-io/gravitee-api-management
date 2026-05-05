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
package io.gravitee.apim.core.log.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.analytics_engine.model.*;
import io.gravitee.apim.core.analytics_engine.service_provider.AnalyticsQueryContextProvider;
import io.gravitee.apim.core.observability.model.FilterOperator;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.CustomLog;

@CustomLog
@UseCase
public class NativeApiLogSummaryUseCase {

    private final AnalyticsQueryContextProvider queryContextProvider;

    public NativeApiLogSummaryUseCase(AnalyticsQueryContextProvider queryContextProvider) {
        this.queryContextProvider = queryContextProvider;
    }

    public record Input(ExecutionContext executionContext, String apiId, Instant from, Instant to) {}

    public record Output(Map<String, Long> countByConnectionStatus) {}

    public Output execute(Input input) {
        var facetsRequest = buildFacetsRequest(input);
        var queryContext = queryContextProvider.resolve(facetsRequest);

        var responses = new ArrayList<FacetsResponse>();
        queryContext.forEach((service, request) -> {
            try {
                responses.add(service.searchFacets(input.executionContext(), request));
            } catch (RuntimeException e) {
                log.error("Failed to search native facets [apiId={},from={},to={}]", input.apiId(), input.from(), input.to(), e);
                throw e;
            }
        });

        return new Output(extractCounts(FacetsResponse.merge(responses)));
    }

    private static FacetsRequest buildFacetsRequest(Input input) {
        return new FacetsRequest(
            new TimeRange(input.from(), input.to()),
            List.of(new Filter(FilterSpec.Name.API, FilterOperator.EQ, input.apiId())),
            List.of(
                new FacetMetricMeasuresRequest(MetricSpec.Name.NATIVE_CONNECTIONS_SUMMARY, List.of(MetricSpec.Measure.COUNT), List.of())
            ),
            List.of(FacetSpec.Name.NATIVE_CONNECTION_STATUS),
            null,
            null
        );
    }

    private static Map<String, Long> extractCounts(FacetsResponse response) {
        var counts = new HashMap<String, Long>();
        for (var metric : response.metrics()) {
            if (metric.metric() != MetricSpec.Name.NATIVE_CONNECTIONS_SUMMARY) {
                continue;
            }
            for (var bucket : metric.buckets()) {
                bucket
                    .measures()
                    .stream()
                    .filter(measure -> measure.name() == MetricSpec.Measure.COUNT)
                    .findFirst()
                    .ifPresent(measure -> counts.merge(bucket.key(), measure.value().longValue(), Long::sum));
            }
        }
        return counts;
    }
}

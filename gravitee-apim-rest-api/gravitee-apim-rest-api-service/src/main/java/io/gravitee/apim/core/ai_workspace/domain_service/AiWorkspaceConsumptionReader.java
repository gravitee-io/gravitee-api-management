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
package io.gravitee.apim.core.ai_workspace.domain_service;

import io.gravitee.apim.core.ai_workspace.model.AiWorkspaceConsumption;
import io.gravitee.apim.core.analytics_engine.model.FacetBucketResponse;
import io.gravitee.apim.core.analytics_engine.model.FacetMetricMeasuresRequest;
import io.gravitee.apim.core.analytics_engine.model.FacetSpec;
import io.gravitee.apim.core.analytics_engine.model.FacetsRequest;
import io.gravitee.apim.core.analytics_engine.model.FacetsResponse;
import io.gravitee.apim.core.analytics_engine.model.Filter;
import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import io.gravitee.apim.core.analytics_engine.model.MetricSpec;
import io.gravitee.apim.core.analytics_engine.model.TimeRange;
import io.gravitee.apim.core.analytics_engine.query_service.AnalyticsEngineQueryService;
import io.gravitee.apim.core.observability.model.FilterOperator;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;

/**
 * The caller's own LLM usage over the last 30 days, the same window the Gamma AI Workspace console shows.
 * A missing or failing analytics store answers with zeros.
 */
@RequiredArgsConstructor
public class AiWorkspaceConsumptionReader {

    static final Duration WINDOW = Duration.ofDays(30);

    private final AnalyticsEngineQueryService analyticsEngineQueryService;

    public AiWorkspaceConsumption read(ExecutionContext executionContext, String apiProductId, String applicationId) {
        Instant to = Instant.now();
        Instant from = to.minus(WINDOW);
        try {
            FacetsResponse response = analyticsEngineQueryService.searchFacets(
                executionContext,
                request(apiProductId, applicationId, from, to)
            );
            if (response == null || response.metrics() == null) {
                return AiWorkspaceConsumption.zeros(from, to);
            }
            return new AiWorkspaceConsumption(
                sum(response, MetricSpec.Name.LLM_PROMPT_TOTAL_TOKEN),
                sum(response, MetricSpec.Name.HTTP_REQUESTS),
                sumDouble(response, MetricSpec.Name.LLM_PROMPT_TOKEN_TOTAL_COST),
                from,
                to
            );
        } catch (RuntimeException e) {
            return AiWorkspaceConsumption.zeros(from, to);
        }
    }

    private static FacetsRequest request(String apiProductId, String applicationId, Instant from, Instant to) {
        return new FacetsRequest(
            new TimeRange(from, to),
            List.of(
                new Filter(FilterSpec.Name.API_PRODUCT, FilterOperator.IN, List.of(apiProductId)),
                new Filter(FilterSpec.Name.APPLICATION, FilterOperator.IN, List.of(applicationId))
            ),
            List.of(
                new FacetMetricMeasuresRequest(MetricSpec.Name.HTTP_REQUESTS, List.of(MetricSpec.Measure.COUNT), List.of()),
                new FacetMetricMeasuresRequest(MetricSpec.Name.LLM_PROMPT_TOTAL_TOKEN, List.of(MetricSpec.Measure.COUNT), List.of()),
                new FacetMetricMeasuresRequest(MetricSpec.Name.LLM_PROMPT_TOKEN_TOTAL_COST, List.of(MetricSpec.Measure.COUNT), List.of())
            ),
            List.of(FacetSpec.Name.APPLICATION),
            1,
            List.of()
        );
    }

    private static long sum(FacetsResponse response, MetricSpec.Name metric) {
        return (long) sumDouble(response, metric);
    }

    private static double sumDouble(FacetsResponse response, MetricSpec.Name metric) {
        return response
            .metrics()
            .stream()
            .filter(entry -> entry.metric() == metric)
            .flatMap(entry -> entry.buckets() == null ? java.util.stream.Stream.empty() : entry.buckets().stream())
            .mapToDouble(AiWorkspaceConsumptionReader::count)
            .sum();
    }

    private static double count(FacetBucketResponse bucket) {
        if (bucket.measures() == null) {
            return 0d;
        }
        return bucket
            .measures()
            .stream()
            .filter(measure -> measure.name() == MetricSpec.Measure.COUNT && measure.value() != null)
            .mapToDouble(measure -> measure.value().doubleValue())
            .findFirst()
            .orElse(0d);
    }
}

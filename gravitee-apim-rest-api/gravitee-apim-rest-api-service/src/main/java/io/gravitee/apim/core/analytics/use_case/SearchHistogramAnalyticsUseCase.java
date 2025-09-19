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
package io.gravitee.apim.core.analytics.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.analytics.domain_service.AnalyticsMetadataProvider;
import io.gravitee.apim.core.analytics.domain_service.ApiAnalyticsSpecification;
import io.gravitee.apim.core.analytics.model.Aggregation;
import io.gravitee.apim.core.analytics.model.EventAnalytics;
import io.gravitee.apim.core.analytics.model.HistogramAnalytics;
import io.gravitee.apim.core.analytics.model.Term;
import io.gravitee.apim.core.analytics.model.Timestamp;
import io.gravitee.apim.core.analytics.query_service.AnalyticsQueryService;
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@UseCase
public class SearchHistogramAnalyticsUseCase {

    private final ApiCrudService apiCrudService;
    private final AnalyticsQueryService analyticsQueryService;
    private final List<AnalyticsMetadataProvider> metadataProviders;

    public Output execute(ExecutionContext executionContext, Input input) {
        Api api = apiCrudService.get(input.api);
        ApiAnalyticsSpecification.forSearchHistogramAnalytics().throwIfNotSatisfied(api, executionContext, input.from(), input.to());

        if (api.getType() == ApiType.NATIVE) {
            return executeV4NativeAPICase(executionContext, input);
        }

        return executeV4APICase(executionContext, input);
    }

    private Output executeV4APICase(ExecutionContext executionContext, Input input) {
        var histogramQuery = new AnalyticsQueryService.HistogramQuery(
            AnalyticsQueryService.SearchTermId.forApi(input.api),
            Instant.ofEpochMilli(input.from()),
            Instant.ofEpochMilli(input.to()),
            Duration.ofMillis(input.interval()),
            input.aggregations(),
            input.query(),
            null
        );
        var result = analyticsQueryService
            .searchHistogramAnalytics(executionContext, histogramQuery)
            .map(HistogramAnalytics::buckets)
            .orElse(List.of());

        Map<String, Map<String, Map<String, String>>> metadata = new HashMap<>();

        // Process count buckets to collect metadata
        List<HistogramAnalytics.CountBucket> countBuckets = result
            .stream()
            .filter(bucket -> bucket instanceof HistogramAnalytics.CountBucket)
            .map(bucket -> (HistogramAnalytics.CountBucket) bucket)
            .toList();

        for (HistogramAnalytics.CountBucket countBucket : countBuckets) {
            var providerOpt = metadataProviders
                .stream()
                .filter(p -> p.appliesTo(AnalyticsMetadataProvider.Field.of(countBucket.getField())))
                .findFirst();

            if (providerOpt.isPresent()) {
                AnalyticsMetadataProvider provider = providerOpt.get();

                // Get all keys for this bucket
                List<String> keys = new ArrayList<>(countBucket.getCounts().keySet());

                // Get metadata for all keys at once
                Map<String, Map<String, String>> bucketMetadata = provider.provide(keys, executionContext.getEnvironmentId());

                metadata.put(countBucket.getName(), bucketMetadata);
            } else {
                metadata.put(countBucket.getName(), Collections.emptyMap());
            }
        }

        return new Output(
            new Timestamp(Instant.ofEpochMilli(input.from()), Instant.ofEpochMilli(input.to()), Duration.ofMillis(input.interval())),
            result,
            metadata
        );
    }

    private Output executeV4NativeAPICase(ExecutionContext executionContext, Input input) {
        Instant from = Instant.ofEpochMilli(input.from());
        Instant to = Instant.ofEpochMilli(input.to());
        var histogramQuery = new AnalyticsQueryService.HistogramQuery(
            AnalyticsQueryService.SearchTermId.forApi(input.api),
            from,
            to,
            Duration.ofMillis(input.interval()),
            input.aggregations(),
            input.query(),
            (input.terms() != null && input.terms().isPresent()) ? input.terms().get() : List.of()
        );

        Optional<EventAnalytics> eventAnalytics = analyticsQueryService.searchEventAnalytics(executionContext, histogramQuery);
        // Dump analytics data into metric buckets.
        List<HistogramAnalytics.Bucket> buckets = new ArrayList<>();
        Timestamp timestamp = new Timestamp(from, to, Duration.ofMillis(input.interval()));
        eventAnalytics.ifPresent(
            (analytics ->
                    analytics
                        .values()
                        .forEach((aggName, values) -> {
                            if (!values.isEmpty()) {
                                String field = values.keySet().iterator().next();
                                List<Long> valueList = values.get(field);
                                buckets.add(new HistogramAnalytics.MetricBucket(aggName, field, valueList));
                            }
                        }))
        );

        return new Output(timestamp, buckets, Collections.emptyMap());
    }

    public record Input(
        String api,
        long from,
        long to,
        long interval,
        List<Aggregation> aggregations,
        Optional<String> query,
        Optional<List<Term>> terms
    ) {}

    public record Output(
        Timestamp timestamp,
        List<HistogramAnalytics.Bucket> values,
        Map<String, Map<String, Map<String, String>>> metadata
    ) {}
}

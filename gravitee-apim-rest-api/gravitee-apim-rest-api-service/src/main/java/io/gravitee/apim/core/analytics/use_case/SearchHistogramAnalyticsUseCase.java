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
import io.gravitee.apim.core.analytics.model.HistogramAnalytics;
import io.gravitee.apim.core.analytics.model.Timestamp;
import io.gravitee.apim.core.analytics.query_service.AnalyticsQueryService;
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
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
        ApiAnalyticsSpecification
            .forSearchHistogramAnalytics()
            .throwIfNotSatisfied(apiCrudService.get(input.api), executionContext, input.from(), input.to());

        var histogramQuery = new AnalyticsQueryService.HistogramQuery(
            AnalyticsQueryService.SearchTermId.forApi(input.api),
            Instant.ofEpochMilli(input.from()),
            Instant.ofEpochMilli(input.to()),
            Duration.ofMillis(input.interval()),
            input.aggregations(),
            input.query()
        );
        var result = analyticsQueryService
            .searchHistogramAnalytics(executionContext, histogramQuery)
            .map(io.gravitee.apim.core.analytics.model.HistogramAnalytics::buckets)
            .orElse(List.of());

        Map<String, Map<String, Map<String, String>>> metadata = result
            .stream()
            .filter(bucket -> bucket instanceof HistogramAnalytics.CountBucket)
            .map(bucket -> (HistogramAnalytics.CountBucket) bucket)
            .collect(
                Collectors.toMap(
                    HistogramAnalytics.CountBucket::getName,
                    countBucket ->
                        metadataProviders
                            .stream()
                            .filter(p -> p.appliesTo(AnalyticsMetadataProvider.Field.of(countBucket.getField())))
                            .findFirst()
                            .map(provider ->
                                countBucket
                                    .getCounts()
                                    .keySet()
                                    .stream()
                                    .collect(
                                        Collectors.toMap(
                                            category -> category,
                                            category -> provider.provide(category, executionContext.getEnvironmentId())
                                        )
                                    )
                            )
                            .orElseGet(Collections::emptyMap)
                )
            );

        return new Output(
            new Timestamp(Instant.ofEpochMilli(input.from()), Instant.ofEpochMilli(input.to()), Duration.ofMillis(input.interval())),
            result,
            metadata
        );
    }

    public record Input(String api, long from, long to, long interval, List<Aggregation> aggregations, Optional<String> query) {}

    public record Output(
        Timestamp timestamp,
        List<HistogramAnalytics.Bucket> values,
        Map<String, Map<String, Map<String, String>>> metadata
    ) {}
}

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
import io.gravitee.apim.core.analytics.model.GroupByAnalytics;
import io.gravitee.apim.core.analytics.query_service.AnalyticsQueryService;
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@UseCase
public class SearchGroupByAnalyticsUseCase {

    private final ApiCrudService apiCrudService;
    private final AnalyticsQueryService analyticsQueryService;
    private final List<AnalyticsMetadataProvider> metadataProviders;

    public Output execute(ExecutionContext executionContext, Input input) {
        ApiAnalyticsSpecification
            .forSearchGroupByAnalytics()
            .throwIfNotSatisfied(apiCrudService.get(input.api()), executionContext, input.from(), input.to());

        var groupByQuery = new AnalyticsQueryService.GroupByQuery(
            input.api(),
            Instant.ofEpochMilli(input.from()),
            Instant.ofEpochMilli(input.to()),
            input.field(),
            input.groups(),
            input.order(),
            input.query() // pass query parameter
        );
        var result = analyticsQueryService.searchGroupByAnalytics(executionContext, groupByQuery).orElse(null);

        if (result == null) {
            return new Output(null, null);
        }

        var provider = metadataProviders.stream().filter(p -> p.appliesTo(AnalyticsMetadataProvider.Field.of(input.field()))).findFirst();

        var metadata = Stream
            .iterate(0, i -> i + 1)
            .limit(result.getOrder().size())
            .collect(
                Collectors.toMap(
                    i -> result.getOrder().get(i),
                    i -> {
                        Map<String, String> itemMetadata = provider
                            .map(p -> p.provide(result.getOrder().get(i), executionContext.getEnvironmentId()))
                            .map(HashMap::new)
                            .orElseGet(HashMap::new);
                        itemMetadata.put("order", String.valueOf(i));
                        return itemMetadata;
                    }
                )
            );

        return new Output(result, metadata);
    }

    public record Input(
        String api,
        long from,
        long to,
        String field,
        List<AnalyticsQueryService.GroupByQuery.Group> groups,
        Optional<AnalyticsQueryService.GroupByQuery.Order> order,
        Optional<String> query // new query parameter
    ) {}

    public record Output(GroupByAnalytics analytics, Map<String, Map<String, String>> metadata) {}
}

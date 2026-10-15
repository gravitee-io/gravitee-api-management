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
import io.gravitee.apim.core.analytics.domain_service.EnvironmentAnalyticsApisDomainService;
import io.gravitee.apim.core.analytics.model.AnalyticsQueryParameters;
import io.gravitee.apim.core.analytics.query_service.AnalyticsQueryService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.rest.api.model.v4.analytics.TopHitsApis;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.CustomLog;

@CustomLog
@UseCase
@AllArgsConstructor
public class SearchEnvironmentTopHitsApisCountUseCase {

    AnalyticsQueryService analyticsQueryService;
    EnvironmentAnalyticsApisDomainService environmentAnalyticsApisDomainService;

    public Output execute(Input input) {
        var envId = input.executionContext().getEnvironmentId();
        var apis = environmentAnalyticsApisDomainService
            .findReadableApis(input.executionContext(), input.userId())
            .stream()
            .collect(Collectors.toMap(Api::getId, value -> value));
        var apiIds = apis.keySet().stream().toList();

        log.debug("Searching top API hits, found: {} APIs for env: {}", apiIds.size(), envId);
        if (apiIds.isEmpty()) {
            return noTopHits();
        }

        return analyticsQueryService
            .searchTopHitsApis(input.executionContext(), input.parameters().withApiIds(apiIds))
            .map(topHitsApis -> sortByCountAndUpdateTopHitsWithApiNames(apis, topHitsApis))
            .map(Output::new)
            .orElseGet(SearchEnvironmentTopHitsApisCountUseCase::noTopHits);
    }

    private static Output noTopHits() {
        return new Output(TopHitsApis.builder().data(List.of()).build());
    }

    private TopHitsApis sortByCountAndUpdateTopHitsWithApiNames(Map<String, Api> apis, TopHitsApis topHitsApis) {
        var data = topHitsApis
            .getData()
            .stream()
            .sorted(Comparator.comparingLong(TopHitsApis.TopHitApi::count).reversed())
            .filter(topHitApi -> apis.containsKey(topHitApi.id()))
            .map(topHitApi -> {
                var api = apis.get(topHitApi.id());
                return TopHitsApis.TopHitApi.builder()
                    .id(topHitApi.id())
                    .name(api.getName())
                    .count(topHitApi.count())
                    .definitionVersion(Optional.ofNullable(api.getDefinitionVersion()).orElse(DefinitionVersion.V2))
                    .build();
            })
            .toList();
        return TopHitsApis.builder().data(data).build();
    }

    @Builder
    public record Input(ExecutionContext executionContext, AnalyticsQueryParameters parameters, String userId) {}

    public record Output(TopHitsApis topHitsApis) {}
}

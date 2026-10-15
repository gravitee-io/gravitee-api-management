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
import io.gravitee.rest.api.model.v4.analytics.TopFailedApis;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

@CustomLog
@UseCase
@RequiredArgsConstructor
public class SearchEnvironmentTopFailedApisUseCase {

    private final AnalyticsQueryService analyticsQueryService;
    private final EnvironmentAnalyticsApisDomainService environmentAnalyticsApisDomainService;

    public Output execute(Input input) {
        var envId = input.executionContext().getEnvironmentId();
        var apis = environmentAnalyticsApisDomainService
            .findReadableApis(input.executionContext(), input.userId())
            .stream()
            .collect(Collectors.toMap(Api::getId, value -> value));
        var apiIds = apis.keySet().stream().toList();

        log.debug("Searching top failed API, found: {} APIs for env: {}", apiIds.size(), envId);
        if (apiIds.isEmpty()) {
            return noTopFailedApis();
        }

        return analyticsQueryService
            .searchTopFailedApis(input.executionContext(), input.parameters().withApiIds(apiIds))
            .map(topHitsApis -> sortByCountAndUpdateTopHitsWithApiNames(apis, topHitsApis))
            .map(SearchEnvironmentTopFailedApisUseCase.Output::new)
            .orElseGet(SearchEnvironmentTopFailedApisUseCase::noTopFailedApis);
    }

    private static Output noTopFailedApis() {
        return new Output(TopFailedApis.builder().data(List.of()).build());
    }

    private TopFailedApis sortByCountAndUpdateTopHitsWithApiNames(Map<String, Api> apis, TopFailedApis topFailedApis) {
        var failedApis = topFailedApis
            .getData()
            .stream()
            .sorted(Comparator.comparingLong(TopFailedApis.TopFailedApi::failedCalls).reversed())
            .filter(topFailedApi -> apis.containsKey(topFailedApi.id()))
            .map(topFailedApi -> {
                var api = apis.get(topFailedApi.id());
                return TopFailedApis.TopFailedApi.builder()
                    .id(topFailedApi.id())
                    .name(api.getName())
                    .definitionVersion(Optional.ofNullable(api.getDefinitionVersion()).orElse(DefinitionVersion.V2))
                    .failedCalls(topFailedApi.failedCalls())
                    .failedCallsRatio(topFailedApi.failedCallsRatio())
                    .build();
            })
            .toList();
        return TopFailedApis.builder().data(failedApis).build();
    }

    @Builder
    public record Input(ExecutionContext executionContext, AnalyticsQueryParameters parameters, String userId) {}

    public record Output(TopFailedApis topFailedApis) {}
}

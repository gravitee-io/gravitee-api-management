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
import io.gravitee.apim.core.application.query_service.ApplicationQueryService;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.gravitee.rest.api.model.analytics.TopHitsApps;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

@CustomLog
@RequiredArgsConstructor
@UseCase
public class SearchEnvironmentTopAppsByRequestCountUseCase {

    private final AnalyticsQueryService analyticsQueryService;
    private final EnvironmentAnalyticsApisDomainService environmentAnalyticsApisDomainService;
    private final ApplicationQueryService applicationQueryService;

    public Output execute(Input input) {
        var envId = input.executionContext().getEnvironmentId();
        var apisIds = environmentAnalyticsApisDomainService
            .findReadableApis(input.executionContext(), input.userId())
            .stream()
            .map(Api::getId)
            .toList();

        log.debug("Searching top Apps hits with {} APIs for env: {}", apisIds.size(), envId);
        if (apisIds.isEmpty()) {
            return noTopHitsApps();
        }

        return analyticsQueryService
            .searchTopHitsApps(input.executionContext(), input.parameters().withApiIds(apisIds))
            .map(topHitsApps -> sortByCountAndUpdateTopHitsWithAppNames(envId, topHitsApps))
            .map(Output::new)
            .orElseGet(SearchEnvironmentTopAppsByRequestCountUseCase::noTopHitsApps);
    }

    private static Output noTopHitsApps() {
        return new Output(TopHitsApps.builder().data(List.of()).build());
    }

    private TopHitsApps sortByCountAndUpdateTopHitsWithAppNames(String envId, TopHitsApps topHitsApps) {
        var applications = applicationQueryService
            .findByEnvironment(envId)
            .stream()
            .collect(Collectors.toMap(BaseApplicationEntity::getId, Function.identity()));

        var data = topHitsApps
            .getData()
            .stream()
            .sorted(Comparator.comparingLong(TopHitsApps.TopHitApp::count).reversed())
            .filter(topHitApp -> applications.containsKey(topHitApp.id()))
            .map(topHitApp ->
                TopHitsApps.TopHitApp.builder()
                    .id(topHitApp.id())
                    .name(applications.get(topHitApp.id()).getName())
                    .count(topHitApp.count())
                    .build()
            )
            .toList();
        return TopHitsApps.builder().data(data).build();
    }

    @Builder
    public record Input(ExecutionContext executionContext, AnalyticsQueryParameters parameters, String userId) {}

    public record Output(TopHitsApps topHitsApps) {}
}

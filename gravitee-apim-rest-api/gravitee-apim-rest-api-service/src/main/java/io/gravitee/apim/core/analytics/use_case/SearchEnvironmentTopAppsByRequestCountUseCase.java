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
import io.gravitee.apim.core.analytics.model.AnalyticsQueryParameters;
import io.gravitee.apim.core.analytics.query_service.AnalyticsQueryService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.ApiFieldFilter;
import io.gravitee.apim.core.api.model.ApiSearchCriteria;
import io.gravitee.apim.core.api.query_service.ApiQueryService;
import io.gravitee.apim.core.application.query_service.ApplicationQueryService;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.gravitee.rest.api.model.analytics.TopHitsApps;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@UseCase
public class SearchEnvironmentTopAppsByRequestCountUseCase {

    private final AnalyticsQueryService analyticsQueryService;
    private final ApiQueryService apiQueryService;
    private final ApplicationQueryService applicationQueryService;

    public Output execute(Input input) {
        var envId = input.executionContext().getEnvironmentId();
        var v4Apis = getAllV4ApisForEnv(envId);
        var v4ApiIds = v4Apis.keySet().stream().toList();

        log.info("Searching top Apps hits with v4 APIs for env: {}", envId);

        return analyticsQueryService
            .searchTopHitsApps(input.executionContext(), input.parameters().withApiIds(v4ApiIds))
            .map(topHitsApps -> sortByCountAndUpdateTopHitsWithAppNames(envId, topHitsApps))
            .map(Output::new)
            .orElse(new Output(TopHitsApps.builder().data(List.of()).build()));
    }

    private Map<String, Api> getAllV4ApisForEnv(String envId) {
        return apiQueryService
            .search(
                ApiSearchCriteria.builder().environmentId(envId).definitionVersion(List.of(DefinitionVersion.V4)).build(),
                null,
                ApiFieldFilter.builder().pictureExcluded(true).definitionExcluded(true).build()
            )
            .collect(Collectors.toMap(Api::getId, value -> value));
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
                TopHitsApps.TopHitApp
                    .builder()
                    .id(topHitApp.id())
                    .name(applications.get(topHitApp.id()).getName())
                    .count(topHitApp.count())
                    .build()
            )
            .toList();
        return TopHitsApps.builder().data(data).build();
    }

    @Builder
    public record Input(ExecutionContext executionContext, AnalyticsQueryParameters parameters) {}

    public record Output(TopHitsApps topHitsApps) {}
}

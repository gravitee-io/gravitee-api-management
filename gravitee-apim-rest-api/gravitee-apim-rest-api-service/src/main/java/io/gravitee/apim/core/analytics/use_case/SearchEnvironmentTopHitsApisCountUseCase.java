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
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.rest.api.model.v4.analytics.TopHitsApis;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UseCase
@AllArgsConstructor
public class SearchEnvironmentTopHitsApisCountUseCase {

    AnalyticsQueryService analyticsQueryService;
    ApiQueryService apiQueryService;

    public Output execute(Input input) {
        var envId = input.executionContext().getEnvironmentId();
        var v4Apis = getAllV4ApisForEnv(envId);
        var v4ApiIds = v4Apis.keySet().stream().toList();

        log.info("Searching top API hits, found: {} v4 APIs for env: {}", v4ApiIds.size(), envId);

        return analyticsQueryService
            .searchTopHitsApis(input.executionContext(), input.parameters().withApiIds(v4ApiIds))
            .map(topHitsApis -> sortByCountAndUpdateTopHitsWithApiNames(v4Apis, topHitsApis))
            .map(Output::new)
            .orElse(new Output());
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

    private TopHitsApis sortByCountAndUpdateTopHitsWithApiNames(Map<String, Api> apis, TopHitsApis topHitsApis) {
        var data = topHitsApis
            .getData()
            .stream()
            .sorted(Comparator.comparingLong(TopHitsApis.TopHitApi::count).reversed())
            .map(topHitApi ->
                TopHitsApis.TopHitApi.builder().id(topHitApi.id()).name(apis.get(topHitApi.id()).getName()).count(topHitApi.count()).build()
            )
            .toList();
        return TopHitsApis.builder().data(data).build();
    }

    @Builder
    public record Input(ExecutionContext executionContext, AnalyticsQueryParameters parameters) {}

    public record Output(Optional<TopHitsApis> topHitsApis) {
        Output(TopHitsApis topHitsApis) {
            this(Optional.of(topHitsApis));
        }

        Output() {
            this(TopHitsApis.builder().build());
        }
    }
}

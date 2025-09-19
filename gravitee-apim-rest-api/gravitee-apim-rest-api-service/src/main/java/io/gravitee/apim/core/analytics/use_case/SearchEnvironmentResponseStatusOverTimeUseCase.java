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

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.analytics.model.ResponseStatusOvertime;
import io.gravitee.apim.core.analytics.query_service.AnalyticsQueryService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.ApiFieldFilter;
import io.gravitee.apim.core.api.model.ApiSearchCriteria;
import io.gravitee.apim.core.api.query_service.ApiQueryService;
import io.gravitee.apim.core.utils.DurationUtils;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@UseCase
public class SearchEnvironmentResponseStatusOverTimeUseCase {

    private final AnalyticsQueryService analyticsQueryService;
    private final ApiQueryService apiQueryService;

    public Output execute(ExecutionContext executionContext, Input input) {
        Duration interval = DurationUtils.buildIntervalFromTimePeriod(input.from(), input.to());
        var apiIdsByDefinitionVersion = apisIdsForEnv(input.environmentId);
        var apiIds = apiIdsByDefinitionVersion.values().stream().flatMap(Collection::stream).toList();

        var result = analyticsQueryService.searchResponseStatusOvertime(
            executionContext,
            new AnalyticsQueryService.ResponseStatusOverTimeQuery(
                apiIds,
                input.from(),
                input.to(),
                interval,
                apiIdsByDefinitionVersion.keySet()
            )
        );

        return new Output(result);
    }

    private Map<DefinitionVersion, List<String>> apisIdsForEnv(String envId) {
        return apiQueryService
            .search(
                ApiSearchCriteria.builder()
                    .environmentId(envId)
                    .definitionVersion(EnumSet.of(DefinitionVersion.V4, DefinitionVersion.V2))
                    .build(),
                null,
                ApiFieldFilter.builder().pictureExcluded(true).definitionExcluded(true).build()
            )
            .collect(groupingBy(SearchEnvironmentResponseStatusOverTimeUseCase::getDefinitionVersion, mapping(Api::getId, toList())));
    }

    private static DefinitionVersion getDefinitionVersion(Api api) {
        return api.getDefinitionVersion() != null ? api.getDefinitionVersion() : DefinitionVersion.V2;
    }

    public record Input(String environmentId, Instant from, Instant to) {}

    public record Output(ResponseStatusOvertime responseStatusOvertime) {}
}

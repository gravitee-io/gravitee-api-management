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
import io.gravitee.apim.core.analytics.model.AnalyticsQueryParameters;
import io.gravitee.apim.core.analytics.query_service.AnalyticsQueryService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.ApiFieldFilter;
import io.gravitee.apim.core.api.model.ApiSearchCriteria;
import io.gravitee.apim.core.api.query_service.ApiQueryService;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.rest.api.model.v4.analytics.ResponseStatusRanges;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UseCase
@AllArgsConstructor
public class SearchEnvironmentResponseStatusRangesUseCase {

    AnalyticsQueryService analyticsQueryService;
    ApiQueryService apiQueryService;

    public Output execute(Input input) {
        var envId = input.executionContext().getEnvironmentId();
        var apiIds = apisIdsForEnv(envId);
        var apis = apiIds.values().stream().flatMap(List::stream).toList();

        log.debug("Searching environment API response status ranges, found: {} v4 APIs for env: {}", apis.size(), envId);
        return analyticsQueryService
            .searchResponseStatusRanges(
                input.executionContext(),
                input.parameters().withApiIds(apis).withDefinitionVersions(apiIds.keySet())
            )
            .map(SearchEnvironmentResponseStatusRangesUseCase.Output::new)
            .orElse(new SearchEnvironmentResponseStatusRangesUseCase.Output());
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
            .collect(groupingBy(SearchEnvironmentResponseStatusRangesUseCase::getDefinitionVersion, mapping(Api::getId, toList())));
    }

    private static DefinitionVersion getDefinitionVersion(Api api) {
        return api.getDefinitionVersion() != null ? api.getDefinitionVersion() : DefinitionVersion.V2;
    }

    @Builder
    public record Input(ExecutionContext executionContext, AnalyticsQueryParameters parameters) {}

    public record Output(Optional<ResponseStatusRanges> responseStatusRanges) {
        Output(ResponseStatusRanges responseStatusRanges) {
            this(Optional.of(responseStatusRanges));
        }

        Output() {
            this(new ResponseStatusRanges());
        }
    }
}

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
import io.gravitee.rest.api.model.v4.analytics.RequestResponseTime;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UseCase
@AllArgsConstructor
public class SearchEnvironmentRequestResponseTimeUseCase {

    private final ApiQueryService apiQueryService;
    private final AnalyticsQueryService analyticsQueryService;

    public Output execute(Input input) {
        var envId = input.executionContext().getEnvironmentId();
        var v4Apis = getAllV4ApisIdsForEnv(envId);
        var apisId = v4Apis.values().stream().flatMap(Collection::stream).toList();

        log.info("Searching Request Response Time, found: {} v4 APIs for env: {}", v4Apis.size(), envId);

        var requestResponseTime = analyticsQueryService.searchRequestResponseTime(
            input.executionContext(),
            input.parameters().withApiIds(apisId).withDefinitionVersions(v4Apis.keySet())
        );

        return new Output(requestResponseTime);
    }

    private Map<DefinitionVersion, List<String>> getAllV4ApisIdsForEnv(String envId) {
        return apiQueryService
            .search(
                ApiSearchCriteria
                    .builder()
                    .environmentId(envId)
                    .definitionVersion(List.of(DefinitionVersion.V4, DefinitionVersion.V2))
                    .build(),
                null,
                ApiFieldFilter.builder().pictureExcluded(true).definitionExcluded(true).build()
            )
            .collect(groupingBy(this::getDefinitionVersion, mapping(Api::getId, toList())));
    }

    private DefinitionVersion getDefinitionVersion(Api api) {
        return api.getDefinitionVersion() != null ? api.getDefinitionVersion() : DefinitionVersion.V2;
    }

    @Builder
    public record Input(ExecutionContext executionContext, AnalyticsQueryParameters parameters) {}

    public record Output(Optional<RequestResponseTime> requestResponseTime) {
        Output(RequestResponseTime requestResponseTime) {
            this(Optional.of(requestResponseTime));
        }
    }
}

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
import io.gravitee.apim.core.analytics.model.StatusRangesQueryParameters;
import io.gravitee.apim.core.analytics.query_service.AnalyticsQueryService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.ApiFieldFilter;
import io.gravitee.apim.core.api.model.ApiSearchCriteria;
import io.gravitee.apim.core.api.query_service.ApiQueryService;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.rest.api.model.v4.analytics.ResponseStatusRanges;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.List;
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
        var v4ApiIds = apiQueryService
            .search(
                ApiSearchCriteria.builder().environmentId(envId).definitionVersion(List.of(DefinitionVersion.V4)).build(),
                null,
                ApiFieldFilter.builder().pictureExcluded(true).definitionExcluded(true).build()
            )
            .map(Api::getId)
            .toList();

        log.info("Searching environment API response status ranges, found: {} v4 APIs for env: {}", v4ApiIds.size(), envId);
        return analyticsQueryService
            .searchResponseStatusRanges(input.executionContext(), input.parameters().withApiIds(v4ApiIds))
            .map(SearchEnvironmentResponseStatusRangesUseCase.Output::new)
            .orElse(new SearchEnvironmentResponseStatusRangesUseCase.Output());
    }

    @Builder
    public record Input(ExecutionContext executionContext, StatusRangesQueryParameters parameters) {}

    public record Output(Optional<ResponseStatusRanges> responseStatusRanges) {
        Output(ResponseStatusRanges responseStatusRanges) {
            this(Optional.of(responseStatusRanges));
        }

        Output() {
            this(new ResponseStatusRanges());
        }
    }
}

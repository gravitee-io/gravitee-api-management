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
import io.gravitee.rest.api.model.v4.analytics.ResponseStatusRanges;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.CustomLog;

@CustomLog
@UseCase
@AllArgsConstructor
public class SearchEnvironmentResponseStatusRangesUseCase {

    AnalyticsQueryService analyticsQueryService;
    EnvironmentAnalyticsApisDomainService environmentAnalyticsApisDomainService;

    public Output execute(Input input) {
        var envId = input.executionContext().getEnvironmentId();
        var apiIds = environmentAnalyticsApisDomainService.findReadableApiIdsByDefinitionVersion(input.executionContext(), input.userId());
        var apis = apiIds.values().stream().flatMap(List::stream).toList();

        log.debug("Searching environment API response status ranges, found: {} v4 APIs for env: {}", apis.size(), envId);
        if (apis.isEmpty()) {
            return new SearchEnvironmentResponseStatusRangesUseCase.Output();
        }
        return analyticsQueryService
            .searchResponseStatusRanges(
                input.executionContext(),
                input.parameters().withApiIds(apis).withDefinitionVersions(apiIds.keySet())
            )
            .map(SearchEnvironmentResponseStatusRangesUseCase.Output::new)
            .orElse(new SearchEnvironmentResponseStatusRangesUseCase.Output());
    }

    @Builder
    public record Input(ExecutionContext executionContext, AnalyticsQueryParameters parameters, String userId) {}

    public record Output(Optional<ResponseStatusRanges> responseStatusRanges) {
        Output(ResponseStatusRanges responseStatusRanges) {
            this(Optional.of(responseStatusRanges));
        }

        Output() {
            this(new ResponseStatusRanges());
        }
    }
}

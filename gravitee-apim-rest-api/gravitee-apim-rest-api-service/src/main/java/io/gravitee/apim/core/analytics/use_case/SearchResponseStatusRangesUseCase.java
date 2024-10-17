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
import io.gravitee.apim.core.analytics.query_service.AnalyticsQueryService;
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.rest.api.model.v4.analytics.ResponseStatusRanges;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static io.gravitee.apim.core.analytics.utils.AnalyticsUtils.validateHttpV4Api;

@Slf4j
@RequiredArgsConstructor
@UseCase
public class SearchResponseStatusRangesUseCase {

    private final AnalyticsQueryService analyticsQueryService;
    private final ApiCrudService apiCrudService;

    public Output execute(ExecutionContext executionContext, Input input) {
        validateApiRequirements(input);

        return analyticsQueryService.searchResponseStatusRanges(executionContext, input.apiId()).map(Output::new).orElse(new Output());
    }

    private void validateApiRequirements(Input input) {
        final Api api = apiCrudService.get(input.apiId);
        validateHttpV4Api(api, input.environmentId);
    }

    public record Input(String apiId, String environmentId) {}

    public record Output(Optional<ResponseStatusRanges> responseStatusRanges) {
        Output(ResponseStatusRanges responseStatusRanges) {
            this(Optional.of(responseStatusRanges));
        }

        Output() {
            this(new ResponseStatusRanges());
        }
    }
}

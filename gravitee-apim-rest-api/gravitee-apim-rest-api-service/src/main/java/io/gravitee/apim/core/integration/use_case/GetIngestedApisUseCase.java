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
package io.gravitee.apim.core.integration.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.query_service.ApiQueryService;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.common.PageableImpl;
import java.util.Optional;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetIngestedApisUseCase {

    private final ApiQueryService apiQueryService;

    public Output execute(Input input) {
        var pageable = input.pageable.orElse(new PageableImpl(1, 10));

        Page<Api> ingestedApis = apiQueryService.findByIntegrationId(input.integrationId, pageable);

        return new Output(ingestedApis);
    }

    @Builder
    public record Input(String integrationId, Optional<Pageable> pageable) {
        public Input(String integrationId) {
            this(integrationId, Optional.empty());
        }

        public Input(String integrationId, Pageable pageable) {
            this(integrationId, Optional.of(pageable));
        }
    }

    public record Output(Page<Api> ingestedApis) {}
}

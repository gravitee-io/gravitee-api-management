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
import io.gravitee.apim.core.integration.model.Integration;
import io.gravitee.apim.core.integration.query_service.IntegrationQueryService;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.common.PageableImpl;
import java.util.Optional;
import lombok.Builder;

/**
 * @author Remi Baptiste (remi.baptiste at graviteesource.com)
 * @author GraviteeSource Team
 */
@UseCase
public class GetIntegrationsUseCase {

    private final IntegrationQueryService integrationQueryService;

    public GetIntegrationsUseCase(IntegrationQueryService integrationQueryService) {
        this.integrationQueryService = integrationQueryService;
    }

    public GetIntegrationsUseCase.Output execute(GetIntegrationsUseCase.Input input) {
        var environmentId = input.environmentId();
        var pageable = input.pageable.orElse(new PageableImpl(1, 10));

        Page<Integration> integrations = integrationQueryService.findByEnvironment(environmentId, pageable);

        return new GetIntegrationsUseCase.Output(integrations);
    }

    @Builder
    public record Input(String environmentId, Optional<Pageable> pageable) {
        public Input(String environmentId) {
            this(environmentId, Optional.empty());
        }

        public Input(String environmentId, Pageable pageable) {
            this(environmentId, Optional.of(pageable));
        }
    }

    public record Output(Page<Integration> integrations) {}
}

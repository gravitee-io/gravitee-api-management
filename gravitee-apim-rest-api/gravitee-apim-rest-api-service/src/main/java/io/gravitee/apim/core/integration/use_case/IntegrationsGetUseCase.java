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

import io.gravitee.apim.core.integration.crud_service.IntegrationCrudService;
import io.gravitee.apim.core.integration.model.IntegrationEntity;
import java.util.Set;
import lombok.Builder;

/**
 * @author Remi Baptiste (remi.baptiste at graviteesource.com)
 * @author GraviteeSource Team
 */
public class IntegrationsGetUseCase {

    private final IntegrationCrudService integrationCrudService;

    public IntegrationsGetUseCase(IntegrationCrudService integrationCrudService) {
        this.integrationCrudService = integrationCrudService;
    }

    public IntegrationsGetUseCase.Output execute(IntegrationsGetUseCase.Input input) {
        var environmentId = input.environmentId();

        Set<IntegrationEntity> integrations = integrationCrudService.findByEnvironment(environmentId);

        return new IntegrationsGetUseCase.Output(integrations);
    }

    @Builder
    public record Input(String environmentId) {}

    public record Output(Set<IntegrationEntity> integrations) {}
}

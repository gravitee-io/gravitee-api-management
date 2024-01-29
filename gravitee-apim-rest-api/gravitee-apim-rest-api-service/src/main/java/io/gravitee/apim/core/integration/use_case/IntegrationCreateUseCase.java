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
import io.gravitee.apim.core.integration.domain_service.IntegrationDomainService;
import io.gravitee.apim.core.integration.model.IntegrationEntity;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

/**
 * @author Remi Baptiste (remi.baptiste at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
public class IntegrationCreateUseCase {

    private final IntegrationCrudService integrationCrudService;
    private final IntegrationDomainService integrationDomainService;

    public IntegrationCreateUseCase.Output execute(final IntegrationCreateUseCase.Input input) {
        var integrationToCreate = input.integration();
        IntegrationEntity integrationCreated = integrationCrudService.create(integrationToCreate);

        integrationDomainService.startIntegration(integrationCreated);

        return new Output(integrationCreated);
    }

    @Builder
    public record Input(IntegrationEntity integration) {}

    public record Output(IntegrationEntity integration) {}
}

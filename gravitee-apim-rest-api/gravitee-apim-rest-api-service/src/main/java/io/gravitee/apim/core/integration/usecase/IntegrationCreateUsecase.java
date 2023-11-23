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
package io.gravitee.apim.core.integration.usecase;

import io.gravitee.apim.core.integration.crud_service.IntegrationCrudService;
import io.gravitee.apim.core.integration.domain_service.IntegrationDomainService;
import io.gravitee.apim.core.integration.model.Integration;
import lombok.Builder;

/**
 * @author Remi Baptiste (remi.baptiste at graviteesource.com)
 * @author GraviteeSource Team
 */
public class IntegrationCreateUsecase {

    private IntegrationCrudService integrationCrudService;

    private IntegrationDomainService integrationDomainService;

    public IntegrationCreateUsecase(IntegrationCrudService integrationCrudService, IntegrationDomainService integrationDomainService) {
        this.integrationCrudService = integrationCrudService;
        this.integrationDomainService = integrationDomainService;
    }

    public Output execute(Input input) {
        var integrationToCreate = input.integration();

        Integration integrationCreated = integrationCrudService.createIntegration(integrationToCreate);

        integrationDomainService.startIntegration(integrationCreated);

        return new Output(integrationCreated);
    }

    @Builder
    public record Input(Integration integration) {}

    public record Output(Integration createdIntegration) {}
}

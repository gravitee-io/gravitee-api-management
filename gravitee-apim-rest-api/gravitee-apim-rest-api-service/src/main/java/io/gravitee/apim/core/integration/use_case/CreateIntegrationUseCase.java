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

import static io.gravitee.apim.core.exception.NotAllowedDomainException.noLicenseForFederation;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.exception.NotAllowedDomainException;
import io.gravitee.apim.core.integration.crud_service.IntegrationCrudService;
import io.gravitee.apim.core.integration.model.Integration;
import io.gravitee.apim.core.license.domain_service.LicenseDomainService;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.service.common.UuidString;
import lombok.Builder;

/**
 * @author Remi Baptiste (remi.baptiste at graviteesource.com)
 * @author GraviteeSource Team
 */
@UseCase
public class CreateIntegrationUseCase {

    private final IntegrationCrudService integrationCrudService;
    private final LicenseDomainService licenseDomainService;

    public CreateIntegrationUseCase(IntegrationCrudService integrationCrudService, LicenseDomainService licenseDomainService) {
        this.integrationCrudService = integrationCrudService;
        this.licenseDomainService = licenseDomainService;
    }

    public Output execute(Input input) {
        if (!licenseDomainService.isFederationFeatureAllowed(input.organizationId())) {
            throw noLicenseForFederation();
        }

        var now = TimeProvider.now();

        var integrationToCreate = Integration
            .builder()
            .id(UuidString.generateRandom())
            .name(input.integration.getName())
            .description(input.integration.getDescription())
            .provider(input.integration.getProvider())
            .environmentId(input.integration.getEnvironmentId())
            .agentStatus(Integration.AgentStatus.DISCONNECTED)
            .createdAt(now)
            .updatedAt(now)
            .build();

        Integration integrationCreated = integrationCrudService.create(integrationToCreate);

        return new Output(integrationCreated);
    }

    @Builder
    public record Input(Integration integration, String organizationId) {}

    public record Output(Integration createdIntegration) {}
}

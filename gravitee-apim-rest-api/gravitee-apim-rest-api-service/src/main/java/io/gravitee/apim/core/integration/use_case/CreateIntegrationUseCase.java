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
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.integration.crud_service.IntegrationCrudService;
import io.gravitee.apim.core.integration.model.Integration;
import io.gravitee.apim.core.license.domain_service.LicenseDomainService;
import io.gravitee.apim.core.membership.domain_service.IntegrationPrimaryOwnerDomainService;
import io.gravitee.apim.core.membership.domain_service.IntegrationPrimaryOwnerFactory;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.service.common.UuidString;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

/**
 * @author Remi Baptiste (remi.baptiste at graviteesource.com)
 * @author GraviteeSource Team
 */
@UseCase
@RequiredArgsConstructor
public class CreateIntegrationUseCase {

    private final IntegrationCrudService integrationCrudService;
    private final LicenseDomainService licenseDomainService;
    private final IntegrationPrimaryOwnerFactory integrationPrimaryOwnerFactory;
    private final IntegrationPrimaryOwnerDomainService integrationPrimaryOwnerDomainService;

    public Output execute(Input input) {
        if (!licenseDomainService.isFederationFeatureAllowed(input.auditInfo.organizationId())) {
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
            .createdAt(now)
            .updatedAt(now)
            .build();

        Integration integrationCreated = integrationCrudService.create(integrationToCreate);

        var primaryOwner = integrationPrimaryOwnerFactory.createForNewIntegration(
            input.auditInfo.organizationId(),
            input.auditInfo.environmentId(),
            input.auditInfo.actor().userId()
        );

        integrationPrimaryOwnerDomainService.createIntegrationPrimaryOwnerMembership(
            integrationCreated.getId(),
            primaryOwner,
            input.auditInfo
        );

        return new Output(integrationCreated);
    }

    @Builder
    public record Input(Integration integration, AuditInfo auditInfo) {}

    public record Output(Integration createdIntegration) {}
}

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
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.group.domain_service.ValidateGroupsDomainService;
import io.gravitee.apim.core.group.model.Group;
import io.gravitee.apim.core.integration.crud_service.IntegrationCrudService;
import io.gravitee.apim.core.integration.exception.IntegrationGroupValidationException;
import io.gravitee.apim.core.integration.exception.IntegrationNotFoundException;
import io.gravitee.apim.core.integration.model.Integration;
import io.gravitee.apim.core.license.domain_service.LicenseDomainService;
import io.gravitee.common.utils.TimeProvider;
import java.util.Set;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@UseCase
public class UpdateIntegrationUseCase {

    private final IntegrationCrudService integrationCrudService;
    private final LicenseDomainService licenseDomainService;
    private final ValidateGroupsDomainService validateGroupsDomainService;

    public Output execute(Input input) {
        if (!licenseDomainService.isFederationFeatureAllowed(input.auditInfo.organizationId())) {
            throw noLicenseForFederation();
        }

        var now = TimeProvider.now();
        var integrationId = input.integration.getId();

        var validatedGroups = validateGroups(input);

        var integration = integrationCrudService.findById(integrationId).orElseThrow(() -> new IntegrationNotFoundException(integrationId));
        var integrationToUpdate = integration
            .toBuilder()
            .name(input.integration.getName())
            .description(input.integration.getDescription())
            .groups(validatedGroups)
            .updatedAt(now)
            .build();

        return new Output(integrationCrudService.update(integrationToUpdate));
    }

    private Set<String> validateGroups(Input input) {
        var validationResult = validateGroupsDomainService.validateAndSanitize(
            new ValidateGroupsDomainService.Input(input.auditInfo.environmentId(), input.integration.getGroups())
        );

        if (validationResult.errors().isPresent() && !validationResult.errors().get().isEmpty()) {
            validationResult.errors().get().forEach(error -> log.error(error.getMessage(), error));
            throw new IntegrationGroupValidationException(input.integration.getId());
        }

        return validationResult.value().isPresent() ? validationResult.value().get().groups() : Set.of();
    }

    @Builder
    public record Input(Integration integration, AuditInfo auditInfo) {}

    public record Output(Integration integration) {}
}

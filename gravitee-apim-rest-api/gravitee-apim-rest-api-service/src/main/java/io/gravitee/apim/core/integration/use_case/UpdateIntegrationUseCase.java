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
import io.gravitee.apim.core.group.domain_service.ValidateGroupsDomainService;
import io.gravitee.apim.core.group.model.Group;
import io.gravitee.apim.core.integration.crud_service.IntegrationCrudService;
import io.gravitee.apim.core.integration.exception.IntegrationGroupValidationException;
import io.gravitee.apim.core.integration.exception.IntegrationNotFoundException;
import io.gravitee.apim.core.integration.model.Integration;
import io.gravitee.apim.core.license.domain_service.LicenseDomainService;
import java.util.Collection;
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

        var integrationId = input.integrationId();

        var validatedGroups = validateGroups(input);

        var integration = integrationCrudService.findById(integrationId).orElseThrow(() -> new IntegrationNotFoundException(integrationId));
        var integrationToUpdate = switch (integration) {
            case Integration.ApiIntegration apiIntegration -> apiIntegration.update(
                input.updateFields().name(),
                input.updateFields().description(),
                validatedGroups
            );
            case Integration.A2aIntegration a2aIntegration -> a2aIntegration.update(
                input.updateFields().name(),
                input.updateFields().description(),
                validatedGroups,
                input.updateFields().wellKnownUrls()
            );
        };

        return new Output(integrationCrudService.update(integrationToUpdate));
    }

    private Set<String> validateGroups(Input input) {
        var validationResult = validateGroupsDomainService.validateAndSanitize(
            new ValidateGroupsDomainService.Input(
                input.auditInfo.environmentId(),
                input.updateFields().groups(),
                null,
                Group.GroupEvent.API_CREATE,
                false
            )
        );

        if (validationResult.errors().isPresent() && !validationResult.errors().get().isEmpty()) {
            validationResult
                .errors()
                .get()
                .forEach(error -> log.error(error.getMessage(), error));
            throw new IntegrationGroupValidationException(input.integrationId());
        }

        return validationResult.value().isPresent() ? validationResult.value().get().groups() : Set.of();
    }

    @Builder
    public record Input(String integrationId, UpdateFields updateFields, AuditInfo auditInfo) {
        public record UpdateFields(
            String name,
            String description,
            Set<String> groups,
            Collection<Integration.A2aIntegration.WellKnownUrl> wellKnownUrls
        ) {}
    }

    public record Output(Integration integration) {}
}

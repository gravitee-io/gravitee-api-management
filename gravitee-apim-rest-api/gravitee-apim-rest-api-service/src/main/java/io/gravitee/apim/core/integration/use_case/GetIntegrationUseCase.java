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
import io.gravitee.apim.core.async_job.query_service.AsyncJobQueryService;
import io.gravitee.apim.core.integration.crud_service.IntegrationCrudService;
import io.gravitee.apim.core.integration.exception.IntegrationNotFoundException;
import io.gravitee.apim.core.integration.model.Integration;
import io.gravitee.apim.core.integration.model.IntegrationView;
import io.gravitee.apim.core.integration.service_provider.IntegrationAgent;
import io.gravitee.apim.core.license.domain_service.LicenseDomainService;
import io.gravitee.apim.core.membership.domain_service.IntegrationPrimaryOwnerDomainService;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

/**
 * @author Remi Baptiste (remi.baptiste at graviteesource.com)
 * @author GraviteeSource Team
 */
@UseCase
@RequiredArgsConstructor
public class GetIntegrationUseCase {

    private final IntegrationCrudService integrationCrudService;
    private final AsyncJobQueryService asyncJobQueryService;
    private final LicenseDomainService licenseDomainService;
    private final IntegrationAgent integrationAgent;
    private final IntegrationPrimaryOwnerDomainService integrationPrimaryOwnerDomainService;

    public Output execute(Input input) {
        var integrationId = input.integrationId();

        if (!licenseDomainService.isFederationFeatureAllowed(input.organizationId())) {
            throw noLicenseForFederation();
        }

        var integration = integrationCrudService
            .findByIdAndEnvironment(integrationId, input.environmentId())
            .orElseThrow(() -> new IntegrationNotFoundException(integrationId));
        var primaryOwner = integrationPrimaryOwnerDomainService
            .getIntegrationPrimaryOwner(input.organizationId(), integration.id())
            .map(po -> new IntegrationView.PrimaryOwner(po.id(), po.email(), po.displayName()))
            .onErrorComplete()
            .blockingGet();

        return switch (integration) {
            case Integration.ApiIntegration apiIntegration -> {
                var agentStatus = integrationAgent
                    .getAgentStatusFor(integrationId)
                    .map(status -> IntegrationView.AgentStatus.valueOf(status.name()))
                    .blockingGet();

                var pendingJob = asyncJobQueryService.findPendingJobFor(integrationId);

                yield new Output(new IntegrationView(apiIntegration, agentStatus, pendingJob.orElse(null), primaryOwner));
            }
            case Integration.A2aIntegration a2aIntegration -> new Output(new IntegrationView(a2aIntegration, primaryOwner));
        };
    }

    @Builder
    public record Input(String integrationId, String organizationId, String environmentId) {}

    public record Output(IntegrationView integration) {}
}

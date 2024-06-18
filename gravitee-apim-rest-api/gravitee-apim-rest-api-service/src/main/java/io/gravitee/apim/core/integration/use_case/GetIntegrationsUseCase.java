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
import io.gravitee.apim.core.integration.model.Integration;
import io.gravitee.apim.core.integration.model.IntegrationView;
import io.gravitee.apim.core.integration.query_service.IntegrationQueryService;
import io.gravitee.apim.core.integration.service_provider.IntegrationAgent;
import io.gravitee.apim.core.license.domain_service.LicenseDomainService;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.reactivex.rxjava3.core.Flowable;
import java.util.Optional;
import lombok.Builder;

/**
 * @author Remi Baptiste (remi.baptiste at graviteesource.com)
 * @author GraviteeSource Team
 */
@UseCase
public class GetIntegrationsUseCase {

    private final IntegrationQueryService integrationQueryService;
    private final LicenseDomainService licenseDomainService;
    private final IntegrationAgent integrationAgent;

    public GetIntegrationsUseCase(
        IntegrationQueryService integrationQueryService,
        LicenseDomainService licenseDomainService,
        IntegrationAgent integrationAgent
    ) {
        this.integrationQueryService = integrationQueryService;
        this.licenseDomainService = licenseDomainService;
        this.integrationAgent = integrationAgent;
    }

    public GetIntegrationsUseCase.Output execute(GetIntegrationsUseCase.Input input) {
        var environmentId = input.environmentId();
        var pageable = input.pageable.orElse(new PageableImpl(1, 10));

        if (!licenseDomainService.isFederationFeatureAllowed(input.organizationId())) {
            throw noLicenseForFederation();
        }

        Page<Integration> page = integrationQueryService.findByEnvironment(environmentId, pageable);

        var pageContent = Flowable
            .fromIterable(page.getContent())
            .flatMap(integration ->
                integrationAgent
                    .getAgentStatusFor(integration.getId())
                    .map(status -> new IntegrationView(integration, IntegrationView.AgentStatus.valueOf(status.name())))
                    .toFlowable()
            )
            .toList()
            .blockingGet();

        return new GetIntegrationsUseCase.Output(
            new Page<>(pageContent, page.getPageNumber(), (int) page.getPageElements(), page.getTotalElements())
        );
    }

    @Builder
    public record Input(String organizationId, String environmentId, Optional<Pageable> pageable) {
        public Input(String organizationId, String environmentId) {
            this(organizationId, environmentId, Optional.empty());
        }

        public Input(String organizationId, String environmentId, Pageable pageable) {
            this(organizationId, environmentId, Optional.of(pageable));
        }
    }

    public record Output(Page<IntegrationView> integrations) {}
}

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
import io.gravitee.apim.core.integration.model.Integration;
import io.gravitee.apim.core.integration.model.IntegrationView;
import io.gravitee.apim.core.integration.query_service.IntegrationQueryService;
import io.gravitee.apim.core.integration.service_provider.IntegrationAgent;
import io.gravitee.apim.core.license.domain_service.LicenseDomainService;
import io.gravitee.apim.core.membership.domain_service.IntegrationPrimaryOwnerDomainService;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import java.util.Collection;
import java.util.Optional;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

/**
 * @author Remi Baptiste (remi.baptiste at graviteesource.com)
 * @author GraviteeSource Team
 */
@UseCase
@RequiredArgsConstructor
public class GetIntegrationsUseCase {

    private final IntegrationQueryService integrationQueryService;
    private final LicenseDomainService licenseDomainService;
    private final IntegrationAgent integrationAgent;
    private final IntegrationPrimaryOwnerDomainService integrationPrimaryOwnerDomainService;
    private final AsyncJobQueryService asyncJobQueryService;

    public GetIntegrationsUseCase.Output execute(GetIntegrationsUseCase.Input input) {
        var pageable = input.pageable.orElse(new PageableImpl(1, 10));

        if (!licenseDomainService.isFederationFeatureAllowed(input.context().getOrganizationId())) {
            throw noLicenseForFederation();
        }

        Page<Integration> page = integrationQueryService.findByEnvironmentAndContext(
            input.context().getEnvironmentId(),
            input.userId(),
            input.groups(),
            input.isAdmin,
            pageable
        );

        var pageContent = Flowable
            .fromIterable(page.getContent())
            .flatMapSingle(integration ->
                Single.zip(
                    integrationAgent
                        .getAgentStatusFor(integration.getId())
                        .map(status -> IntegrationView.AgentStatus.valueOf(status.name())),
                    Maybe
                        .fromOptional(asyncJobQueryService.findPendingJobFor(integration.getId()))
                        .map(Optional::of)
                        .defaultIfEmpty(Optional.empty()),
                    integrationPrimaryOwnerDomainService
                        .getIntegrationPrimaryOwner(input.context().getOrganizationId(), integration.getId())
                        .map(po -> Optional.of(new IntegrationView.PrimaryOwner(po.id(), po.email(), po.displayName())))
                        .defaultIfEmpty(Optional.empty()),
                    (agentStatus, pendingJob, primaryOwner) ->
                        new IntegrationView(integration, agentStatus, pendingJob.orElse(null), primaryOwner.orElse(null))
                )
            )
            .toList()
            .blockingGet();

        return new GetIntegrationsUseCase.Output(
            new Page<>(pageContent, page.getPageNumber(), (int) page.getPageElements(), page.getTotalElements())
        );
    }

    @Builder
    public record Input(ExecutionContext context, String userId, Collection<String> groups, boolean isAdmin, Optional<Pageable> pageable) {
        public Input(ExecutionContext context, String userId, Collection<String> groups, boolean isAdmin) {
            this(context, userId, groups, isAdmin, Optional.empty());
        }

        public Input(ExecutionContext context, String userId, Collection<String> groups, boolean isAdmin, Pageable pageable) {
            this(context, userId, groups, isAdmin, Optional.of(pageable));
        }
    }

    public record Output(Page<IntegrationView> integrations) {}
}

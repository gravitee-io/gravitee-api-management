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
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.ApiFieldFilter;
import io.gravitee.apim.core.api.model.ApiSearchCriteria;
import io.gravitee.apim.core.api.model.factory.ApiModelFactory;
import io.gravitee.apim.core.api.query_service.ApiQueryService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.integration.crud_service.IntegrationCrudService;
import io.gravitee.apim.core.integration.exception.IntegrationNotFoundException;
import io.gravitee.apim.core.integration.model.IntegrationApi;
import io.gravitee.apim.core.integration.service_provider.IntegrationAgent;
import io.gravitee.apim.core.license.domain_service.LicenseDomainService;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class DiscoveryUseCase {

    private final IntegrationAgent integrationAgent;
    private final LicenseDomainService licenseDomainService;
    private final ApiQueryService apiQueryService;
    private final IntegrationCrudService integrationCrudService;

    public Single<Output> execute(Input input) {
        if (!licenseDomainService.isFederationFeatureAllowed(input.auditInfo.organizationId())) {
            return Single.error(noLicenseForFederation());
        }

        var integrationId = input.integrationId;

        Function<IntegrationApi, Output.State> computeState = apiStateComputing(input.auditInfo().environmentId(), integrationId);

        return Maybe
            .fromOptional(integrationCrudService.findById(integrationId))
            .filter(integration -> integration.getEnvironmentId().equals(input.auditInfo.environmentId()))
            .switchIfEmpty(Single.error(new IntegrationNotFoundException(integrationId)))
            .flatMapPublisher(integration -> integrationAgent.discoverApis(integration.getId()))
            .map(discoveredApi -> new Output.PreviewApi(discoveredApi, computeState.apply(discoveredApi)))
            .toList()
            .map(Output::new);
    }

    private Function<IntegrationApi, Output.State> apiStateComputing(String environmentId, String integrationId) {
        var alreadyIngestedApisIds = apiQueryService
            .search(
                ApiSearchCriteria.builder().integrationId(integrationId).build(),
                null,
                ApiFieldFilter.builder().pictureExcluded(true).definitionExcluded(true).build()
            )
            .map(Api::getId)
            .collect(Collectors.toSet());

        return discoveredApi -> {
            var discoveredApiId = ApiModelFactory.generateFederatedApiId(environmentId, integrationId, discoveredApi);
            return alreadyIngestedApisIds.contains(discoveredApiId) ? Output.State.UPDATE : Output.State.NEW;
        };
    }

    public record Input(String integrationId, AuditInfo auditInfo) {}

    public record Output(Collection<PreviewApi> apis) {
        public record PreviewApi(String id, String name, State state) {
            public PreviewApi(IntegrationApi api, State state) {
                this(api.id(), api.name(), state);
            }
        }

        public enum State {
            NEW,
            UPDATE,
        }
    }
}

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

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.api.domain_service.CreateApiDomainService;
import io.gravitee.apim.core.api.domain_service.ValidateFederatedApiDomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.integration.crud_service.IntegrationCrudService;
import io.gravitee.apim.core.integration.exception.IntegrationNotFoundException;
import io.gravitee.apim.core.integration.model.Asset;
import io.gravitee.apim.core.integration.model.Integration;
import io.gravitee.apim.core.integration.service_provider.IntegrationAgent;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerFactory;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.rest.api.service.common.UuidString;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import lombok.extern.slf4j.Slf4j;

@UseCase
@Slf4j
public class DiscoverIntegrationAssetUseCase {

    private final IntegrationCrudService integrationCrudService;
    private final ApiPrimaryOwnerFactory apiPrimaryOwnerFactory;
    private final ValidateFederatedApiDomainService validateFederatedApi;
    private final CreateApiDomainService createApiDomainService;
    private final IntegrationAgent integrationAgent;

    public DiscoverIntegrationAssetUseCase(
        IntegrationCrudService integrationCrudService,
        ApiPrimaryOwnerFactory apiPrimaryOwnerFactory,
        ValidateFederatedApiDomainService validateFederatedApi,
        CreateApiDomainService apiCrudService,
        IntegrationAgent integrationAgent
    ) {
        this.integrationCrudService = integrationCrudService;
        this.apiPrimaryOwnerFactory = apiPrimaryOwnerFactory;
        this.validateFederatedApi = validateFederatedApi;
        this.createApiDomainService = apiCrudService;
        this.integrationAgent = integrationAgent;
    }

    public Completable execute(Input input) {
        var integrationId = input.integrationId;
        var auditInfo = input.auditInfo;
        var organizationId = auditInfo.organizationId();
        var environmentId = auditInfo.environmentId();

        var primaryOwner = apiPrimaryOwnerFactory.createForNewApi(organizationId, environmentId, input.auditInfo.actor().userId());

        return Single
            .fromCallable(() ->
                integrationCrudService
                    .findById(integrationId)
                    .filter(integration -> integration.getEnvironmentId().equals(environmentId))
                    .orElseThrow(() -> new IntegrationNotFoundException(integrationId))
            )
            .flatMapPublisher(integration ->
                integrationAgent
                    .fetchAllAssets(integration)
                    .doOnNext(asset -> {
                        try {
                            createApiDomainService.create(
                                adaptAssetToApi(asset, integration),
                                primaryOwner,
                                auditInfo,
                                validateFederatedApi::validateAndSanitizeForCreation
                            );
                        } catch (Exception e) {
                            log.warn("An error occurred while importing asset {}", asset, e);
                        }
                    })
            )
            .ignoreElements();
    }

    public record Input(String integrationId, AuditInfo auditInfo) {}

    public Api adaptAssetToApi(Asset asset, Integration integration) {
        var now = TimeProvider.now();
        return Api
            .builder()
            .id(UuidString.generateRandom())
            .version(asset.version())
            .definitionVersion(DefinitionVersion.FEDERATED)
            .name(asset.name())
            .description(asset.description())
            .createdAt(now)
            .updatedAt(now)
            .environmentId(integration.getEnvironmentId())
            .lifecycleState(null)
            .build();
    }
}

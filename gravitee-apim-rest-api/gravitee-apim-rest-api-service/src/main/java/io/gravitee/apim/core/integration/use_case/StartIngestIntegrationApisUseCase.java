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
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.domain_service.ApiIndexerDomainService;
import io.gravitee.apim.core.api.domain_service.CreateApiDomainService;
import io.gravitee.apim.core.api.domain_service.UpdateFederatedApiDomainService;
import io.gravitee.apim.core.api.domain_service.ValidateFederatedApiDomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.async_job.crud_service.AsyncJobCrudService;
import io.gravitee.apim.core.async_job.model.AsyncJob;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.exception.NotAllowedDomainException;
import io.gravitee.apim.core.integration.crud_service.IntegrationCrudService;
import io.gravitee.apim.core.integration.exception.FederatedAgentIngestionException;
import io.gravitee.apim.core.integration.exception.IntegrationNotFoundException;
import io.gravitee.apim.core.integration.model.Integration;
import io.gravitee.apim.core.integration.service_provider.A2aAgentFetcher;
import io.gravitee.apim.core.integration.service_provider.IntegrationAgent;
import io.gravitee.apim.core.license.domain_service.LicenseDomainService;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerFactory;
import io.gravitee.apim.core.plan.crud_service.PlanCrudService;
import io.gravitee.apim.core.plan.domain_service.CreatePlanDomainService;
import io.gravitee.apim.core.plan.domain_service.UpdatePlanDomainService;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.federation.FederatedAgent;
import io.gravitee.definition.model.federation.FederatedPlan;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.rest.api.model.context.OriginContext;
import io.gravitee.rest.api.model.v4.plan.PlanSecurityType;
import io.gravitee.rest.api.service.common.UuidString;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

@UseCase
@CustomLog
@RequiredArgsConstructor
public class StartIngestIntegrationApisUseCase {

    private final IntegrationCrudService integrationCrudService;
    private final AsyncJobCrudService asyncJobCrudService;
    private final IntegrationAgent integrationAgent;
    private final LicenseDomainService licenseDomainService;
    private final CreateApiDomainService createApiDomainService;
    private final UpdateFederatedApiDomainService updateFederatedApiDomainService;
    private final ApiIndexerDomainService apiIndexerDomainService;
    private final A2aAgentFetcher a2aAgentFetcher;
    private final ApiPrimaryOwnerFactory apiPrimaryOwnerFactory;
    private final ValidateFederatedApiDomainService validateFederatedApi;
    private final CreatePlanDomainService createPlanDomainService;
    private final UpdatePlanDomainService updatePlanDomainService;
    private final PlanCrudService planCrudService;
    private final ApiCrudService apiCrudService;

    public Single<AsyncJob.Status> execute(Input input) {
        var auditInfo = input.auditInfo;
        var integrationId = input.integrationId;
        var organizationId = auditInfo.organizationId();
        var environmentId = auditInfo.environmentId();

        if (!licenseDomainService.isFederationFeatureAllowed(organizationId)) {
            return Single.error(NotAllowedDomainException.noLicenseForFederation());
        }
        var integ = integrationCrudService.findById(integrationId).filter(integration -> integration.environmentId().equals(environmentId));
        if (integ.isEmpty()) {
            return Single.error(new IntegrationNotFoundException(integrationId));
        }
        return switch (integ.get()) {
            case Integration.ApiIntegration apiIntegration -> Single.just(apiIntegration)
                .flatMap(integration -> startApiIngestion(input, integration, auditInfo))
                .doOnError(throwable -> log.error("Error to start ingest {}", integrationId, throwable));
            case Integration.A2aIntegration a2aIntegration -> a2aIngestions(a2aIntegration, auditInfo);
        };
    }

    private Single<AsyncJob.Status> startApiIngestion(Input input, Integration.ApiIntegration integration, AuditInfo auditInfo) {
        return integrationAgent
            .startIngest(integration.id(), UuidString.generateRandom(), input.apiIds())
            .map(ingestStarted -> {
                log.info("Ingestion started for integration {}", integration.id());

                if (ingestStarted.total() == 0) {
                    log.info("No APIs to ingest for integration {}", integration.id());
                    return AsyncJob.Status.SUCCESS;
                }

                asyncJobCrudService.create(
                    newIngestJob(ingestStarted.ingestJobId(), integration, auditInfo.actor().userId(), ingestStarted.total())
                );

                return AsyncJob.Status.PENDING;
            });
    }

    private Single<AsyncJob.Status> a2aIngestions(Integration.A2aIntegration a2aIntegration, AuditInfo auditInfo) {
        try (var bulk = apiIndexerDomainService.bulk(auditInfo)) {
            return Flowable.fromIterable(a2aIntegration.wellKnownUrls())
                .flatMapMaybe(url -> a2aIngestion(bulk, url.url(), a2aIntegration, auditInfo))
                .toList()
                .flatMap(failedUrls -> {
                    if (!failedUrls.isEmpty()) {
                        return Single.error(new FederatedAgentIngestionException(failedUrls));
                    } else {
                        return Single.just(AsyncJob.Status.SUCCESS);
                    }
                });
        } catch (Exception e) {
            return Single.error(new FederatedAgentIngestionException("Ingestion failed", e));
        }
    }

    private Maybe<String> a2aIngestion(
        ApiIndexerDomainService.Bulk bulk,
        String url,
        Integration.A2aIntegration a2aIntegration,
        AuditInfo auditInfo
    ) {
        return a2aAgentFetcher
            .fetchAgentCard(url)
            .toMaybe()
            .flatMap(federatedAgent -> {
                var owner = apiPrimaryOwnerFactory.createForNewApi(
                    auditInfo.organizationId(),
                    a2aIntegration.environmentId(),
                    auditInfo.actor().userId()
                );
                String id = UuidString.generateForEnvironment(
                    a2aIntegration.environmentId(),
                    a2aIntegration.id(),
                    url,
                    federatedAgent.getUrl()
                );

                Api api = Api.builder()
                    .id(id)
                    .name(federatedAgent.getName())
                    .description(federatedAgent.getDescription())
                    .version(federatedAgent.getVersion())
                    .createdAt(TimeProvider.now())
                    .updatedAt(TimeProvider.now())
                    .definitionVersion(DefinitionVersion.FEDERATED_AGENT)
                    .environmentId(a2aIntegration.environmentId())
                    .originContext(new OriginContext.Integration(a2aIntegration.id(), a2aIntegration.name(), a2aIntegration.provider()))
                    .apiDefinitionValue(federatedAgent)
                    .build();
                UnaryOperator<Api> updater = update(api);

                apiCrudService
                    .findById(id)
                    .ifPresentOrElse(
                        previous -> updateFederatedApiDomainService.update(id, updater, bulk.auditInfo(), owner, bulk.get()),
                        () ->
                            createApiDomainService.create(
                                api,
                                owner,
                                auditInfo,
                                e -> validateFederatedApi.validateAndSanitizeForCreation(e, owner),
                                bulk.get()
                            )
                    );

                Plan plan = fromIntegration(api, federatedAgent);
                planCrudService
                    .findById(plan.getId())
                    .ifPresentOrElse(
                        existingPlan -> updatePlanDomainService.update(plan, List.of(), Map.of(), api, bulk.auditInfo()),
                        () -> createPlanDomainService.create(plan, List.of(), api, bulk.auditInfo())
                    );

                return Maybe.<String>empty();
            })
            .onErrorReturnItem(url);
    }

    public AsyncJob newIngestJob(String id, Integration integration, String initiatorId, Long total) {
        var now = TimeProvider.now();
        return AsyncJob.builder()
            .id(id)
            .sourceId(integration.id())
            .environmentId(integration.environmentId())
            .initiatorId(initiatorId)
            .type(AsyncJob.Type.FEDERATED_APIS_INGESTION)
            .status(AsyncJob.Status.PENDING)
            .upperLimit(total)
            .createdAt(now)
            .updatedAt(now)
            .deadLine(now.plus(Duration.ofMinutes(5)))
            .build();
    }

    static UnaryOperator<Api> update(Api newOne) {
        return previousApi ->
            previousApi
                .toBuilder()
                .name(newOne.getName())
                .description(newOne.getDescription())
                .version(newOne.getVersion())
                .apiDefinitionValue(newOne.getApiDefinitionValue())
                .build();
    }

    public static Plan fromIntegration(Api api, FederatedAgent federatedAgent) {
        var id = UuidString.generateForEnvironment(api.getId(), PlanSecurityType.KEY_LESS.getLabel());
        var now = TimeProvider.now();
        var oid = federatedAgent.getProvider() != null ? federatedAgent.getProvider().organization() : null;
        return Plan.builder()
            .id(id)
            .name("Key less plan")
            .description("Default plan")
            .referenceId(api.getId())
            .referenceType(io.gravitee.rest.api.model.v4.plan.GenericPlanEntity.ReferenceType.API)
            .federatedPlanDefinition(
                FederatedPlan.builder()
                    .id(id)
                    .providerId(oid)
                    .security(PlanSecurity.builder().type(PlanSecurityType.KEY_LESS.getLabel()).build())
                    .status(PlanStatus.PUBLISHED)
                    .build()
            )
            .characteristics(List.of())
            .environmentId(api.getEnvironmentId())
            .createdAt(now)
            .updatedAt(now)
            .validation(Plan.PlanValidationType.MANUAL)
            .build();
    }

    public record Input(String integrationId, List<String> apiIds, AuditInfo auditInfo) {}
}

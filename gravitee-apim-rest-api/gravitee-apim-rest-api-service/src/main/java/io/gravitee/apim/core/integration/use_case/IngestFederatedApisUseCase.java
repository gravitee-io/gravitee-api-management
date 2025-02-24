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

import static io.gravitee.apim.core.utils.CollectionUtils.stream;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.domain_service.ApiIndexerDomainService;
import io.gravitee.apim.core.api.domain_service.ApiMetadataDomainService;
import io.gravitee.apim.core.api.domain_service.CreateApiDomainService;
import io.gravitee.apim.core.api.domain_service.UpdateFederatedApiDomainService;
import io.gravitee.apim.core.api.domain_service.ValidateFederatedApiDomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.ApiMetadata;
import io.gravitee.apim.core.api.model.factory.ApiModelFactory;
import io.gravitee.apim.core.async_job.crud_service.AsyncJobCrudService;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.documentation.domain_service.ClearIngestedApiDocumentationDomainService;
import io.gravitee.apim.core.documentation.domain_service.CreateApiDocumentationDomainService;
import io.gravitee.apim.core.documentation.domain_service.HomepageDomainService;
import io.gravitee.apim.core.documentation.domain_service.UpdateApiDocumentationDomainService;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.documentation.query_service.PageQueryService;
import io.gravitee.apim.core.integration.crud_service.IntegrationCrudService;
import io.gravitee.apim.core.integration.model.IntegrationApi;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerFactory;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.apim.core.metadata.model.Metadata;
import io.gravitee.apim.core.notification.domain_service.TriggerNotificationDomainService;
import io.gravitee.apim.core.notification.model.hook.portal.FederatedApisIngestionCompleteHookContext;
import io.gravitee.apim.core.plan.crud_service.PlanCrudService;
import io.gravitee.apim.core.plan.domain_service.CreatePlanDomainService;
import io.gravitee.apim.core.plan.domain_service.UpdatePlanDomainService;
import io.gravitee.apim.core.plan.model.factory.PlanModelFactory;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.service.common.UuidString;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@Slf4j
@RequiredArgsConstructor
public class IngestFederatedApisUseCase {

    private final AsyncJobCrudService asyncJobCrudService;
    private final ApiPrimaryOwnerFactory apiPrimaryOwnerFactory;
    private final ValidateFederatedApiDomainService validateFederatedApi;
    private final ApiCrudService apiCrudService;
    private final PlanCrudService planCrudService;
    private final PageQueryService pageQueryService;
    private final CreateApiDomainService createApiDomainService;
    private final UpdateFederatedApiDomainService updateFederatedApiDomainService;
    private final CreatePlanDomainService createPlanDomainService;
    private final UpdatePlanDomainService updatePlanDomainService;
    private final CreateApiDocumentationDomainService createApiDocumentationDomainService;
    private final UpdateApiDocumentationDomainService updateApiDocumentationDomainService;
    private final TriggerNotificationDomainService triggerNotificationDomainService;
    private final ApiMetadataDomainService apiMetadataDomainService;
    private final ApiIndexerDomainService apiIndexerDomainService;
    private final HomepageDomainService homepageDomainService;
    private final ClearIngestedApiDocumentationDomainService clearIngestedApiDocumentationDomainService;
    private final IntegrationCrudService integrationCrudService;

    public Completable execute(Input input) {
        log.info("Ingesting {} federated APIs [jobId={}]", input.apisToIngest().size(), input.ingestJobId);
        var ingestJobId = input.ingestJobId;
        var organizationId = input.organizationId();

        return Maybe
            .defer(() -> Maybe.fromOptional(asyncJobCrudService.findById(ingestJobId)))
            .subscribeOn(Schedulers.computation())
            .doOnSuccess(job -> {
                var environmentId = job.getEnvironmentId();
                var userId = job.getInitiatorId();
                var auditInfo = new AuditInfo(organizationId, environmentId, new AuditActor(userId, null, null));
                var primaryOwner = apiPrimaryOwnerFactory.createForNewApi(organizationId, environmentId, userId);

                try (var bulk = apiIndexerDomainService.bulk(auditInfo)) {
                    for (IntegrationApi api : input.apisToIngest) {
                        var integration = integrationCrudService
                            .findById(job.getSourceId())
                            .orElseThrow(() -> new IllegalStateException("Integration %s not found".formatted(job.getSourceId())));
                        var federatedApi = ApiModelFactory.fromIngestionJob(api, job, integration);

                        apiCrudService
                            .findById(federatedApi.getId())
                            .ifPresentOrElse(
                                existingApi -> updateApi(bulk, federatedApi, api, auditInfo, primaryOwner),
                                () -> createApi(bulk, federatedApi, api, auditInfo, primaryOwner)
                            );
                        List<ApiMetadata> metadata = metadata(api, federatedApi);
                        apiMetadataDomainService.saveApiMetadata(federatedApi.getId(), metadata, bulk.auditInfo());
                    }
                }
                if (input.completed) {
                    asyncJobCrudService.update(job.complete());
                    triggerNotificationDomainService.triggerPortalNotification(
                        organizationId,
                        environmentId,
                        new FederatedApisIngestionCompleteHookContext(job.getSourceId())
                    );
                } else {
                    asyncJobCrudService.delay(job.getId(), TimeProvider.now().plusMinutes(5));
                }
            })
            .ignoreElement();
    }

    private void createApi(
        ApiIndexerDomainService.Bulk bulk,
        Api federatedApi,
        IntegrationApi integrationApi,
        AuditInfo auditInfo,
        PrimaryOwnerEntity primaryOwner
    ) {
        try {
            createApiDomainService.create(
                federatedApi,
                primaryOwner,
                auditInfo,
                newApi -> validateFederatedApi.validateAndSanitizeForCreation(newApi, primaryOwner),
                bulk.get()
            );

            stream(integrationApi.plans())
                .map(plan -> PlanModelFactory.fromIntegration(plan, federatedApi))
                .forEach(p -> createPlanDomainService.create(p, List.of(), federatedApi, bulk.auditInfo()));

            stream(integrationApi.pages())
                .flatMap(page -> buildPage(page, integrationApi, federatedApi.getId()))
                .forEach(page -> createApiDocumentationDomainService.createPage(page, bulk.auditInfo()));
        } catch (Exception e) {
            log.warn("An error occurred while importing api {}", federatedApi, e);
        }
    }

    private void updateApi(
        ApiIndexerDomainService.Bulk bulk,
        Api federatedApi,
        IntegrationApi integrationApi,
        AuditInfo auditInfo,
        PrimaryOwnerEntity primaryOwner
    ) {
        log.debug("API already ingested [id={}] [name={}], performing update", federatedApi.getId(), federatedApi.getName());
        try {
            UnaryOperator<Api> updater = update(federatedApi);
            updateFederatedApiDomainService.update(federatedApi.getId(), updater, auditInfo, primaryOwner, bulk.get());

            stream(integrationApi.plans())
                .map(p -> PlanModelFactory.fromIntegration(p, federatedApi))
                .forEach(p ->
                    planCrudService
                        .findById(p.getId())
                        .ifPresentOrElse(
                            existingPlan -> updatePlanDomainService.update(p, List.of(), Map.of(), null, bulk.auditInfo()),
                            () -> createPlanDomainService.create(p, List.of(), federatedApi, bulk.auditInfo())
                        )
                );

            var ingestedPagesNames = integrationApi.pages().stream().map(IntegrationApi.Page::filename).toList();
            clearIngestedApiDocumentationDomainService.clearIngestedPagesOf(federatedApi.getId(), ingestedPagesNames, bulk.auditInfo());
            var existingPages = pageQueryService
                .searchByApiId(federatedApi.getId())
                .stream()
                .collect(Collectors.toMap(Page::getName, Function.identity()));
            List<Page> updatedOrNewPages = stream(integrationApi.pages())
                .flatMap(page -> buildPage(page, integrationApi, federatedApi.getId()))
                .map(page -> {
                    /*
                     * We let agent choose coherent page name and rely on it to updating
                     */
                    Page existingPage = existingPages.get(page.getName());
                    if (existingPage == null) {
                        return createApiDocumentationDomainService.createPage(page, bulk.auditInfo());
                    } else {
                        var pageWithProperCreatedAt = page
                            .toBuilder()
                            .createdAt(existingPage.getCreatedAt())
                            .id(existingPage.getId())
                            .build();
                        return updateApiDocumentationDomainService.updatePage(pageWithProperCreatedAt, existingPage, bulk.auditInfo());
                    }
                })
                .toList();
            updatedOrNewPages
                .stream()
                .filter(Page::isHomepage)
                .sorted(Comparator.comparing(Page::getCreatedAt).reversed())
                .map(Page::getId)
                .distinct()
                .findFirst()
                .ifPresent(homepageId -> homepageDomainService.setPreviousHomepageToFalse(federatedApi.getId(), homepageId));
        } catch (Exception e) {
            log.warn("An error occurred while updating api {}", federatedApi, e);
        }
    }

    private Stream<Page> buildPage(IntegrationApi.Page page, IntegrationApi integrationApi, String referenceId) {
        if (page == null || page.pageType() == null) {
            return Stream.empty();
        }
        return switch (page.pageType()) {
            case SWAGGER -> Stream.of(buildSwaggerPage(referenceId, page));
            case ASYNCAPI -> Stream.of(buildAsyncApiPage(referenceId, page));
            case ASCIIDOC, MARKDOWN, MARKDOWN_TEMPLATE -> {
                log.error("Impossible to import {} documentation for {}", page.pageType(), integrationApi.name());
                yield Stream.empty();
            }
        };
    }

    private Page buildSwaggerPage(String referenceId, IntegrationApi.Page page) {
        var now = Date.from(TimeProvider.instantNow());
        return Page
            .builder()
            .id(UuidString.generateRandom())
            .name(page.filename())
            .content(page.content())
            .type(Page.Type.SWAGGER)
            .referenceId(referenceId)
            .referenceType(Page.ReferenceType.API)
            .published(true)
            .visibility(Page.Visibility.PRIVATE)
            .homepage(true)
            .configuration(Map.of("tryIt", "true", "viewer", "Swagger"))
            .createdAt(now)
            .updatedAt(now)
            .ingested(true)
            .build();
    }

    private Page buildAsyncApiPage(String referenceId, IntegrationApi.Page page) {
        var now = Date.from(TimeProvider.instantNow());
        return Page
            .builder()
            .id(UuidString.generateRandom())
            .name(page.filename())
            .content(page.content())
            .type(Page.Type.ASYNCAPI)
            .referenceId(referenceId)
            .referenceType(Page.ReferenceType.API)
            .published(true)
            .visibility(Page.Visibility.PRIVATE)
            .homepage(true)
            .createdAt(now)
            .updatedAt(now)
            .ingested(true)
            .build();
    }

    static UnaryOperator<Api> update(Api newOne) {
        return previousApi ->
            previousApi
                .toBuilder()
                .name(newOne.getName())
                .description(newOne.getDescription())
                .version(newOne.getVersion())
                .federatedApiDefinition(newOne.getFederatedApiDefinition())
                .build();
    }

    private static List<ApiMetadata> metadata(IntegrationApi api, Api federatedApi) {
        return stream(api.metadata())
            .map(md -> {
                var format =
                    switch (md.format()) {
                        case STRING -> Metadata.MetadataFormat.STRING;
                        case NUMERIC -> Metadata.MetadataFormat.NUMERIC;
                        case MAIL -> Metadata.MetadataFormat.MAIL;
                        case DATE -> Metadata.MetadataFormat.DATE;
                        case URL -> Metadata.MetadataFormat.URL;
                        case BOOLEAN -> Metadata.MetadataFormat.BOOLEAN;
                    };
                return ApiMetadata
                    .builder()
                    .apiId(federatedApi.getId())
                    .name(md.name())
                    .key(md.name())
                    .value(md.value())
                    .format(format)
                    .build();
            })
            .toList();
    }

    public record Input(String organizationId, String ingestJobId, List<IntegrationApi> apisToIngest, boolean completed) {}
}

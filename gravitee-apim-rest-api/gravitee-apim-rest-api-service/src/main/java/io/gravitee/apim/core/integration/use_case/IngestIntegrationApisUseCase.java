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
import io.gravitee.apim.core.api.domain_service.CreateApiDomainService;
import io.gravitee.apim.core.api.domain_service.ValidateFederatedApiDomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.factory.ApiModelFactory;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.documentation.domain_service.CreateApiDocumentationDomainService;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.integration.crud_service.IntegrationCrudService;
import io.gravitee.apim.core.integration.exception.IntegrationNotFoundException;
import io.gravitee.apim.core.integration.model.IntegrationApi;
import io.gravitee.apim.core.integration.service_provider.IntegrationAgent;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerFactory;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.apim.core.plan.domain_service.CreatePlanDomainService;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.definition.model.federation.FederatedPlan;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.rest.api.model.v4.plan.PlanSecurityType;
import io.gravitee.rest.api.service.common.UuidString;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@UseCase
@Slf4j
public class IngestIntegrationApisUseCase {

    private final IntegrationCrudService integrationCrudService;
    private final ApiPrimaryOwnerFactory apiPrimaryOwnerFactory;
    private final ValidateFederatedApiDomainService validateFederatedApi;
    private final ApiCrudService apiCrudService;
    private final CreateApiDomainService createApiDomainService;
    private final CreatePlanDomainService createPlanDomainService;
    private final IntegrationAgent integrationAgent;
    private final CreateApiDocumentationDomainService createApiDocumentationDomainService;

    public IngestIntegrationApisUseCase(
        IntegrationCrudService integrationCrudService,
        ApiPrimaryOwnerFactory apiPrimaryOwnerFactory,
        ValidateFederatedApiDomainService validateFederatedApi,
        ApiCrudService apiCrudService,
        CreateApiDomainService createApiDomainService,
        CreatePlanDomainService createPlanDomainService,
        IntegrationAgent integrationAgent,
        CreateApiDocumentationDomainService createApiDocumentationDomainService
    ) {
        this.integrationCrudService = integrationCrudService;
        this.apiPrimaryOwnerFactory = apiPrimaryOwnerFactory;
        this.validateFederatedApi = validateFederatedApi;
        this.apiCrudService = apiCrudService;
        this.createApiDomainService = createApiDomainService;
        this.createPlanDomainService = createPlanDomainService;
        this.integrationAgent = integrationAgent;
        this.createApiDocumentationDomainService = createApiDocumentationDomainService;
    }

    public Completable execute(Input input) {
        var integrationId = input.integrationId;
        var auditInfo = input.auditInfo;
        var organizationId = auditInfo.organizationId();
        var environmentId = auditInfo.environmentId();

        return Single
            .fromCallable(() ->
                integrationCrudService
                    .findById(integrationId)
                    .filter(integration -> integration.getEnvironmentId().equals(environmentId))
                    .orElseThrow(() -> new IntegrationNotFoundException(integrationId))
            )
            .flatMapPublisher(integration -> {
                var primaryOwner = apiPrimaryOwnerFactory.createForNewApi(organizationId, environmentId, input.auditInfo.actor().userId());
                return integrationAgent
                    .fetchAllApis(integration)
                    .doOnNext(api -> {
                        var federatedApi = ApiModelFactory.fromIntegration(api, integration);

                        if (apiCrudService.existsById(federatedApi.getId())) {
                            log.debug("API already ingested [id={}] [name={}]", api.id(), api.name());
                            return;
                        }

                        createApi(federatedApi, primaryOwner, auditInfo);

                        if (api.plans() != null) {
                            createPlans(api.plans(), federatedApi, auditInfo);
                        }

                        if (api.pages() != null) {
                            createDocumentation(api, federatedApi.getId(), auditInfo);
                        }
                    });
            })
            .ignoreElements();
    }

    private void createDocumentation(IntegrationApi integrationApi, String referenceId, AuditInfo auditInfo) {
        integrationApi
            .pages()
            .stream()
            .filter(page -> page.pageType() == IntegrationApi.PageType.SWAGGER)
            .map(page -> buildSwaggerPage(integrationApi.name(), referenceId, page.content()))
            .forEach(page -> createApiDocumentationDomainService.createPage(page, auditInfo));
    }

    private Page buildSwaggerPage(String name, String referenceId, String content) {
        var now = Date.from(TimeProvider.instantNow());
        return Page
            .builder()
            .id(UuidString.generateRandom())
            .name(name.concat("-oas.yml"))
            .content(content)
            .type(Page.Type.valueOf(IntegrationApi.PageType.SWAGGER.name()))
            .referenceId(referenceId)
            .referenceType(Page.ReferenceType.API)
            .published(true)
            .visibility(Page.Visibility.PRIVATE)
            .homepage(true)
            .configuration(Map.of("tryIt", "true", "viewer", "Swagger"))
            .createdAt(now)
            .updatedAt(now)
            .build();
    }

    private void createApi(Api federatedApi, PrimaryOwnerEntity primaryOwner, AuditInfo auditInfo) {
        try {
            createApiDomainService.create(federatedApi, primaryOwner, auditInfo, validateFederatedApi::validateAndSanitizeForCreation);
        } catch (Exception e) {
            log.warn("An error occurred while importing api {}", federatedApi, e);
        }
    }

    private void createPlans(List<IntegrationApi.Plan> plans, Api federatedApi, AuditInfo auditInfo) {
        plans
            .stream()
            .map(p -> {
                var id = UuidString.generateRandom();
                var now = TimeProvider.now();
                return Plan
                    .builder()
                    .id(id)
                    .name(p.name())
                    .description(p.description())
                    .apiId(federatedApi.getId())
                    .federatedPlanDefinition(
                        FederatedPlan
                            .builder()
                            .id(id)
                            .providerId(p.id())
                            .security(PlanSecurity.builder().type(PlanSecurityType.valueOf(p.type().name()).getLabel()).build())
                            .status(PlanStatus.PUBLISHED)
                            .build()
                    )
                    .createdAt(now)
                    .updatedAt(now)
                    .validation(Plan.PlanValidationType.MANUAL)
                    .build();
            })
            .forEach(p -> {
                createPlanDomainService.create(p, Collections.emptyList(), federatedApi, auditInfo);
            });
    }

    public record Input(String integrationId, AuditInfo auditInfo) {}
}

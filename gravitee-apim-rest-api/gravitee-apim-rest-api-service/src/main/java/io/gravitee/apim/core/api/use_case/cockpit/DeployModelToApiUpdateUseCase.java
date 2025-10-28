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
package io.gravitee.apim.core.api.use_case.cockpit;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.api.domain_service.ApiStateDomainService;
import io.gravitee.apim.core.api.domain_service.OAIDomainService;
import io.gravitee.apim.core.api.domain_service.UpdateApiDomainService;
import io.gravitee.apim.core.api.model.factory.ApiModelFactory;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.documentation.crud_service.PageCrudService;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.plan.crud_service.PlanCrudService;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.rest.api.service.common.UuidString;
import java.util.Date;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UseCase
public class DeployModelToApiUpdateUseCase extends AbstractDeployModelToApiUseCase {

    private final OAIDomainService oaiDomainService;
    private final PlanCrudService planCrudService;
    private final PageCrudService pageCrudService;

    public DeployModelToApiUpdateUseCase(
        OAIDomainService oaiDomainService,
        UpdateApiDomainService updateApiDomainService,
        ApiStateDomainService apiStateDomainService,
        PlanCrudService planCrudService,
        PageCrudService pageCrudService
    ) {
        super(updateApiDomainService, apiStateDomainService);
        this.oaiDomainService = oaiDomainService;
        this.planCrudService = planCrudService;
        this.pageCrudService = pageCrudService;
    }

    public DeployModelToApiUpdateUseCase.Output execute(DeployModelToApiUpdateUseCase.Input input) {
        var organizationId = input.auditInfo().organizationId();
        var environmentId = input.auditInfo().environmentId();
        var importSwaggerDescriptorEntity = configure(input.mode(), input.swaggerDefinition());
        var importDefinition = oaiDomainService.convert(organizationId, environmentId, importSwaggerDescriptorEntity, true, false);

        managePlan(input);
        manageDocumentationPage(input, importDefinition.getPages().getFirst());

        importDefinition.getApiExport().setId(input.apiId());
        importDefinition.getApiExport().setCrossId(input.apiCrossId());
        importDefinition.getApiExport().setLabels(input.labels());

        var apiUpdated = updateApiDomainService.updateV4(
            ApiModelFactory.fromApiExport(importDefinition.getApiExport(), environmentId),
            input.auditInfo()
        );
        var api = manageApiState(apiUpdated, input.auditInfo(), input.mode());

        return new DeployModelToApiUpdateUseCase.Output(api);
    }

    private void managePlan(final Input input) {
        final var plans = planCrudService.findByApiId(input.apiId());

        if (plans.stream().noneMatch(plan -> plan.getPlanStatus() == PlanStatus.PUBLISHED)) {
            planCrudService.create(
                new Plan(input.apiId(), defaultPlanDefinition)
                    .toBuilder()
                    .needRedeployAt(Date.from(TimeProvider.now().toInstant()))
                    .validation(Plan.PlanValidationType.AUTO)
                    .build()
            );
        }
    }

    private void manageDocumentationPage(final Input input, final Page pageToCreate) {
        final var pages = pageCrudService.findByApiId(input.apiId());
        final var swaggerDocumentationPageCount = pages
            .stream()
            .filter(page -> page.getType() == Page.Type.SWAGGER)
            .count();

        if (swaggerDocumentationPageCount > 1) {
            log.error("More than one Swagger documentation page already exists for this API.");
            return;
        }

        if (swaggerDocumentationPageCount == 0) {
            log.error("No Swagger documentation page exists for this API.");
            pageToCreate.setId(UuidString.generateRandom());
            pageToCreate.setCreatedAt(Date.from(TimeProvider.now().toInstant()));
            pageToCreate.setUpdatedAt(Date.from(TimeProvider.now().toInstant()));
            pageToCreate.setReferenceId(input.apiId());
            pageCrudService.createDocumentationPage(pageToCreate);
            return;
        }

        final var currentPage = pages.getFirst();
        currentPage.setContent(pageToCreate.getContent());
        currentPage.setUpdatedAt(Date.from(TimeProvider.now().toInstant()));
        pageCrudService.updateDocumentationPage(currentPage);
    }

    public record Input(
        String swaggerDefinition,
        AuditInfo auditInfo,
        String apiId,
        String apiCrossId,
        AbstractDeployModelToApiUseCase.Mode mode,
        List<String> labels
    ) {}
}

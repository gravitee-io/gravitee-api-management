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
import io.gravitee.apim.core.api.domain_service.ImportDefinitionCreateDomainService;
import io.gravitee.apim.core.api.domain_service.OAIDomainService;
import io.gravitee.apim.core.api.domain_service.cockpit.DeployModelToApiDomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.ApiWithFlows;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.plan.model.PlanWithFlows;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UseCase
public class DeployModelToApiCreateUseCase {

    private final OAIDomainService oaiDomainService;
    private final ImportDefinitionCreateDomainService importDefinitionCreateDomainService;
    private final DeployModelToApiDomainService deployModelToApiDomainService;

    public DeployModelToApiCreateUseCase(
        OAIDomainService oaiDomainService,
        ImportDefinitionCreateDomainService importDefinitionCreateDomainService,
        DeployModelToApiDomainService deployModelToApiDomainService
    ) {
        this.oaiDomainService = oaiDomainService;
        this.importDefinitionCreateDomainService = importDefinitionCreateDomainService;
        this.deployModelToApiDomainService = deployModelToApiDomainService;
    }

    public DeployModelToApiCreateUseCase.Output execute(DeployModelToApiCreateUseCase.Input input) {
        log.debug("API [crossId: {}] will be created with mode {}.", input.apiCrossId(), input.mode().name());

        var organizationId = input.auditInfo().organizationId();
        var environmentId = input.auditInfo().environmentId();
        var importSwaggerDescriptorEntity = deployModelToApiDomainService.configure(input.mode(), input.swaggerDefinition());
        var importDefinition = oaiDomainService.convert(organizationId, environmentId, importSwaggerDescriptorEntity, true, false);

        importDefinition.getApiExport().setCrossId(input.apiCrossId());
        importDefinition.getApiExport().setLabels(input.labels());
        importDefinition.setPlans(
            Set.of(
                new PlanWithFlows(
                    new Plan(null, deployModelToApiDomainService.createDefaultPlan())
                        .toBuilder()
                        .validation(Plan.PlanValidationType.AUTO)
                        .build(),
                    List.of()
                )
            )
        );

        final ApiWithFlows apiWithFlows = importDefinitionCreateDomainService.create(input.auditInfo(), importDefinition);
        final var api = deployModelToApiDomainService.manageApiState(apiWithFlows, input.auditInfo, input.mode());

        log.info("API [id: {} / crossId: {}] v4 created.", api.getId(), api.getCrossId());

        return new DeployModelToApiCreateUseCase.Output(api);
    }

    public record Input(
        String swaggerDefinition,
        AuditInfo auditInfo,
        String apiCrossId,
        DeployModelToApiDomainService.Mode mode,
        List<String> labels
    ) {}

    public record Output(Api api) {}
}

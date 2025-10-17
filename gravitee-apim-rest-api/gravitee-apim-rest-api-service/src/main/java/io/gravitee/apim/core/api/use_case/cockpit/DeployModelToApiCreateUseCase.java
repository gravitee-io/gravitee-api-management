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
import io.gravitee.apim.core.api.domain_service.ImportDefinitionCreateDomainService;
import io.gravitee.apim.core.api.domain_service.OAIDomainService;
import io.gravitee.apim.core.api.domain_service.UpdateApiDomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.ApiWithFlows;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.plan.model.PlanWithFlows;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.rest.api.model.ImportSwaggerDescriptorEntity;
import io.gravitee.rest.api.service.common.UuidString;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UseCase
public class DeployModelToApiCreateUseCase {

    public enum Mode {
        DOCUMENTED,
        MOCKED,
        PUBLISHED,
    }

    private static final io.gravitee.definition.model.v4.plan.Plan defaultPlanDefinition =
        io.gravitee.definition.model.v4.plan.Plan.builder()
            .id(UuidString.generateRandom())
            .name("Default plan")
            .mode(PlanMode.STANDARD)
            .status(PlanStatus.PUBLISHED)
            .security(PlanSecurity.builder().type("key-less").build())
            .build();

    public record Input(String swaggerDefinition, AuditInfo auditInfo, String apiCrossId, Mode mode, List<String> labels) {}

    public record Output(Api api) {}

    private final OAIDomainService oaiDomainService;
    private final ImportDefinitionCreateDomainService importDefinitionCreateDomainService;
    private final UpdateApiDomainService updateApiDomainService;
    private final ApiStateDomainService apiStateDomainService;

    public DeployModelToApiCreateUseCase(
        OAIDomainService oaiDomainService,
        ImportDefinitionCreateDomainService importDefinitionCreateDomainService,
        UpdateApiDomainService updateApiDomainService,
        ApiStateDomainService apiStateDomainService
    ) {
        this.oaiDomainService = oaiDomainService;
        this.importDefinitionCreateDomainService = importDefinitionCreateDomainService;
        this.updateApiDomainService = updateApiDomainService;
        this.apiStateDomainService = apiStateDomainService;
    }

    public DeployModelToApiCreateUseCase.Output execute(DeployModelToApiCreateUseCase.Input input) {
        var organizationId = input.auditInfo().organizationId();
        var environmentId = input.auditInfo().environmentId();
        var importSwaggerDescriptorEntity = configure(input.mode(), input.swaggerDefinition());
        var importDefinition = oaiDomainService.convert(organizationId, environmentId, importSwaggerDescriptorEntity, true, false);

        importDefinition.getApiExport().setCrossId(input.apiCrossId());
        importDefinition.getApiExport().setLabels(input.labels());
        importDefinition.setPlans(
            Set.of(
                new PlanWithFlows(
                    new Plan(null, defaultPlanDefinition).toBuilder().validation(Plan.PlanValidationType.AUTO).build(),
                    List.of()
                )
            )
        );

        final ApiWithFlows apiWithFlows = importDefinitionCreateDomainService.create(input.auditInfo(), importDefinition);
        final var api = manageApiState(apiWithFlows, input.auditInfo, input.mode());

        return new DeployModelToApiCreateUseCase.Output(api);
    }

    private ImportSwaggerDescriptorEntity configure(Mode mode, String swaggerDefinition) {
        var importSwaggerDescriptor = ImportSwaggerDescriptorEntity.builder().payload(swaggerDefinition);

        log.debug("API will be Documented.");
        importSwaggerDescriptor.withDocumentation(true);

        if (mode == Mode.MOCKED || mode == Mode.PUBLISHED) {
            log.debug("API will be Mocked.");
            importSwaggerDescriptor.withPolicyPaths(true);
            importSwaggerDescriptor.withPolicies(List.of("mock"));
        }

        return importSwaggerDescriptor.build();
    }

    private Api manageApiState(Api api, AuditInfo audit, Mode mode) {
        // API should be published
        if (mode == Mode.PUBLISHED) {
            log.debug("Published v4 API.");

            api.setVisibility(Api.Visibility.PUBLIC);
            api.setApiLifecycleState(Api.ApiLifecycleState.PUBLISHED);
        }

        var updatedApi = updateApiDomainService.updateV4(api, audit);

        // API should be started
        if (mode == Mode.MOCKED || mode == Mode.PUBLISHED) {
            log.debug("Started v4 API.");
            apiStateDomainService.start(api, audit);
        }

        // Force API deployment if out of sync
        if (!apiStateDomainService.isSynchronized(api, audit)) {
            apiStateDomainService.deploy(api, "Managed by Gravitee Cloud", audit);
        }

        return updatedApi;
    }
}

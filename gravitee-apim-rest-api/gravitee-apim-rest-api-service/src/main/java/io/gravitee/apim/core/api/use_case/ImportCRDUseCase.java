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
package io.gravitee.apim.core.api.use_case;

import io.gravitee.apim.core.api.domain_service.ApiMetadataDomainService;
import io.gravitee.apim.core.api.domain_service.CreateApiDomainService;
import io.gravitee.apim.core.api.domain_service.DeployApiDomainService;
import io.gravitee.apim.core.api.model.ApiCRD;
import io.gravitee.apim.core.api.model.ApiCRDStatus;
import io.gravitee.apim.core.api.query_service.ApiQueryService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.plan.domain_service.CreatePlanDomainService;
import io.gravitee.definition.model.DefinitionContext;
import io.gravitee.rest.api.model.v4.plan.BasePlanEntity;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.stream.Collectors;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ImportCRDUseCase {

    private final ApiQueryService apiQueryService;
    private final CreateApiDomainService createApiDomainService;
    private final CreatePlanDomainService createPlanDomainService;
    private final ApiMetadataDomainService apiMetadataDomainService;
    private final DeployApiDomainService deployApiDomainService;

    public ImportCRDUseCase(
        ApiQueryService apiQueryService,
        CreateApiDomainService createApiDomainService,
        CreatePlanDomainService createPlanDomainService,
        ApiMetadataDomainService apiMetadataDomainService,
        DeployApiDomainService deployApiDomainService
    ) {
        this.apiQueryService = apiQueryService;
        this.createApiDomainService = createApiDomainService;
        this.createPlanDomainService = createPlanDomainService;
        this.apiMetadataDomainService = apiMetadataDomainService;
        this.deployApiDomainService = deployApiDomainService;
    }

    public record Output(ApiCRDStatus status) {}

    public record Input(AuditInfo auditInfo, ApiCRD crd) {}

    public Output execute(Input input) {
        var api = apiQueryService.findByEnvironmentIdAndCrossId(input.auditInfo.environmentId(), input.crd.getCrossId());
        if (api.isPresent()) {
            throw new TechnicalDomainException("Update operation not implemented yet");
        }

        var status = this.create(input);

        return new Output(status);
    }

    private ApiCRDStatus create(Input input) {
        try {
            var api = createApiDomainService.create(input.crd, input.auditInfo);
            apiMetadataDomainService.saveApiMetadata(api.getId(), input.crd.getMetadata(), input.auditInfo);

            var planNameIdMapping = input.crd
                .getPlans()
                .stream()
                .map(plan -> createPlanDomainService.create(plan.toBuilder().apiId(api.getId()).build(), input.auditInfo))
                .collect(Collectors.toMap(BasePlanEntity::getName, BasePlanEntity::getId));

            if (input.crd.getDefinitionContext().getSyncFrom().equals(DefinitionContext.ORIGIN_MANAGEMENT)) {
                deployApiDomainService.deploy(api, "Import via Kubernetes operator", input.auditInfo);
            }

            return ApiCRDStatus
                .builder()
                .id(api.getId())
                .crossId(api.getCrossId())
                .environmentId(input.auditInfo.environmentId())
                .organizationId(input.auditInfo.organizationId())
                .state(api.getLifecycleState().name())
                .plans(planNameIdMapping)
                .build();
        } catch (Exception e) {
            throw new TechnicalManagementException(e);
        }
    }
}

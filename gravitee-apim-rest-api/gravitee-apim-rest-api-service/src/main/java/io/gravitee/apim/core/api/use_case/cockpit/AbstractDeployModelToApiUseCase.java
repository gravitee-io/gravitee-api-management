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

import io.gravitee.apim.core.api.domain_service.ApiStateDomainService;
import io.gravitee.apim.core.api.domain_service.UpdateApiDomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.rest.api.model.ImportSwaggerDescriptorEntity;
import io.gravitee.rest.api.service.common.UuidString;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractDeployModelToApiUseCase {

    protected final io.gravitee.definition.model.v4.plan.Plan defaultPlanDefinition = io.gravitee.definition.model.v4.plan.Plan.builder()
        .id(UuidString.generateRandom())
        .name("Default plan")
        .mode(PlanMode.STANDARD)
        .status(PlanStatus.PUBLISHED)
        .security(PlanSecurity.builder().type("key-less").build())
        .build();

    protected final UpdateApiDomainService updateApiDomainService;
    protected final ApiStateDomainService apiStateDomainService;

    protected AbstractDeployModelToApiUseCase(UpdateApiDomainService updateApiDomainService, ApiStateDomainService apiStateDomainService) {
        this.updateApiDomainService = updateApiDomainService;
        this.apiStateDomainService = apiStateDomainService;
    }

    protected ImportSwaggerDescriptorEntity configure(DeployModelToApiUpdateUseCase.Mode mode, String swaggerDefinition) {
        var importSwaggerDescriptor = ImportSwaggerDescriptorEntity.builder().payload(swaggerDefinition);

        log.debug("API will be Documented.");
        importSwaggerDescriptor.withDocumentation(true);

        if (mode == DeployModelToApiUpdateUseCase.Mode.MOCKED || mode == DeployModelToApiUpdateUseCase.Mode.PUBLISHED) {
            log.debug("API will be Mocked.");
            importSwaggerDescriptor.withPolicyPaths(true);
            importSwaggerDescriptor.withPolicies(List.of("mock"));
        }

        return importSwaggerDescriptor.build();
    }

    protected Api manageApiState(Api api, AuditInfo audit, DeployModelToApiUpdateUseCase.Mode mode) {
        // API should be published
        if (mode == DeployModelToApiUpdateUseCase.Mode.PUBLISHED) {
            log.debug("Published v4 API.");

            api.setVisibility(Api.Visibility.PUBLIC);
            api.setApiLifecycleState(Api.ApiLifecycleState.PUBLISHED);
        }

        var updatedApi = updateApiDomainService.updateV4(api, audit);

        // API should be started
        if (mode == DeployModelToApiUpdateUseCase.Mode.MOCKED || mode == DeployModelToApiUpdateUseCase.Mode.PUBLISHED) {
            log.debug("Started v4 API.");
            apiStateDomainService.start(api, audit);
        }

        // Force API deployment if out of sync
        if (!apiStateDomainService.isSynchronized(api, audit)) {
            apiStateDomainService.deploy(api, "Managed by Gravitee Cloud", audit);
        }

        return updatedApi;
    }

    public enum Mode {
        DOCUMENTED,
        MOCKED,
        PUBLISHED,
    }

    public record Output(Api api) {}
}

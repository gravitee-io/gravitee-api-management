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

import static io.gravitee.apim.core.api.domain_service.ApiIndexerDomainService.oneShotIndexation;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.api.domain_service.CreateApiDomainService;
import io.gravitee.apim.core.api.domain_service.ValidateAgentApiDomainService;
import io.gravitee.apim.core.api.exception.ApiInvalidTypeException;
import io.gravitee.apim.core.api.model.ApiWithFlows;
import io.gravitee.apim.core.api.model.NewAgentApi;
import io.gravitee.apim.core.api.model.factory.ApiModelFactory;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerFactory;
import io.gravitee.apim.core.plan.domain_service.CreatePlanDomainService;
import io.gravitee.apim.core.plan.model.PlanWithFlows;
import io.gravitee.definition.model.v4.ApiType;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;

/**
 * Imports an agent from an exported definition: recreates the agent and its plans, reusing the create and
 * plan-creation paths. Deliberately narrow (agent + plans): the rest of the export envelope (members, metadata,
 * pages) is not replayed, so we stay free of the V4 import machinery.
 */
@UseCase
@RequiredArgsConstructor
public class ImportAgentApiUseCase {

    private final ApiPrimaryOwnerFactory apiPrimaryOwnerFactory;
    private final CreateApiDomainService createApiDomainService;
    private final CreatePlanDomainService createPlanDomainService;
    private final ValidateAgentApiDomainService validateAgentApiDomainService;

    public record Input(NewAgentApi agent, Set<PlanWithFlows> plans, AuditInfo auditInfo) {
        public Input {
            if (agent == null || agent.getType() != ApiType.AGENT) {
                throw new ApiInvalidTypeException(List.of(ApiType.AGENT));
            }
        }
    }

    public record Output(ApiWithFlows apiWithFlows) {}

    public Output execute(Input input) {
        var auditInfo = input.auditInfo();

        var primaryOwner = apiPrimaryOwnerFactory.createForNewApi(
            auditInfo.organizationId(),
            auditInfo.environmentId(),
            auditInfo.actor().userId()
        );

        var newApi = ApiModelFactory.fromNewAgentApi(input.agent(), auditInfo.environmentId());

        var created = createApiDomainService.create(
            newApi,
            primaryOwner,
            auditInfo,
            api -> validateAgentApiDomainService.validateAndSanitize(api, auditInfo.environmentId()),
            oneShotIndexation(auditInfo)
        );

        if (input.plans() != null) {
            input
                .plans()
                .forEach(plan -> {
                    // Import creates a copy: drop the exported plan id so a fresh one is minted (else it collides
                    // with the source plan on a same-environment re-import).
                    var copy = plan.toBuilder().id(null).build();
                    createPlanDomainService.create(copy, copy.getFlows(), created, auditInfo);
                });
        }

        return new Output(created);
    }
}

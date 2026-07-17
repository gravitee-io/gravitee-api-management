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
import io.gravitee.apim.core.api.domain_service.UpdateAgentApiDomainService;
import io.gravitee.apim.core.api.domain_service.ValidateAgentApiDomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.NewAgentApi;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerDomainService;
import io.gravitee.definition.model.DefinitionVersion;
import java.util.function.UnaryOperator;
import lombok.RequiredArgsConstructor;

/**
 * Updates an agent with a full payload (management fields + agent definition body). Sibling of
 * {@link CreateAgentApiUseCase}: both {@code kind=standalone} and {@code kind=workflow} are supported, the body is
 * passthrough.
 */
@UseCase
@RequiredArgsConstructor
public class UpdateAgentApiUseCase {

    private final ApiPrimaryOwnerDomainService apiPrimaryOwnerDomainService;
    private final UpdateAgentApiDomainService updateAgentApiDomainService;
    private final ValidateAgentApiDomainService validateAgentApiDomainService;

    public record Input(String apiId, NewAgentApi agent, AuditInfo auditInfo) {}

    public record Output(Api updatedApi) {}

    public Output execute(Input input) {
        var auditInfo = input.auditInfo();

        var primaryOwner = apiPrimaryOwnerDomainService.getApiPrimaryOwner(auditInfo.organizationId(), input.apiId());

        var updated = updateAgentApiDomainService.update(
            input.apiId(),
            updater(input.apiId(), input.agent(), auditInfo.environmentId()),
            auditInfo,
            primaryOwner,
            oneShotIndexation(auditInfo)
        );

        return new Output(updated);
    }

    /**
     * Merges the desired agent state into the current API: management fields plus a freshly rebuilt {@code AgentApi}
     * definition. The persisted {@code definitionVersion} stays {@code V4} (AGENT is only a REST discriminator).
     */
    UnaryOperator<Api> updater(String apiId, NewAgentApi newOne, String environmentId) {
        return currentApi -> {
            var newDefinition = newOne.toApiDefinitionBuilder().id(apiId).definitionVersion(DefinitionVersion.V4).build();
            var updatedApi = currentApi
                .toBuilder()
                .name(newOne.getName())
                .description(newOne.getDescription())
                .version(newOne.getApiVersion())
                .visibility(newOne.getVisibility() == null ? currentApi.getVisibility() : newOne.getVisibility())
                .groups(newOne.getGroups())
                .apiDefinitionValue(newDefinition)
                .build();
            return validateAgentApiDomainService.validateAndSanitize(updatedApi, environmentId);
        };
    }
}

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
package io.gravitee.apim.core.ai_workspace.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.ai_workspace.domain_service.AiWorkspaceMembershipQuery;
import io.gravitee.apim.core.ai_workspace.exception.AiWorkspaceNotFoundException;
import io.gravitee.apim.core.ai_workspace.model.AiWorkspaceMembership;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.api_product.model.ApiProductKind;
import io.gravitee.apim.core.api_product.query_service.ApiProductQueryService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Set;
import java.util.stream.Collectors;

@UseCase
public class GetMyAiWorkspaceConsumptionUseCase {

    private final AiWorkspaceMembershipQuery memberships;
    private final ApiProductQueryService apiProductQueryService;

    public GetMyAiWorkspaceConsumptionUseCase(AiWorkspaceMembershipQuery memberships, ApiProductQueryService apiProductQueryService) {
        this.memberships = memberships;
        this.apiProductQueryService = apiProductQueryService;
    }

    public record Input(ExecutionContext executionContext, Set<String> applicationIds, String aiWorkspaceId) {}

    public record Output(String applicationId, Set<String> apiIds) {}

    public Output execute(Input input) {
        String id = input.aiWorkspaceId();
        if (id == null || id.isBlank()) {
            throw new AiWorkspaceNotFoundException(id);
        }
        AiWorkspaceMembership membership = memberships
            .find(input.executionContext(), input.applicationIds(), Set.of(id))
            .stream()
            .filter(item -> id.equals(item.apiProductId()))
            .findFirst()
            .orElseThrow(() -> new AiWorkspaceNotFoundException(id));
        ApiProduct product = apiProductQueryService
            .findByEnvironmentIdAndIdIn(input.executionContext().getEnvironmentId(), Set.of(id))
            .stream()
            .filter(item -> item.getKind() == ApiProductKind.AI_WORKSPACE)
            .findFirst()
            .orElseThrow(() -> new AiWorkspaceNotFoundException(id));
        return new Output(membership.applicationId(), apiIds(product));
    }

    private static Set<String> apiIds(ApiProduct product) {
        if (product.getApiIds() == null) {
            return Set.of();
        }
        return product
            .getApiIds()
            .stream()
            .filter(apiId -> apiId != null && !apiId.isBlank())
            .collect(Collectors.toSet());
    }
}

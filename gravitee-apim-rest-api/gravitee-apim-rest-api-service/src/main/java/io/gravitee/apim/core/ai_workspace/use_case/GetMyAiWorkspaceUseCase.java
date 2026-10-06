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
import io.gravitee.apim.core.ai_workspace.domain_service.AiWorkspaceBudgetReader;
import io.gravitee.apim.core.ai_workspace.domain_service.AiWorkspaceEndpointReader;
import io.gravitee.apim.core.ai_workspace.domain_service.AiWorkspaceMembershipQuery;
import io.gravitee.apim.core.ai_workspace.exception.AiWorkspaceNotFoundException;
import io.gravitee.apim.core.ai_workspace.model.AiWorkspaceBudget;
import io.gravitee.apim.core.ai_workspace.model.AiWorkspaceDetails;
import io.gravitee.apim.core.ai_workspace.model.AiWorkspaceKey;
import io.gravitee.apim.core.ai_workspace.model.AiWorkspaceMembership;
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api_key.model.ApiKeyEntity;
import io.gravitee.apim.core.api_key.query_service.ApiKeyQueryService;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.api_product.model.ApiProductKind;
import io.gravitee.apim.core.api_product.query_service.ApiProductQueryService;
import io.gravitee.apim.core.flow.crud_service.FlowCrudService;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@UseCase
public class GetMyAiWorkspaceUseCase {

    private final AiWorkspaceMembershipQuery memberships;
    private final ApiProductQueryService apiProductQueryService;
    private final FlowCrudService flowCrudService;
    private final ApiCrudService apiCrudService;
    private final ApiKeyQueryService apiKeyQueryService;

    public GetMyAiWorkspaceUseCase(
        AiWorkspaceMembershipQuery memberships,
        ApiProductQueryService apiProductQueryService,
        FlowCrudService flowCrudService,
        ApiCrudService apiCrudService,
        ApiKeyQueryService apiKeyQueryService
    ) {
        this.memberships = memberships;
        this.apiProductQueryService = apiProductQueryService;
        this.flowCrudService = flowCrudService;
        this.apiCrudService = apiCrudService;
        this.apiKeyQueryService = apiKeyQueryService;
    }

    public record Input(ExecutionContext executionContext, Set<String> applicationIds, String aiWorkspaceId) {}

    public record Output(AiWorkspaceDetails details) {}

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
        Map<String, List<Flow>> flowsByPlan = membership.planId() == null || membership.planId().isBlank()
            ? Map.of()
            : flowCrudService.getPlanV4Flows(Set.of(membership.planId()));
        List<Api> apis = product.getApiIds() == null
            ? List.of()
            : product.getApiIds().stream().map(apiCrudService::findById).flatMap(Optional::stream).toList();
        return new Output(
            new AiWorkspaceDetails(
                product.getId(),
                product.getName(),
                product.getDescription(),
                budget(membership, flowsByPlan),
                AiWorkspaceEndpointReader.read(input.executionContext().getEnvironmentId(), apis),
                key(membership.subscriptionId())
            )
        );
    }

    private AiWorkspaceKey key(String subscriptionId) {
        if (subscriptionId == null || subscriptionId.isBlank()) {
            return null;
        }
        return apiKeyQueryService
            .findBySubscription(subscriptionId)
            .filter(candidate -> candidate.getKey() != null && !candidate.getKey().isBlank())
            .filter(candidate -> !candidate.isRevoked() && !candidate.isExpired())
            .max(Comparator.comparing(ApiKeyEntity::getCreatedAt, Comparator.nullsFirst(Comparator.naturalOrder())))
            .map(GetMyAiWorkspaceUseCase::toKey)
            .orElse(null);
    }

    private static AiWorkspaceKey toKey(ApiKeyEntity entity) {
        OffsetDateTime createdAt = entity.getCreatedAt() == null ? null : entity.getCreatedAt().toOffsetDateTime();
        return new AiWorkspaceKey(entity.getKey(), entity.isPaused() ? "PAUSED" : "ACTIVE", createdAt);
    }

    private static AiWorkspaceBudget budget(AiWorkspaceMembership membership, Map<String, List<Flow>> flowsByPlan) {
        if (membership.planId() == null) {
            return null;
        }
        return AiWorkspaceBudgetReader.read(flowsByPlan.getOrDefault(membership.planId(), List.of()), membership.planId()).orElse(null);
    }
}

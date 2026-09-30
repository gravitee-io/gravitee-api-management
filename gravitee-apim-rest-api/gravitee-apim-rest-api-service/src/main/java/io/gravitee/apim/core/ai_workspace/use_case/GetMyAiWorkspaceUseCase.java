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
import io.gravitee.apim.core.ai_workspace.domain_service.AiWorkspaceModelsReader;
import io.gravitee.apim.core.ai_workspace.exception.AiWorkspaceNotFoundException;
import io.gravitee.apim.core.ai_workspace.model.AiWorkspaceDetails;
import io.gravitee.apim.core.ai_workspace.model.AiWorkspaceKey;
import io.gravitee.apim.core.ai_workspace.model.AiWorkspaceMembership;
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api_key.query_service.ApiKeyQueryService;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.api_product.model.ApiProductKind;
import io.gravitee.apim.core.api_product.query_service.ApiProductQueryService;
import io.gravitee.apim.core.flow.crud_service.FlowCrudService;
import io.gravitee.apim.core.subscription.query_service.SubscriptionSearchQueryService;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@UseCase
public class GetMyAiWorkspaceUseCase {

    static final String KEY_STATUS_ACTIVE = "ACTIVE";

    private final AiWorkspaceMembershipQuery memberships;
    private final ApiProductQueryService apiProductQueryService;
    private final FlowCrudService flowCrudService;
    private final ApiCrudService apiCrudService;
    private final ApiKeyQueryService apiKeyQueryService;

    public GetMyAiWorkspaceUseCase(
        SubscriptionSearchQueryService subscriptionSearchQueryService,
        ApiProductQueryService apiProductQueryService,
        FlowCrudService flowCrudService,
        ApiCrudService apiCrudService,
        ApiKeyQueryService apiKeyQueryService
    ) {
        this.memberships = new AiWorkspaceMembershipQuery(subscriptionSearchQueryService);
        this.apiProductQueryService = apiProductQueryService;
        this.flowCrudService = flowCrudService;
        this.apiCrudService = apiCrudService;
        this.apiKeyQueryService = apiKeyQueryService;
    }

    public record Input(ExecutionContext executionContext, Set<String> applicationIds, String aiWorkspaceId) {}

    public record Output(AiWorkspaceDetails details) {}

    public Output execute(Input input) {
        AiWorkspaceMembership membership = memberships
            .findOne(input.executionContext(), input.applicationIds(), input.aiWorkspaceId())
            .orElseThrow(() -> new AiWorkspaceNotFoundException(input.aiWorkspaceId()));

        ApiProduct product = apiProductQueryService
            .findById(membership.apiProductId())
            .filter(candidate -> input.executionContext().getEnvironmentId().equals(candidate.getEnvironmentId()))
            .filter(candidate -> candidate.getKind() == ApiProductKind.AI_WORKSPACE)
            .orElseThrow(() -> new AiWorkspaceNotFoundException(input.aiWorkspaceId()));

        io.gravitee.definition.model.v4.Api proxy = proxyDefinition(product);
        return new Output(
            new AiWorkspaceDetails(
                product.getId(),
                product.getName(),
                product.getDescription(),
                membership.planId() == null
                    ? null
                    : AiWorkspaceBudgetReader.read(flowCrudService.getPlanV4Flows(membership.planId())).orElse(null),
                proxy == null ? null : AiWorkspaceEndpointReader.read(proxy).orElse(null),
                key(membership.subscriptionId()),
                proxy == null ? List.of() : AiWorkspaceModelsReader.read(proxy)
            )
        );
    }

    private io.gravitee.definition.model.v4.Api proxyDefinition(ApiProduct product) {
        if (product.getApiIds() == null) {
            return null;
        }
        for (String apiId : product.getApiIds()) {
            Optional<Api> api = findApi(apiId);
            if (api.isPresent() && api.get().getType() == ApiType.LLM_PROXY) {
                return api.get().getApiDefinitionHttpV4();
            }
        }
        return null;
    }

    private Optional<Api> findApi(String apiId) {
        try {
            return Optional.ofNullable(apiCrudService.get(apiId));
        } catch (ApiNotFoundException e) {
            return Optional.empty();
        }
    }

    private AiWorkspaceKey key(String subscriptionId) {
        return apiKeyQueryService
            .findBySubscription(subscriptionId)
            .filter(candidate -> !candidate.isRevoked() && !candidate.isExpired() && !candidate.isPaused())
            .filter(candidate -> candidate.getKey() != null && !candidate.getKey().isBlank())
            .findFirst()
            .map(candidate -> new AiWorkspaceKey(candidate.getKey(), KEY_STATUS_ACTIVE, toInstant(candidate.getCreatedAt())))
            .orElse(null);
    }

    private static Instant toInstant(ZonedDateTime createdAt) {
        return createdAt == null ? null : createdAt.toInstant();
    }
}

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
import io.gravitee.apim.core.ai_workspace.domain_service.AiWorkspaceMembershipQuery;
import io.gravitee.apim.core.ai_workspace.model.AiWorkspaceBudget;
import io.gravitee.apim.core.ai_workspace.model.AiWorkspaceMembership;
import io.gravitee.apim.core.ai_workspace.model.AiWorkspacePage;
import io.gravitee.apim.core.ai_workspace.model.AiWorkspaceSummary;
import io.gravitee.apim.core.api_product.model.ApiProductKind;
import io.gravitee.apim.core.api_product.query_service.ApiProductQueryService;
import io.gravitee.apim.core.flow.crud_service.FlowCrudService;
import io.gravitee.apim.core.subscription.query_service.SubscriptionSearchQueryService;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@UseCase
public class ListMyAiWorkspacesUseCase {

    private static final Comparator<String> NULLS_LAST = Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER);

    private final AiWorkspaceMembershipQuery memberships;
    private final ApiProductQueryService apiProductQueryService;
    private final FlowCrudService flowCrudService;

    public ListMyAiWorkspacesUseCase(
        SubscriptionSearchQueryService subscriptionSearchQueryService,
        ApiProductQueryService apiProductQueryService,
        FlowCrudService flowCrudService
    ) {
        this.memberships = new AiWorkspaceMembershipQuery(subscriptionSearchQueryService);
        this.apiProductQueryService = apiProductQueryService;
        this.flowCrudService = flowCrudService;
    }

    public record Input(ExecutionContext executionContext, Set<String> applicationIds, String name, int page, int size) {}

    public record Output(AiWorkspacePage page) {}

    public Output execute(Input input) {
        int page = Math.max(input.page(), 1);
        int size = input.size() < 1 ? 10 : Math.min(input.size(), 100);
        String name = input.name() == null ? "" : input.name().trim().toLowerCase(Locale.ROOT);

        List<AiWorkspaceMembership> memberships = this.memberships.find(input.executionContext(), input.applicationIds());
        Set<String> planIds = memberships
            .stream()
            .map(AiWorkspaceMembership::planId)
            .filter(planId -> planId != null && !planId.isBlank())
            .collect(Collectors.toSet());
        Map<String, List<Flow>> flowsByPlan = planIds.isEmpty() ? Map.of() : flowCrudService.getPlanV4Flows(planIds);

        List<AiWorkspaceSummary> matched = memberships
            .stream()
            .map(membership -> toSummary(input.executionContext(), membership, flowsByPlan))
            .flatMap(java.util.Optional::stream)
            .filter(summary -> name.isEmpty() || (summary.name() != null && summary.name().toLowerCase(Locale.ROOT).contains(name)))
            .sorted(Comparator.comparing(AiWorkspaceSummary::name, NULLS_LAST).thenComparing(AiWorkspaceSummary::id, NULLS_LAST))
            .toList();

        int from = Math.min((page - 1) * size, matched.size());
        int to = Math.min(from + size, matched.size());
        return new Output(new AiWorkspacePage(List.copyOf(matched.subList(from, to)), page, size, matched.size()));
    }

    private java.util.Optional<AiWorkspaceSummary> toSummary(
        ExecutionContext executionContext,
        AiWorkspaceMembership membership,
        Map<String, List<Flow>> flowsByPlan
    ) {
        return apiProductQueryService
            .findById(membership.apiProductId())
            .filter(product -> executionContext.getEnvironmentId().equals(product.getEnvironmentId()))
            .filter(product -> product.getKind() == ApiProductKind.AI_WORKSPACE)
            .map(product ->
                new AiWorkspaceSummary(product.getId(), product.getName(), product.getDescription(), budget(membership, flowsByPlan))
            );
    }

    private static AiWorkspaceBudget budget(AiWorkspaceMembership membership, Map<String, List<Flow>> flowsByPlan) {
        if (membership.planId() == null) {
            return null;
        }
        return AiWorkspaceBudgetReader.read(flowsByPlan.getOrDefault(membership.planId(), List.of())).orElse(null);
    }
}

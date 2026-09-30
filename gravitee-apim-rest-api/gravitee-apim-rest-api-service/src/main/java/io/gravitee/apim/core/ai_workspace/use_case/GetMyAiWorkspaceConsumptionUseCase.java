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
import io.gravitee.apim.core.ai_workspace.domain_service.AiWorkspaceConsumptionReader;
import io.gravitee.apim.core.ai_workspace.domain_service.AiWorkspaceMembershipQuery;
import io.gravitee.apim.core.ai_workspace.exception.AiWorkspaceNotFoundException;
import io.gravitee.apim.core.ai_workspace.model.AiWorkspaceConsumption;
import io.gravitee.apim.core.ai_workspace.model.AiWorkspaceMembership;
import io.gravitee.apim.core.analytics_engine.model.MetricSpec;
import io.gravitee.apim.core.analytics_engine.query_service.AnalyticsEngineQueryService;
import io.gravitee.apim.core.api_product.model.ApiProductKind;
import io.gravitee.apim.core.api_product.query_service.ApiProductQueryService;
import io.gravitee.apim.core.subscription.query_service.SubscriptionSearchQueryService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.List;
import java.util.Set;

@UseCase
public class GetMyAiWorkspaceConsumptionUseCase {

    private final AiWorkspaceMembershipQuery memberships;
    private final ApiProductQueryService apiProductQueryService;
    private final AiWorkspaceConsumptionReader consumption;

    public GetMyAiWorkspaceConsumptionUseCase(
        SubscriptionSearchQueryService subscriptionSearchQueryService,
        ApiProductQueryService apiProductQueryService,
        List<AnalyticsEngineQueryService> analyticsEngineQueryServices
    ) {
        this.memberships = new AiWorkspaceMembershipQuery(subscriptionSearchQueryService);
        this.apiProductQueryService = apiProductQueryService;
        this.consumption = new AiWorkspaceConsumptionReader(httpAnalytics(analyticsEngineQueryServices));
    }

    /**
     * LLM usage is stored with the HTTP data-plane metrics. Several analytics services exist; pick the one that
     * serves those metrics instead of asking Spring to choose a single implementation.
     */
    private static AnalyticsEngineQueryService httpAnalytics(List<AnalyticsEngineQueryService> services) {
        if (services.size() == 1) {
            return services.get(0);
        }
        return services
            .stream()
            .filter(service -> service.metrics() != null && service.metrics().contains(MetricSpec.Name.LLM_PROMPT_TOTAL_TOKEN))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No analytics service serves LLM consumption metrics"));
    }

    public record Input(ExecutionContext executionContext, Set<String> applicationIds, String aiWorkspaceId) {}

    public record Output(AiWorkspaceConsumption consumption) {}

    public Output execute(Input input) {
        AiWorkspaceMembership membership = memberships
            .findOne(input.executionContext(), input.applicationIds(), input.aiWorkspaceId())
            .orElseThrow(() -> new AiWorkspaceNotFoundException(input.aiWorkspaceId()));

        apiProductQueryService
            .findById(membership.apiProductId())
            .filter(product -> input.executionContext().getEnvironmentId().equals(product.getEnvironmentId()))
            .filter(product -> product.getKind() == ApiProductKind.AI_WORKSPACE)
            .orElseThrow(() -> new AiWorkspaceNotFoundException(input.aiWorkspaceId()));

        return new Output(consumption.read(input.executionContext(), membership.apiProductId(), membership.applicationId()));
    }
}

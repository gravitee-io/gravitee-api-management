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

import static org.assertj.core.api.Assertions.assertThat;

import inmemory.ApiProductQueryServiceInMemory;
import inmemory.FlowCrudServiceInMemory;
import inmemory.SubscriptionSearchQueryServiceInMemory;
import io.gravitee.apim.core.ai_workspace.domain_service.AiWorkspaceMembershipQuery;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.api_product.model.ApiProductKind;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.SubscriptionStatus;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ListMyAiWorkspacesUseCaseTest {

    private static final String ENV = "DEFAULT";
    private static final ExecutionContext CONTEXT = new ExecutionContext("org", ENV);

    private final SubscriptionSearchQueryServiceInMemory subscriptions = new SubscriptionSearchQueryServiceInMemory();
    private final ApiProductQueryServiceInMemory products = new ApiProductQueryServiceInMemory();
    private final FlowCrudServiceInMemory flows = new FlowCrudServiceInMemory();
    private final ListMyAiWorkspacesUseCase useCase = new ListMyAiWorkspacesUseCase(
        new AiWorkspaceMembershipQuery(subscriptions),
        products,
        flows
    );

    @BeforeEach
    void setUp() {
        subscriptions.reset();
        products.reset();
        flows.reset();
    }

    @Test
    void lists_mapped_workspaces_with_budget() {
        products.initWith(
            List.of(
                product("ws-1", "Alpha Workspace", ENV, ApiProductKind.AI_WORKSPACE),
                product("ws-2", "Beta", ENV, ApiProductKind.AI_WORKSPACE),
                product("other", "Alpha Catalog", ENV, null),
                product("elsewhere", "Alpha Elsewhere", "OTHER", ApiProductKind.AI_WORKSPACE)
            )
        );
        flows.savePlanFlows("plan-1", List.of(budgetFlow(10_000_000, 1440)));
        subscriptions.initWith(
            List.of(
                subscription("sub-new", "app-1", "ws-1", "plan-1", SubscriptionStatus.ACCEPTED, new Date(2_000)),
                subscription("sub-old", "app-1", "ws-1", "plan-1", SubscriptionStatus.ACCEPTED, new Date(1_000)),
                subscription("sub-beta", "app-1", "ws-2", "missing-plan", SubscriptionStatus.ACCEPTED, new Date(1_000)),
                subscription("sub-pending", "app-1", "ws-1", "plan-1", SubscriptionStatus.PENDING, new Date(1_000)),
                subscription("sub-catalog", "app-1", "other", "plan-1", SubscriptionStatus.ACCEPTED, new Date(1_000)),
                subscription("sub-env", "app-1", "elsewhere", "plan-1", SubscriptionStatus.ACCEPTED, new Date(1_000))
            )
        );

        var workspaces = useCase.execute(new ListMyAiWorkspacesUseCase.Input(CONTEXT, Set.of("app-1"))).workspaces();

        assertThat(workspaces)
            .extracting(summary -> summary.id())
            .containsExactly("ws-1", "ws-2");
        assertThat(workspaces.get(0).budget().amount()).isEqualByComparingTo(new BigDecimal("10.00"));
        assertThat(workspaces.get(0).budget().period()).isEqualTo("DAY");
        assertThat(workspaces.get(1).budget()).isNull();
    }

    @Test
    void ignores_products_that_are_not_ai_workspaces() {
        products.initWith(List.of(product("other", "Catalog", ENV, null)));
        subscriptions.initWith(
            List.of(subscription("sub-catalog", "app-1", "other", "plan-1", SubscriptionStatus.ACCEPTED, new Date(1_000)))
        );

        var workspaces = useCase.execute(new ListMyAiWorkspacesUseCase.Input(CONTEXT, Set.of("app-1"))).workspaces();

        assertThat(workspaces).isEmpty();
    }

    @Test
    void does_not_list_another_callers_workspace() {
        products.initWith(List.of(product("ws-2", "Beta", ENV, ApiProductKind.AI_WORKSPACE)));
        subscriptions.initWith(
            List.of(subscription("sub-other-user", "app-2", "ws-2", "plan-1", SubscriptionStatus.ACCEPTED, new Date(1_000)))
        );

        var workspaces = useCase.execute(new ListMyAiWorkspacesUseCase.Input(CONTEXT, Set.of("app-1"))).workspaces();

        assertThat(workspaces).isEmpty();
    }

    @Test
    void reads_workspaces_past_the_first_subscription_page() {
        products.initWith(
            List.of(product("ws-1", "Alpha", ENV, ApiProductKind.AI_WORKSPACE), product("ws-2", "Beta", ENV, ApiProductKind.AI_WORKSPACE))
        );
        List<SubscriptionEntity> page = new ArrayList<>();
        for (int index = 0; index < 500; index++) {
            page.add(subscription("sub-" + index, "app-1", "ws-1", "plan-1", SubscriptionStatus.ACCEPTED, new Date(index)));
        }
        page.add(subscription("sub-last", "app-1", "ws-2", "plan-1", SubscriptionStatus.ACCEPTED, new Date(501)));
        subscriptions.initWith(page);

        var workspaces = useCase.execute(new ListMyAiWorkspacesUseCase.Input(CONTEXT, Set.of("app-1"))).workspaces();

        assertThat(workspaces)
            .extracting(summary -> summary.id())
            .containsExactly("ws-1", "ws-2");
    }

    @Test
    void empty_when_the_caller_has_no_applications() {
        var workspaces = useCase.execute(new ListMyAiWorkspacesUseCase.Input(CONTEXT, Set.of())).workspaces();

        assertThat(workspaces).isEmpty();
    }

    private static ApiProduct product(String id, String name, String environmentId, ApiProductKind kind) {
        return ApiProduct.builder().id(id).name(name).description(name + " description").environmentId(environmentId).kind(kind).build();
    }

    private static SubscriptionEntity subscription(
        String id,
        String applicationId,
        String apiProductId,
        String planId,
        SubscriptionStatus status,
        Date createdAt
    ) {
        return SubscriptionEntity.builder()
            .id(id)
            .application(applicationId)
            .referenceId(apiProductId)
            .referenceType("API_PRODUCT")
            .plan(planId)
            .status(status)
            .createdAt(createdAt)
            .build();
    }

    private static Flow budgetFlow(long microDollars, int minutes) {
        Step step = new Step();
        step.setPolicy("cost-ratelimit");
        step.setConfiguration("{\"rate\":{\"limit\":" + microDollars + ",\"periodTime\":" + minutes + "}}");
        Flow flow = new Flow();
        flow.setRequest(List.of(step));
        return flow;
    }
}

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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import inmemory.ApiCrudServiceInMemory;
import inmemory.ApiProductQueryServiceInMemory;
import inmemory.FlowCrudServiceInMemory;
import inmemory.SubscriptionSearchQueryServiceInMemory;
import io.gravitee.apim.core.ai_workspace.domain_service.AiWorkspaceMembershipQuery;
import io.gravitee.apim.core.ai_workspace.exception.AiWorkspaceNotFoundException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.api_product.model.ApiProductKind;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.listener.http.Path;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.SubscriptionStatus;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetMyAiWorkspaceUseCaseTest {

    private static final String ENV = "DEFAULT";
    private static final ExecutionContext CONTEXT = new ExecutionContext("org", ENV);

    private final SubscriptionSearchQueryServiceInMemory subscriptions = new SubscriptionSearchQueryServiceInMemory();
    private final ApiProductQueryServiceInMemory products = new ApiProductQueryServiceInMemory();
    private final FlowCrudServiceInMemory flows = new FlowCrudServiceInMemory();
    private final ApiCrudServiceInMemory apis = new ApiCrudServiceInMemory();
    private final GetMyAiWorkspaceUseCase useCase = new GetMyAiWorkspaceUseCase(
        new AiWorkspaceMembershipQuery(subscriptions),
        products,
        flows,
        apis
    );

    @BeforeEach
    void setUp() {
        subscriptions.reset();
        products.reset();
        flows.reset();
        apis.reset();
    }

    @Test
    void returns_the_workspace_budget_and_endpoint() {
        products.initWith(List.of(product("ws-1", "Alpha", ApiProductKind.AI_WORKSPACE, Set.of("api-1"))));
        subscriptions.initWith(List.of(subscription("app-1", "ws-1")));
        flows.savePlanFlows("plan-1", List.of(budgetFlow()));
        apis.initWith(List.of(proxy("api-1", ENV, "/alpha/")));

        var details = useCase.execute(new GetMyAiWorkspaceUseCase.Input(CONTEXT, Set.of("app-1"), "ws-1")).details();

        assertThat(details.id()).isEqualTo("ws-1");
        assertThat(details.name()).isEqualTo("Alpha");
        assertThat(details.description()).isEqualTo("desc");
        assertThat(details.budget().amount()).isEqualByComparingTo(new BigDecimal("5.00"));
        assertThat(details.budget().period()).isEqualTo("DAY");
        assertThat(details.endpointUrl()).isEqualTo("/alpha/");
    }

    @Test
    void returns_the_workspace_when_it_has_no_proxy() {
        products.initWith(List.of(product("ws-1", "Alpha", ApiProductKind.AI_WORKSPACE, Set.of())));
        subscriptions.initWith(List.of(subscription("app-1", "ws-1")));

        var details = useCase.execute(new GetMyAiWorkspaceUseCase.Input(CONTEXT, Set.of("app-1"), "ws-1")).details();

        assertThat(details.endpointUrl()).isNull();
        assertThat(details.budget()).isNull();
    }

    @Test
    void rejects_an_unknown_id_a_non_workspace_and_another_callers_workspace() {
        products.initWith(
            List.of(product("catalog", "Catalog", null, Set.of()), product("ws-2", "Beta", ApiProductKind.AI_WORKSPACE, Set.of()))
        );
        subscriptions.initWith(List.of(subscription("app-1", "catalog"), subscription("app-2", "ws-2").toBuilder().id("sub-2").build()));

        assertThatThrownBy(() -> useCase.execute(new GetMyAiWorkspaceUseCase.Input(CONTEXT, Set.of("app-1"), " "))).isInstanceOf(
            AiWorkspaceNotFoundException.class
        );
        assertThatThrownBy(() -> useCase.execute(new GetMyAiWorkspaceUseCase.Input(CONTEXT, Set.of("app-1"), "missing"))).isInstanceOf(
            AiWorkspaceNotFoundException.class
        );
        assertThatThrownBy(() -> useCase.execute(new GetMyAiWorkspaceUseCase.Input(CONTEXT, Set.of("app-1"), "catalog"))).isInstanceOf(
            AiWorkspaceNotFoundException.class
        );
        assertThatThrownBy(() -> useCase.execute(new GetMyAiWorkspaceUseCase.Input(CONTEXT, Set.of("app-1"), "ws-2"))).isInstanceOf(
            AiWorkspaceNotFoundException.class
        );
    }

    private static ApiProduct product(String id, String name, ApiProductKind kind, Set<String> apiIds) {
        return ApiProduct.builder().id(id).name(name).description("desc").environmentId(ENV).kind(kind).apiIds(apiIds).build();
    }

    private static SubscriptionEntity subscription(String applicationId, String apiProductId) {
        return SubscriptionEntity.builder()
            .id("sub-1")
            .application(applicationId)
            .referenceId(apiProductId)
            .referenceType("API_PRODUCT")
            .plan("plan-1")
            .status(SubscriptionStatus.ACCEPTED)
            .createdAt(new Date(1_000))
            .build();
    }

    private static Flow budgetFlow() {
        Step step = new Step();
        step.setPolicy("cost-ratelimit");
        step.setConfiguration("{\"rate\":{\"limit\":5000000,\"periodTime\":1440}}");
        Flow flow = new Flow();
        flow.setRequest(List.of(step));
        return flow;
    }

    private static Api proxy(String id, String environmentId, String path) {
        return Api.builder()
            .id(id)
            .environmentId(environmentId)
            .type(ApiType.LLM_PROXY)
            .apiDefinitionHttpV4(
                io.gravitee.definition.model.v4.Api.builder()
                    .listeners(List.of(HttpListener.builder().paths(List.of(Path.builder().path(path).build())).build()))
                    .build()
            )
            .build();
    }
}

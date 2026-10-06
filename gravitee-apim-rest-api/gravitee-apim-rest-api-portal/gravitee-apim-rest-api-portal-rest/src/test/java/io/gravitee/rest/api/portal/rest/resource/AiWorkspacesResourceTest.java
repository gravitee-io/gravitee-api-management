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
package io.gravitee.rest.api.portal.rest.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;

import inmemory.ApiProductQueryServiceInMemory;
import inmemory.FlowCrudServiceInMemory;
import inmemory.SubscriptionSearchQueryServiceInMemory;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.api_product.model.ApiProductKind;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.SubscriptionStatus;
import io.gravitee.rest.api.model.application.ApplicationListItem;
import io.gravitee.rest.api.portal.rest.model.AiWorkspaceBudget;
import io.gravitee.rest.api.portal.rest.model.AiWorkspacesResponse;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.core.Response;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AiWorkspacesResourceTest extends AbstractResourceTest {

    @Autowired
    private SubscriptionSearchQueryServiceInMemory subscriptions;

    @Autowired
    private ApiProductQueryServiceInMemory products;

    @Autowired
    private FlowCrudServiceInMemory flows;

    @Override
    protected String contextPath() {
        return "ai-workspaces/";
    }

    @BeforeEach
    void setUp() {
        resetAllMocks();
        GraviteeContext.setCurrentEnvironment("DEFAULT");
        doReturn(Set.of(application("app-1", USER_NAME))).when(applicationService).findByUser(any(ExecutionContext.class), eq(USER_NAME));
    }

    @AfterEach
    void tearDown() {
        subscriptions.reset();
        products.reset();
        flows.reset();
        GraviteeContext.cleanContext();
    }

    @Test
    void lists_the_callers_workspace_with_its_budget() {
        products.initWith(List.of(workspace("ws-1", "Alpha", ApiProductKind.AI_WORKSPACE)));
        subscriptions.initWith(List.of(subscription("app-1", "ws-1")));
        flows.savePlanFlows("plan-1", List.of(budgetFlow()));

        Response response = target().request().get();

        assertThat(response.getStatus()).isEqualTo(200);
        AiWorkspacesResponse body = response.readEntity(AiWorkspacesResponse.class);
        assertThat(body.getData())
            .extracting(item -> item.getName())
            .containsExactly("Alpha");
        assertThat(body.getData().get(0).getBudget().getAmount()).isEqualTo(5.0);
        assertThat(body.getData().get(0).getBudget().getPeriod().getValue()).isEqualTo("DAY");
        assertThat(pagination(body)).containsEntry("total", 1);
    }

    @Test
    void lists_a_workspace_when_the_budget_cannot_be_read() {
        products.initWith(List.of(workspace("ws-1", "Alpha", ApiProductKind.AI_WORKSPACE)));
        subscriptions.initWith(List.of(subscription("app-1", "ws-1")));

        Response response = target().request().get();

        assertThat(response.getStatus()).isEqualTo(200);
        AiWorkspacesResponse body = response.readEntity(AiWorkspacesResponse.class);
        assertThat(body.getData()).hasSize(1);
        assertThat(body.getData().get(0).getBudget()).isNull();
    }

    @Test
    void ignores_products_that_are_not_ai_workspaces() {
        products.initWith(List.of(workspace("catalog", "Catalog", null)));
        subscriptions.initWith(List.of(subscription("app-1", "catalog")));

        Response response = target().request().get();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.readEntity(AiWorkspacesResponse.class).getData()).isEmpty();
    }

    @Test
    void does_not_list_a_workspace_when_the_caller_is_only_a_member() {
        products.initWith(List.of(workspace("ws-1", "Alpha", ApiProductKind.AI_WORKSPACE)));
        subscriptions.initWith(List.of(subscription("app-1", "ws-1")));
        doReturn(Set.of(application("app-1", "someone-else")))
            .when(applicationService)
            .findByUser(any(ExecutionContext.class), eq(USER_NAME));

        Response response = target().request().get();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.readEntity(AiWorkspacesResponse.class).getData()).isEmpty();
    }

    @Test
    void does_not_list_another_callers_workspace() {
        products.initWith(List.of(workspace("ws-2", "Beta", ApiProductKind.AI_WORKSPACE)));
        subscriptions.initWith(List.of(subscription("app-2", "ws-2")));

        Response response = target().request().get();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.readEntity(AiWorkspacesResponse.class).getData()).isEmpty();
    }

    @Test
    void empty_page_when_the_caller_has_no_workspace() {
        Response response = target().request().get();

        assertThat(response.getStatus()).isEqualTo(200);
        AiWorkspacesResponse body = response.readEntity(AiWorkspacesResponse.class);
        assertThat(body.getData()).isEmpty();
    }

    @Test
    void filters_by_name_before_paging() {
        products.initWith(
            List.of(
                workspace("ws-b", "Bravo", ApiProductKind.AI_WORKSPACE),
                workspace("ws-a", "Alpha", ApiProductKind.AI_WORKSPACE),
                workspace("ws-c", "Alpine", ApiProductKind.AI_WORKSPACE)
            )
        );
        subscriptions.initWith(
            List.of(
                subscription("app-1", "ws-b"),
                subscription("app-1", "ws-a").toBuilder().id("sub-2").referenceId("ws-a").build(),
                subscription("app-1", "ws-c").toBuilder().id("sub-3").referenceId("ws-c").build()
            )
        );

        Response response = target().queryParam("name", "alp").queryParam("page", 1).queryParam("size", 1).request().get();

        assertThat(response.getStatus()).isEqualTo(200);
        AiWorkspacesResponse body = response.readEntity(AiWorkspacesResponse.class);
        assertThat(body.getData())
            .extracting(item -> item.getName())
            .containsExactly("Alpha");
        assertThat(pagination(body)).containsEntry("total", 2).containsEntry("current_page", 1);
    }

    @Test
    void blank_name_returns_the_full_list_and_an_unknown_name_returns_an_empty_page() {
        products.initWith(
            List.of(workspace("ws-a", "Alpha", ApiProductKind.AI_WORKSPACE), workspace("ws-b", "Bravo", ApiProductKind.AI_WORKSPACE))
        );
        subscriptions.initWith(
            List.of(subscription("app-1", "ws-a"), subscription("app-1", "ws-b").toBuilder().id("sub-2").referenceId("ws-b").build())
        );

        Response blank = target().queryParam("name", " ").request().get();
        assertThat(blank.readEntity(AiWorkspacesResponse.class).getData()).hasSize(2);

        Response none = target().queryParam("name", "zzzz").request().get();
        assertThat(none.getStatus()).isEqualTo(200);
        assertThat(none.readEntity(AiWorkspacesResponse.class).getData()).isEmpty();
    }

    @Test
    void pages_the_list() {
        products.initWith(
            List.of(workspace("ws-b", "Bravo", ApiProductKind.AI_WORKSPACE), workspace("ws-a", "Alpha", ApiProductKind.AI_WORKSPACE))
        );
        subscriptions.initWith(
            List.of(subscription("app-1", "ws-b"), subscription("app-1", "ws-a").toBuilder().id("sub-2").referenceId("ws-a").build())
        );
        flows.savePlanFlows("plan-1", List.of(budgetFlow()));

        Response response = target().queryParam("page", 2).queryParam("size", 1).request().get();

        assertThat(response.getStatus()).isEqualTo(200);
        AiWorkspacesResponse body = response.readEntity(AiWorkspacesResponse.class);
        assertThat(body.getData())
            .extracting(item -> item.getName())
            .containsExactly("Bravo");
        assertThat(pagination(body)).containsEntry("total", 2).containsEntry("current_page", 2);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> pagination(AiWorkspacesResponse body) {
        return (Map<String, Object>) body.getMetadata().get("pagination");
    }

    private static ApplicationListItem application(String id, String ownerId) {
        return ApplicationListItem.builder().id(id).primaryOwner(PrimaryOwnerEntity.builder().id(ownerId).type("USER").build()).build();
    }

    private static ApiProduct workspace(String id, String name, ApiProductKind kind) {
        return ApiProduct.builder().id(id).name(name).description("desc").environmentId("DEFAULT").kind(kind).build();
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
}

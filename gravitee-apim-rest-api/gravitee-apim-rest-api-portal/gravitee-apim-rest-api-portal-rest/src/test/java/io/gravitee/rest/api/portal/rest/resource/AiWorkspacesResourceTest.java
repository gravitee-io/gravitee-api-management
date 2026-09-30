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

import inmemory.ApiCrudServiceInMemory;
import inmemory.ApiKeyQueryServiceInMemory;
import inmemory.ApiProductQueryServiceInMemory;
import inmemory.FlowCrudServiceInMemory;
import inmemory.SubscriptionSearchQueryServiceInMemory;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api_key.model.ApiKeyEntity;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.api_product.model.ApiProductKind;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.listener.http.Path;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.SubscriptionStatus;
import io.gravitee.rest.api.model.application.ApplicationListItem;
import io.gravitee.rest.api.portal.rest.model.AiWorkspacesResponse;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.core.Response;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AiWorkspacesResourceTest extends AbstractResourceTest {

    private static final String SECRET = "sk-live-do-not-leak";

    @Autowired
    private SubscriptionSearchQueryServiceInMemory subscriptions;

    @Autowired
    private ApiProductQueryServiceInMemory products;

    @Autowired
    private FlowCrudServiceInMemory flows;

    @Autowired
    private ApiCrudServiceInMemory apis;

    @Autowired
    private ApiKeyQueryServiceInMemory keys;

    @Override
    protected String contextPath() {
        return "ai-workspaces/";
    }

    @BeforeEach
    void setUp() {
        resetAllMocks();
        GraviteeContext.setCurrentEnvironment("DEFAULT");
        doReturn(Set.of(ApplicationListItem.builder().id("app-1").build()))
            .when(applicationService)
            .findByUser(any(ExecutionContext.class), eq(USER_NAME));
    }

    @AfterEach
    void tearDown() {
        subscriptions.reset();
        products.reset();
        flows.reset();
        apis.reset();
        keys.reset();
        GraviteeContext.cleanContext();
    }

    @Test
    void lists_workspaces_matching_the_name() {
        products.initWith(List.of(workspace("Alpha"), workspace("Beta").toBuilder().id("ws-2").name("Beta").build()));
        subscriptions.initWith(List.of(subscription("ws-1"), subscription("ws-2").toBuilder().id("sub-2").referenceId("ws-2").build()));
        flows.savePlanFlows("plan-1", List.of(budgetFlow()));

        Response response = target().queryParam("name", "alp").request().get();

        assertThat(response.getStatus()).isEqualTo(200);
        AiWorkspacesResponse body = response.readEntity(AiWorkspacesResponse.class);
        assertThat(body.getTotal()).isEqualTo(1);
        assertThat(body.getData())
            .extracting(item -> item.getName())
            .containsExactly("Alpha");
        assertThat(body.getData().get(0).getBudget().getAmount()).isEqualTo(5.0);
        assertThat(body.getData().get(0).getBudget().getPeriod()).isEqualTo("DAY");
    }

    @Test
    void paging_is_applied_after_the_name_filter() {
        products.initWith(
            List.of(
                workspace("Alpha"),
                workspace("Alpine").toBuilder().id("ws-2").name("Alpine").build(),
                workspace("Beta").toBuilder().id("ws-3").name("Beta").build()
            )
        );
        subscriptions.initWith(
            List.of(
                subscription("ws-1"),
                subscription("ws-2").toBuilder().id("sub-2").referenceId("ws-2").build(),
                subscription("ws-3").toBuilder().id("sub-3").referenceId("ws-3").build()
            )
        );

        Response response = target().queryParam("name", "alp").queryParam("page", 1).queryParam("size", 1).request().get();

        assertThat(response.getStatus()).isEqualTo(200);
        AiWorkspacesResponse body = response.readEntity(AiWorkspacesResponse.class);
        assertThat(body.getTotal()).isEqualTo(2);
        assertThat(body.getData())
            .extracting(item -> item.getName())
            .containsExactly("Alpha");
    }

    @Test
    void lists_a_workspace_when_the_budget_cannot_be_read() {
        products.initWith(List.of(workspace("Alpha")));
        subscriptions.initWith(List.of(subscription("ws-1")));

        Response response = target().request().get();

        assertThat(response.getStatus()).isEqualTo(200);
        AiWorkspacesResponse body = response.readEntity(AiWorkspacesResponse.class);
        assertThat(body.getTotal()).isEqualTo(1);
        assertThat(body.getData().get(0).getBudget()).isNull();
    }

    @Test
    void returns_details_without_provider_credentials() {
        products.initWith(List.of(workspace("Alpha")));
        subscriptions.initWith(List.of(subscription("ws-1")));
        apis.initWith(List.of(proxy()));
        keys.initWith(
            List.of(
                ApiKeyEntity.builder().id("active").key("live-key").subscriptions(List.of("sub-1")).createdAt(ZonedDateTime.now()).build()
            )
        );

        Response response = target("ws-1").request().get();

        assertThat(response.getStatus()).isEqualTo(200);
        String raw = response.readEntity(String.class);
        assertThat(raw).contains("live-key").contains("gpt-4o").contains("/llm-proxy");
        assertThat(raw).doesNotContain(SECRET).doesNotContain("authentication").doesNotContain("openai");
    }

    @Test
    void unmapped_workspace_is_not_found() {
        Response response = target("missing").request().get();

        assertThat(response.getStatus()).isEqualTo(404);
    }

    private static ApiProduct workspace(String name) {
        return ApiProduct.builder()
            .id("ws-1")
            .name(name)
            .description("desc")
            .environmentId("DEFAULT")
            .kind(ApiProductKind.AI_WORKSPACE)
            .apiIds(Set.of("proxy-1"))
            .build();
    }

    private static SubscriptionEntity subscription(String apiProductId) {
        return SubscriptionEntity.builder()
            .id("sub-1")
            .application("app-1")
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

    private static Api proxy() {
        HttpListener listener = new HttpListener();
        listener.setPaths(List.of(new Path("/llm-proxy")));
        Endpoint endpoint = new Endpoint();
        endpoint.setConfiguration(
            "{\"provider\":\"openai\",\"authentication\":{\"apiKey\":\"" +
                SECRET +
                "\"},\"models\":[{\"name\":\"gpt-4o\",\"inputPrice\":2.5,\"outputPrice\":10}]}"
        );
        EndpointGroup group = new EndpointGroup();
        group.setEndpoints(List.of(endpoint));
        io.gravitee.definition.model.v4.Api definition = new io.gravitee.definition.model.v4.Api();
        definition.setListeners(List.of(listener));
        definition.setEndpointGroups(List.of(group));
        return Api.builder().id("proxy-1").type(ApiType.LLM_PROXY).environmentId("DEFAULT").build().setApiDefinitionValue(definition);
    }
}

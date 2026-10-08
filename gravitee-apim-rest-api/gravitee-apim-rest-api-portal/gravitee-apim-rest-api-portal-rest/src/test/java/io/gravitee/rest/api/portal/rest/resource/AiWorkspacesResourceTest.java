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
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import inmemory.ApiCrudServiceInMemory;
import inmemory.ApiKeyQueryServiceInMemory;
import inmemory.ApiProductQueryServiceInMemory;
import inmemory.FlowCrudServiceInMemory;
import inmemory.SubscriptionSearchQueryServiceInMemory;
import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import io.gravitee.apim.core.analytics_engine.model.Measure;
import io.gravitee.apim.core.analytics_engine.model.MeasuresRequest;
import io.gravitee.apim.core.analytics_engine.model.MeasuresResponse;
import io.gravitee.apim.core.analytics_engine.model.MetricMeasuresResponse;
import io.gravitee.apim.core.analytics_engine.model.MetricSpec;
import io.gravitee.apim.core.analytics_engine.use_case.ComputeMeasuresUseCase;
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
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.SubscriptionStatus;
import io.gravitee.rest.api.model.application.ApplicationListItem;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.portal.rest.model.AiWorkspace;
import io.gravitee.rest.api.portal.rest.model.AiWorkspaceBudget;
import io.gravitee.rest.api.portal.rest.model.AiWorkspaceConsumption;
import io.gravitee.rest.api.portal.rest.model.AiWorkspacesResponse;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.core.Response;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

class AiWorkspacesResourceTest extends AbstractResourceTest {

    @Autowired
    private SubscriptionSearchQueryServiceInMemory subscriptions;

    @Autowired
    private ApiProductQueryServiceInMemory products;

    @Autowired
    private FlowCrudServiceInMemory flows;

    @Autowired
    private ApiCrudServiceInMemory apis;

    @Autowired
    private ApiKeyQueryServiceInMemory apiKeys;

    @Autowired
    private ComputeMeasuresUseCase computeMeasuresUseCase;

    @Override
    protected String contextPath() {
        return "ai-workspaces/";
    }

    @BeforeEach
    void setUp() {
        resetAllMocks();
        GraviteeContext.setCurrentEnvironment("DEFAULT");
        when(
            parameterService.findAsBoolean(any(), eq(Key.PORTAL_NEXT_AI_WORKSPACES_ENABLED), eq(ParameterReferenceType.ENVIRONMENT))
        ).thenReturn(true);
        doReturn(Set.of(application("app-1", USER_NAME))).when(applicationService).findByUser(any(ExecutionContext.class), eq(USER_NAME));
    }

    @AfterEach
    void tearDown() {
        subscriptions.reset();
        products.reset();
        flows.reset();
        apis.reset();
        apiKeys.reset();
        reset(computeMeasuresUseCase);
        GraviteeContext.cleanContext();
    }

    @Test
    void answers_404_when_ai_workspaces_are_disabled() {
        when(
            parameterService.findAsBoolean(any(), eq(Key.PORTAL_NEXT_AI_WORKSPACES_ENABLED), eq(ParameterReferenceType.ENVIRONMENT))
        ).thenReturn(false);
        products.initWith(List.of(workspace("ws-1", "Alpha", ApiProductKind.AI_WORKSPACE).toBuilder().apiIds(Set.of("proxy-1")).build()));
        subscriptions.initWith(List.of(subscription("app-1", "ws-1")));

        assertThat(target().request().get().getStatus()).isEqualTo(404);
        assertThat(target().path("ws-1").request().get().getStatus()).isEqualTo(404);
        assertThat(target().path("ws-1").path("consumption").request().get().getStatus()).isEqualTo(404);
        verify(computeMeasuresUseCase, never()).executeForApis(any(), any(), any());
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
    void returns_the_workspace_with_its_budget_and_endpoint() {
        products.initWith(List.of(workspace("ws-1", "Alpha", ApiProductKind.AI_WORKSPACE).toBuilder().apiIds(Set.of("api-1")).build()));
        subscriptions.initWith(List.of(subscription("app-1", "ws-1")));
        flows.savePlanFlows("plan-1", List.of(budgetFlow()));
        apis.initWith(List.of(proxy("api-1", "/alpha/")));

        Response response = target().path("ws-1").request().get();

        assertThat(response.getStatus()).isEqualTo(200);
        AiWorkspace body = response.readEntity(AiWorkspace.class);
        assertThat(body.getName()).isEqualTo("Alpha");
        assertThat(body.getDescription()).isEqualTo("desc");
        assertThat(body.getBudget().getAmount()).isEqualTo(5.0);
        assertThat(body.getBudget().getPeriod().getValue()).isEqualTo("DAY");
        assertThat(body.getEndpointUrl()).isEqualTo("/alpha/");
        assertThat(body.getKey()).isNull();
    }

    @Test
    void returns_the_callers_key_and_not_another_subscribers() {
        products.initWith(List.of(workspace("ws-1", "Alpha", ApiProductKind.AI_WORKSPACE)));
        subscriptions.initWith(
            List.of(subscription("app-1", "ws-1"), subscription("app-2", "ws-1").toBuilder().id("sub-2").application("app-2").build())
        );
        ZonedDateTime createdAt = ZonedDateTime.of(2026, 3, 4, 5, 6, 7, 0, ZoneOffset.UTC);
        apiKeys.initWith(
            List.of(
                ApiKeyEntity.builder().key("caller-key").subscriptions(List.of("sub-1")).createdAt(createdAt).build(),
                ApiKeyEntity.builder().key("other-key").subscriptions(List.of("sub-2")).createdAt(createdAt).build()
            )
        );

        Response response = target().path("ws-1").request().get();

        assertThat(response.getStatus()).isEqualTo(200);
        AiWorkspace body = response.readEntity(AiWorkspace.class);
        assertThat(body.getKey().getValue()).isEqualTo("caller-key");
        assertThat(body.getKey().getStatus().getValue()).isEqualTo("ACTIVE");
        assertThat(body.getKey().getCreatedAt()).isEqualTo(createdAt.toOffsetDateTime());
    }

    @Test
    void returns_only_the_callers_consumption_over_30_days() {
        products.initWith(List.of(workspace("ws-1", "Alpha", ApiProductKind.AI_WORKSPACE).toBuilder().apiIds(Set.of("proxy-1")).build()));
        subscriptions.initWith(
            List.of(subscription("app-1", "ws-1"), subscription("app-2", "ws-1").toBuilder().id("sub-2").application("app-2").build())
        );
        when(computeMeasuresUseCase.executeForApis(any(), any(), any())).thenReturn(
            new ComputeMeasuresUseCase.Output(
                new MeasuresResponse(
                    List.of(
                        new MetricMeasuresResponse(
                            MetricSpec.Name.LLM_PROMPT_TOTAL_TOKEN,
                            MetricSpec.Unit.NUMBER,
                            List.of(new Measure(MetricSpec.Measure.COUNT, 12))
                        ),
                        new MetricMeasuresResponse(
                            MetricSpec.Name.HTTP_REQUESTS,
                            MetricSpec.Unit.NUMBER,
                            List.of(new Measure(MetricSpec.Measure.COUNT, 3))
                        ),
                        new MetricMeasuresResponse(
                            MetricSpec.Name.LLM_PROMPT_TOKEN_TOTAL_COST,
                            MetricSpec.Unit.NUMBER,
                            List.of(new Measure(MetricSpec.Measure.COUNT, 1.5))
                        )
                    )
                )
            )
        );

        Response response = target().path("ws-1").path("consumption").request().get();

        assertThat(response.getStatus()).isEqualTo(200);
        AiWorkspaceConsumption body = response.readEntity(AiWorkspaceConsumption.class);
        assertThat(body.getTokens()).isEqualTo(12L);
        assertThat(body.getRequests()).isEqualTo(3L);
        assertThat(body.getCost()).isEqualTo(1.5);
        assertThat(Duration.between(body.getFrom(), body.getTo())).isEqualTo(Duration.ofDays(30));

        ArgumentCaptor<MeasuresRequest> request = ArgumentCaptor.forClass(MeasuresRequest.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Set<String>> apis = ArgumentCaptor.forClass(Set.class);
        verify(computeMeasuresUseCase).executeForApis(any(), request.capture(), apis.capture());
        assertThat(apis.getValue()).containsExactly("proxy-1");
        assertThat(request.getValue().filters())
            .extracting(filter -> filter.name(), filter -> filter.value())
            .containsExactly(tuple(FilterSpec.Name.API_PRODUCT, "ws-1"), tuple(FilterSpec.Name.APPLICATION, "app-1"));
    }

    @Test
    void returns_zeros_when_analytics_cannot_be_read() {
        products.initWith(List.of(workspace("ws-1", "Alpha", ApiProductKind.AI_WORKSPACE).toBuilder().apiIds(Set.of("proxy-1")).build()));
        subscriptions.initWith(List.of(subscription("app-1", "ws-1")));
        when(computeMeasuresUseCase.executeForApis(any(), any(), any())).thenThrow(new IllegalStateException("analytics down"));

        Response response = target().path("ws-1").path("consumption").request().get();

        assertThat(response.getStatus()).isEqualTo(200);
        AiWorkspaceConsumption body = response.readEntity(AiWorkspaceConsumption.class);
        assertThat(body.getTokens()).isEqualTo(0L);
        assertThat(body.getRequests()).isEqualTo(0L);
        assertThat(body.getCost()).isEqualTo(0.0);
        assertThat(Duration.between(body.getFrom(), body.getTo())).isEqualTo(Duration.ofDays(30));
    }

    @Test
    void does_not_read_consumption_for_a_workspace_the_caller_cannot_see() {
        products.initWith(List.of(workspace("missing", "Missing", ApiProductKind.AI_WORKSPACE)));

        assertThat(target().path("missing").path("consumption").request().get().getStatus()).isEqualTo(404);
        verify(computeMeasuresUseCase, never()).executeForApis(any(), any(), any());
    }

    @Test
    void returns_the_workspace_when_it_has_no_proxy() {
        products.initWith(List.of(workspace("ws-1", "Alpha", ApiProductKind.AI_WORKSPACE)));
        subscriptions.initWith(List.of(subscription("app-1", "ws-1")));

        Response response = target().path("ws-1").request().get();

        assertThat(response.getStatus()).isEqualTo(200);
        AiWorkspace body = response.readEntity(AiWorkspace.class);
        assertThat(body.getEndpointUrl()).isNull();
        assertThat(body.getBudget()).isNull();
        assertThat(body.getModels()).isEmpty();
    }

    @Test
    void returns_models_and_omits_provider_configuration() throws Exception {
        String secret = "sk-live-do-not-leak";
        String target = "https://provider.example/secret-target";
        products.initWith(List.of(workspace("ws-1", "Alpha", ApiProductKind.AI_WORKSPACE).toBuilder().apiIds(Set.of("api-1")).build()));
        subscriptions.initWith(List.of(subscription("app-1", "ws-1")));
        apis.initWith(List.of(proxyWithProviders("api-1", "/alpha/", secret, target)));

        Response response = target().path("ws-1").request().get();

        assertThat(response.getStatus()).isEqualTo(200);
        String raw = response.readEntity(String.class);
        assertThat(raw).doesNotContain(secret).doesNotContain(target).doesNotContain("authentication").doesNotContain("OPEN_AI");
        JsonNode models = new ObjectMapper().readTree(raw).get("models");
        assertThat(models).hasSize(3);
        assertThat(models.get(0).get("name").asText()).isEqualTo("gpt-4o");
        assertThat(models.get(0).get("inputPrice").asDouble()).isEqualTo(2.5);
        assertThat(models.get(0).get("outputPrice").asDouble()).isEqualTo(10.0);
        assertThat(models.get(1).get("name").asText()).isEqualTo("draft");
        assertThat(absent(models.get(1).get("inputPrice"))).isTrue();
        assertThat(absent(models.get(1).get("outputPrice"))).isTrue();
        assertThat(models.get(2).get("name").asText()).isEqualTo("claude");
        assertThat(absent(models.get(2).get("inputPrice"))).isTrue();
        assertThat(models.get(2).get("outputPrice").asDouble()).isEqualTo(0.5);
    }

    @Test
    void does_not_reveal_a_workspace_the_caller_cannot_see() {
        products.initWith(List.of(workspace("missing", "Missing", ApiProductKind.AI_WORKSPACE), workspace("catalog", "Catalog", null)));
        subscriptions.initWith(List.of(subscription("app-1", "catalog")));

        assertThat(target().path("missing").request().get().getStatus()).isEqualTo(404);
        assertThat(target().path("catalog").request().get().getStatus()).isEqualTo(404);
        assertThat(target().path("ws-2").request().get().getStatus()).isEqualTo(404);

        products.initWith(List.of(workspace("ws-1", "Alpha", ApiProductKind.AI_WORKSPACE)));
        subscriptions.initWith(List.of(subscription("app-1", "ws-1")));
        doReturn(Set.of(application("app-1", "someone-else")))
            .when(applicationService)
            .findByUser(any(ExecutionContext.class), eq(USER_NAME));
        assertThat(target().path("ws-1").request().get().getStatus()).isEqualTo(404);
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

    private static boolean absent(JsonNode node) {
        return node == null || node.isNull() || node.isMissingNode();
    }

    private static Api proxyWithProviders(String id, String path, String secret, String target) {
        String openAi =
            "{\"provider\":\"OPEN_AI\",\"target\":\"" +
            target +
            "\",\"authentication\":{\"apiKey\":\"" +
            secret +
            "\"},\"models\":[{\"name\":\"gpt-4o\",\"inputPrice\":2.5,\"outputPrice\":10},{\"name\":\"draft\"}]}";
        String anthropic =
            "{\"provider\":\"ANTHROPIC\",\"authentication\":{\"token\":\"" +
            secret +
            "\"},\"models\":[{\"name\":\"claude\",\"outputPrice\":0.5}]}";
        return Api.builder()
            .id(id)
            .environmentId("DEFAULT")
            .type(ApiType.LLM_PROXY)
            .apiDefinitionHttpV4(
                io.gravitee.definition.model.v4.Api.builder()
                    .listeners(List.of(HttpListener.builder().paths(List.of(Path.builder().path(path).build())).build()))
                    .endpointGroups(
                        List.of(
                            EndpointGroup.builder()
                                .name("OpenAI")
                                .type("llm")
                                .endpoints(List.of(Endpoint.builder().name("openai").type("llm-proxy").configuration(openAi).build()))
                                .build(),
                            EndpointGroup.builder()
                                .name("Anthropic")
                                .type("llm")
                                .endpoints(List.of(Endpoint.builder().name("anthropic").type("llm-proxy").configuration(anthropic).build()))
                                .build()
                        )
                    )
                    .build()
            )
            .build();
    }

    private static Api proxy(String id, String path) {
        return Api.builder()
            .id(id)
            .environmentId("DEFAULT")
            .type(ApiType.LLM_PROXY)
            .apiDefinitionHttpV4(
                io.gravitee.definition.model.v4.Api.builder()
                    .listeners(List.of(HttpListener.builder().paths(List.of(Path.builder().path(path).build())).build()))
                    .build()
            )
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

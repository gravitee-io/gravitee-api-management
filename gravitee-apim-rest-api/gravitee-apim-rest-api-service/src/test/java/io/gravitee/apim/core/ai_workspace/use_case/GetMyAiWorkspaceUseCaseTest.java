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
import inmemory.ApiKeyQueryServiceInMemory;
import inmemory.ApiProductQueryServiceInMemory;
import inmemory.FlowCrudServiceInMemory;
import inmemory.SubscriptionSearchQueryServiceInMemory;
import io.gravitee.apim.core.ai_workspace.exception.AiWorkspaceNotFoundException;
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
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetMyAiWorkspaceUseCaseTest {

    private static final String SECRET = "sk-live-do-not-leak";
    private static final ExecutionContext CONTEXT = new ExecutionContext("org", "DEFAULT");

    private final SubscriptionSearchQueryServiceInMemory subscriptions = new SubscriptionSearchQueryServiceInMemory();
    private final ApiProductQueryServiceInMemory products = new ApiProductQueryServiceInMemory();
    private final FlowCrudServiceInMemory flows = new FlowCrudServiceInMemory();
    private final ApiCrudServiceInMemory apis = new ApiCrudServiceInMemory();
    private final ApiKeyQueryServiceInMemory keys = new ApiKeyQueryServiceInMemory();
    private final GetMyAiWorkspaceUseCase useCase = new GetMyAiWorkspaceUseCase(subscriptions, products, flows, apis, keys);

    @BeforeEach
    void setUp() {
        subscriptions.reset();
        products.reset();
        flows.reset();
        apis.reset();
        keys.reset();
    }

    @Test
    void returns_endpoint_active_key_and_models_without_credentials() {
        products.initWith(List.of(workspace()));
        flows.savePlanFlows("plan-1", List.of(budgetFlow()));
        subscriptions.initWith(List.of(subscription("sub-1", "app-1")));
        apis.initWith(List.of(proxy()));
        keys.initWith(
            List.of(
                ApiKeyEntity.builder().id("revoked").key("revoked-key").subscriptions(List.of("sub-1")).revoked(true).build(),
                ApiKeyEntity.builder()
                    .id("active")
                    .key("live-key")
                    .subscriptions(List.of("sub-1"))
                    .createdAt(ZonedDateTime.of(2026, 1, 2, 3, 4, 5, 0, ZoneOffset.UTC))
                    .build()
            )
        );

        var details = useCase.execute(new GetMyAiWorkspaceUseCase.Input(CONTEXT, Set.of("app-1"), "ws-1")).details();

        assertThat(details.endpointUrl()).isEqualTo("/llm-proxy");
        assertThat(details.key().value()).isEqualTo("live-key");
        assertThat(details.key().status()).isEqualTo("ACTIVE");
        assertThat(details.key().createdAt()).isEqualTo(ZonedDateTime.of(2026, 1, 2, 3, 4, 5, 0, ZoneOffset.UTC).toInstant());
        assertThat(details.models())
            .extracting(model -> model.name())
            .containsExactly("gpt-4o");
        assertThat(details.toString()).doesNotContain(SECRET).doesNotContain("authentication").doesNotContain("openai");
    }

    @Test
    void unknown_or_unmapped_workspace_is_not_found() {
        products.initWith(List.of(workspace()));
        subscriptions.initWith(List.of(subscription("sub-1", "app-1")));

        assertThatThrownBy(() -> useCase.execute(new GetMyAiWorkspaceUseCase.Input(CONTEXT, Set.of("someone-else"), "ws-1"))).isInstanceOf(
            AiWorkspaceNotFoundException.class
        );
        assertThatThrownBy(() -> useCase.execute(new GetMyAiWorkspaceUseCase.Input(CONTEXT, Set.of("app-1"), "missing"))).isInstanceOf(
            AiWorkspaceNotFoundException.class
        );
    }

    @Test
    void each_mapped_user_sees_only_their_own_key() {
        products.initWith(List.of(workspace()));
        subscriptions.initWith(List.of(subscription("sub-1", "app-1"), subscription("sub-2", "app-2")));
        keys.initWith(
            List.of(
                ApiKeyEntity.builder().id("key-1").key("key-for-app-1").subscriptions(List.of("sub-1")).build(),
                ApiKeyEntity.builder().id("key-2").key("key-for-app-2").subscriptions(List.of("sub-2")).build()
            )
        );

        var first = useCase.execute(new GetMyAiWorkspaceUseCase.Input(CONTEXT, Set.of("app-1"), "ws-1")).details();
        var second = useCase.execute(new GetMyAiWorkspaceUseCase.Input(CONTEXT, Set.of("app-2"), "ws-1")).details();

        assertThat(first.key().value()).isEqualTo("key-for-app-1");
        assertThat(second.key().value()).isEqualTo("key-for-app-2");
    }

    @Test
    void catalog_product_is_not_an_ai_workspace() {
        products.initWith(List.of(workspace().toBuilder().kind(null).build()));
        subscriptions.initWith(List.of(subscription("sub-1", "app-1")));

        assertThatThrownBy(() -> useCase.execute(new GetMyAiWorkspaceUseCase.Input(CONTEXT, Set.of("app-1"), "ws-1"))).isInstanceOf(
            AiWorkspaceNotFoundException.class
        );
    }

    @Test
    void missing_proxy_or_usable_key_leaves_those_fields_empty() {
        products.initWith(List.of(workspace()));
        subscriptions.initWith(List.of(subscription("sub-1", "app-1")));
        keys.initWith(
            List.of(ApiKeyEntity.builder().id("revoked").key("revoked-key").subscriptions(List.of("sub-1")).revoked(true).build())
        );

        var details = useCase.execute(new GetMyAiWorkspaceUseCase.Input(CONTEXT, Set.of("app-1"), "ws-1")).details();

        assertThat(details.endpointUrl()).isNull();
        assertThat(details.key()).isNull();
        assertThat(details.models()).isEmpty();
    }

    private static ApiProduct workspace() {
        return ApiProduct.builder()
            .id("ws-1")
            .name("Alpha")
            .description("desc")
            .environmentId("DEFAULT")
            .kind(ApiProductKind.AI_WORKSPACE)
            .apiIds(Set.of("proxy-1"))
            .build();
    }

    private static SubscriptionEntity subscription(String id, String applicationId) {
        return SubscriptionEntity.builder()
            .id(id)
            .application(applicationId)
            .referenceId("ws-1")
            .referenceType("API_PRODUCT")
            .plan("plan-1")
            .status(SubscriptionStatus.ACCEPTED)
            .createdAt(new Date(1_000))
            .build();
    }

    private static Flow budgetFlow() {
        Step step = new Step();
        step.setPolicy("cost-ratelimit");
        step.setConfiguration("{\"rate\":{\"limit\":1000000,\"periodTime\":60}}");
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

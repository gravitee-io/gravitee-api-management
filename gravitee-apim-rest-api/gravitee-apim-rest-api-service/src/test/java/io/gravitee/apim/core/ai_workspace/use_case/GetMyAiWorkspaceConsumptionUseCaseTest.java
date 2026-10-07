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

import inmemory.ApiProductQueryServiceInMemory;
import inmemory.SubscriptionSearchQueryServiceInMemory;
import io.gravitee.apim.core.ai_workspace.domain_service.AiWorkspaceMembershipQuery;
import io.gravitee.apim.core.ai_workspace.exception.AiWorkspaceNotFoundException;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.api_product.model.ApiProductKind;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.SubscriptionStatus;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetMyAiWorkspaceConsumptionUseCaseTest {

    private static final String ENV = "DEFAULT";
    private static final ExecutionContext CONTEXT = new ExecutionContext("org", ENV);

    private final SubscriptionSearchQueryServiceInMemory subscriptions = new SubscriptionSearchQueryServiceInMemory();
    private final ApiProductQueryServiceInMemory products = new ApiProductQueryServiceInMemory();
    private final GetMyAiWorkspaceConsumptionUseCase useCase = new GetMyAiWorkspaceConsumptionUseCase(
        new AiWorkspaceMembershipQuery(subscriptions),
        products
    );

    @BeforeEach
    void setUp() {
        subscriptions.reset();
        products.reset();
    }

    @Test
    void returns_the_application_of_the_callers_subscription() {
        products.initWith(List.of(workspace("ws-1").toBuilder().apiIds(Set.of("proxy-1")).build()));
        subscriptions.initWith(
            List.of(subscription("sub-1", "app-1"), subscription("sub-2", "app-2").toBuilder().createdAt(new Date(2_000)).build())
        );

        var caller = useCase.execute(new GetMyAiWorkspaceConsumptionUseCase.Input(CONTEXT, Set.of("app-1"), "ws-1"));
        var other = useCase.execute(new GetMyAiWorkspaceConsumptionUseCase.Input(CONTEXT, Set.of("app-2"), "ws-1"));

        assertThat(caller.applicationId()).isEqualTo("app-1");
        assertThat(caller.apiIds()).containsExactly("proxy-1");
        assertThat(other.applicationId()).isEqualTo("app-2");
        assertThat(other.apiIds()).containsExactly("proxy-1");
    }

    @Test
    void rejects_an_unknown_id_a_non_workspace_and_another_callers_workspace() {
        products.initWith(List.of(ApiProduct.builder().id("catalog").name("Catalog").environmentId(ENV).build(), workspace("ws-2")));
        subscriptions.initWith(List.of(subscription("sub-1", "app-1").toBuilder().referenceId("catalog").build()));

        assertThatThrownBy(() -> useCase.execute(new GetMyAiWorkspaceConsumptionUseCase.Input(CONTEXT, Set.of("app-1"), " "))).isInstanceOf(
            AiWorkspaceNotFoundException.class
        );
        assertThatThrownBy(() ->
            useCase.execute(new GetMyAiWorkspaceConsumptionUseCase.Input(CONTEXT, Set.of("app-1"), "missing"))
        ).isInstanceOf(AiWorkspaceNotFoundException.class);
        assertThatThrownBy(() ->
            useCase.execute(new GetMyAiWorkspaceConsumptionUseCase.Input(CONTEXT, Set.of("app-1"), "catalog"))
        ).isInstanceOf(AiWorkspaceNotFoundException.class);
        assertThatThrownBy(() ->
            useCase.execute(new GetMyAiWorkspaceConsumptionUseCase.Input(CONTEXT, Set.of("app-1"), "ws-2"))
        ).isInstanceOf(AiWorkspaceNotFoundException.class);
    }

    private static ApiProduct workspace(String id) {
        return ApiProduct.builder().id(id).name(id).environmentId(ENV).kind(ApiProductKind.AI_WORKSPACE).build();
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
}

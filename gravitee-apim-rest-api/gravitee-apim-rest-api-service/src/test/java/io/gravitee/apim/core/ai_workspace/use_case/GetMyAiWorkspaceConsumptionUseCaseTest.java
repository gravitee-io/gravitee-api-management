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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import inmemory.ApiProductQueryServiceInMemory;
import inmemory.SubscriptionSearchQueryServiceInMemory;
import io.gravitee.apim.core.ai_workspace.exception.AiWorkspaceNotFoundException;
import io.gravitee.apim.core.analytics_engine.model.FacetBucketResponse;
import io.gravitee.apim.core.analytics_engine.model.FacetsRequest;
import io.gravitee.apim.core.analytics_engine.model.FacetsResponse;
import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import io.gravitee.apim.core.analytics_engine.model.Measure;
import io.gravitee.apim.core.analytics_engine.model.MetricFacetsResponse;
import io.gravitee.apim.core.analytics_engine.model.MetricSpec;
import io.gravitee.apim.core.analytics_engine.query_service.AnalyticsEngineQueryService;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.api_product.model.ApiProductKind;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.SubscriptionStatus;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class GetMyAiWorkspaceConsumptionUseCaseTest {

    private static final ExecutionContext CONTEXT = new ExecutionContext("org", "DEFAULT");

    private final SubscriptionSearchQueryServiceInMemory subscriptions = new SubscriptionSearchQueryServiceInMemory();
    private final ApiProductQueryServiceInMemory products = new ApiProductQueryServiceInMemory();
    private final AnalyticsEngineQueryService analytics = mock(AnalyticsEngineQueryService.class);
    private final GetMyAiWorkspaceConsumptionUseCase useCase = new GetMyAiWorkspaceConsumptionUseCase(
        subscriptions,
        products,
        List.of(analytics)
    );

    @BeforeEach
    void setUp() {
        when(analytics.metrics()).thenReturn(
            Set.of(MetricSpec.Name.LLM_PROMPT_TOTAL_TOKEN, MetricSpec.Name.HTTP_REQUESTS, MetricSpec.Name.LLM_PROMPT_TOKEN_TOTAL_COST)
        );
        subscriptions.reset();
        products.reset();
        products.initWith(
            List.of(ApiProduct.builder().id("ws-1").name("Alpha").environmentId("DEFAULT").kind(ApiProductKind.AI_WORKSPACE).build())
        );
        subscriptions.initWith(
            List.of(
                SubscriptionEntity.builder()
                    .id("sub-1")
                    .application("app-1")
                    .referenceId("ws-1")
                    .referenceType("API_PRODUCT")
                    .status(SubscriptionStatus.ACCEPTED)
                    .createdAt(new Date(1_000))
                    .build()
            )
        );
    }

    @Test
    void sums_the_callers_traffic_over_thirty_days() {
        when(analytics.searchFacets(any(), any())).thenReturn(
            new FacetsResponse(
                List.of(
                    metric(MetricSpec.Name.HTTP_REQUESTS, 4),
                    metric(MetricSpec.Name.LLM_PROMPT_TOTAL_TOKEN, 120),
                    metric(MetricSpec.Name.LLM_PROMPT_TOKEN_TOTAL_COST, 1.5)
                )
            )
        );

        var consumption = useCase.execute(new GetMyAiWorkspaceConsumptionUseCase.Input(CONTEXT, Set.of("app-1"), "ws-1")).consumption();

        assertThat(consumption.requests()).isEqualTo(4);
        assertThat(consumption.tokens()).isEqualTo(120);
        assertThat(consumption.cost()).isEqualTo(1.5);
        assertThat(Duration.between(consumption.from(), consumption.to())).isEqualTo(Duration.ofDays(30));
    }

    @Test
    void requests_only_the_callers_application_and_workspace() {
        when(analytics.searchFacets(any(), any())).thenReturn(new FacetsResponse(List.of()));

        useCase.execute(new GetMyAiWorkspaceConsumptionUseCase.Input(CONTEXT, Set.of("app-1"), "ws-1"));

        ArgumentCaptor<FacetsRequest> request = ArgumentCaptor.forClass(FacetsRequest.class);
        verify(analytics).searchFacets(any(), request.capture());
        assertThat(request.getValue().filters())
            .anySatisfy(filter -> {
                assertThat(filter.name()).isEqualTo(FilterSpec.Name.API_PRODUCT);
                assertThat(filter.value()).isEqualTo(List.of("ws-1"));
            })
            .anySatisfy(filter -> {
                assertThat(filter.name()).isEqualTo(FilterSpec.Name.APPLICATION);
                assertThat(filter.value()).isEqualTo(List.of("app-1"));
            });
    }

    @Test
    void analytics_failure_returns_zeros() {
        when(analytics.searchFacets(any(), any())).thenThrow(new RuntimeException("analytics down"));

        var consumption = useCase.execute(new GetMyAiWorkspaceConsumptionUseCase.Input(CONTEXT, Set.of("app-1"), "ws-1")).consumption();

        assertThat(consumption.requests()).isZero();
        assertThat(consumption.tokens()).isZero();
        assertThat(consumption.cost()).isZero();
        assertThat(Duration.between(consumption.from(), consumption.to())).isEqualTo(Duration.ofDays(30));
    }

    @Test
    void unmapped_workspace_is_not_found() {
        assertThatThrownBy(() ->
            useCase.execute(new GetMyAiWorkspaceConsumptionUseCase.Input(CONTEXT, Set.of("other"), "ws-1"))
        ).isInstanceOf(AiWorkspaceNotFoundException.class);
    }

    private static MetricFacetsResponse metric(MetricSpec.Name name, Number value) {
        return new MetricFacetsResponse(
            name,
            null,
            List.of(new FacetBucketResponse("app-1", "app", List.of(), List.of(new Measure(MetricSpec.Measure.COUNT, value))))
        );
    }
}

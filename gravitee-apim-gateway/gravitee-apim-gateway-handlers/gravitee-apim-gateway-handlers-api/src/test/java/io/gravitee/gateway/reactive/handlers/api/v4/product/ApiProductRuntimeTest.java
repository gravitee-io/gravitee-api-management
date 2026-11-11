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
package io.gravitee.gateway.reactive.handlers.api.v4.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.common.component.AbstractLifecycleComponent;
import io.gravitee.common.component.Lifecycle;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.util.LinkedMultiValueMap;
import io.gravitee.common.util.MultiValueMap;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.selector.HttpSelector;
import io.gravitee.definition.model.v4.plan.Plan;
import io.gravitee.gateway.handlers.api.ReactableApiProduct;
import io.gravitee.gateway.reactive.api.context.http.HttpExecutionContext;
import io.gravitee.gateway.reactive.api.context.http.HttpRequest;
import io.gravitee.gateway.reactive.handlers.api.v4.processor.pathparameters.PathParametersExtractor;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ApiProductRuntimeTest {

    @Nested
    class PathParameters {

        private final HttpExecutionContext ctx = mock(HttpExecutionContext.class);
        private final HttpRequest request = mock(HttpRequest.class);
        private final MultiValueMap<String, String> pathParameters = new LinkedMultiValueMap<>();

        @Test
        void should_add_the_path_parameters_the_plan_flows_declare() {
            givenRequest("/orders/42");

            runtimeWithPlanFlowPath("/orders/:orderId").extractPathParameters(ctx);

            assertThat(pathParameters.getFirst("orderId")).isEqualTo("42");
        }

        @Test
        void should_keep_the_value_the_member_api_extracted_on_a_name_clash() {
            givenRequest("/orders/42");
            pathParameters.add("orderId", "from-the-api");

            runtimeWithPlanFlowPath("/orders/:orderId").extractPathParameters(ctx);

            assertThat(pathParameters.get("orderId")).containsExactly("from-the-api");
        }

        @Test
        void should_not_touch_the_request_without_an_extractor() {
            new ApiProductRuntime(product(Set.of("api-1")), null, null, null, List.of()).extractPathParameters(ctx);

            verify(ctx, never()).request();
        }

        private void givenRequest(String pathInfo) {
            when(ctx.request()).thenReturn(request);
            when(request.method()).thenReturn(HttpMethod.GET);
            when(request.pathInfo()).thenReturn(pathInfo);
            when(request.pathParameters()).thenReturn(pathParameters);
        }

        private ApiProductRuntime runtimeWithPlanFlowPath(String path) {
            HttpSelector selector = new HttpSelector();
            selector.setPath(path);
            Plan plan = Plan.builder()
                .id("plan-1")
                .flows(List.of(Flow.builder().enabled(true).selectors(List.of(selector)).build()))
                .build();
            Api plansOnly = new Api();
            plansOnly.setPlans(List.of(plan));
            return new ApiProductRuntime(product(Set.of("api-1")), null, null, new PathParametersExtractor(plansOnly), List.of());
        }
    }

    @Nested
    class Membership {

        @Test
        void should_serve_the_apis_the_product_lists() {
            ApiProductRuntime runtime = new ApiProductRuntime(product(Set.of("api-1")), null, null, null, List.of());

            assertThat(runtime.servesApi("api-1")).isTrue();
            assertThat(runtime.servesApi("api-2")).isFalse();
        }

        @Test
        void should_serve_no_api_when_the_product_lists_none() {
            assertThat(new ApiProductRuntime(product(null), null, null, null, List.of()).servesApi("api-1")).isFalse();
        }
    }

    @Nested
    class StartAndStop {

        @Test
        void should_start_components_in_order_and_stop_them_in_reverse_order() throws Exception {
            StringBuilder events = new StringBuilder();
            ApiProductRuntime runtime = new ApiProductRuntime(
                product(Set.of("api-1")),
                null,
                null,
                null,
                List.of(new Recording("policies", events), new Recording("resources", events))
            );

            runtime.start();
            runtime.stop();

            assertThat(events).hasToString("start policies,start resources,stop resources,stop policies,");
        }

        @Test
        void should_stop_every_component_and_throw_the_first_failure() throws Exception {
            Recording policies = new Recording("policies", new StringBuilder());
            FailingToStop first = new FailingToStop("pool already closed");
            FailingToStop second = new FailingToStop("connection reset");
            ApiProductRuntime runtime = new ApiProductRuntime(product(Set.of("api-1")), null, null, null, List.of(policies, second, first));
            runtime.start();

            assertThatThrownBy(runtime::stop)
                .hasMessage("pool already closed")
                .satisfies(failure ->
                    assertThat(failure.getSuppressed()).extracting(Throwable::getMessage).containsExactly("connection reset")
                );
            assertThat(policies.lifecycleState()).isEqualTo(Lifecycle.State.STOPPED);
        }

        @Test
        void should_count_no_request_in_once_retired() {
            ApiProductRuntime runtime = new ApiProductRuntime(product(Set.of("api-1")), null, null, null, List.of());
            assertThat(runtime.tryAcquire()).isTrue();

            runtime.retire();

            assertThat(runtime.tryAcquire()).isFalse();
            assertThat(runtime.inFlight()).isEqualTo(1);
        }
    }

    private static ReactableApiProduct product(Set<String> apiIds) {
        return ReactableApiProduct.builder().id("product-1").environmentId("env-1").apiIds(apiIds).build();
    }

    private static class Recording extends AbstractLifecycleComponent<Recording> {

        private final String name;
        private final StringBuilder events;

        Recording(String name, StringBuilder events) {
            this.name = name;
            this.events = events;
        }

        @Override
        protected void doStart() {
            events.append("start ").append(name).append(',');
        }

        @Override
        protected void doStop() {
            events.append("stop ").append(name).append(',');
        }
    }

    private static class FailingToStop extends AbstractLifecycleComponent<FailingToStop> {

        private final String message;

        FailingToStop(String message) {
            this.message = message;
        }

        @Override
        protected void doStart() {}

        @Override
        protected void doStop() {
            throw new IllegalStateException(message);
        }
    }
}

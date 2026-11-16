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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.reactive.api.ExecutionPhase;
import io.gravitee.gateway.reactive.api.context.http.HttpExecutionContext;
import io.gravitee.gateway.reactive.handlers.api.v4.flow.FlowChain;
import io.reactivex.rxjava3.core.Completable;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ApiProductFlowChainsTest {

    private final HttpExecutionContext ctx = mock(HttpExecutionContext.class);
    private final FlowChain productPlanFlowChain = mock(FlowChain.class);
    private final FlowChain productFlowChain = mock(FlowChain.class);
    private final List<String> ran = new ArrayList<>();

    @Test
    void should_run_the_product_flows_once_the_product_plan_flows_have_completed() {
        when(productPlanFlowChain.execute(ctx, ExecutionPhase.REQUEST)).thenReturn(recording("product-plan"));
        when(productFlowChain.execute(ctx, ExecutionPhase.REQUEST)).thenReturn(recording("product"));

        new ApiProductFlowChains(productPlanFlowChain, productFlowChain).execute(ctx, ExecutionPhase.REQUEST).test().assertComplete();

        assertThat(ran).containsExactly("product-plan", "product");
    }

    @Test
    void should_not_run_the_product_flows_when_the_product_plan_flows_interrupt() {
        when(productPlanFlowChain.execute(ctx, ExecutionPhase.REQUEST)).thenReturn(
            Completable.error(new IllegalStateException("interrupted"))
        );
        when(productFlowChain.execute(ctx, ExecutionPhase.REQUEST)).thenReturn(recording("product"));

        new ApiProductFlowChains(productPlanFlowChain, productFlowChain)
            .execute(ctx, ExecutionPhase.REQUEST)
            .test()
            .assertError(IllegalStateException.class);

        assertThat(ran).isEmpty();
    }

    private Completable recording(String chain) {
        return Completable.fromRunnable(() -> ran.add(chain));
    }
}

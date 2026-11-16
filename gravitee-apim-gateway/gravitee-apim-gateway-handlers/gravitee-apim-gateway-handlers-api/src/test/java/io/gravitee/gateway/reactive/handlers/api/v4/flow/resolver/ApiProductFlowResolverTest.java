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
package io.gravitee.gateway.reactive.handlers.api.v4.flow.resolver;

import static org.mockito.Mockito.mock;

import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.gateway.reactive.api.context.base.BaseExecutionContext;
import io.gravitee.gateway.reactive.api.context.http.HttpPlainExecutionContext;
import io.gravitee.gateway.reactive.core.condition.ConditionFilter;
import java.util.List;
import org.junit.jupiter.api.Test;

class ApiProductFlowResolverTest {

    @SuppressWarnings("unchecked")
    private final ConditionFilter<BaseExecutionContext, Flow> filter = mock(ConditionFilter.class);

    private final HttpPlainExecutionContext ctx = mock(HttpPlainExecutionContext.class);

    @Test
    void should_provide_the_enabled_product_flows_in_order() {
        Flow first = Flow.builder().name("first").enabled(true).build();
        Flow disabled = Flow.builder().name("disabled").enabled(false).build();
        Flow second = Flow.builder().name("second").enabled(true).build();

        new ApiProductFlowResolver(List.of(first, disabled, second), filter).provideFlows(ctx).test().assertResult(first, second);
    }

    @Test
    void should_provide_no_flow_for_a_product_without_flows() {
        new ApiProductFlowResolver(null, filter).provideFlows(ctx).test().assertResult();
    }
}

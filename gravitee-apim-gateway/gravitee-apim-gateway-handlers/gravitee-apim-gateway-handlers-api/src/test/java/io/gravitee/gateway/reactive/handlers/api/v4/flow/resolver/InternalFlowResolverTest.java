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

import static org.mockito.Mockito.when;

import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.selector.ConditionSelector;
import io.gravitee.definition.model.v4.flow.selector.HttpSelector;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.gateway.reactive.api.context.http.HttpPlainExecutionContext;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InternalFlowResolverTest {

    @Mock
    private Api api;

    @Mock
    private HttpPlainExecutionContext ctx;

    @Test
    void should_provide_the_internal_flow() {
        final Flow internalFlow = Flow.builder().name("guardians").build();
        when(api.getInternalFlow()).thenReturn(internalFlow);

        final InternalFlowResolver cut = new InternalFlowResolver(api);

        cut.provideFlows(ctx).test().assertResult(internalFlow);
    }

    @Test
    void should_name_the_internal_flow_internal_when_it_has_no_name() {
        final Step step = Step.builder().policy("guardian-policy").build();
        when(api.getInternalFlow()).thenReturn(Flow.builder().request(List.of(step)).build());

        final InternalFlowResolver cut = new InternalFlowResolver(api);

        cut
            .provideFlows(ctx)
            .test()
            .assertValue(flow -> "internal".equals(flow.getName()) && flow.getRequest().equals(List.of(step)));
    }

    @Test
    void should_provide_no_flow_when_the_internal_flow_is_disabled() {
        when(api.getInternalFlow()).thenReturn(Flow.builder().enabled(false).build());

        final InternalFlowResolver cut = new InternalFlowResolver(api);

        cut.provideFlows(ctx).test().assertResult();
    }

    @Test
    void should_provide_no_flow_when_there_is_no_internal_flow() {
        when(api.getInternalFlow()).thenReturn(null);

        final InternalFlowResolver cut = new InternalFlowResolver(api);

        cut.provideFlows(ctx).test().assertResult();
    }

    @Test
    void should_resolve_the_internal_flow_without_evaluating_its_selectors_or_condition() {
        // The context is not stubbed: neither the selector nor the condition can be evaluated against it.
        final Flow internalFlow = Flow.builder()
            .name("guardians")
            .selectors(
                List.of(
                    HttpSelector.builder().path("/never-called").build(),
                    ConditionSelector.builder().condition("{#request.headers['never-set'] != null}").build()
                )
            )
            .build();
        when(api.getInternalFlow()).thenReturn(internalFlow);

        final InternalFlowResolver cut = new InternalFlowResolver(api);

        cut.resolve(ctx).test().assertResult(internalFlow);
    }

    @Test
    void should_resolve_no_flow_when_the_internal_flow_is_disabled() {
        when(api.getInternalFlow()).thenReturn(Flow.builder().enabled(false).build());

        final InternalFlowResolver cut = new InternalFlowResolver(api);

        cut.resolve(ctx).test().assertResult();
    }
}

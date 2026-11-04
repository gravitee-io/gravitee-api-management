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
package io.gravitee.gateway.reactive.handlers.api.v4.flow;

import static io.gravitee.gateway.reactive.api.ExecutionPhase.REQUEST;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.execution.FlowExecution;
import io.gravitee.gateway.opentelemetry.TracingContext;
import io.gravitee.gateway.reactive.core.context.DefaultExecutionContext;
import io.gravitee.gateway.reactive.core.context.MutableRequest;
import io.gravitee.gateway.reactive.core.context.MutableResponse;
import io.gravitee.gateway.reactive.core.context.interruption.InterruptionFailureException;
import io.gravitee.gateway.reactive.handlers.api.v4.Api;
import io.gravitee.gateway.reactive.handlers.api.v4.flow.resolver.FlowResolverFactory;
import io.gravitee.gateway.reactive.policy.HttpPolicyChain;
import io.gravitee.gateway.reactive.v4.flow.FlowResolver;
import io.gravitee.gateway.reactive.v4.policy.PolicyChainFactory;
import io.gravitee.reporter.api.v4.metric.Metrics;
import io.reactivex.rxjava3.core.Flowable;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FlowChainFactoryTest {

    @Mock
    private PolicyChainFactory policyChainFactory;

    @Mock
    private FlowResolverFactory flowResolverFactory;

    @Mock
    private FlowResolver internalFlowResolver;

    @Mock
    private FlowResolver apiFlowResolver;

    private FlowChainFactory cut;
    private Api api;
    private DefaultExecutionContext ctx;

    @BeforeEach
    void init() {
        cut = new FlowChainFactory(policyChainFactory, flowResolverFactory);
        ctx = new DefaultExecutionContext(mock(MutableRequest.class), mock(MutableResponse.class));
        ctx.metrics(new Metrics());
    }

    @Test
    void should_still_interrupt_a_match_required_api_when_only_the_internal_flow_ran() {
        givenAnApiRequiringAFlowMatch();
        final Flow internalFlow = Flow.builder().name("internal").build();
        when(flowResolverFactory.forInternalFlow(api)).thenReturn(internalFlowResolver);
        when(internalFlowResolver.resolve(ctx)).thenReturn(Flowable.just(internalFlow));
        when(policyChainFactory.create("internal", internalFlow, REQUEST)).thenReturn(new HttpPolicyChain("internal", List.of(), REQUEST));
        doReturn(apiFlowResolver).when(flowResolverFactory).forApi(api);
        when(flowResolverFactory.deferredConditionFilter(api)).thenReturn(FlowResolverFactory.NO_DEFERRED_CONDITION);
        when(apiFlowResolver.resolve(ctx)).thenReturn(Flowable.empty());

        final FlowChain internalChain = cut.createInternalFlow(api, TracingContext.noop());
        final FlowChain apiChain = cut.createApiFlow(api, TracingContext.noop());

        internalChain.execute(ctx, REQUEST).andThen(apiChain.execute(ctx, REQUEST)).test().assertError(InterruptionFailureException.class);
    }

    private void givenAnApiRequiringAFlowMatch() {
        final FlowExecution flowExecution = new FlowExecution();
        flowExecution.setMatchRequired(true);
        final io.gravitee.definition.model.v4.Api definition = new io.gravitee.definition.model.v4.Api();
        definition.setFlowExecution(flowExecution);
        api = new Api(definition);
    }
}

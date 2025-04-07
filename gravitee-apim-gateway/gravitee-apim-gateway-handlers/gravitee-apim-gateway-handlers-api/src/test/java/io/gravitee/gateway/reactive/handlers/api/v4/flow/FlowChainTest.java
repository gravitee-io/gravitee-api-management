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

import static io.gravitee.gateway.reactive.handlers.api.v4.flow.FlowChain.INTERNAL_CONTEXT_ATTRIBUTES_FLOWS_MATCHED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.gravitee.definition.model.v4.flow.FlowV4Impl;
import io.gravitee.gateway.reactive.api.ExecutionPhase;
import io.gravitee.gateway.reactive.api.context.ExecutionContext;
import io.gravitee.gateway.reactive.core.context.interruption.InterruptionFailureException;
import io.gravitee.gateway.reactive.policy.HttpPolicyChain;
import io.gravitee.gateway.reactive.v4.flow.FlowResolver;
import io.gravitee.gateway.reactive.v4.policy.PolicyChainFactory;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.observers.TestObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class FlowChainTest {

    protected static final String FLOW_CHAIN_ID = "unit-test";
    protected static final String MOCK_ERROR_MESSAGE = "Mock error";

    @Mock
    private ExecutionContext ctx;

    @Mock
    private FlowResolver flowResolver;

    @Mock
    private PolicyChainFactory policyChainFactory;

    private FlowChain cut;

    @BeforeEach
    public void init() {
        cut = new FlowChain(FLOW_CHAIN_ID, flowResolver, policyChainFactory);
    }

    @Test
    void should_execute_when_page_is_Request() {
        final FlowV4Impl flow1 = mock(FlowV4Impl.class);
        final FlowV4Impl flow2 = mock(FlowV4Impl.class);

        final Flowable<FlowV4Impl> resolvedFlows = Flowable.just(flow1, flow2);
        when(flowResolver.resolve(ctx)).thenReturn(resolvedFlows);

        final HttpPolicyChain policyChain1 = mock(HttpPolicyChain.class);
        final HttpPolicyChain policyChain2 = mock(HttpPolicyChain.class);

        when(policyChainFactory.create(FLOW_CHAIN_ID, flow1, ExecutionPhase.REQUEST)).thenReturn(policyChain1);
        when(policyChainFactory.create(FLOW_CHAIN_ID, flow2, ExecutionPhase.REQUEST)).thenReturn(policyChain2);

        when(policyChain1.execute(ctx)).thenReturn(Completable.complete());
        when(policyChain2.execute(ctx)).thenReturn(Completable.complete());

        final TestObserver<Void> obs = cut.execute(ctx, ExecutionPhase.REQUEST).test();

        obs.assertResult();

        verify(ctx, times(1)).setInternalAttribute(eq("flow." + FLOW_CHAIN_ID), any());
    }

    @Test
    void should_execute_when_page_is_MessageRequest() {
        final FlowV4Impl flow1 = mock(FlowV4Impl.class);
        final FlowV4Impl flow2 = mock(FlowV4Impl.class);

        final Flowable<FlowV4Impl> resolvedFlows = Flowable.just(flow1, flow2);
        when(flowResolver.resolve(ctx)).thenReturn(resolvedFlows);

        final HttpPolicyChain policyChain1 = mock(HttpPolicyChain.class);
        final HttpPolicyChain policyChain2 = mock(HttpPolicyChain.class);

        when(policyChainFactory.create(FLOW_CHAIN_ID, flow1, ExecutionPhase.MESSAGE_REQUEST)).thenReturn(policyChain1);
        when(policyChainFactory.create(FLOW_CHAIN_ID, flow2, ExecutionPhase.MESSAGE_REQUEST)).thenReturn(policyChain2);

        when(policyChain1.execute(ctx)).thenReturn(Completable.complete());
        when(policyChain2.execute(ctx)).thenReturn(Completable.complete());

        final TestObserver<Void> obs = cut.execute(ctx, ExecutionPhase.MESSAGE_REQUEST).test();

        obs.assertResult();

        verify(ctx, times(1)).setInternalAttribute(eq("flow." + FLOW_CHAIN_ID), any());
    }

    @Test
    void should_execute_when_page_is_Response() {
        final FlowV4Impl flow1 = mock(FlowV4Impl.class);
        final FlowV4Impl flow2 = mock(FlowV4Impl.class);

        final Flowable<FlowV4Impl> resolvedFlows = Flowable.just(flow1, flow2);
        when(flowResolver.resolve(ctx)).thenReturn(resolvedFlows);

        final HttpPolicyChain policyChain1 = mock(HttpPolicyChain.class);
        final HttpPolicyChain policyChain2 = mock(HttpPolicyChain.class);

        when(policyChainFactory.create(FLOW_CHAIN_ID, flow1, ExecutionPhase.RESPONSE)).thenReturn(policyChain1);
        when(policyChainFactory.create(FLOW_CHAIN_ID, flow2, ExecutionPhase.RESPONSE)).thenReturn(policyChain2);

        when(policyChain1.execute(ctx)).thenReturn(Completable.complete());
        when(policyChain2.execute(ctx)).thenReturn(Completable.complete());

        final TestObserver<Void> obs = cut.execute(ctx, ExecutionPhase.RESPONSE).test();

        obs.assertResult();

        verify(ctx, times(1)).setInternalAttribute(eq("flow." + FLOW_CHAIN_ID), any());
    }

    @Test
    void should_execute_when_page_is_MessageResponse() {
        final FlowV4Impl flow1 = mock(FlowV4Impl.class);
        final FlowV4Impl flow2 = mock(FlowV4Impl.class);

        final Flowable<FlowV4Impl> resolvedFlows = Flowable.just(flow1, flow2);
        when(flowResolver.resolve(ctx)).thenReturn(resolvedFlows);

        final HttpPolicyChain policyChain1 = mock(HttpPolicyChain.class);
        final HttpPolicyChain policyChain2 = mock(HttpPolicyChain.class);

        when(policyChainFactory.create(FLOW_CHAIN_ID, flow1, ExecutionPhase.MESSAGE_RESPONSE)).thenReturn(policyChain1);
        when(policyChainFactory.create(FLOW_CHAIN_ID, flow2, ExecutionPhase.MESSAGE_RESPONSE)).thenReturn(policyChain2);

        when(policyChain1.execute(ctx)).thenReturn(Completable.complete());
        when(policyChain2.execute(ctx)).thenReturn(Completable.complete());

        final TestObserver<Void> obs = cut.execute(ctx, ExecutionPhase.MESSAGE_RESPONSE).test();

        obs.assertResult();

        verify(ctx, times(1)).setInternalAttribute(eq("flow." + FLOW_CHAIN_ID), any());
    }

    @Test
    void should_execute_and_reuse_flow_resolved() {
        final FlowV4Impl flow1 = mock(FlowV4Impl.class);
        final FlowV4Impl flow2 = mock(FlowV4Impl.class);

        final Flowable<FlowV4Impl> resolvedFlows = Flowable.just(flow1, flow2);
        when(ctx.getInternalAttribute(eq("flow." + FLOW_CHAIN_ID))).thenReturn(resolvedFlows);

        final HttpPolicyChain policyChain1 = mock(HttpPolicyChain.class);
        final HttpPolicyChain policyChain2 = mock(HttpPolicyChain.class);

        when(policyChainFactory.create(FLOW_CHAIN_ID, flow1, ExecutionPhase.RESPONSE)).thenReturn(policyChain1);
        when(policyChainFactory.create(FLOW_CHAIN_ID, flow2, ExecutionPhase.RESPONSE)).thenReturn(policyChain2);

        when(policyChain1.execute(ctx)).thenReturn(Completable.complete());
        when(policyChain2.execute(ctx)).thenReturn(Completable.complete());

        final TestObserver<Void> obs = cut.execute(ctx, ExecutionPhase.RESPONSE).test();

        obs.assertResult();
        // Make sure no flow resolution occurred when already resolved.
        verify(ctx, times(0)).setInternalAttribute(eq("flow." + FLOW_CHAIN_ID), any());
        verifyNoInteractions(flowResolver);
    }

    @Test
    void should_execute_only_flow1_if_error() {
        final FlowV4Impl flow1 = mock(FlowV4Impl.class);
        final FlowV4Impl flow2 = mock(FlowV4Impl.class);

        final Flowable<FlowV4Impl> resolvedFlows = Flowable.just(flow1, flow2);
        when(flowResolver.resolve(ctx)).thenReturn(resolvedFlows);

        final HttpPolicyChain policyChain1 = mock(HttpPolicyChain.class);

        when(policyChainFactory.create(FLOW_CHAIN_ID, flow1, ExecutionPhase.REQUEST)).thenReturn(policyChain1);
        when(policyChain1.execute(ctx)).thenReturn(Completable.error(new RuntimeException(MOCK_ERROR_MESSAGE)));

        final TestObserver<Void> obs = cut.execute(ctx, ExecutionPhase.REQUEST).test();

        obs.assertError(RuntimeException.class);
        obs.assertError(t -> MOCK_ERROR_MESSAGE.equals(t.getMessage()));

        // Make sure policy chain has not been created for flow2.
        verify(policyChainFactory, times(0)).create(FLOW_CHAIN_ID, flow2, ExecutionPhase.REQUEST);
    }

    @Test
    void should_interrupt_when_no_current_match_and_no_previous_match() {
        cut = new FlowChain(FLOW_CHAIN_ID, flowResolver, policyChainFactory, true, true);
        lenient().when(ctx.getInternalAttribute(eq(INTERNAL_CONTEXT_ATTRIBUTES_FLOWS_MATCHED))).thenReturn(false);

        when(ctx.interruptWith(any())).thenAnswer(inv -> Completable.error(new InterruptionFailureException(inv.getArgument(0))));
        final Flowable<FlowV4Impl> resolvedFlows = Flowable.empty();
        when(flowResolver.resolve(ctx)).thenReturn(resolvedFlows);

        final TestObserver<Void> obs = cut.execute(ctx, ExecutionPhase.REQUEST).test();

        obs.assertError(InterruptionFailureException.class);
    }

    @Test
    void should_interrupt_when_no_current_match_and_no_previous_value() {
        cut = new FlowChain(FLOW_CHAIN_ID, flowResolver, policyChainFactory, true, true);
        lenient().when(ctx.getInternalAttribute(eq(INTERNAL_CONTEXT_ATTRIBUTES_FLOWS_MATCHED))).thenReturn(null);

        when(ctx.interruptWith(any())).thenAnswer(inv -> Completable.error(new InterruptionFailureException(inv.getArgument(0))));
        final Flowable<FlowV4Impl> resolvedFlows = Flowable.empty();
        when(flowResolver.resolve(ctx)).thenReturn(resolvedFlows);

        final TestObserver<Void> obs = cut.execute(ctx, ExecutionPhase.REQUEST).test();

        obs.assertError(InterruptionFailureException.class);

        verify(ctx).setInternalAttribute(INTERNAL_CONTEXT_ATTRIBUTES_FLOWS_MATCHED, false);
    }

    @Test
    void should_not_interrupt_when_no_match_but_previous_match() {
        cut = new FlowChain(FLOW_CHAIN_ID, flowResolver, policyChainFactory, true, true);
        lenient().when(ctx.getInternalAttribute(eq(INTERNAL_CONTEXT_ATTRIBUTES_FLOWS_MATCHED))).thenReturn(true);
        final Flowable<FlowV4Impl> resolvedFlows = Flowable.empty();
        when(flowResolver.resolve(ctx)).thenReturn(resolvedFlows);

        final TestObserver<Void> obs = cut.execute(ctx, ExecutionPhase.REQUEST).test();

        obs.assertResult();
    }

    @Test
    void should_not_set_flows_matched_attribute_when_validate_flow_matching_is_false() {
        cut = new FlowChain(FLOW_CHAIN_ID, flowResolver, policyChainFactory);
        final Flowable<FlowV4Impl> resolvedFlows = Flowable.empty();
        when(flowResolver.resolve(ctx)).thenReturn(resolvedFlows);

        final TestObserver<Void> obs = cut.execute(ctx, ExecutionPhase.REQUEST).test();

        obs.assertResult();
        verify(ctx, never()).setInternalAttribute(eq(INTERNAL_CONTEXT_ATTRIBUTES_FLOWS_MATCHED), anyBoolean());
    }
}

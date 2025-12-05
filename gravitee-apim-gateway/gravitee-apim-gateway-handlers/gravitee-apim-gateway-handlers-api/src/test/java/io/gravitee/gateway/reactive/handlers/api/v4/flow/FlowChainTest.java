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

import static io.gravitee.gateway.reactive.api.ExecutionPhase.MESSAGE_REQUEST;
import static io.gravitee.gateway.reactive.api.ExecutionPhase.MESSAGE_RESPONSE;
import static io.gravitee.gateway.reactive.api.ExecutionPhase.REQUEST;
import static io.gravitee.gateway.reactive.api.ExecutionPhase.RESPONSE;
import static io.gravitee.gateway.reactive.handlers.api.v4.flow.FlowChain.INTERNAL_CONTEXT_ATTRIBUTES_FLOWS_MATCHED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.gateway.reactive.api.ExecutionPhase;
import io.gravitee.gateway.reactive.api.context.http.HttpMessageExecutionContext;
import io.gravitee.gateway.reactive.api.context.http.HttpPlainExecutionContext;
import io.gravitee.gateway.reactive.api.policy.http.HttpPolicy;
import io.gravitee.gateway.reactive.core.context.DefaultExecutionContext;
import io.gravitee.gateway.reactive.core.context.MutableRequest;
import io.gravitee.gateway.reactive.core.context.MutableResponse;
import io.gravitee.gateway.reactive.core.context.interruption.InterruptionFailureException;
import io.gravitee.gateway.reactive.policy.HttpPolicyChain;
import io.gravitee.gateway.reactive.v4.flow.FlowResolver;
import io.gravitee.gateway.reactive.v4.policy.PolicyChainFactory;
import io.gravitee.reporter.api.v4.metric.Metrics;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.observers.TestObserver;
import java.util.ArrayList;
import java.util.List;
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
    private Flow flow1, flow2;

    @Mock
    private FlowResolver flowResolver;

    @Mock
    private PolicyChainFactory policyChainFactory;

    private DefaultExecutionContext ctx;
    private static List<String> executionOrder;
    private HttpPolicy policy1, policy2, policy3, policy4, policy5, policy6;

    private FlowChain cut;

    @BeforeEach
    void init() {
        cut = new FlowChain(FLOW_CHAIN_ID, flowResolver, policyChainFactory);
        ctx = buildExecutionContext();
        executionOrder = new ArrayList<>();
        lenient().when(flowResolver.resolve(ctx)).thenReturn(Flowable.just(flow1, flow2));

        policy1 = new TestPolicy("policy1");
        policy2 = new TestPolicy("policy2");
        policy3 = new TestPolicy("policy3");
        policy4 = new TestPolicy("policy4");
        policy5 = new TestPolicy("policy5");
        policy6 = new TestPolicy("policy6");
    }

    @Test
    void should_execute_on_request() {
        buildPolicyChain("pc-flow1", flow1, REQUEST, policy1);
        buildPolicyChain("pc-flow2", flow2, REQUEST, policy2);

        final TestObserver<Void> obs = cut.execute(ctx, REQUEST).test();

        obs.assertResult();

        assertThat(executionOrder).containsExactly("policy1-onRequest", "policy2-onRequest");
    }

    @Test
    void should_execute_on_message_request() {
        buildPolicyChain("pc-flow1", flow1, MESSAGE_REQUEST, policy1);
        buildPolicyChain("pc-flow2", flow2, MESSAGE_REQUEST, policy2);

        final TestObserver<Void> obs = cut.execute(ctx, MESSAGE_REQUEST).test();

        obs.assertResult();

        assertThat(executionOrder).containsExactly("policy1-onMessageRequest", "policy2-onMessageRequest");
    }

    @Test
    void should_execute_on_response() {
        buildPolicyChain("pc-flow1", flow1, RESPONSE, policy1);
        buildPolicyChain("pc-flow2", flow2, RESPONSE, policy2);

        // Any time a RESPONSE phase is executed, the response actions set during the request phase are supposed to be executed.
        buildPolicyChain("pc-flow1", flow1, REQUEST);
        buildPolicyChain("pc-flow2", flow2, REQUEST);

        final TestObserver<Void> obs = cut.execute(ctx, RESPONSE).test();

        obs.assertResult();

        assertThat(executionOrder).containsExactly("policy1-onResponse", "policy2-onResponse");
    }

    @Test
    void should_execute_on_message_response() {
        buildPolicyChain("pc-flow1", flow1, MESSAGE_RESPONSE, policy1);
        buildPolicyChain("pc-flow2", flow2, MESSAGE_RESPONSE, policy2);

        final TestObserver<Void> obs = cut.execute(ctx, MESSAGE_RESPONSE).test();

        obs.assertResult();

        assertThat(executionOrder).containsExactly("policy1-onMessageResponse", "policy2-onMessageResponse");
    }

    @Test
    void should_execute_and_reuse_flow_resolved() {
        buildPolicyChain("pc-flow1", flow1, REQUEST, policy1);
        buildPolicyChain("pc-flow2", flow2, REQUEST, policy2);

        // Force the resolved flows to be already present in the context.
        ctx.setInternalAttribute("flow." + FLOW_CHAIN_ID, Flowable.just(flow1, flow2));

        final TestObserver<Void> obs = cut.execute(ctx, REQUEST).test();

        obs.assertResult();

        assertThat(executionOrder).containsExactly("policy1-onRequest", "policy2-onRequest");

        // Make sure no flow resolution occurred when already resolved.
        verifyNoInteractions(flowResolver);
    }

    @Test
    void should_execute_only_flow1_if_error() {
        buildPolicyChain("pc-flow1", flow1, REQUEST, new TestPolicy("policy1", true));
        buildPolicyChain("pc-flow2", flow2, REQUEST, policy2);

        final TestObserver<Void> obs = cut.execute(ctx, REQUEST).test();

        obs.assertError(RuntimeException.class);
        obs.assertError(t -> MOCK_ERROR_MESSAGE.equals(t.getMessage()));

        // Make sure policy chain has not been created for flow2.
        verify(policyChainFactory, times(0)).create(FLOW_CHAIN_ID, flow2, REQUEST);
    }

    @Test
    void should_interrupt_when_no_current_match_and_no_previous_match() {
        cut = new FlowChain(FLOW_CHAIN_ID, flowResolver, policyChainFactory, true, true);
        ctx.setInternalAttribute(INTERNAL_CONTEXT_ATTRIBUTES_FLOWS_MATCHED, false);

        when(flowResolver.resolve(ctx)).thenReturn(Flowable.empty());

        final TestObserver<Void> obs = cut.execute(ctx, REQUEST).test();

        obs.assertError(InterruptionFailureException.class);
    }

    @Test
    void should_interrupt_when_no_current_match_and_no_previous_value() {
        cut = new FlowChain(FLOW_CHAIN_ID, flowResolver, policyChainFactory, true, true);
        ctx.setInternalAttribute(INTERNAL_CONTEXT_ATTRIBUTES_FLOWS_MATCHED, null);

        when(flowResolver.resolve(ctx)).thenReturn(Flowable.empty());

        final TestObserver<Void> obs = cut.execute(ctx, REQUEST).test();

        obs.assertError(InterruptionFailureException.class);

        assertThat((boolean) ctx.getInternalAttribute(INTERNAL_CONTEXT_ATTRIBUTES_FLOWS_MATCHED)).isEqualTo(false);
    }

    @Test
    void should_not_interrupt_when_no_match_but_previous_match() {
        cut = new FlowChain(FLOW_CHAIN_ID, flowResolver, policyChainFactory, true, true);
        ctx.setInternalAttribute(INTERNAL_CONTEXT_ATTRIBUTES_FLOWS_MATCHED, true);

        when(flowResolver.resolve(ctx)).thenReturn(Flowable.empty());
        final TestObserver<Void> obs = cut.execute(ctx, REQUEST).test();

        obs.assertResult();
    }

    @Test
    void should_not_set_flows_matched_attribute_when_validate_flow_matching_is_false() {
        cut = new FlowChain(FLOW_CHAIN_ID, flowResolver, policyChainFactory);
        when(flowResolver.resolve(ctx)).thenReturn(Flowable.empty());

        final TestObserver<Void> obs = cut.execute(ctx, REQUEST).test();

        obs.assertResult();
        assertThat((Boolean) ctx.getInternalAttribute(INTERNAL_CONTEXT_ATTRIBUTES_FLOWS_MATCHED)).isNull();
    }

    @Test
    void should_execute_on_response_actions_in_reversed_order_before_response_policies() {
        final Flow flow = mock(Flow.class);

        buildPolicyChain("pc-flow1", flow, REQUEST, policy1, policy2, policy3);
        buildPolicyChain("pc-flow2", flow, RESPONSE, policy4, policy5, policy6);

        when(flowResolver.resolve(ctx)).thenReturn(Flowable.just(flow));

        final TestObserver<Void> obs = cut.execute(ctx, REQUEST).andThen(cut.execute(ctx, ExecutionPhase.RESPONSE)).test();

        obs.assertResult();

        assertThat(executionOrder).containsExactly(
            "policy1-onRequest",
            "policy2-onRequest",
            "policy3-onRequest",
            "policy3-actionActionOnResponse",
            "policy2-actionActionOnResponse",
            "policy1-actionActionOnResponse",
            "policy4-onResponse",
            "policy5-onResponse",
            "policy6-onResponse"
        );
    }

    private static @NonNull DefaultExecutionContext buildExecutionContext() {
        final MutableRequest request = mock(MutableRequest.class);
        final MutableResponse response = mock(MutableResponse.class);
        final DefaultExecutionContext ctx = new DefaultExecutionContext(request, response);
        ctx.metrics(new Metrics());
        return ctx;
    }

    private void buildPolicyChain(String id, Flow flow, ExecutionPhase phase, HttpPolicy... policies) {
        HttpPolicyChain httpPolicyChain = new HttpPolicyChain(id, List.of(policies), phase);
        lenient().when(policyChainFactory.create(FLOW_CHAIN_ID, flow, phase)).thenReturn(httpPolicyChain);
    }

    private static class TestPolicy implements HttpPolicy {

        private final String id;
        private final boolean error;

        public TestPolicy(String id) {
            this.id = id;
            this.error = false;
        }

        public TestPolicy(String id, boolean error) {
            this.id = id;
            this.error = error;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public Completable onRequest(HttpPlainExecutionContext ctx) {
            return Completable.defer(() -> {
                executionOrder.add(id + "-onRequest");

                if (error) {
                    return Completable.error(new RuntimeException(MOCK_ERROR_MESSAGE));
                }

                ctx.addActionOnResponse(this, ctx1 ->
                    Completable.complete().doOnComplete(() -> executionOrder.add(id + "-actionActionOnResponse"))
                );
                return Completable.complete();
            });
        }

        @Override
        public Completable onResponse(HttpPlainExecutionContext ctx) {
            return Completable.defer(() -> {
                executionOrder.add(id + "-onResponse");

                if (error) {
                    return Completable.error(new RuntimeException(MOCK_ERROR_MESSAGE));
                }

                return Completable.complete();
            });
        }

        @Override
        public Completable onMessageRequest(HttpMessageExecutionContext ctx) {
            return Completable.defer(() -> {
                executionOrder.add(id + "-onMessageRequest");

                if (error) {
                    return Completable.error(new RuntimeException(MOCK_ERROR_MESSAGE));
                }

                return Completable.complete();
            });
        }

        @Override
        public Completable onMessageResponse(HttpMessageExecutionContext ctx) {
            return Completable.defer(() -> {
                executionOrder.add(id + "-onMessageResponse");
                if (error) {
                    return Completable.error(new RuntimeException(MOCK_ERROR_MESSAGE));
                }
                return Completable.complete();
            });
        }
    }
}

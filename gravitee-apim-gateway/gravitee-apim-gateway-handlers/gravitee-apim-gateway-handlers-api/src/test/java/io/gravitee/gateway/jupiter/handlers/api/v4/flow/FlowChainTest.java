package io.gravitee.gateway.jupiter.handlers.api.v4.flow;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.gateway.jupiter.api.ExecutionPhase;
import io.gravitee.gateway.jupiter.api.context.ExecutionContext;
import io.gravitee.gateway.jupiter.policy.PolicyChain;
import io.gravitee.gateway.jupiter.v4.flow.FlowResolver;
import io.gravitee.gateway.jupiter.v4.policy.PolicyChainFactory;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.observers.TestObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
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
    public void shouldExecuteOnRequest() {
        final Flow flow1 = mock(Flow.class);
        final Flow flow2 = mock(Flow.class);

        final Flowable<Flow> resolvedFlows = Flowable.just(flow1, flow2);
        when(flowResolver.resolve(ctx)).thenReturn(resolvedFlows);

        final PolicyChain policyChain1 = mock(PolicyChain.class);
        final PolicyChain policyChain2 = mock(PolicyChain.class);

        when(policyChainFactory.create(FLOW_CHAIN_ID, flow1, ExecutionPhase.REQUEST)).thenReturn(policyChain1);
        when(policyChainFactory.create(FLOW_CHAIN_ID, flow2, ExecutionPhase.REQUEST)).thenReturn(policyChain2);

        when(policyChain1.execute(ctx)).thenReturn(Completable.complete());
        when(policyChain2.execute(ctx)).thenReturn(Completable.complete());

        final TestObserver<Void> obs = cut.execute(ctx, ExecutionPhase.REQUEST).test();

        obs.assertResult();

        verify(ctx, times(1)).setInternalAttribute(eq("flow." + FLOW_CHAIN_ID), any());
    }

    @Test
    public void shouldExecuteOnMessageRequest() {
        final Flow flow1 = mock(Flow.class);
        final Flow flow2 = mock(Flow.class);

        final Flowable<Flow> resolvedFlows = Flowable.just(flow1, flow2);
        when(flowResolver.resolve(ctx)).thenReturn(resolvedFlows);

        final PolicyChain policyChain1 = mock(PolicyChain.class);
        final PolicyChain policyChain2 = mock(PolicyChain.class);

        when(policyChainFactory.create(FLOW_CHAIN_ID, flow1, ExecutionPhase.MESSAGE_REQUEST)).thenReturn(policyChain1);
        when(policyChainFactory.create(FLOW_CHAIN_ID, flow2, ExecutionPhase.MESSAGE_REQUEST)).thenReturn(policyChain2);

        when(policyChain1.execute(ctx)).thenReturn(Completable.complete());
        when(policyChain2.execute(ctx)).thenReturn(Completable.complete());

        final TestObserver<Void> obs = cut.execute(ctx, ExecutionPhase.MESSAGE_REQUEST).test();

        obs.assertResult();

        verify(ctx, times(1)).setInternalAttribute(eq("flow." + FLOW_CHAIN_ID), any());
    }

    @Test
    public void shouldExecuteOnResponse() {
        final Flow flow1 = mock(Flow.class);
        final Flow flow2 = mock(Flow.class);

        final Flowable<Flow> resolvedFlows = Flowable.just(flow1, flow2);
        when(flowResolver.resolve(ctx)).thenReturn(resolvedFlows);

        final PolicyChain policyChain1 = mock(PolicyChain.class);
        final PolicyChain policyChain2 = mock(PolicyChain.class);

        when(policyChainFactory.create(FLOW_CHAIN_ID, flow1, ExecutionPhase.RESPONSE)).thenReturn(policyChain1);
        when(policyChainFactory.create(FLOW_CHAIN_ID, flow2, ExecutionPhase.RESPONSE)).thenReturn(policyChain2);

        when(policyChain1.execute(ctx)).thenReturn(Completable.complete());
        when(policyChain2.execute(ctx)).thenReturn(Completable.complete());

        final TestObserver<Void> obs = cut.execute(ctx, ExecutionPhase.RESPONSE).test();

        obs.assertResult();

        verify(ctx, times(1)).setInternalAttribute(eq("flow." + FLOW_CHAIN_ID), any());
    }

    @Test
    public void shouldExecuteOnMessageResponse() {
        final Flow flow1 = mock(Flow.class);
        final Flow flow2 = mock(Flow.class);

        final Flowable<Flow> resolvedFlows = Flowable.just(flow1, flow2);
        when(flowResolver.resolve(ctx)).thenReturn(resolvedFlows);

        final PolicyChain policyChain1 = mock(PolicyChain.class);
        final PolicyChain policyChain2 = mock(PolicyChain.class);

        when(policyChainFactory.create(FLOW_CHAIN_ID, flow1, ExecutionPhase.MESSAGE_RESPONSE)).thenReturn(policyChain1);
        when(policyChainFactory.create(FLOW_CHAIN_ID, flow2, ExecutionPhase.MESSAGE_RESPONSE)).thenReturn(policyChain2);

        when(policyChain1.execute(ctx)).thenReturn(Completable.complete());
        when(policyChain2.execute(ctx)).thenReturn(Completable.complete());

        final TestObserver<Void> obs = cut.execute(ctx, ExecutionPhase.MESSAGE_RESPONSE).test();

        obs.assertResult();

        verify(ctx, times(1)).setInternalAttribute(eq("flow." + FLOW_CHAIN_ID), any());
    }

    @Test
    public void shouldExecuteAndReusePreviousFlowResolution() {
        final Flow flow1 = mock(Flow.class);
        final Flow flow2 = mock(Flow.class);

        final Flowable<Flow> resolvedFlows = Flowable.just(flow1, flow2);
        when(ctx.getInternalAttribute(eq("flow." + FLOW_CHAIN_ID))).thenReturn(resolvedFlows);

        final PolicyChain policyChain1 = mock(PolicyChain.class);
        final PolicyChain policyChain2 = mock(PolicyChain.class);

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
    public void shouldExecuteOnlyFlow1IfError() {
        final Flow flow1 = mock(Flow.class);
        final Flow flow2 = mock(Flow.class);

        final Flowable<Flow> resolvedFlows = Flowable.just(flow1, flow2);
        when(flowResolver.resolve(ctx)).thenReturn(resolvedFlows);

        final PolicyChain policyChain1 = mock(PolicyChain.class);

        when(policyChainFactory.create(FLOW_CHAIN_ID, flow1, ExecutionPhase.REQUEST)).thenReturn(policyChain1);
        when(policyChain1.execute(ctx)).thenReturn(Completable.error(new RuntimeException(MOCK_ERROR_MESSAGE)));

        final TestObserver<Void> obs = cut.execute(ctx, ExecutionPhase.REQUEST).test();

        obs.assertErrorMessage(MOCK_ERROR_MESSAGE);

        // Make sure policy chain has not been created for flow2.
        verify(policyChainFactory, times(0)).create(FLOW_CHAIN_ID, flow2, ExecutionPhase.REQUEST);
    }
}

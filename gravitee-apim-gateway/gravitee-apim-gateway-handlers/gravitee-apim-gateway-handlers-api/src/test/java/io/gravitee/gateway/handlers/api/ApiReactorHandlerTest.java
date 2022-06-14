/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.handlers.api;

import static io.gravitee.common.component.Lifecycle.State.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.context.MutableExecutionContext;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.core.endpoint.lifecycle.GroupLifecycleManager;
import io.gravitee.gateway.core.processor.chain.DefaultStreamableProcessorChain;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.handlers.api.processor.OnErrorProcessorChainFactory;
import io.gravitee.gateway.handlers.api.processor.RequestProcessorChainFactory;
import io.gravitee.gateway.policy.DirectPolicyChain;
import io.gravitee.gateway.policy.NoOpPolicyChain;
import io.gravitee.gateway.policy.PolicyManager;
import io.gravitee.gateway.resource.ResourceLifecycleManager;
import io.gravitee.node.api.Node;
import io.gravitee.policy.api.PolicyResult;
import io.gravitee.reporter.api.http.Metrics;
import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.Schedulers;
import java.util.Date;
import java.util.List;
import java.util.concurrent.*;
import junit.framework.AssertionFailedError;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiReactorHandlerTest {

    @Mock
    Handler<ExecutionContext> handler;

    @Mock
    private GroupLifecycleManager groupLifecycleManager;

    @Mock
    private PolicyManager policyManager;

    @Mock
    private ResourceLifecycleManager resourceLifecycleManager;

    @Mock
    private MutableExecutionContext executionContext;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private RequestProcessorChainFactory requestProcessorChainFactory;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private OnErrorProcessorChainFactory errorProcessorChainFactory;

    @Mock
    Node node;

    @Mock
    Api api;

    @Mock
    Request request;

    @Before
    public void setUp() {
        when(node.lifecycleState()).thenReturn(STARTED);
        when(request.metrics()).thenReturn(Metrics.on(0).build());
        when(executionContext.request()).thenReturn(request);
        when(api.getDeployedAt()).thenReturn(new Date());
    }

    @Test
    public void stopShouldWaitWithPendingRequest() throws Exception {
        final long pendingRequestsTimeout = 10000L;

        ApiReactorHandler handler = createHandler(pendingRequestsTimeout);

        // handle one request that will not terminate
        handler.doHandle(executionContext);

        TestObserver<Boolean> stopObserver = stopAndObserve(handler);
        assertFalse(stopObserver.awaitTerminalEvent(1, TimeUnit.SECONDS));

        verify(policyManager, never()).stop();
        verify(resourceLifecycleManager, never()).stop();
        verify(groupLifecycleManager, never()).stop();
    }

    @Test
    public void stopShouldNotWaitWithNoPendingRequest() throws Exception {
        final long pendingRequestsTimeout = 15000L;

        ApiReactorHandler handler = createHandler(pendingRequestsTimeout);

        TestObserver<Boolean> stopObserver = stopAndObserve(handler);
        assertTrue(stopObserver.awaitTerminalEvent(1, TimeUnit.SECONDS));

        verify(policyManager, times(1)).stop();
        verify(resourceLifecycleManager, times(1)).stop();
        verify(groupLifecycleManager, times(1)).stop();
    }

    @Test
    public void stopShouldNotWaitForFailedRequests() throws Exception {
        final long pendingRequestsTimeout = 5000L;

        // make any handled request fail
        DirectPolicyChain failingPolicyChain = new DirectPolicyChain(PolicyResult.failure("error"), executionContext);
        when(requestProcessorChainFactory.create()).thenReturn(new DefaultStreamableProcessorChain<>(List.of(failingPolicyChain)));

        NoOpPolicyChain noOpPolicyChain = new NoOpPolicyChain(executionContext);
        when(errorProcessorChainFactory.create()).thenReturn(new DefaultStreamableProcessorChain<>(List.of(noOpPolicyChain)));

        ApiReactorHandler handler = createHandler(pendingRequestsTimeout);

        // handle one request
        handler.doHandle(executionContext);

        verify(errorProcessorChainFactory, times(1)).create();
        verify(requestProcessorChainFactory, times(1)).create();

        TestObserver<Boolean> stopObserver = stopAndObserve(handler);
        assertTrue(stopObserver.awaitTerminalEvent(1, TimeUnit.SECONDS));

        verify(policyManager, times(1)).stop();
        verify(resourceLifecycleManager, times(1)).stop();
        verify(groupLifecycleManager, times(1)).stop();
    }

    private ApiReactorHandler createHandler(long pendingRequestsTimeout) {
        ApiReactorHandler apiReactorHandler = new ApiReactorHandler(api);
        apiReactorHandler.handler(handler);
        apiReactorHandler.setNode(node);
        apiReactorHandler.setPendingRequestsTimeout(pendingRequestsTimeout);
        apiReactorHandler.setGroupLifecycleManager(groupLifecycleManager);
        apiReactorHandler.setResourceLifecycleManager(resourceLifecycleManager);
        apiReactorHandler.setPolicyManager(policyManager);
        apiReactorHandler.setRequestProcessorChain(requestProcessorChainFactory);
        apiReactorHandler.setErrorProcessorChain(errorProcessorChainFactory);
        return apiReactorHandler;
    }

    private static TestObserver<Boolean> stopAndObserve(ApiReactorHandler handler) {
        return Observable.fromCallable(() -> doStop(handler)).subscribeOn(Schedulers.io()).test();
    }

    private static boolean doStop(ApiReactorHandler handler) {
        try {
            handler.doStop();
            return true;
        } catch (Exception e) {
            throw new AssertionFailedError(e.getMessage());
        }
    }
}

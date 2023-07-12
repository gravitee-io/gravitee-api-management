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
package io.gravitee.gateway.jupiter.handlers.api.v4;

import static io.gravitee.gateway.jupiter.api.ExecutionPhase.REQUEST;
import static io.gravitee.gateway.jupiter.api.ExecutionPhase.RESPONSE;
import static io.reactivex.Completable.complete;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.gateway.core.component.CompositeComponentProvider;
import io.gravitee.gateway.jupiter.api.connector.entrypoint.async.EntrypointAsyncConnector;
import io.gravitee.gateway.jupiter.api.context.ContextAttributes;
import io.gravitee.gateway.jupiter.api.invoker.Invoker;
import io.gravitee.gateway.jupiter.core.context.MutableExecutionContext;
import io.gravitee.gateway.jupiter.core.context.MutableRequest;
import io.gravitee.gateway.jupiter.core.context.MutableResponse;
import io.gravitee.gateway.jupiter.core.v4.entrypoint.HttpEntrypointConnectorResolver;
import io.gravitee.gateway.jupiter.handlers.api.flow.FlowChain;
import io.gravitee.gateway.jupiter.handlers.api.flow.FlowChainFactory;
import io.gravitee.gateway.jupiter.handlers.api.v4.security.SecurityChain;
import io.gravitee.gateway.jupiter.policy.PolicyManager;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AsyncApiReactorTest {

    public static final String CONTEXT_PATH = "context-path";
    public static final String API_ID = "api-id";
    public static final String ORGANIZATION_ID = "organization-id";
    public static final String ENVIRONMENT_ID = "environment-id";

    @InjectMocks
    private AsyncApiReactor asyncApiReactor;

    @Mock
    private Api api;

    @Mock
    private CompositeComponentProvider apiComponentProvider;

    @Mock
    private PolicyManager policyManager;

    @Mock
    private HttpEntrypointConnectorResolver asyncEntrypointResolver;

    @Mock
    private Invoker defaultInvoker;

    @Mock
    private FlowChainFactory flowChainFactory;

    @Mock
    private MutableExecutionContext executionContext;

    @Mock
    private MutableRequest request;

    @Mock
    private MutableResponse response;

    @Mock
    private EntrypointAsyncConnector entrypointConnector;

    @Mock
    private FlowChain platformFlowChain;

    @Mock
    private SecurityChain securityChain;

    @BeforeEach
    public void init() {
        lenient().when(executionContext.request()).thenReturn(request);
        lenient().when(executionContext.response()).thenReturn(response);
        lenient().when(request.contextPath()).thenReturn(CONTEXT_PATH);
        lenient().when(api.getId()).thenReturn(API_ID);
        lenient().when(api.getDeployedAt()).thenReturn(new Date());
        lenient().when(api.getOrganizationId()).thenReturn(ORGANIZATION_ID);
        lenient().when(api.getEnvironmentId()).thenReturn(ENVIRONMENT_ID);
    }

    @Test
    public void shouldReturnAsyncApiType() {
        ApiType apiType = asyncApiReactor.apiType();
        assertThat(apiType).isEqualTo(ApiType.ASYNC);
    }

    @Test
    public void shouldPrepareContextAttributes() {
        asyncApiReactor.handle(executionContext);

        verify(executionContext).setAttribute(ContextAttributes.ATTR_CONTEXT_PATH, CONTEXT_PATH);
        verify(executionContext).setAttribute(ContextAttributes.ATTR_API, API_ID);
        verify(executionContext).setAttribute(ContextAttributes.ATTR_ORGANIZATION, ORGANIZATION_ID);
        verify(executionContext).setAttribute(ContextAttributes.ATTR_ENVIRONMENT, ENVIRONMENT_ID);
        verify(executionContext).setInternalAttribute(ContextAttributes.ATTR_API, api);
    }

    @Test
    public void shouldReturn404WhenNoEntrypoint() {
        when(asyncEntrypointResolver.resolve(executionContext)).thenReturn(null);

        asyncApiReactor.handle(executionContext).test();

        verify(response).status(404);
        verify(response).reason("No entrypoint matches the incoming request");
        verify(response).end();
    }

    @Test
    public void shouldExecuteFlowChainWhenEntrypointFound() {
        when(asyncEntrypointResolver.resolve(executionContext)).thenReturn(entrypointConnector);

        ReflectionTestUtils.setField(asyncApiReactor, "platformFlowChain", platformFlowChain);
        ReflectionTestUtils.setField(asyncApiReactor, "securityChain", securityChain);
        when(platformFlowChain.execute(executionContext, REQUEST)).thenReturn(complete());
        when(securityChain.execute(executionContext)).thenReturn(complete());
        when(entrypointConnector.handleRequest(executionContext)).thenReturn(complete());
        when(entrypointConnector.handleResponse(executionContext)).thenReturn(complete());
        when(platformFlowChain.execute(executionContext, RESPONSE)).thenReturn(complete());

        asyncApiReactor.handle(executionContext).test();

        // verify flow chain has been executed in the right order
        InOrder inOrder = inOrder(platformFlowChain, securityChain, entrypointConnector, entrypointConnector, platformFlowChain);
        inOrder.verify(platformFlowChain).execute(executionContext, REQUEST);
        inOrder.verify(securityChain).execute(executionContext);
        inOrder.verify(entrypointConnector).handleRequest(executionContext);
        inOrder.verify(entrypointConnector).handleResponse(executionContext);
        inOrder.verify(platformFlowChain).execute(executionContext, RESPONSE);
    }
}

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
package io.gravitee.gateway.jupiter.handlers.api.v4;

import static io.gravitee.gateway.jupiter.api.ExecutionPhase.REQUEST;
import static io.gravitee.gateway.jupiter.api.ExecutionPhase.RESPONSE;
import static io.reactivex.Completable.complete;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.gateway.core.component.CompositeComponentProvider;
import io.gravitee.gateway.jupiter.api.connector.entrypoint.async.EntrypointAsyncConnector;
import io.gravitee.gateway.jupiter.api.context.ExecutionContext;
import io.gravitee.gateway.jupiter.api.invoker.Invoker;
import io.gravitee.gateway.jupiter.core.context.MutableMessageExecutionContext;
import io.gravitee.gateway.jupiter.core.context.MutableMessageRequest;
import io.gravitee.gateway.jupiter.core.context.MutableMessageResponse;
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
class ASyncApiReactorTest {

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
    private MutableMessageExecutionContext messageExecutionContext;

    @Mock
    private MutableMessageRequest messageRequest;

    @Mock
    private MutableMessageResponse messageResponse;

    @Mock
    private EntrypointAsyncConnector entrypointConnector;

    @Mock
    private FlowChain platformFlowChain;

    @Mock
    private SecurityChain securityChain;

    @BeforeEach
    public void init() {
        lenient().when(messageExecutionContext.request()).thenReturn(messageRequest);
        lenient().when(messageExecutionContext.response()).thenReturn(messageResponse);
        lenient().when(messageRequest.contextPath()).thenReturn(CONTEXT_PATH);
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
        asyncApiReactor.handle(messageExecutionContext);

        verify(messageExecutionContext).setAttribute(ExecutionContext.ATTR_CONTEXT_PATH, CONTEXT_PATH);
        verify(messageExecutionContext).setAttribute(ExecutionContext.ATTR_API, API_ID);
        verify(messageExecutionContext).setAttribute(ExecutionContext.ATTR_ORGANIZATION, ORGANIZATION_ID);
        verify(messageExecutionContext).setAttribute(ExecutionContext.ATTR_ENVIRONMENT, ENVIRONMENT_ID);
        verify(messageExecutionContext).setInternalAttribute(ExecutionContext.ATTR_API, api);
    }

    @Test
    public void shouldReturn404WhenNoEntrypoint() {
        when(asyncEntrypointResolver.resolve(messageExecutionContext)).thenReturn(null);

        asyncApiReactor.handle(messageExecutionContext).test();

        verify(messageResponse).status(404);
        verify(messageResponse).reason("No entrypoint matches the incoming request");
        verify(messageResponse).end();
    }

    @Test
    public void shouldExecuteFlowChainWhenEntrypointFound() {
        when(asyncEntrypointResolver.resolve(messageExecutionContext)).thenReturn(entrypointConnector);

        ReflectionTestUtils.setField(asyncApiReactor, "platformFlowChain", platformFlowChain);
        ReflectionTestUtils.setField(asyncApiReactor, "securityChain", securityChain);
        when(platformFlowChain.execute(messageExecutionContext, REQUEST)).thenReturn(complete());
        when(securityChain.execute(messageExecutionContext)).thenReturn(complete());
        when(entrypointConnector.handleRequest(messageExecutionContext)).thenReturn(complete());
        when(entrypointConnector.handleResponse(messageExecutionContext)).thenReturn(complete());
        when(platformFlowChain.execute(messageExecutionContext, RESPONSE)).thenReturn(complete());

        asyncApiReactor.handle(messageExecutionContext).test();

        // verify flow chain has been executed in the right order
        InOrder inOrder = inOrder(platformFlowChain, securityChain, entrypointConnector, entrypointConnector, platformFlowChain);
        inOrder.verify(platformFlowChain).execute(messageExecutionContext, REQUEST);
        inOrder.verify(securityChain).execute(messageExecutionContext);
        inOrder.verify(entrypointConnector).handleRequest(messageExecutionContext);
        inOrder.verify(entrypointConnector).handleResponse(messageExecutionContext);
        inOrder.verify(platformFlowChain).execute(messageExecutionContext, RESPONSE);
    }
}

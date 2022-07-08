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
package io.gravitee.gateway.security.core;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.policy.Policy;
import io.gravitee.gateway.policy.StreamType;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SecurityPolicyResolverTest {

    private SecurityPolicyResolver securityPolicyResolver;

    @Mock
    private AuthenticationHandlerSelector handlerSelector;

    @Mock
    private Request request;

    @Mock
    private ExecutionContext executionContext;

    @Before
    public void setUp() {
        securityPolicyResolver = new SecurityPolicyResolver();
        securityPolicyResolver.setAuthenticationHandlerSelector(handlerSelector);

        when(executionContext.request()).thenReturn(request);
    }

    @Test
    public void shouldReturnRequestPolicyChain_onRequest() {
        AuthenticationHandler securityProvider = mock(AuthenticationHandler.class);
        when(securityProvider.name()).thenReturn("my-provider");
        when(securityProvider.handle(executionContext)).thenReturn(Collections.emptyList());
        when(handlerSelector.select(executionContext)).thenReturn(securityProvider);

        List<Policy> policies = securityPolicyResolver.resolve(StreamType.ON_REQUEST, executionContext);

        assertNotNull(policies);
    }
}

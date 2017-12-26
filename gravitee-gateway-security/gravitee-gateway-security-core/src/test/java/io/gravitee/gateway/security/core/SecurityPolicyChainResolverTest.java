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

import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.policy.*;
import io.gravitee.gateway.policy.impl.PolicyChain;
import io.gravitee.gateway.policy.impl.RequestPolicyChain;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SecurityPolicyChainResolverTest {

    private SecurityPolicyChainResolver securityPolicyChainResolver;

    @Mock
    private SecurityProviderManager securityManager;

    @Mock
    private PolicyManager policyManager;

    @Mock
    private Request request;

    @Mock
    private Response response;

    @Mock
    private ExecutionContext executionContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        securityPolicyChainResolver = new SecurityPolicyChainResolver();
        securityPolicyChainResolver.setSecurityManager(securityManager);
        securityPolicyChainResolver.setPolicyManager(policyManager);
    }

    @Test
    public void shouldReturnNoOpPolicyChain_onResponse() {
        PolicyChain policyChain = securityPolicyChainResolver.resolve(StreamType.ON_RESPONSE, request, response, executionContext);

        assertEquals(NoOpPolicyChain.class, policyChain.getClass());
    }

    @Test
    public void shouldReturnUnauthorizedPolicyChain_onRequest() {
        when(securityManager.resolve(request)).thenReturn(null);

        PolicyChain policyChain = securityPolicyChainResolver.resolve(StreamType.ON_REQUEST, request, response, executionContext);

        assertEquals(DirectPolicyChain.class, policyChain.getClass());
    }

    @Test
    public void shouldReturnRequestPolicyChain_onRequest() {
        AuthenticationHandler securityProvider = mock(AuthenticationHandler.class);
        when(securityProvider.name()).thenReturn("my-provider");
        when(securityProvider.handle(executionContext)).thenReturn(Collections.emptyList());

        Policy policy = mock(Policy.class);
        when(policyManager.create(StreamType.ON_REQUEST, "my-policy", null)).thenReturn(policy);
        when(securityManager.resolve(request)).thenReturn(securityProvider);

        PolicyChain policyChain = securityPolicyChainResolver.resolve(StreamType.ON_REQUEST, request, response, executionContext);

        assertEquals(RequestPolicyChain.class, policyChain.getClass());
    }
}

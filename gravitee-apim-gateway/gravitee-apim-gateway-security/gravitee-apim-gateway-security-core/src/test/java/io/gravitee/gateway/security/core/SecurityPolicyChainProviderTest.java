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
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.core.processor.StreamableProcessor;
import io.gravitee.gateway.policy.DirectPolicyChain;
import io.gravitee.gateway.policy.Policy;
import io.gravitee.gateway.policy.PolicyResolver;
import io.gravitee.gateway.policy.StreamType;
import io.gravitee.gateway.policy.impl.OrderedPolicyChain;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SecurityPolicyChainProviderTest {

    private SecurityPolicyChainProvider securityPolicyChainResolver;

    @Mock
    private PolicyResolver policyResolver;

    @Mock
    private ExecutionContext executionContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        securityPolicyChainResolver = new SecurityPolicyChainProvider(policyResolver);
    }

    @Test
    public void shouldReturnRequestPolicyChain() {
        Mockito
            .when(policyResolver.resolve(StreamType.ON_REQUEST, executionContext))
            .thenReturn(Collections.singletonList(Mockito.mock(Policy.class)));
        StreamableProcessor<ExecutionContext, Buffer> processor = securityPolicyChainResolver.provide(executionContext);

        Assert.assertEquals(OrderedPolicyChain.class, processor.getClass());
    }

    @Test
    public void shouldReturnUnauthorizedPolicyChain_onRequest() {
        Mockito.when(policyResolver.resolve(StreamType.ON_REQUEST, executionContext)).thenReturn(null);

        StreamableProcessor<ExecutionContext, Buffer> processor = securityPolicyChainResolver.provide(executionContext);

        Assert.assertEquals(DirectPolicyChain.class, processor.getClass());
        Assert.assertNotNull(((DirectPolicyChain) processor).policyResult());
        Assert.assertEquals(SecurityPolicyChainProvider.PLAN_UNRESOLVABLE, ((DirectPolicyChain) processor).policyResult().key());
    }
}

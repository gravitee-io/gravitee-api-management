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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class SecurityPolicyChainProviderTest {

    private SecurityPolicyChainProvider securityPolicyChainResolver;

    @Mock
    private PolicyResolver policyResolver;

    @Mock
    private ExecutionContext executionContext;

    @BeforeEach
    public void setUp() {
        securityPolicyChainResolver = new SecurityPolicyChainProvider(policyResolver);
    }

    @Test
    public void shouldReturnRequestPolicyChain() {
        Mockito.when(policyResolver.resolve(StreamType.ON_REQUEST, executionContext)).thenReturn(
            Collections.singletonList(Mockito.mock(Policy.class))
        );
        StreamableProcessor<ExecutionContext, Buffer> processor = securityPolicyChainResolver.provide(executionContext);

        Assertions.assertEquals(OrderedPolicyChain.class, processor.getClass());
    }

    @Test
    public void shouldReturnUnauthorizedPolicyChain_onRequest() {
        Mockito.when(policyResolver.resolve(StreamType.ON_REQUEST, executionContext)).thenReturn(null);

        StreamableProcessor<ExecutionContext, Buffer> processor = securityPolicyChainResolver.provide(executionContext);

        Assertions.assertEquals(DirectPolicyChain.class, processor.getClass());
        Assertions.assertNotNull(((DirectPolicyChain) processor).policyResult());
        Assertions.assertEquals(SecurityPolicyChainProvider.PLAN_UNRESOLVABLE, ((DirectPolicyChain) processor).policyResult().key());
    }
}

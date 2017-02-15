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
package io.gravitee.gateway.security.keyless;

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.security.core.SecurityPolicy;
import org.junit.Assert;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class KeylessSecurityProviderTest {

    private KeylessSecurityProvider securityProvider = new KeylessSecurityProvider();

    @Test
    public void shouldHandleRequest() {
        Request request = mock(Request.class);
        when(request.headers()).thenReturn(new HttpHeaders());

        boolean handle = securityProvider.canHandle(request);
        Assert.assertTrue(handle);
    }

    @Test
    public void shouldReturnSingletonSecurityPolicy() {
        ExecutionContext executionContext = mock(ExecutionContext.class);

        SecurityPolicy securityPolicy1 = securityProvider.create(executionContext);
        SecurityPolicy securityPolicy2 = securityProvider.create(executionContext);

        Assert.assertEquals(KeylessSecurityProvider.POLICY, securityPolicy1);
        Assert.assertEquals(KeylessSecurityProvider.POLICY, securityPolicy2);

        Assert.assertEquals(KeylessSecurityProvider.POLICY.policy(), KeylessSecurityProvider.KEYLESS_POLICY);
        Assert.assertNull(KeylessSecurityProvider.POLICY.configuration(), null);
    }

    @Test
    public void shouldReturnName() {
        Assert.assertEquals("key_less", securityProvider.name());
    }

    @Test
    public void shouldReturnOrder() {
        Assert.assertEquals(1000, securityProvider.order());
    }
}

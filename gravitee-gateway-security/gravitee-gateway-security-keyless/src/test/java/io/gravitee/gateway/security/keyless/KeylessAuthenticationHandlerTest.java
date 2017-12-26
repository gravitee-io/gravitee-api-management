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
import io.gravitee.gateway.security.core.PluginAuthenticationPolicy;
import io.gravitee.gateway.security.core.AuthenticationPolicy;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class KeylessAuthenticationHandlerTest {

    @InjectMocks
    private KeylessAuthenticationHandler authenticationHandler = new KeylessAuthenticationHandler();

    @Test
    public void shouldHandleRequest() {
        Request request = mock(Request.class);
        when(request.headers()).thenReturn(new HttpHeaders());

        boolean handle = authenticationHandler.canHandle(request);
        Assert.assertTrue(handle);
    }

    @Test
    public void shouldReturnPolicies() {
        ExecutionContext executionContext = mock(ExecutionContext.class);

        List<AuthenticationPolicy> keylessProviderPolicies = authenticationHandler.handle(executionContext);

        Assert.assertEquals(1, keylessProviderPolicies.size());

        PluginAuthenticationPolicy policy = (PluginAuthenticationPolicy) keylessProviderPolicies.iterator().next();
        Assert.assertEquals(policy.name(), KeylessAuthenticationHandler.KEYLESS_POLICY);
    }

    @Test
    public void shouldReturnName() {
        Assert.assertEquals("key_less", authenticationHandler.name());
    }

    @Test
    public void shouldReturnOrder() {
        Assert.assertEquals(1000, authenticationHandler.order());
    }
}

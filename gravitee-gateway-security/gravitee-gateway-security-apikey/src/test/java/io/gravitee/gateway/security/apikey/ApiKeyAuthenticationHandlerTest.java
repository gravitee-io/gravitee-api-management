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
package io.gravitee.gateway.security.apikey;

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.util.LinkedMultiValueMap;
import io.gravitee.common.util.MultiValueMap;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.security.core.PluginAuthenticationPolicy;
import io.gravitee.gateway.security.core.AuthenticationPolicy;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiKeyAuthenticationHandlerTest {

    @InjectMocks
    private ApiKeyAuthenticationHandler authenticationHandler = new ApiKeyAuthenticationHandler();

    @Test
    public void shouldNotHandleRequest() {
        Request request = mock(Request.class);
        when(request.headers()).thenReturn(new HttpHeaders());

        MultiValueMap<String, String> parameters = mock(MultiValueMap.class);
        when(request.parameters()).thenReturn(parameters);

        boolean handle = authenticationHandler.canHandle(request);
        Assert.assertFalse(handle);
    }

    @Test
    public void shouldHandleRequestUsingHeaders() {
        Request request = mock(Request.class);
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Gravitee-Api-Key", "xxxxx-xxxx-xxxxx");
        when(request.headers()).thenReturn(headers);

        boolean handle = authenticationHandler.canHandle(request);
        Assert.assertTrue(handle);
    }

    @Test
    public void shouldHandleRequestUsingQueryParameters() {
        Request request = mock(Request.class);

        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.put("api-key", Collections.singletonList("xxxxx-xxxx-xxxxx"));
        when(request.parameters()).thenReturn(parameters);

        HttpHeaders headers = new HttpHeaders();
        when(request.headers()).thenReturn(headers);

        boolean handle = authenticationHandler.canHandle(request);
        Assert.assertTrue(handle);
    }

    @Test
    public void shouldReturnPolicies() {
        ExecutionContext executionContext = mock(ExecutionContext.class);

        List<AuthenticationPolicy> apikeyProviderPolicies = authenticationHandler.handle(executionContext);

        Assert.assertEquals(1, apikeyProviderPolicies.size());

        PluginAuthenticationPolicy policy = (PluginAuthenticationPolicy) apikeyProviderPolicies.iterator().next();
        Assert.assertEquals(policy.name(), ApiKeyAuthenticationHandler.API_KEY_POLICY);
    }

    @Test
    public void shouldReturnName() {
        Assert.assertEquals("api_key", authenticationHandler.name());
    }

    @Test
    public void shouldReturnOrder() {
        Assert.assertEquals(500, authenticationHandler.order());
    }
}

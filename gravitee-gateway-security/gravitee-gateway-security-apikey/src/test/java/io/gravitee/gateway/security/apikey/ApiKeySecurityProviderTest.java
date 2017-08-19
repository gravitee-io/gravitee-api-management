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
import io.gravitee.gateway.security.core.SecurityPolicy;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import java.util.Collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiKeySecurityProviderTest {

    private ApiKeySecurityProvider securityProvider;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        securityProvider = new ApiKeySecurityProvider();
    }

    @Test
    public void shouldNotHandleRequest() {
        Request request = mock(Request.class);
        when(request.headers()).thenReturn(new HttpHeaders());

        MultiValueMap<String, String> parameters = mock(MultiValueMap.class);
        when(request.parameters()).thenReturn(parameters);

        boolean handle = securityProvider.canHandle(request);
        Assert.assertFalse(handle);
    }

    @Test
    public void shouldHandleRequestUsingHeaders() {
        Request request = mock(Request.class);
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Gravitee-Api-Key", "xxxxx-xxxx-xxxxx");
        when(request.headers()).thenReturn(headers);

        boolean handle = securityProvider.canHandle(request);
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

        boolean handle = securityProvider.canHandle(request);
        Assert.assertTrue(handle);
    }

    @Test
    public void shouldReturnSingletonSecurityPolicy() {
        ExecutionContext executionContext = mock(ExecutionContext.class);

        SecurityPolicy securityPolicy1 = securityProvider.create(executionContext);
        SecurityPolicy securityPolicy2 = securityProvider.create(executionContext);

        Assert.assertEquals(ApiKeySecurityProvider.POLICY, securityPolicy1);
        Assert.assertEquals(ApiKeySecurityProvider.POLICY, securityPolicy2);

        Assert.assertEquals(ApiKeySecurityProvider.POLICY.policy(), ApiKeySecurityProvider.API_KEY_POLICY);
        Assert.assertEquals(ApiKeySecurityProvider.POLICY.configuration(), ApiKeySecurityProvider.API_KEY_POLICY_CONFIGURATION);
    }

    @Test
    public void shouldReturnName() {
        Assert.assertEquals("api_key", securityProvider.name());
    }

    @Test
    public void shouldReturnOrder() {
        Assert.assertEquals(500, securityProvider.order());
    }
}

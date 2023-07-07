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
package io.gravitee.gateway.security.oauth2;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.common.util.LinkedMultiValueMap;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.security.core.AuthenticationContext;
import io.gravitee.gateway.security.core.AuthenticationPolicy;
import io.gravitee.gateway.security.core.HookAuthenticationPolicy;
import io.gravitee.gateway.security.core.PluginAuthenticationPolicy;
import io.gravitee.gateway.security.oauth2.policy.CheckSubscriptionPolicy;
import java.util.Iterator;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class OAuth2AuthenticationHandlerTest {

    @InjectMocks
    private OAuth2AuthenticationHandler authenticationHandler = new OAuth2AuthenticationHandler();

    @Mock
    private AuthenticationContext authenticationContext;

    @Mock
    private Request request;

    @Before
    public void setUp() {
        when(authenticationContext.request()).thenReturn(request);
    }

    @Test
    public void shouldNotHandleRequest_noAuthorizationHeader() {
        when(request.headers()).thenReturn(HttpHeaders.create());

        boolean handle = authenticationHandler.canHandle(authenticationContext);
        Assert.assertFalse(handle);
    }

    @Test
    public void shouldNotHandleRequest_invalidAuthorizationHeader() {
        HttpHeaders headers = HttpHeaders.create();
        when(request.headers()).thenReturn(headers);

        headers.add(HttpHeaderNames.AUTHORIZATION, "");

        boolean handle = authenticationHandler.canHandle(authenticationContext);
        Assert.assertFalse(handle);
    }

    @Test
    public void shouldNotHandleRequest_noBearerAuthorizationHeader() {
        HttpHeaders headers = HttpHeaders.create();
        when(request.headers()).thenReturn(headers);

        headers.add(HttpHeaderNames.AUTHORIZATION, "Basic xxx-xx-xxx-xx-xx");

        boolean handle = authenticationHandler.canHandle(authenticationContext);
        Assert.assertFalse(handle);
    }

    @Test
    public void shouldHandleRequest_validAuthorizationHeader() {
        HttpHeaders headers = HttpHeaders.create();
        when(request.headers()).thenReturn(headers);

        headers.add(HttpHeaderNames.AUTHORIZATION, OAuth2AuthenticationHandler.BEARER_AUTHORIZATION_TYPE + " xxx-xx-xxx-xx-xx");

        boolean handle = authenticationHandler.canHandle(authenticationContext);
        Assert.assertTrue(handle);
    }

    @Test
    public void shouldHandleRequest_ignoreCaseAuthorizationHeader() {
        HttpHeaders headers = HttpHeaders.create();
        when(request.headers()).thenReturn(headers);

        headers.add(HttpHeaderNames.AUTHORIZATION, "BeaRer xxx-xx-xxx-xx-xx");

        boolean handle = authenticationHandler.canHandle(authenticationContext);
        Assert.assertTrue(handle);
    }

    @Test
    public void shouldHandleRequest_validQueryParameter() {
        HttpHeaders headers = HttpHeaders.create();
        when(request.headers()).thenReturn(headers);
        LinkedMultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        when(request.parameters()).thenReturn(parameters);
        parameters.add("access_token", "xxx-xx-xxx-xx-xx");

        boolean handle = authenticationHandler.canHandle(authenticationContext);
        Assert.assertTrue(handle);
    }

    @Test
    public void shouldNotHandleRequest_noBearerValue() {
        HttpHeaders headers = HttpHeaders.create();
        when(request.headers()).thenReturn(headers);

        headers.add(HttpHeaderNames.AUTHORIZATION, OAuth2AuthenticationHandler.BEARER_AUTHORIZATION_TYPE + " ");

        boolean handle = authenticationHandler.canHandle(authenticationContext);
        Assert.assertFalse(handle);
    }

    @Test
    public void shouldReturnPolicies() {
        ExecutionContext executionContext = mock(ExecutionContext.class);

        List<AuthenticationPolicy> oauth2ProviderPolicies = authenticationHandler.handle(executionContext);

        Assert.assertEquals(2, oauth2ProviderPolicies.size());
        Iterator<AuthenticationPolicy> policyIte = oauth2ProviderPolicies.iterator();
        PluginAuthenticationPolicy policy = (PluginAuthenticationPolicy) policyIte.next();
        Assert.assertEquals(OAuth2AuthenticationHandler.AUTHENTICATION_HANDLER_NAME, policy.name());

        HookAuthenticationPolicy policy2 = (HookAuthenticationPolicy) policyIte.next();
        Assert.assertEquals(CheckSubscriptionPolicy.class, policy2.clazz());
    }

    @Test
    public void shouldReturnName() {
        Assert.assertEquals(OAuth2AuthenticationHandler.AUTHENTICATION_HANDLER_NAME, authenticationHandler.name());
    }

    @Test
    public void shouldReturnOrder() {
        Assert.assertEquals(100, authenticationHandler.order());
    }
}

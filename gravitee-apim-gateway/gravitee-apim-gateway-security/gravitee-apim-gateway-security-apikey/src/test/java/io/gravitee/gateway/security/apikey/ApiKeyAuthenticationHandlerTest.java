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

import static java.util.Optional.of;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.util.LinkedMultiValueMap;
import io.gravitee.common.util.MultiValueMap;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.security.core.AuthenticationContext;
import io.gravitee.gateway.security.core.AuthenticationPolicy;
import io.gravitee.gateway.security.core.PluginAuthenticationPolicy;
import io.gravitee.reporter.api.http.Metrics;
import io.gravitee.reporter.api.http.SecurityType;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.model.ApiKey;
import java.util.Collections;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(Metrics.class)
public class ApiKeyAuthenticationHandlerTest {

    @InjectMocks
    private ApiKeyAuthenticationHandler authenticationHandler = new ApiKeyAuthenticationHandler();

    @Mock
    private Request request;

    @Mock
    private Metrics metrics;

    @Mock
    private AuthenticationContext authenticationContext;

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @Before
    public void init() {
        initMocks(this);
    }

    @Test
    public void shouldNotHandleRequest() {
        when(authenticationContext.request()).thenReturn(request);
        when(request.headers()).thenReturn(new HttpHeaders());

        MultiValueMap<String, String> parameters = mock(MultiValueMap.class);
        when(request.parameters()).thenReturn(parameters);

        boolean handle = authenticationHandler.canHandle(authenticationContext);
        Assert.assertFalse(handle);
    }

    @Test
    public void shouldHandleRequestUsingHeaders() throws TechnicalException {
        when(authenticationContext.request()).thenReturn(request);
        when(request.metrics()).thenReturn(metrics);
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Gravitee-Api-Key", "xxxxx-xxxx-xxxxx");
        when(request.headers()).thenReturn(headers);
        when(apiKeyRepository.findById("xxxxx-xxxx-xxxxx")).thenReturn(of(new ApiKey()));

        boolean handle = authenticationHandler.canHandle(authenticationContext);
        Assert.assertTrue(handle);
        verify(metrics).setSecurityType(SecurityType.API_KEY);
        verify(metrics).setSecurityToken("xxxxx-xxxx-xxxxx");
    }

    @Test
    public void shouldHandleRequestUsingQueryParameters() throws TechnicalException {
        when(authenticationContext.request()).thenReturn(request);
        when(request.metrics()).thenReturn(metrics);
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.put("api-key", Collections.singletonList("xxxxx-xxxx-xxxxx"));
        when(request.parameters()).thenReturn(parameters);
        when(apiKeyRepository.findById("xxxxx-xxxx-xxxxx")).thenReturn(of(new ApiKey()));

        HttpHeaders headers = new HttpHeaders();
        when(request.headers()).thenReturn(headers);

        boolean handle = authenticationHandler.canHandle(authenticationContext);
        Assert.assertTrue(handle);
        verify(metrics).setSecurityType(SecurityType.API_KEY);
        verify(metrics).setSecurityToken("xxxxx-xxxx-xxxxx");
    }

    @Test
    public void shouldHandleRequestUsingQueryParameters_emptyApiKey() {
        when(authenticationContext.request()).thenReturn(request);
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.put("api-key", Collections.singletonList(""));
        when(request.parameters()).thenReturn(parameters);

        HttpHeaders headers = new HttpHeaders();
        when(request.headers()).thenReturn(headers);

        boolean handle = authenticationHandler.canHandle(authenticationContext);
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
    /*
    @Test
    public void shouldNotHandleRequest_wrongCriteria() throws TechnicalException {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Gravitee-Api-Key", "xxxxx-xxxx-xxxxx");
        when(request.headers()).thenReturn(headers);

        AuthenticationContext authenticationContext = mock(AuthenticationContext.class);
        when(authenticationContext.getId()).thenReturn("wrong-plan-id");

        ApiKey apiKey = mock(ApiKey.class);
        when(apiKey.getPlan()).thenReturn("plan-id");

        when(apiKeyRepository.findById("xxxxx-xxxx-xxxxx")).thenReturn(Optional.of(apiKey));

        boolean handle = authenticationHandler.canHandle(request, authenticationContext);
        Assert.assertFalse(handle);
    }

    @Test
    public void shouldHandleRequest_withCriteria() throws TechnicalException {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Gravitee-Api-Key", "xxxxx-xxxx-xxxxx");
        when(request.headers()).thenReturn(headers);

        AuthenticationContext authenticationContext = mock(AuthenticationContext.class);
        when(authenticationContext.getId()).thenReturn("plan-id");

        ApiKey apiKey = mock(ApiKey.class);
        when(apiKey.getPlan()).thenReturn("plan-id");

        when(apiKeyRepository.findById("xxxxx-xxxx-xxxxx")).thenReturn(Optional.of(apiKey));

        boolean handle = authenticationHandler.canHandle(request, authenticationContext);
        Assert.assertTrue(handle);
    }
    */
}

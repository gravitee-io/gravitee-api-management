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
package io.gravitee.gateway.security.apikey;

import static io.gravitee.gateway.security.core.AuthenticationContext.TOKEN_TYPE_API_KEY;
import static java.util.Optional.of;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

import io.gravitee.common.http.GraviteeHttpHeader;
import io.gravitee.common.util.LinkedMultiValueMap;
import io.gravitee.common.util.MultiValueMap;
import io.gravitee.definition.model.Api;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.api.service.ApiKey;
import io.gravitee.gateway.api.service.ApiKeyService;
import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.security.core.AuthenticationContext;
import io.gravitee.gateway.security.core.AuthenticationPolicy;
import io.gravitee.gateway.security.core.PluginAuthenticationPolicy;
import io.gravitee.reporter.api.http.Metrics;
import io.gravitee.reporter.api.http.SecurityType;
import io.gravitee.repository.exceptions.TechnicalException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.env.Environment;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
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
    private ApiKeyService apiKeyService;

    @Mock
    private Api api;

    @Before
    public void init() {
        initMocks(this);

        ComponentProvider provider = mock(ComponentProvider.class);

        when(provider.getComponent(ApiKeyService.class)).thenReturn(apiKeyService);
        when(provider.getComponent(Api.class)).thenReturn(api);

        Environment environment = mock(Environment.class);
        when(environment.getProperty(eq("policy.api-key.header"), anyString())).thenReturn(GraviteeHttpHeader.X_GRAVITEE_API_KEY);
        when(environment.getProperty(eq("policy.api-key.param"), anyString())).thenReturn("api-key");

        when(provider.getComponent(Environment.class)).thenReturn(environment);
        authenticationHandler.resolve(provider);
    }

    @Test
    public void shouldNotHandleRequest() {
        when(authenticationContext.request()).thenReturn(request);
        when(request.headers()).thenReturn(HttpHeaders.create());

        MultiValueMap<String, String> parameters = mock(MultiValueMap.class);
        when(request.parameters()).thenReturn(parameters);

        boolean handle = authenticationHandler.canHandle(authenticationContext);
        Assert.assertFalse(handle);
    }

    @Test
    public void shouldHandleRequestUsingHeaders() throws TechnicalException {
        when(authenticationContext.request()).thenReturn(request);
        when(request.metrics()).thenReturn(metrics);
        when(api.getId()).thenReturn("api-id");
        HttpHeaders headers = HttpHeaders.create();
        headers.set("X-Gravitee-Api-Key", "xxxxx-xxxx-xxxxx");
        when(request.headers()).thenReturn(headers);
        when(apiKeyService.getByApiAndKey("api-id", "xxxxx-xxxx-xxxxx")).thenReturn(of(new ApiKey()));

        boolean handle = authenticationHandler.canHandle(authenticationContext);
        Assert.assertTrue(handle);
        verify(metrics).setSecurityType(SecurityType.API_KEY);
        verify(metrics).setSecurityToken("xxxxx-xxxx-xxxxx");
    }

    @Test
    public void shouldHandleRequestUsingQueryParameters() throws TechnicalException {
        when(authenticationContext.request()).thenReturn(request);
        when(request.metrics()).thenReturn(metrics);
        when(api.getId()).thenReturn("api-id");
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.put("api-key", Collections.singletonList("xxxxx-xxxx-xxxxx"));
        when(request.parameters()).thenReturn(parameters);
        when(apiKeyService.getByApiAndKey("api-id", "xxxxx-xxxx-xxxxx")).thenReturn(of(new ApiKey()));

        HttpHeaders headers = HttpHeaders.create();
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

        HttpHeaders headers = HttpHeaders.create();
        when(request.headers()).thenReturn(headers);

        boolean handle = authenticationHandler.canHandle(authenticationContext);
        Assert.assertTrue(handle);
    }

    @Test
    public void shouldReturnPolicies() {
        ExecutionContext executionContext = mock(ExecutionContext.class);

        List<AuthenticationPolicy> apikeyProviderPolicies = authenticationHandler.handle(executionContext);

        assertEquals(1, apikeyProviderPolicies.size());

        Iterator<AuthenticationPolicy> policyIterator = apikeyProviderPolicies.iterator();

        PluginAuthenticationPolicy policy = (PluginAuthenticationPolicy) policyIterator.next();
        assertEquals(policy.name(), ApiKeyAuthenticationHandler.API_KEY_POLICY);
    }

    @Test
    public void shouldReturnName() {
        assertEquals("api_key", authenticationHandler.name());
    }

    @Test
    public void shouldReturnOrder() {
        assertEquals(500, authenticationHandler.order());
    }

    @Test
    public void shouldReuturnTokenType() {
        assertEquals(TOKEN_TYPE_API_KEY, authenticationHandler.tokenType());
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

        when(apiKeyRepository.findByKey("xxxxx-xxxx-xxxxx")).thenReturn(Optional.of(apiKey));

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

        when(apiKeyRepository.findByKey("xxxxx-xxxx-xxxxx")).thenReturn(Optional.of(apiKey));

        boolean handle = authenticationHandler.canHandle(request, authenticationContext);
        Assert.assertTrue(handle);
    }
    */
}

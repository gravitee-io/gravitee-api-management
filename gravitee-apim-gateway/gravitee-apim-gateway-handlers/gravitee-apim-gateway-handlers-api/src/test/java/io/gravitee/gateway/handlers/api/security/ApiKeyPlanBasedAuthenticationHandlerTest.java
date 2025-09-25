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
package io.gravitee.gateway.handlers.api.security;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.definition.model.Plan;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.service.ApiKey;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.api.service.SubscriptionService;
import io.gravitee.gateway.security.core.AuthenticationContext;
import io.gravitee.gateway.security.core.AuthenticationHandler;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiKeyPlanBasedAuthenticationHandlerTest {

    private static final String APIKEY_CONTEXT_ATTRIBUTE = "apikey";

    @InjectMocks
    private ApiKeyPlanBasedAuthenticationHandler apiKeyPlanBasedAuthenticationHandler;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private AuthenticationHandler authenticationHandler;

    @Mock
    private Plan plan;

    @Mock
    private AuthenticationContext authenticationContext;

    @Mock
    private ApiKey apiKey;

    @Mock
    private Subscription subscription;

    @Test
    public void shouldNotHandle() {
        apiKeyPlanBasedAuthenticationHandler = new ApiKeyPlanBasedAuthenticationHandler(authenticationHandler, plan, subscriptionService);
        when(authenticationHandler.canHandle(any())).thenReturn(false);

        boolean canHandle = apiKeyPlanBasedAuthenticationHandler.canHandle(authenticationContext);

        assertFalse(canHandle);
        verify(authenticationContext, never()).get(APIKEY_CONTEXT_ATTRIBUTE);
        verify(plan, never()).getId();
    }

    @Test
    public void shouldHandle_ifNoKeyInContext() {
        apiKeyPlanBasedAuthenticationHandler = new ApiKeyPlanBasedAuthenticationHandler(authenticationHandler, plan, subscriptionService);
        AuthenticationContext context = mock(AuthenticationContext.class);
        when(authenticationHandler.canHandle(any())).thenReturn(true);

        when(context.contains(APIKEY_CONTEXT_ATTRIBUTE)).thenReturn(false);

        boolean canHandle = apiKeyPlanBasedAuthenticationHandler.canHandle(context);

        assertTrue(canHandle);
        verify(context, never()).get(APIKEY_CONTEXT_ATTRIBUTE);
        verify(plan, never()).getId();
    }

    @Test
    public void shouldHandle_ifKeyNotFound() {
        apiKeyPlanBasedAuthenticationHandler = new ApiKeyPlanBasedAuthenticationHandler(authenticationHandler, plan, subscriptionService);
        when(authenticationHandler.canHandle(any())).thenReturn(true);

        when(authenticationContext.contains(APIKEY_CONTEXT_ATTRIBUTE)).thenReturn(true);
        when(authenticationContext.get(APIKEY_CONTEXT_ATTRIBUTE)).thenReturn(empty());

        boolean canHandle = apiKeyPlanBasedAuthenticationHandler.canHandle(authenticationContext);

        assertFalse(canHandle);
        verify(authenticationContext, times(1)).get(APIKEY_CONTEXT_ATTRIBUTE);
        verify(plan, never()).getId();
    }

    @Test
    public void shouldNotHandle_ifKeyFoundAndPlanNotMatch() {
        when(plan.getId()).thenReturn("planId");
        apiKeyPlanBasedAuthenticationHandler = new ApiKeyPlanBasedAuthenticationHandler(authenticationHandler, plan, subscriptionService);
        when(authenticationHandler.canHandle(any())).thenReturn(true);

        when(authenticationContext.contains(APIKEY_CONTEXT_ATTRIBUTE)).thenReturn(true);
        when(authenticationContext.get(APIKEY_CONTEXT_ATTRIBUTE)).thenReturn((of(apiKey)));

        when(apiKey.getPlan()).thenReturn("planId2");

        boolean canHandle = apiKeyPlanBasedAuthenticationHandler.canHandle(authenticationContext);

        assertFalse(canHandle);
        verify(authenticationContext, times(1)).get(APIKEY_CONTEXT_ATTRIBUTE);
        verify(plan, times(1)).getId();
    }

    @Test
    public void shouldNotHandle_ifKeyFoundAndPlanMatchAndNoSubscription() {
        when(plan.getId()).thenReturn("planId");
        when(plan.getApi()).thenReturn("apiId");
        when(apiKey.getPlan()).thenReturn("planId");
        when(apiKey.getKey()).thenReturn("api-key");

        apiKeyPlanBasedAuthenticationHandler = new ApiKeyPlanBasedAuthenticationHandler(authenticationHandler, plan, subscriptionService);
        when(authenticationHandler.canHandle(any())).thenReturn(true);

        when(authenticationContext.contains(APIKEY_CONTEXT_ATTRIBUTE)).thenReturn(true);
        when(authenticationContext.get(APIKEY_CONTEXT_ATTRIBUTE)).thenReturn((of(apiKey)));

        boolean canHandle = apiKeyPlanBasedAuthenticationHandler.canHandle(authenticationContext);

        assertFalse(canHandle);
        verify(authenticationContext, times(1)).get(APIKEY_CONTEXT_ATTRIBUTE);
        verify(subscriptionService, times(1)).getByApiAndSecurityToken(
            eq("apiId"),
            argThat(s -> "API_KEY".equals(s.getTokenType()) && "api-key".equals(s.getTokenValue())),
            eq("planId")
        );
    }

    @Test
    public void shouldNotHandle_ifKeyFoundAndPlanMatchAndSubscriptionWithInvalidTime() {
        when(plan.getId()).thenReturn("planId");
        when(plan.getApi()).thenReturn("apiId");
        when(apiKey.getPlan()).thenReturn("planId");
        when(apiKey.getKey()).thenReturn("api-key");

        apiKeyPlanBasedAuthenticationHandler = new ApiKeyPlanBasedAuthenticationHandler(authenticationHandler, plan, subscriptionService);
        when(authenticationHandler.canHandle(any())).thenReturn(true);
        when(authenticationContext.request()).thenReturn(mock(Request.class));

        when(authenticationContext.contains(APIKEY_CONTEXT_ATTRIBUTE)).thenReturn(true);
        when(authenticationContext.get(APIKEY_CONTEXT_ATTRIBUTE)).thenReturn((of(apiKey)));

        when(
            subscriptionService.getByApiAndSecurityToken(
                eq("apiId"),
                argThat(s -> "API_KEY".equals(s.getTokenType()) && "api-key".equals(s.getTokenValue())),
                eq("planId")
            )
        ).thenReturn(Optional.of(subscription));
        when(subscription.isTimeValid(anyLong())).thenReturn(false);

        boolean canHandle = apiKeyPlanBasedAuthenticationHandler.canHandle(authenticationContext);

        assertFalse(canHandle);
    }

    @Test
    public void shouldHandle_ifKeyFoundAndPlanMatchAndSubscriptionWithValidTime() {
        when(plan.getId()).thenReturn("planId");
        when(plan.getApi()).thenReturn("apiId");
        when(apiKey.getPlan()).thenReturn("planId");
        when(apiKey.getKey()).thenReturn("api-key");

        apiKeyPlanBasedAuthenticationHandler = new ApiKeyPlanBasedAuthenticationHandler(authenticationHandler, plan, subscriptionService);
        when(authenticationHandler.canHandle(any())).thenReturn(true);
        when(authenticationContext.request()).thenReturn(mock(Request.class));

        when(authenticationContext.contains(APIKEY_CONTEXT_ATTRIBUTE)).thenReturn(true);
        when(authenticationContext.get(APIKEY_CONTEXT_ATTRIBUTE)).thenReturn((of(apiKey)));

        when(
            subscriptionService.getByApiAndSecurityToken(
                eq("apiId"),
                argThat(s -> "API_KEY".equals(s.getTokenType()) && "api-key".equals(s.getTokenValue())),
                eq("planId")
            )
        ).thenReturn(Optional.of(subscription));
        when(subscription.isTimeValid(anyLong())).thenReturn(true);

        boolean canHandle = apiKeyPlanBasedAuthenticationHandler.canHandle(authenticationContext);

        assertTrue(canHandle);
    }
}

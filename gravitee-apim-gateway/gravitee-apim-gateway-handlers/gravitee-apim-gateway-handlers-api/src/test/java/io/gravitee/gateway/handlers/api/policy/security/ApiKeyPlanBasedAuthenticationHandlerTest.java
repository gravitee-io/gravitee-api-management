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
package io.gravitee.gateway.handlers.api.policy.security;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.definition.model.Plan;
import io.gravitee.gateway.handlers.api.definition.ApiKey;
import io.gravitee.gateway.handlers.api.policy.security.apikey.ApiKeyPlanBasedAuthenticationHandler;
import io.gravitee.gateway.security.core.AuthenticationContext;
import io.gravitee.gateway.security.core.AuthenticationHandler;
import org.junit.Test;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiKeyPlanBasedAuthenticationHandlerTest {

    private static final String APIKEY_CONTEXT_ATTRIBUTE = "apikey";
    private ApiKeyPlanBasedAuthenticationHandler apiKeyPlanBasedAuthenticationHandler;

    @Test
    public void shouldNotHandle() {
        AuthenticationHandler handler = mock(AuthenticationHandler.class);
        Plan plan = mock(Plan.class);
        apiKeyPlanBasedAuthenticationHandler = new ApiKeyPlanBasedAuthenticationHandler(handler, plan);
        AuthenticationContext context = mock(AuthenticationContext.class);
        when(handler.canHandle(any())).thenReturn(false);

        boolean canHandle = apiKeyPlanBasedAuthenticationHandler.canHandle(context);

        assertFalse(canHandle);
        verify(context, never()).get(APIKEY_CONTEXT_ATTRIBUTE);
        verify(plan, never()).getId();
    }

    @Test
    public void shouldHandle_ifKeyNotFound() {
        AuthenticationHandler handler = mock(AuthenticationHandler.class);
        Plan plan = mock(Plan.class);
        apiKeyPlanBasedAuthenticationHandler = new ApiKeyPlanBasedAuthenticationHandler(handler, plan);
        AuthenticationContext context = mock(AuthenticationContext.class);
        when(handler.canHandle(any())).thenReturn(true);
        when(context.get(APIKEY_CONTEXT_ATTRIBUTE)).thenReturn(empty());

        boolean canHandle = apiKeyPlanBasedAuthenticationHandler.canHandle(context);

        assertTrue(canHandle);
        verify(context, times(1)).get(APIKEY_CONTEXT_ATTRIBUTE);
        verify(plan, never()).getId();
    }

    @Test
    public void shouldHandle_ifKeyFoundAndPlanMatch() {
        AuthenticationHandler handler = mock(AuthenticationHandler.class);
        Plan plan = mock(Plan.class);
        when(plan.getId()).thenReturn("planId");
        apiKeyPlanBasedAuthenticationHandler = new ApiKeyPlanBasedAuthenticationHandler(handler, plan);
        AuthenticationContext context = mock(AuthenticationContext.class);
        when(handler.canHandle(any())).thenReturn(true);
        ApiKey key = mock(ApiKey.class);
        when(context.get(APIKEY_CONTEXT_ATTRIBUTE)).thenReturn((of(key)));
        when(key.getPlan()).thenReturn("planId");

        boolean canHandle = apiKeyPlanBasedAuthenticationHandler.canHandle(context);

        assertTrue(canHandle);
        verify(context, times(1)).get(APIKEY_CONTEXT_ATTRIBUTE);
        verify(plan, times(1)).getId();
    }

    @Test
    public void shouldNotHandle_ifKeyFoundAndPlanNotMatch() {
        AuthenticationHandler handler = mock(AuthenticationHandler.class);
        Plan plan = mock(Plan.class);
        when(plan.getId()).thenReturn("planId");
        apiKeyPlanBasedAuthenticationHandler = new ApiKeyPlanBasedAuthenticationHandler(handler, plan);
        AuthenticationContext context = mock(AuthenticationContext.class);
        when(handler.canHandle(any())).thenReturn(true);
        ApiKey key = mock(ApiKey.class);
        when(context.get(APIKEY_CONTEXT_ATTRIBUTE)).thenReturn((of(key)));
        when(key.getPlan()).thenReturn("planId2");

        boolean canHandle = apiKeyPlanBasedAuthenticationHandler.canHandle(context);

        assertFalse(canHandle);
        verify(context, times(1)).get(APIKEY_CONTEXT_ATTRIBUTE);
        verify(plan, times(1)).getId();
    }
}

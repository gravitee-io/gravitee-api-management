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

import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.handlers.api.definition.Plan;
import io.gravitee.gateway.security.core.AuthenticationHandler;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PlanBasedAuthenticationHandlerEnhancerTest {

    private PlanBasedAuthenticationHandlerEnhancer authenticationHandlerEnhancer;

    @Mock
    private Api api;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        authenticationHandlerEnhancer = new PlanBasedAuthenticationHandlerEnhancer();
        authenticationHandlerEnhancer.setApi(api);

        when(api.getName()).thenReturn("My API");
        when(api.getVersion()).thenReturn("v1");
    }

    @Test
    public void shouldNotResolveKeylessPolicy_becauseNoPlan() {
        AuthenticationHandler authenticationHandler = mock(AuthenticationHandler.class);
        when(authenticationHandler.name()).thenReturn("keyless");

        List<AuthenticationHandler> SecurityProviders =
                authenticationHandlerEnhancer.filter(Collections.singletonList(authenticationHandler));

        assertNotNull(SecurityProviders);
        assertTrue(SecurityProviders.isEmpty());
    }

    @Test
    public void shouldNotResolveKeylessPolicy_becauseOnePlanApikey() {
        AuthenticationHandler authenticationHandler = mock(AuthenticationHandler.class);
        when(authenticationHandler.name()).thenReturn("keyless");

        Plan plan1 = new Plan();
        plan1.setSecurity("apikey");
        when(api.getPlans()).thenReturn(Collections.singletonList(plan1));

        List<AuthenticationHandler> SecurityProviders =
                authenticationHandlerEnhancer.filter(Collections.singletonList(authenticationHandler));

        assertNotNull(SecurityProviders);
        assertTrue(SecurityProviders.isEmpty());
    }

    @Test
    public void shouldResolveKeylessPolicy_becauseOnePlanKeyless() {
        AuthenticationHandler authenticationHandler = mock(AuthenticationHandler.class);
        when(authenticationHandler.name()).thenReturn("keyless");

        Plan plan1 = new Plan();
        plan1.setSecurity("keyless");
        when(api.getPlans()).thenReturn(Collections.singletonList(plan1));

        List<AuthenticationHandler> SecurityProviders =
                authenticationHandlerEnhancer.filter(Collections.singletonList(authenticationHandler));

        assertNotNull(SecurityProviders);
        assertFalse(SecurityProviders.isEmpty());
        assertEquals("keyless", SecurityProviders.iterator().next().name());
    }
}

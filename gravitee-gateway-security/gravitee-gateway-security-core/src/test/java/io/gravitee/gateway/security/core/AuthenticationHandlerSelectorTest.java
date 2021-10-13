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
package io.gravitee.gateway.security.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.api.Request;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class AuthenticationHandlerSelectorTest {

    @Mock
    private AuthenticationHandlerManager authenticationHandlerManager;

    @Mock
    private Request request;

    private DefaultAuthenticationHandlerSelector authenticationHandlerSelector;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        authenticationHandlerSelector = new DefaultAuthenticationHandlerSelector(authenticationHandlerManager);
    }

    @Test
    public void shouldNotResolveSecurityPolicy() {
        when(authenticationHandlerManager.getAuthenticationHandlers()).thenReturn(Collections.emptyList());

        authenticationHandlerManager.afterPropertiesSet();
        AuthenticationHandler securityProvider = authenticationHandlerSelector.select(request);
        assertNull(securityProvider);
    }

    @Test
    public void shouldResolveSecurityPolicy1() {
        AuthenticationHandler securityProvider1 = mock(AuthenticationHandler.class);
        when(securityProvider1.name()).thenReturn("keyless");
        when(securityProvider1.canHandle(any(AuthenticationContext.class))).thenReturn(true);
        when(securityProvider1.order()).thenReturn(1000);

        AuthenticationHandler securityProvider2 = mock(AuthenticationHandler.class);
        when(securityProvider2.name()).thenReturn("apikey");
        when(securityProvider2.order()).thenReturn(500);

        when(authenticationHandlerManager.getAuthenticationHandlers()).thenReturn(Arrays.asList(securityProvider1, securityProvider2));
        authenticationHandlerManager.afterPropertiesSet();

        AuthenticationHandler securityProvider = authenticationHandlerSelector.select(request);
        assertEquals(securityProvider1, securityProvider);
    }

    @Test
    public void shouldResolveSecurityPolicy2() {
        AuthenticationHandler securityProvider1 = mock(AuthenticationHandler.class);
        when(securityProvider1.name()).thenReturn("keyless");
        when(securityProvider1.order()).thenReturn(1000);

        AuthenticationHandler securityProvider2 = mock(AuthenticationHandler.class);
        when(securityProvider2.name()).thenReturn("apikey");
        when(securityProvider2.canHandle(any(AuthenticationContext.class))).thenReturn(true);
        when(securityProvider2.order()).thenReturn(500);

        when(authenticationHandlerManager.getAuthenticationHandlers()).thenReturn(Arrays.asList(securityProvider1, securityProvider2));

        authenticationHandlerManager.afterPropertiesSet();

        AuthenticationHandler securityProvider = authenticationHandlerSelector.select(request);
        assertEquals(securityProvider2, securityProvider);
    }
}

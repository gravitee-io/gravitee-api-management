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

import io.gravitee.gateway.api.ExecutionContext;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class AuthenticationHandlerSelectorTest {

    @Mock
    private AuthenticationHandlerManager authenticationHandlerManager;

    @Mock
    private ExecutionContext executionContext;

    private DefaultAuthenticationHandlerSelector authenticationHandlerSelector;

    @Before
    public void setUp() {
        authenticationHandlerSelector = new DefaultAuthenticationHandlerSelector(authenticationHandlerManager);
    }

    @Test
    public void shouldNotResolveSecurityPolicy() {
        when(authenticationHandlerManager.getAuthenticationHandlers()).thenReturn(Collections.emptyList());

        AuthenticationHandler securityProvider = authenticationHandlerSelector.select(executionContext);
        assertNull(securityProvider);
    }

    @Test
    public void shouldResolveSecurityPolicy1() {
        AuthenticationHandler securityProvider1 = mock(AuthenticationHandler.class);
        when(securityProvider1.canHandle(any(AuthenticationContext.class))).thenReturn(true);

        AuthenticationHandler securityProvider2 = mock(AuthenticationHandler.class);

        when(authenticationHandlerManager.getAuthenticationHandlers()).thenReturn(Arrays.asList(securityProvider1, securityProvider2));

        AuthenticationHandler securityProvider = authenticationHandlerSelector.select(executionContext);
        assertEquals(securityProvider1, securityProvider);
    }

    @Test
    public void shouldResolveSecurityPolicy2() {
        AuthenticationHandler securityProvider1 = mock(AuthenticationHandler.class);

        AuthenticationHandler securityProvider2 = mock(AuthenticationHandler.class);
        when(securityProvider2.canHandle(any(AuthenticationContext.class))).thenReturn(true);

        when(authenticationHandlerManager.getAuthenticationHandlers()).thenReturn(Arrays.asList(securityProvider1, securityProvider2));

        AuthenticationHandler securityProvider = authenticationHandlerSelector.select(executionContext);
        assertEquals(securityProvider2, securityProvider);
    }
}

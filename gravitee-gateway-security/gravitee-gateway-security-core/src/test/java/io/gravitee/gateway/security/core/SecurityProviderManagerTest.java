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

import io.gravitee.gateway.api.Request;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SecurityProviderManagerTest {

    @Mock
    private SecurityProviderLoader securityProviderLoader;

    private SecurityProviderManager securityManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        securityManager = new SecurityProviderManager();
        securityManager.setSecurityProviderLoader(securityProviderLoader);
    }

    @Test
    public void shouldSortSecurityProviders() {
        AuthenticationHandler securityProvider1 = mock(AuthenticationHandler.class);
        when(securityProvider1.name()).thenReturn("keyless");
        when(securityProvider1.order()).thenReturn(1000);

        AuthenticationHandler securityProvider2 = mock(AuthenticationHandler.class);
        when(securityProvider2.name()).thenReturn("apikey");
        when(securityProvider2.order()).thenReturn(500);

        when(securityProviderLoader.getSecurityProviders()).thenReturn(Arrays.asList(
                securityProvider1, securityProvider2));

        securityManager.initializeSecurityProviders();
        List<AuthenticationHandler> securityProviders = securityManager.getSecurityProviders();

        assertEquals(2, securityProviders.size());
        assertEquals(securityProvider2.name(), securityProviders.get(0).name());
        assertEquals(securityProvider1.name(), securityProviders.get(1).name());
    }

    @Test
    public void shouldFilterSecurityProviders() {
        AuthenticationHandler securityProvider1 = mock(AuthenticationHandler.class);
        when(securityProvider1.name()).thenReturn("keyless");
        when(securityProvider1.order()).thenReturn(1000);

        AuthenticationHandler securityProvider2 = mock(AuthenticationHandler.class);
        when(securityProvider2.name()).thenReturn("apikey");
        when(securityProvider2.order()).thenReturn(500);

        when(securityProviderLoader.getSecurityProviders()).thenReturn(Arrays.asList(
                securityProvider1, securityProvider2));

        AuthenticationHandlerEnhancer securityProviderFilter = mock(AuthenticationHandlerEnhancer.class);
        when(securityProviderFilter.filter(securityProviderLoader.getSecurityProviders()))
                .thenReturn(Collections.singletonList(securityProvider1));
        securityManager.setSecurityProviderFilter(securityProviderFilter);
        securityManager.initializeSecurityProviders();
        List<AuthenticationHandler> securityProviders = securityManager.getSecurityProviders();

        assertEquals(1, securityProviders.size());
        assertEquals(securityProvider1.name(), securityProviders.get(0).name());
    }

    @Test
    public void shouldNotResolveSecurityPolicy() {
        when(securityProviderLoader.getSecurityProviders()).thenReturn(
                Collections.emptyList());

        Request request = mock(Request.class);
        securityManager.initializeSecurityProviders();
        AuthenticationHandler securityProvider = securityManager.resolve(request);
        assertNull(securityProvider);
    }

    @Test
    public void shouldResolveSecurityPolicy1() {
        Request request = mock(Request.class);

        AuthenticationHandler securityProvider1 = mock(AuthenticationHandler.class);
        when(securityProvider1.name()).thenReturn("keyless");
        when(securityProvider1.canHandle(request)).thenReturn(true);
        when(securityProvider1.order()).thenReturn(1000);

        AuthenticationHandler securityProvider2 = mock(AuthenticationHandler.class);
        when(securityProvider2.name()).thenReturn("apikey");
        when(securityProvider2.order()).thenReturn(500);

        when(securityProviderLoader.getSecurityProviders()).thenReturn(
                Arrays.asList(securityProvider1, securityProvider2));
        securityManager.initializeSecurityProviders();

        AuthenticationHandler securityProvider = securityManager.resolve(request);
        assertEquals(securityProvider1, securityProvider);
    }

    @Test
    public void shouldResolveSecurityPolicy2() {
        Request request = mock(Request.class);

        AuthenticationHandler securityProvider1 = mock(AuthenticationHandler.class);
        when(securityProvider1.name()).thenReturn("keyless");
        when(securityProvider1.order()).thenReturn(1000);

        AuthenticationHandler securityProvider2 = mock(AuthenticationHandler.class);
        when(securityProvider2.name()).thenReturn("apikey");
        when(securityProvider2.canHandle(request)).thenReturn(true);
        when(securityProvider2.order()).thenReturn(500);

        when(securityProviderLoader.getSecurityProviders()).thenReturn(
                Arrays.asList(securityProvider1, securityProvider2));

        securityManager.initializeSecurityProviders();

        AuthenticationHandler securityProvider = securityManager.resolve(request);
        assertEquals(securityProvider2, securityProvider);
    }
}

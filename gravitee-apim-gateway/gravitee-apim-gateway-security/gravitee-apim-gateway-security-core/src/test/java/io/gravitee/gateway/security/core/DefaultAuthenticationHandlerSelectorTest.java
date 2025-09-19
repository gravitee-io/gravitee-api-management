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
package io.gravitee.gateway.security.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import io.gravitee.gateway.api.ExecutionContext;
import java.util.Collections;
import java.util.List;
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
public class DefaultAuthenticationHandlerSelectorTest {

    @Mock
    private AuthenticationHandlerManager authenticationHandlerManager;

    @Mock
    private ExecutionContext ctx;

    private DefaultAuthenticationHandlerSelector cut;

    @Before
    public void init() {
        cut = new DefaultAuthenticationHandlerSelector(authenticationHandlerManager);
    }

    @Test
    public void shouldNotResolveSecurityPolicyWhenNoAuthenticationHandler() {
        when(authenticationHandlerManager.getAuthenticationHandlers()).thenReturn(Collections.emptyList());

        final AuthenticationHandler selected = cut.select(ctx);

        assertNull(selected);
    }

    @Test
    public void shouldResolveSecurityPolicy1() {
        final AuthenticationHandler authenticationHandler1 = mock(AuthenticationHandler.class);
        final AuthenticationHandler authenticationHandler2 = mock(AuthenticationHandler.class);
        final AuthenticationHandler authenticationHandler3 = mock(AuthenticationHandler.class);

        when(authenticationHandler1.tokenType()).thenReturn("type");
        when(authenticationHandler2.tokenType()).thenReturn("type");

        when(authenticationHandler1.canHandle(any(AuthenticationContext.class))).thenReturn(true);
        when(authenticationHandlerManager.getAuthenticationHandlers()).thenReturn(
            List.of(authenticationHandler1, authenticationHandler2, authenticationHandler3)
        );

        final AuthenticationHandler selected = cut.select(ctx);

        assertSame(authenticationHandler1, selected);
        verify(authenticationHandler1).canHandle(
            argThat(authContext ->
                Boolean.FALSE.equals(
                    authContext.getInternalAttribute(AuthenticationContext.ATTR_INTERNAL_LAST_SECURITY_HANDLER_SUPPORTING_SAME_TOKEN_TYPE)
                )
            )
        );
        verify(authenticationHandler2, never()).canHandle(any());
        verify(authenticationHandler3, never()).canHandle(any());
    }

    @Test
    public void shouldResolveSecurityPolicy2() {
        final AuthenticationHandler authenticationHandler1 = mock(AuthenticationHandler.class);
        final AuthenticationHandler authenticationHandler2 = mock(AuthenticationHandler.class);
        final AuthenticationHandler authenticationHandler3 = mock(AuthenticationHandler.class);

        when(authenticationHandler1.tokenType()).thenReturn("type1");
        when(authenticationHandler2.tokenType()).thenReturn("type2");
        when(authenticationHandler3.tokenType()).thenReturn("type3");

        when(authenticationHandler2.canHandle(any(AuthenticationContext.class))).thenReturn(true);
        when(authenticationHandlerManager.getAuthenticationHandlers()).thenReturn(
            List.of(authenticationHandler1, authenticationHandler2, authenticationHandler3)
        );

        final AuthenticationHandler selected = cut.select(ctx);

        assertSame(authenticationHandler2, selected);

        verify(authenticationHandler1).canHandle(any());
        verify(authenticationHandler2).canHandle(
            argThat(authContext ->
                Boolean.TRUE.equals(
                    authContext.getInternalAttribute(AuthenticationContext.ATTR_INTERNAL_LAST_SECURITY_HANDLER_SUPPORTING_SAME_TOKEN_TYPE)
                )
            )
        );
        verify(authenticationHandler3, never()).canHandle(any());
    }
}

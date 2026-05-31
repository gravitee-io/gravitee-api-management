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
package io.gravitee.gateway.security.keyless;

import static io.gravitee.gateway.security.core.AuthenticationContext.TOKEN_TYPE_AUTHORIZATION_BEARER;
import static io.gravitee.gateway.security.core.AuthenticationContext.TOKEN_TYPE_NONE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.security.core.AuthenticationContext;
import io.gravitee.gateway.security.core.AuthenticationPolicy;
import io.gravitee.gateway.security.core.PluginAuthenticationPolicy;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class KeylessAuthenticationHandlerTest {

    @InjectMocks
    private KeylessAuthenticationHandler authenticationHandler = new KeylessAuthenticationHandler();

    @Test
    public void shouldHandleRequest() {
        AuthenticationContext context = mock(AuthenticationContext.class);

        boolean handle = authenticationHandler.canHandle(context);
        Assertions.assertTrue(handle);
    }

    @Test
    public void shouldReturnPolicies() {
        ExecutionContext executionContext = mock(ExecutionContext.class);

        List<AuthenticationPolicy> keylessProviderPolicies = authenticationHandler.handle(executionContext);

        Assertions.assertEquals(1, keylessProviderPolicies.size());

        PluginAuthenticationPolicy policy = (PluginAuthenticationPolicy) keylessProviderPolicies.iterator().next();
        Assertions.assertEquals(policy.name(), KeylessAuthenticationHandler.KEYLESS_POLICY);
    }

    @Test
    public void shouldReturnName() {
        Assertions.assertEquals("key_less", authenticationHandler.name());
    }

    @Test
    public void shouldReturnOrder() {
        Assertions.assertEquals(1000, authenticationHandler.order());
    }

    @Test
    public void shouldReuturnTokenType() {
        assertEquals(TOKEN_TYPE_NONE, authenticationHandler.tokenType());
    }
}

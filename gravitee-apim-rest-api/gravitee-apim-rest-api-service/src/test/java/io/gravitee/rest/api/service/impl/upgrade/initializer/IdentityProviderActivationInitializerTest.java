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
package io.gravitee.rest.api.service.impl.upgrade.initializer;

import static org.mockito.Mockito.*;

import io.gravitee.rest.api.model.configuration.identity.IdentityProviderEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.configuration.identity.IdentityProviderActivationService;
import io.gravitee.rest.api.service.configuration.identity.IdentityProviderService;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class IdentityProviderActivationInitializerTest {

    @Mock
    private IdentityProviderService identityProviderService;

    @Mock
    private IdentityProviderActivationService identityProviderActivationService;

    @InjectMocks
    private final IdentityProviderActivationInitializer initializer = new IdentityProviderActivationInitializer();

    @Test
    public void shouldActivateIdentityProviders() {
        when(identityProviderActivationService.findAllByTarget(any())).thenReturn(Set.of());
        IdentityProviderEntity idp = new IdentityProviderEntity();
        idp.setEnabled(true);
        idp.setId("identity-provider");
        when(identityProviderService.findAll(any(ExecutionContext.class))).thenReturn(Set.of(idp));
        initializer.initialize();
        verify(identityProviderActivationService, times(1)).activateIdpOnTargets(
            any(ExecutionContext.class),
            eq("identity-provider"),
            argThat(target -> target.getReferenceId().equals("DEFAULT")),
            argThat(target -> target.getReferenceId().equals("DEFAULT"))
        );
    }

    @Test
    public void testOrder() {
        Assert.assertEquals(InitializerOrder.IDENTITY_PROVIDER_ACTIVATION_INITIALIZER, initializer.getOrder());
    }
}

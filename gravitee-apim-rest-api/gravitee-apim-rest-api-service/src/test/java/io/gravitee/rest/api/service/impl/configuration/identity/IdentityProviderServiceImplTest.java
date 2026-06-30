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
package io.gravitee.rest.api.service.impl.configuration.identity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.repository.management.api.IdentityProviderRepository;
import io.gravitee.repository.management.model.IdentityProvider;
import io.gravitee.repository.management.model.IdentityProviderType;
import io.gravitee.rest.api.model.configuration.identity.UpdateIdentityProviderEntity;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.configuration.identity.IdentityProviderActivationService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class IdentityProviderServiceImplTest {

    @InjectMocks
    private IdentityProviderServiceImpl identityProviderService = new IdentityProviderServiceImpl();

    @Mock
    private IdentityProviderRepository identityProviderRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private RoleService roleService;

    @Mock
    private IdentityProviderActivationService identityProviderActivationService;

    private IdentityProvider existingIdp(List<String> whitelist) {
        IdentityProvider idp = new IdentityProvider();
        idp.setId("idp-1");
        idp.setOrganizationId("DEFAULT");
        idp.setType(IdentityProviderType.OIDC);
        idp.setPersistedClaimsWhitelist(whitelist);
        return idp;
    }

    private IdentityProvider captureUpdated() throws Exception {
        ArgumentCaptor<IdentityProvider> captor = ArgumentCaptor.forClass(IdentityProvider.class);
        verify(identityProviderRepository).update(captor.capture());
        return captor.getValue();
    }

    @Test
    public void update_should_preserve_whitelist_when_payload_omits_it() throws Exception {
        when(identityProviderRepository.findById("idp-1")).thenReturn(Optional.of(existingIdp(List.of("org_id"))));
        when(identityProviderRepository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateIdentityProviderEntity update = new UpdateIdentityProviderEntity();
        update.setName("My IdP");
        // persistedClaimsWhitelist intentionally left null (omitted by a console save unaware of the field)

        identityProviderService.update(GraviteeContext.getExecutionContext(), "idp-1", update);

        assertEquals(List.of("org_id"), captureUpdated().getPersistedClaimsWhitelist());
    }

    @Test
    public void update_should_overwrite_whitelist_when_provided() throws Exception {
        when(identityProviderRepository.findById("idp-1")).thenReturn(Optional.of(existingIdp(List.of("org_id"))));
        when(identityProviderRepository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateIdentityProviderEntity update = new UpdateIdentityProviderEntity();
        update.setName("My IdP");
        update.setPersistedClaimsWhitelist(List.of("tenant"));

        identityProviderService.update(GraviteeContext.getExecutionContext(), "idp-1", update);

        assertEquals(List.of("tenant"), captureUpdated().getPersistedClaimsWhitelist());
    }

    @Test
    public void update_should_clear_whitelist_when_empty_list_provided() throws Exception {
        when(identityProviderRepository.findById("idp-1")).thenReturn(Optional.of(existingIdp(List.of("org_id"))));
        when(identityProviderRepository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateIdentityProviderEntity update = new UpdateIdentityProviderEntity();
        update.setName("My IdP");
        update.setPersistedClaimsWhitelist(List.of());

        identityProviderService.update(GraviteeContext.getExecutionContext(), "idp-1", update);

        assertEquals(List.of(), captureUpdated().getPersistedClaimsWhitelist());
    }
}

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
package io.gravitee.repository.mock.management;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.collections.Sets.newSet;

import io.gravitee.repository.management.api.IdentityProviderRepository;
import io.gravitee.repository.management.model.IdentityProvider;
import io.gravitee.repository.management.model.IdentityProviderType;
import io.gravitee.repository.mock.AbstractRepositoryMock;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class IdentityProviderRepositoryMock extends AbstractRepositoryMock<IdentityProviderRepository> {

    public IdentityProviderRepositoryMock() {
        super(IdentityProviderRepository.class);
    }

    @Override
    protected void prepare(IdentityProviderRepository identityProviderRepository) throws Exception {
        final Map<String, String[]> groupMappings = new HashMap<>();
        groupMappings.put("{#jsonPath('$.email_verified')}", new String[] { "group1, group2" });

        final IdentityProvider newIdentityProvider = mock(IdentityProvider.class);
        when(newIdentityProvider.getName()).thenReturn("My idp 1");
        when(newIdentityProvider.getOrganizationId()).thenReturn("DEFAULT");
        when(newIdentityProvider.getDescription()).thenReturn("Description for my idp 1");
        when(newIdentityProvider.isEnabled()).thenReturn(true);
        when(newIdentityProvider.getCreatedAt()).thenReturn(new Date(1000000000000L));
        when(newIdentityProvider.getUpdatedAt()).thenReturn(new Date(1439032010883L));
        when(newIdentityProvider.getType()).thenReturn(IdentityProviderType.GITHUB);
        when(newIdentityProvider.getEmailRequired()).thenReturn(true);
        when(newIdentityProvider.getSyncMappings()).thenReturn(true);
        when(newIdentityProvider.getGroupMappings()).thenReturn(groupMappings);

        final IdentityProvider identityProvider1 = new IdentityProvider();
        identityProvider1.setId("github");
        identityProvider1.setOrganizationId("DEFAULT");
        identityProvider1.setEnabled(true);
        identityProvider1.setName("Google");
        identityProvider1.setDescription("GitHub Identity Provider");
        identityProvider1.setType(IdentityProviderType.OIDC);
        identityProvider1.setCreatedAt(new Date(1000000000000L));
        identityProvider1.setUpdatedAt(new Date(1486771200000L));
        identityProvider1.setGroupMappings(groupMappings);

        final IdentityProvider identityProviderUpdated = mock(IdentityProvider.class);
        when(identityProviderUpdated.getName()).thenReturn("Google");
        when(identityProviderUpdated.getOrganizationId()).thenReturn("DEFAULT");
        when(identityProviderUpdated.getDescription()).thenReturn("Google Identity Provider");
        when(identityProviderUpdated.getCreatedAt()).thenReturn(new Date(1000000000000L));
        when(identityProviderUpdated.getUpdatedAt()).thenReturn(new Date(1486771200000L));
        when(identityProviderUpdated.getType()).thenReturn(IdentityProviderType.GOOGLE);
        when(identityProviderUpdated.isEnabled()).thenReturn(true);
        when(identityProviderUpdated.getEmailRequired()).thenReturn(true);
        when(identityProviderUpdated.getSyncMappings()).thenReturn(true);
        when(identityProviderUpdated.getGroupMappings()).thenReturn(groupMappings);

        final IdentityProvider identityProvider3 = createMock();

        final Set<IdentityProvider> identityProviders = newSet(newIdentityProvider, identityProvider1, mock(IdentityProvider.class));
        final Set<IdentityProvider> identityProvidersAfterDelete = newSet(newIdentityProvider, identityProvider1);
        final Set<IdentityProvider> identityProvidersAfterAdd = newSet(
            newIdentityProvider,
            identityProvider1,
            mock(IdentityProvider.class),
            mock(IdentityProvider.class)
        );
        final Set<IdentityProvider> identityProvidersByEnv = newSet(newIdentityProvider, identityProvider1, identityProviderUpdated);
        when(identityProviderRepository.findAll())
            .thenReturn(identityProviders, identityProvidersAfterAdd, identityProviders, identityProvidersAfterDelete, identityProviders);
        when(identityProviderRepository.findAllByOrganizationId("DEFAULT")).thenReturn(identityProvidersByEnv);

        when(identityProviderRepository.create(any(IdentityProvider.class))).thenReturn(newIdentityProvider);

        when(identityProviderRepository.findById("new-idp")).thenReturn(of(newIdentityProvider));
        when(identityProviderRepository.findById("unknown")).thenReturn(empty());
        when(identityProviderRepository.findById("idp-1")).thenReturn(of(identityProvider1), of(identityProviderUpdated));
        when(identityProviderRepository.findById("idp-3")).thenReturn(of(identityProvider3));

        when(identityProviderRepository.update(argThat(o -> o == null || o.getId().equals("unknown"))))
            .thenThrow(new IllegalStateException());
    }

    private IdentityProvider createMock() {
        final IdentityProvider identityProvider3 = new IdentityProvider();
        identityProvider3.setId("idp-3");
        identityProvider3.setOrganizationId("DEFAULT");
        identityProvider3.setEnabled(false);
        identityProvider3.setName("Gravitee.io AM");
        identityProvider3.setDescription("Gravitee.io AM Identity Provider");
        identityProvider3.setType(IdentityProviderType.GRAVITEEIO_AM);
        identityProvider3.setCreatedAt(new Date(1000000000000L));
        identityProvider3.setUpdatedAt(new Date(1486771200000L));

        String condition = "{#jsonPath('$.email_verified')}";

        Map<String, String[]> groupMappings = new HashMap<>();
        groupMappings.put(condition, new String[] { "group1", "group2" });
        identityProvider3.setGroupMappings(groupMappings);

        Map<String, String[]> roleMappings = new HashMap<>();
        roleMappings.put(condition, new String[] { "role1", "role2" });
        identityProvider3.setRoleMappings(roleMappings);

        Map<String, String> userProfileMapping = new HashMap<>();
        userProfileMapping.put("sub", "id");
        userProfileMapping.put("firstname", "firstname");
        userProfileMapping.put("email", "mail");
        identityProvider3.setUserProfileMapping(userProfileMapping);

        return identityProvider3;
    }
}

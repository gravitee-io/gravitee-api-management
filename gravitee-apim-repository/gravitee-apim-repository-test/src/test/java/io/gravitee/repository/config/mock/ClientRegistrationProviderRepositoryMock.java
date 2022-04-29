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
package io.gravitee.repository.config.mock;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.collections.Sets.newSet;

import io.gravitee.repository.management.api.ClientRegistrationProviderRepository;
import io.gravitee.repository.management.model.ClientRegistrationProvider;
import java.util.Arrays;
import java.util.Date;
import java.util.Set;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ClientRegistrationProviderRepositoryMock extends AbstractRepositoryMock<ClientRegistrationProviderRepository> {

    public ClientRegistrationProviderRepositoryMock() {
        super(ClientRegistrationProviderRepository.class);
    }

    @Override
    void prepare(ClientRegistrationProviderRepository clientRegistrationProviderRepository) throws Exception {
        final ClientRegistrationProvider newClientRegistrationProvider = mock(ClientRegistrationProvider.class);
        when(newClientRegistrationProvider.getEnvironmentId()).thenReturn("envId");
        when(newClientRegistrationProvider.getName()).thenReturn("new DCR");
        when(newClientRegistrationProvider.getDescription()).thenReturn("Description for my new DCR");
        when(newClientRegistrationProvider.getDiscoveryEndpoint())
            .thenReturn("http://localhost:8092/oidc/.well-known/openid-configuration");
        when(newClientRegistrationProvider.getInitialAccessTokenType())
            .thenReturn(ClientRegistrationProvider.InitialAccessTokenType.CLIENT_CREDENTIALS);
        when(newClientRegistrationProvider.getClientId()).thenReturn("my-client-id");
        when(newClientRegistrationProvider.getClientSecret()).thenReturn("my-client-secret");
        when(newClientRegistrationProvider.getScopes()).thenReturn(Arrays.asList("scope1", "scope2", "scope3"));
        when(newClientRegistrationProvider.getCreatedAt()).thenReturn(new Date(1000000000000L));
        when(newClientRegistrationProvider.getUpdatedAt()).thenReturn(new Date(1486771200000L));

        final ClientRegistrationProvider clientRegistrationProvider1 = new ClientRegistrationProvider();
        clientRegistrationProvider1.setId("github");
        clientRegistrationProvider1.setName("OIDC-1");
        clientRegistrationProvider1.setDescription("GitHub Identity Provider");
        clientRegistrationProvider1.setCreatedAt(new Date(1000000000000L));
        clientRegistrationProvider1.setUpdatedAt(new Date(1486771200000L));

        final ClientRegistrationProvider clientRegistrationProviderUpdated = mock(ClientRegistrationProvider.class);
        when(clientRegistrationProviderUpdated.getName()).thenReturn("OIDC-1");
        when(clientRegistrationProviderUpdated.getDescription()).thenReturn("OIDC 1 Client registration provider");
        when(clientRegistrationProviderUpdated.getCreatedAt()).thenReturn(new Date(1000000000000L));
        when(clientRegistrationProviderUpdated.getUpdatedAt()).thenReturn(new Date(1486771200000L));

        final ClientRegistrationProvider clientRegistrationProvider3 = createMock();

        final Set<ClientRegistrationProvider> clientRegistrationProviders = newSet(
            newClientRegistrationProvider,
            clientRegistrationProvider1,
            mock(ClientRegistrationProvider.class)
        );
        final Set<ClientRegistrationProvider> clientRegistrationProvidersAfterDelete = newSet(
            newClientRegistrationProvider,
            clientRegistrationProvider1
        );
        final Set<ClientRegistrationProvider> clientRegistrationProvidersAfterAdd = newSet(
            newClientRegistrationProvider,
            clientRegistrationProvider1,
            mock(ClientRegistrationProvider.class),
            mock(ClientRegistrationProvider.class)
        );

        when(clientRegistrationProviderRepository.findAll())
            .thenReturn(
                clientRegistrationProviders,
                clientRegistrationProvidersAfterAdd,
                clientRegistrationProviders,
                clientRegistrationProvidersAfterDelete,
                clientRegistrationProviders
            );

        when(clientRegistrationProviderRepository.create(any(ClientRegistrationProvider.class))).thenReturn(newClientRegistrationProvider);

        when(clientRegistrationProviderRepository.findById("new-dcr")).thenReturn(of(newClientRegistrationProvider));
        when(clientRegistrationProviderRepository.findById("unknown")).thenReturn(empty());
        when(clientRegistrationProviderRepository.findById("oidc1"))
            .thenReturn(of(clientRegistrationProvider1), of(clientRegistrationProviderUpdated));
        when(clientRegistrationProviderRepository.findById("oidc3")).thenReturn(of(clientRegistrationProvider3));

        when(clientRegistrationProviderRepository.update(argThat(o -> o == null || o.getId().equals("unknown"))))
            .thenThrow(new IllegalStateException());
    }

    private ClientRegistrationProvider createMock() {
        final ClientRegistrationProvider clientRegistrationProvider3 = new ClientRegistrationProvider();
        clientRegistrationProvider3.setId("oidc3");
        clientRegistrationProvider3.setEnvironmentId("envIdB");
        clientRegistrationProvider3.setName("OIDC-3");
        clientRegistrationProvider3.setDescription("OIDC Client registration provider");
        clientRegistrationProvider3.setDiscoveryEndpoint("http://localhost:8092/oidc/.well-known/openid-configuration");
        clientRegistrationProvider3.setClientId("my-client-id");
        clientRegistrationProvider3.setClientSecret("my-client-secret");
        clientRegistrationProvider3.setScopes(Arrays.asList("scope1", "scope2", "scope3"));
        clientRegistrationProvider3.setRenewClientSecretSupport(true);
        clientRegistrationProvider3.setRenewClientSecretEndpoint("http://localhost/endpoint");
        clientRegistrationProvider3.setRenewClientSecretMethod("POST");

        clientRegistrationProvider3.setCreatedAt(new Date(1000000000000L));
        clientRegistrationProvider3.setUpdatedAt(new Date(1486771200000L));
        clientRegistrationProvider3.setInitialAccessTokenType(ClientRegistrationProvider.InitialAccessTokenType.CLIENT_CREDENTIALS);

        return clientRegistrationProvider3;
    }
}

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
package io.gravitee.repository.management;

import static io.gravitee.repository.utils.DateUtils.compareDate;
import static org.junit.jupiter.api.Assertions.*;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.ClientRegistrationProvider;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ClientRegistrationProviderRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/clientregistrationprovider-tests/";
    }

    @Test
    public void shouldFindAll() throws Exception {
        final Set<ClientRegistrationProvider> clientRegistrationProviders = clientRegistrationProviderRepository.findAll();

        assertNotNull(clientRegistrationProviders);
        assertEquals(5, clientRegistrationProviders.size());
    }

    @Test
    public void shouldFindById() throws Exception {
        final Optional<ClientRegistrationProvider> clientRegistrationProviderOpt = clientRegistrationProviderRepository.findById("oidc3");
        assertNotNull(clientRegistrationProviderOpt);
        assertTrue(clientRegistrationProviderOpt.isPresent());

        final ClientRegistrationProvider clientRegistrationProvider = clientRegistrationProviderOpt.get();
        assertNotNull(clientRegistrationProvider);
        assertEquals("envIdB", clientRegistrationProvider.getEnvironmentId());
        assertEquals("OIDC-3", clientRegistrationProvider.getName());
        assertEquals("OIDC Client registration provider", clientRegistrationProvider.getDescription());
        assertEquals("http://localhost:8092/oidc/.well-known/openid-configuration", clientRegistrationProvider.getDiscoveryEndpoint());
        assertEquals(
            ClientRegistrationProvider.InitialAccessTokenType.CLIENT_CREDENTIALS,
            clientRegistrationProvider.getInitialAccessTokenType()
        );
        assertEquals("my-client-id", clientRegistrationProvider.getClientId());
        assertEquals("my-client-secret", clientRegistrationProvider.getClientSecret());
        assertEquals(3, clientRegistrationProvider.getScopes().size());
        assertEquals("scope1", clientRegistrationProvider.getScopes().iterator().next());
        assertTrue(clientRegistrationProvider.isRenewClientSecretSupport());
        assertEquals("http://localhost/endpoint", clientRegistrationProvider.getRenewClientSecretEndpoint());
        assertEquals("POST", clientRegistrationProvider.getRenewClientSecretMethod());
    }

    @Test
    public void shouldCreate() throws Exception {
        final ClientRegistrationProvider clientRegistrationProvider = new ClientRegistrationProvider();
        clientRegistrationProvider.setId("new-dcr");
        clientRegistrationProvider.setEnvironmentId("envId");
        clientRegistrationProvider.setName("new DCR");
        clientRegistrationProvider.setDescription("Description for my new DCR");
        clientRegistrationProvider.setDiscoveryEndpoint("http://localhost:8092/oidc/.well-known/openid-configuration");
        clientRegistrationProvider.setInitialAccessTokenType(ClientRegistrationProvider.InitialAccessTokenType.CLIENT_CREDENTIALS);
        clientRegistrationProvider.setClientId("my-client-id");
        clientRegistrationProvider.setClientSecret("my-client-secret");
        clientRegistrationProvider.setScopes(Arrays.asList("scope1", "scope2", "scope3"));
        clientRegistrationProvider.setCreatedAt(new Date(1000000000000L));
        clientRegistrationProvider.setUpdatedAt(new Date(1486771200000L));

        int nbClientRegistrationProvidersBeforeCreation = clientRegistrationProviderRepository.findAll().size();
        clientRegistrationProviderRepository.create(clientRegistrationProvider);
        int nbClientRegistrationProvidersAfterCreation = clientRegistrationProviderRepository.findAll().size();

        Assertions.assertEquals(nbClientRegistrationProvidersBeforeCreation + 1, nbClientRegistrationProvidersAfterCreation);

        Optional<ClientRegistrationProvider> optional = clientRegistrationProviderRepository.findById("new-dcr");
        Assertions.assertTrue(optional.isPresent(), "Client registration provider saved not found");

        final ClientRegistrationProvider clientRegistrationProviderSaved = optional.get();
        Assertions.assertEquals(
            clientRegistrationProvider.getEnvironmentId(),
            clientRegistrationProviderSaved.getEnvironmentId(),
            "Invalid saved client registration provider environmentId."
        );
        Assertions.assertEquals(
            clientRegistrationProvider.getName(),
            clientRegistrationProviderSaved.getName(),
            "Invalid saved client registration provider name."
        );
        Assertions.assertEquals(
            clientRegistrationProvider.getDescription(),
            clientRegistrationProviderSaved.getDescription(),
            "Invalid client registration provider description."
        );
        Assertions.assertTrue(
            compareDate(clientRegistrationProvider.getCreatedAt(), clientRegistrationProviderSaved.getCreatedAt()),
            "Invalid client registration provider createdAt."
        );
        Assertions.assertTrue(
            compareDate(clientRegistrationProvider.getUpdatedAt(), clientRegistrationProviderSaved.getUpdatedAt()),
            "Invalid client registration provider updatedAt."
        );
        Assertions.assertEquals(
            clientRegistrationProvider.getDiscoveryEndpoint(),
            clientRegistrationProviderSaved.getDiscoveryEndpoint(),
            "Invalid client registration provider discovery endpoint."
        );
        Assertions.assertEquals(
            clientRegistrationProvider.getInitialAccessTokenType(),
            clientRegistrationProviderSaved.getInitialAccessTokenType(),
            "Invalid client registration provider initial access token type."
        );
        Assertions.assertEquals(
            clientRegistrationProvider.getClientId(),
            clientRegistrationProviderSaved.getClientId(),
            "Invalid client registration provider client id."
        );
        Assertions.assertEquals(
            clientRegistrationProvider.getClientSecret(),
            clientRegistrationProviderSaved.getClientSecret(),
            "Invalid client registration provider client secret."
        );
        Assertions.assertEquals(
            clientRegistrationProvider.getScopes().size(),
            clientRegistrationProviderSaved.getScopes().size(),
            "Invalid client registration provider scopes."
        );
    }

    @Test
    public void shouldCreateWithTruststoreAndKeystore() throws Exception {
        final ClientRegistrationProvider clientRegistrationProvider = new ClientRegistrationProvider();
        clientRegistrationProvider.setId("new-dcr-with-ssl");
        clientRegistrationProvider.setEnvironmentId("envId");
        clientRegistrationProvider.setName("new DCR with SSL");
        clientRegistrationProvider.setDescription("Description for my new DCR with SSL");
        clientRegistrationProvider.setDiscoveryEndpoint("http://localhost:8092/oidc/.well-known/openid-configuration");
        clientRegistrationProvider.setInitialAccessTokenType(ClientRegistrationProvider.InitialAccessTokenType.CLIENT_CREDENTIALS);
        clientRegistrationProvider.setClientId("my-client-id");
        clientRegistrationProvider.setClientSecret("my-client-secret");
        clientRegistrationProvider.setScopes(Arrays.asList("scope1", "scope2"));
        clientRegistrationProvider.setCreatedAt(new Date(1000000000000L));
        clientRegistrationProvider.setUpdatedAt(new Date(1486771200000L));

        // Set truststore fields
        clientRegistrationProvider.setTrustStoreType("JKS");
        clientRegistrationProvider.setTrustStorePath("/path/to/truststore.jks");
        clientRegistrationProvider.setTrustStoreContent("truststore-content-base64");
        clientRegistrationProvider.setTrustStorePassword("truststore-password");

        // Set keystore fields
        clientRegistrationProvider.setKeyStoreType("PKCS12");
        clientRegistrationProvider.setKeyStorePath("/path/to/keystore.p12");
        clientRegistrationProvider.setKeyStoreContent("keystore-content-base64");
        clientRegistrationProvider.setKeyStorePassword("keystore-password");
        clientRegistrationProvider.setKeyStoreAlias("my-alias");
        clientRegistrationProvider.setKeyPassword("key-password");

        int nbClientRegistrationProvidersBeforeCreation = clientRegistrationProviderRepository.findAll().size();
        clientRegistrationProviderRepository.create(clientRegistrationProvider);
        int nbClientRegistrationProvidersAfterCreation = clientRegistrationProviderRepository.findAll().size();

        Assertions.assertEquals(nbClientRegistrationProvidersBeforeCreation + 1, nbClientRegistrationProvidersAfterCreation);

        Optional<ClientRegistrationProvider> optional = clientRegistrationProviderRepository.findById("new-dcr-with-ssl");
        Assertions.assertTrue(optional.isPresent(), "Client registration provider with SSL saved not found");

        final ClientRegistrationProvider clientRegistrationProviderSaved = optional.get();

        // Verify truststore fields
        Assertions.assertEquals("JKS", clientRegistrationProviderSaved.getTrustStoreType(), "Invalid truststore type.");
        Assertions.assertEquals("/path/to/truststore.jks", clientRegistrationProviderSaved.getTrustStorePath(), "Invalid truststore path.");
        Assertions.assertEquals(
            "truststore-content-base64",
            clientRegistrationProviderSaved.getTrustStoreContent(),
            "Invalid truststore content."
        );
        Assertions.assertEquals(
            "truststore-password",
            clientRegistrationProviderSaved.getTrustStorePassword(),
            "Invalid truststore password."
        );

        // Verify keystore fields
        Assertions.assertEquals("PKCS12", clientRegistrationProviderSaved.getKeyStoreType(), "Invalid keystore type.");
        Assertions.assertEquals("/path/to/keystore.p12", clientRegistrationProviderSaved.getKeyStorePath(), "Invalid keystore path.");
        Assertions.assertEquals(
            "keystore-content-base64",
            clientRegistrationProviderSaved.getKeyStoreContent(),
            "Invalid keystore content."
        );
        Assertions.assertEquals("keystore-password", clientRegistrationProviderSaved.getKeyStorePassword(), "Invalid keystore password.");
        Assertions.assertEquals("my-alias", clientRegistrationProviderSaved.getKeyStoreAlias(), "Invalid keystore alias.");
        Assertions.assertEquals("key-password", clientRegistrationProviderSaved.getKeyPassword(), "Invalid key password.");
    }

    @Test
    public void shouldUpdate() throws Exception {
        Optional<ClientRegistrationProvider> optional = clientRegistrationProviderRepository.findById("oidc1");
        Assertions.assertTrue(optional.isPresent(), "Client registration provider to update not found");
        Assertions.assertEquals("OIDC-1", optional.get().getName(), "Invalid saved client registration provider name.");

        final ClientRegistrationProvider identityProvider = optional.get();
        identityProvider.setName("OIDC-1");
        identityProvider.setInitialAccessTokenType(ClientRegistrationProvider.InitialAccessTokenType.CLIENT_CREDENTIALS);
        identityProvider.setDescription("OIDC 1 Client registration provider");
        identityProvider.setCreatedAt(new Date(1000000000000L));
        identityProvider.setUpdatedAt(new Date(1486771200000L));

        // Set truststore and keystore fields
        identityProvider.setTrustStoreType("JKS");
        identityProvider.setTrustStorePath("/path/to/truststore.jks");
        identityProvider.setTrustStoreContent("updated-truststore-content");
        identityProvider.setTrustStorePassword("updated-truststore-password");
        identityProvider.setKeyStoreType("PKCS12");
        identityProvider.setKeyStorePath("/path/to/keystore.p12");
        identityProvider.setKeyStoreContent("updated-keystore-content");
        identityProvider.setKeyStorePassword("updated-keystore-password");
        identityProvider.setKeyStoreAlias("updated-alias");
        identityProvider.setKeyPassword("updated-key-password");

        int nbIdentityProvidersBeforeUpdate = clientRegistrationProviderRepository.findAll().size();
        clientRegistrationProviderRepository.update(identityProvider);
        int nbIdentityProvidersAfterUpdate = clientRegistrationProviderRepository.findAll().size();

        Assertions.assertEquals(nbIdentityProvidersBeforeUpdate, nbIdentityProvidersAfterUpdate);

        Optional<ClientRegistrationProvider> optionalUpdated = clientRegistrationProviderRepository.findById("oidc1");
        Assertions.assertTrue(optionalUpdated.isPresent(), "Client registration provider to update not found");

        final ClientRegistrationProvider identityProviderUpdated = optionalUpdated.get();
        Assertions.assertEquals(
            identityProvider.getEnvironmentId(),
            identityProviderUpdated.getEnvironmentId(),
            "Invalid saved client registration provider environmentId."
        );
        Assertions.assertEquals(
            identityProvider.getName(),
            identityProviderUpdated.getName(),
            "Invalid saved client registration provider name."
        );
        Assertions.assertEquals(
            identityProvider.getDescription(),
            identityProviderUpdated.getDescription(),
            "Invalid client registration provider description."
        );
        Assertions.assertTrue(
            compareDate(identityProvider.getCreatedAt(), identityProviderUpdated.getCreatedAt()),
            "Invalid client registration provider createdAt."
        );
        Assertions.assertTrue(
            compareDate(identityProvider.getUpdatedAt(), identityProviderUpdated.getUpdatedAt()),
            "Invalid client registration provider updatedAt."
        );

        // Verify truststore fields are updated
        Assertions.assertEquals(
            identityProvider.getTrustStoreType(),
            identityProviderUpdated.getTrustStoreType(),
            "Invalid updated truststore type."
        );
        Assertions.assertEquals(
            identityProvider.getTrustStorePath(),
            identityProviderUpdated.getTrustStorePath(),
            "Invalid updated truststore path."
        );
        Assertions.assertEquals(
            identityProvider.getTrustStoreContent(),
            identityProviderUpdated.getTrustStoreContent(),
            "Invalid updated truststore content."
        );
        Assertions.assertEquals(
            identityProvider.getTrustStorePassword(),
            identityProviderUpdated.getTrustStorePassword(),
            "Invalid updated truststore password."
        );

        // Verify keystore fields are updated
        Assertions.assertEquals(
            identityProvider.getKeyStoreType(),
            identityProviderUpdated.getKeyStoreType(),
            "Invalid updated keystore type."
        );
        Assertions.assertEquals(
            identityProvider.getKeyStorePath(),
            identityProviderUpdated.getKeyStorePath(),
            "Invalid updated keystore path."
        );
        Assertions.assertEquals(
            identityProvider.getKeyStoreContent(),
            identityProviderUpdated.getKeyStoreContent(),
            "Invalid updated keystore content."
        );
        Assertions.assertEquals(
            identityProvider.getKeyStorePassword(),
            identityProviderUpdated.getKeyStorePassword(),
            "Invalid updated keystore password."
        );
        Assertions.assertEquals(
            identityProvider.getKeyStoreAlias(),
            identityProviderUpdated.getKeyStoreAlias(),
            "Invalid updated keystore alias."
        );
        Assertions.assertEquals(
            identityProvider.getKeyPassword(),
            identityProviderUpdated.getKeyPassword(),
            "Invalid updated key password."
        );
    }

    @Test
    public void shouldCreateWithNullTruststoreAndKeystore() throws Exception {
        final ClientRegistrationProvider clientRegistrationProvider = new ClientRegistrationProvider();
        clientRegistrationProvider.setId("new-dcr-null-ssl");
        clientRegistrationProvider.setEnvironmentId("envId");
        clientRegistrationProvider.setName("new DCR with null SSL");
        clientRegistrationProvider.setDescription("Description for my new DCR with null SSL");
        clientRegistrationProvider.setDiscoveryEndpoint("http://localhost:8092/oidc/.well-known/openid-configuration");
        clientRegistrationProvider.setInitialAccessTokenType(ClientRegistrationProvider.InitialAccessTokenType.CLIENT_CREDENTIALS);
        clientRegistrationProvider.setClientId("my-client-id");
        clientRegistrationProvider.setClientSecret("my-client-secret");
        clientRegistrationProvider.setScopes(Arrays.asList("scope1"));
        clientRegistrationProvider.setCreatedAt(new Date(1000000000000L));
        clientRegistrationProvider.setUpdatedAt(new Date(1486771200000L));

        // Leave truststore and keystore fields as null to test nullable columns
        clientRegistrationProvider.setTrustStoreType(null);
        clientRegistrationProvider.setTrustStorePath(null);
        clientRegistrationProvider.setTrustStoreContent(null);
        clientRegistrationProvider.setTrustStorePassword(null);
        clientRegistrationProvider.setKeyStoreType(null);
        clientRegistrationProvider.setKeyStorePath(null);
        clientRegistrationProvider.setKeyStoreContent(null);
        clientRegistrationProvider.setKeyStorePassword(null);
        clientRegistrationProvider.setKeyStoreAlias(null);
        clientRegistrationProvider.setKeyPassword(null);

        clientRegistrationProviderRepository.create(clientRegistrationProvider);

        Optional<ClientRegistrationProvider> optional = clientRegistrationProviderRepository.findById("new-dcr-null-ssl");
        Assertions.assertTrue(optional.isPresent(), "Client registration provider with null SSL saved not found");

        final ClientRegistrationProvider clientRegistrationProviderSaved = optional.get();

        // Verify that null values are preserved
        Assertions.assertNull(clientRegistrationProviderSaved.getTrustStoreType(), "Truststore type should be null");
        Assertions.assertNull(clientRegistrationProviderSaved.getTrustStorePath(), "Truststore path should be null");
        Assertions.assertNull(clientRegistrationProviderSaved.getTrustStoreContent(), "Truststore content should be null");
        Assertions.assertNull(clientRegistrationProviderSaved.getTrustStorePassword(), "Truststore password should be null");
        Assertions.assertNull(clientRegistrationProviderSaved.getKeyStoreType(), "Keystore type should be null");
        Assertions.assertNull(clientRegistrationProviderSaved.getKeyStorePath(), "Keystore path should be null");
        Assertions.assertNull(clientRegistrationProviderSaved.getKeyStoreContent(), "Keystore content should be null");
        Assertions.assertNull(clientRegistrationProviderSaved.getKeyStorePassword(), "Keystore password should be null");
        Assertions.assertNull(clientRegistrationProviderSaved.getKeyStoreAlias(), "Keystore alias should be null");
        Assertions.assertNull(clientRegistrationProviderSaved.getKeyPassword(), "Key password should be null");
    }

    @Test
    public void shouldDelete() throws Exception {
        int nbClientRegistrationProvidersBeforeDeletion = clientRegistrationProviderRepository.findAll().size();
        clientRegistrationProviderRepository.delete("oidc3");
        int nbClientRegistrationProvidersAfterDeletion = clientRegistrationProviderRepository.findAll().size();

        Assertions.assertEquals(nbClientRegistrationProvidersBeforeDeletion - 1, nbClientRegistrationProvidersAfterDeletion);
    }

    @Test
    public void shouldNotUpdateUnknownClientRegistrationProvider() throws Exception {
        assertThrows(IllegalStateException.class, () -> {
            ClientRegistrationProvider unknownClientRegistrationProvider = new ClientRegistrationProvider();
            unknownClientRegistrationProvider.setId("unknown");
            clientRegistrationProviderRepository.update(unknownClientRegistrationProvider);
            fail("An unknown client registration provider should not be updated");
        });
    }

    @Test
    public void shouldNotUpdateNull() throws Exception {
        assertThrows(IllegalStateException.class, () -> {
            clientRegistrationProviderRepository.update(null);
            fail("A null client registration provider should not be updated");
        });
    }

    @Test
    public void shouldFindAllByEnvironment() throws Exception {
        String environmentId = "envIdA";
        final Set<ClientRegistrationProvider> clientRegistrationProviders = clientRegistrationProviderRepository.findAllByEnvironment(
            environmentId
        );

        assertNotNull(clientRegistrationProviders);
        assertEquals(2, clientRegistrationProviders.size());
    }

    @Test
    public void should_delete_by_environment_id() throws TechnicalException {
        List<ClientRegistrationProvider> clientRegistrationProvidersBeforeDeletion = clientRegistrationProviderRepository
            .findAllByEnvironment("ToBeDeleted")
            .stream()
            .toList();
        assertEquals(2, clientRegistrationProvidersBeforeDeletion.size());

        List<String> clientRegistrationProvidersDeleted = clientRegistrationProviderRepository.deleteByEnvironmentId("ToBeDeleted");

        assertEquals(2, clientRegistrationProvidersDeleted.size());
        assertEquals(0, clientRegistrationProviderRepository.findAllByEnvironment("ToBeDeleted").size());
    }
}

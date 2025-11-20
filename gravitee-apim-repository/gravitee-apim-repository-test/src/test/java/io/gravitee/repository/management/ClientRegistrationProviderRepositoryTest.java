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
import static org.junit.Assert.*;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.ClientRegistrationProvider;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;

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

        Assert.assertEquals(nbClientRegistrationProvidersBeforeCreation + 1, nbClientRegistrationProvidersAfterCreation);

        Optional<ClientRegistrationProvider> optional = clientRegistrationProviderRepository.findById("new-dcr");
        Assert.assertTrue("Client registration provider saved not found", optional.isPresent());

        final ClientRegistrationProvider clientRegistrationProviderSaved = optional.get();
        Assert.assertEquals(
            "Invalid saved client registration provider environmentId.",
            clientRegistrationProvider.getEnvironmentId(),
            clientRegistrationProviderSaved.getEnvironmentId()
        );
        Assert.assertEquals(
            "Invalid saved client registration provider name.",
            clientRegistrationProvider.getName(),
            clientRegistrationProviderSaved.getName()
        );
        Assert.assertEquals(
            "Invalid client registration provider description.",
            clientRegistrationProvider.getDescription(),
            clientRegistrationProviderSaved.getDescription()
        );
        Assert.assertTrue(
            "Invalid client registration provider createdAt.",
            compareDate(clientRegistrationProvider.getCreatedAt(), clientRegistrationProviderSaved.getCreatedAt())
        );
        Assert.assertTrue(
            "Invalid client registration provider updatedAt.",
            compareDate(clientRegistrationProvider.getUpdatedAt(), clientRegistrationProviderSaved.getUpdatedAt())
        );
        Assert.assertEquals(
            "Invalid client registration provider discovery endpoint.",
            clientRegistrationProvider.getDiscoveryEndpoint(),
            clientRegistrationProviderSaved.getDiscoveryEndpoint()
        );
        Assert.assertEquals(
            "Invalid client registration provider initial access token type.",
            clientRegistrationProvider.getInitialAccessTokenType(),
            clientRegistrationProviderSaved.getInitialAccessTokenType()
        );
        Assert.assertEquals(
            "Invalid client registration provider client id.",
            clientRegistrationProvider.getClientId(),
            clientRegistrationProviderSaved.getClientId()
        );
        Assert.assertEquals(
            "Invalid client registration provider client secret.",
            clientRegistrationProvider.getClientSecret(),
            clientRegistrationProviderSaved.getClientSecret()
        );
        Assert.assertEquals(
            "Invalid client registration provider scopes.",
            clientRegistrationProvider.getScopes().size(),
            clientRegistrationProviderSaved.getScopes().size()
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

        Assert.assertEquals(nbClientRegistrationProvidersBeforeCreation + 1, nbClientRegistrationProvidersAfterCreation);

        Optional<ClientRegistrationProvider> optional = clientRegistrationProviderRepository.findById("new-dcr-with-ssl");
        Assert.assertTrue("Client registration provider with SSL saved not found", optional.isPresent());

        final ClientRegistrationProvider clientRegistrationProviderSaved = optional.get();

        // Verify truststore fields
        Assert.assertEquals("Invalid truststore type.", "JKS", clientRegistrationProviderSaved.getTrustStoreType());
        Assert.assertEquals("Invalid truststore path.", "/path/to/truststore.jks", clientRegistrationProviderSaved.getTrustStorePath());
        Assert.assertEquals(
            "Invalid truststore content.",
            "truststore-content-base64",
            clientRegistrationProviderSaved.getTrustStoreContent()
        );
        Assert.assertEquals("Invalid truststore password.", "truststore-password", clientRegistrationProviderSaved.getTrustStorePassword());

        // Verify keystore fields
        Assert.assertEquals("Invalid keystore type.", "PKCS12", clientRegistrationProviderSaved.getKeyStoreType());
        Assert.assertEquals("Invalid keystore path.", "/path/to/keystore.p12", clientRegistrationProviderSaved.getKeyStorePath());
        Assert.assertEquals("Invalid keystore content.", "keystore-content-base64", clientRegistrationProviderSaved.getKeyStoreContent());
        Assert.assertEquals("Invalid keystore password.", "keystore-password", clientRegistrationProviderSaved.getKeyStorePassword());
        Assert.assertEquals("Invalid keystore alias.", "my-alias", clientRegistrationProviderSaved.getKeyStoreAlias());
        Assert.assertEquals("Invalid key password.", "key-password", clientRegistrationProviderSaved.getKeyPassword());
    }

    @Test
    public void shouldUpdate() throws Exception {
        Optional<ClientRegistrationProvider> optional = clientRegistrationProviderRepository.findById("oidc1");
        Assert.assertTrue("Client registration provider to update not found", optional.isPresent());
        Assert.assertEquals("Invalid saved client registration provider name.", "OIDC-1", optional.get().getName());

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

        Assert.assertEquals(nbIdentityProvidersBeforeUpdate, nbIdentityProvidersAfterUpdate);

        Optional<ClientRegistrationProvider> optionalUpdated = clientRegistrationProviderRepository.findById("oidc1");
        Assert.assertTrue("Client registration provider to update not found", optionalUpdated.isPresent());

        final ClientRegistrationProvider identityProviderUpdated = optionalUpdated.get();
        Assert.assertEquals(
            "Invalid saved client registration provider environmentId.",
            identityProvider.getEnvironmentId(),
            identityProviderUpdated.getEnvironmentId()
        );
        Assert.assertEquals(
            "Invalid saved client registration provider name.",
            identityProvider.getName(),
            identityProviderUpdated.getName()
        );
        Assert.assertEquals(
            "Invalid client registration provider description.",
            identityProvider.getDescription(),
            identityProviderUpdated.getDescription()
        );
        Assert.assertTrue(
            "Invalid client registration provider createdAt.",
            compareDate(identityProvider.getCreatedAt(), identityProviderUpdated.getCreatedAt())
        );
        Assert.assertTrue(
            "Invalid client registration provider updatedAt.",
            compareDate(identityProvider.getUpdatedAt(), identityProviderUpdated.getUpdatedAt())
        );

        // Verify truststore fields are updated
        Assert.assertEquals(
            "Invalid updated truststore type.",
            identityProvider.getTrustStoreType(),
            identityProviderUpdated.getTrustStoreType()
        );
        Assert.assertEquals(
            "Invalid updated truststore path.",
            identityProvider.getTrustStorePath(),
            identityProviderUpdated.getTrustStorePath()
        );
        Assert.assertEquals(
            "Invalid updated truststore content.",
            identityProvider.getTrustStoreContent(),
            identityProviderUpdated.getTrustStoreContent()
        );
        Assert.assertEquals(
            "Invalid updated truststore password.",
            identityProvider.getTrustStorePassword(),
            identityProviderUpdated.getTrustStorePassword()
        );

        // Verify keystore fields are updated
        Assert.assertEquals(
            "Invalid updated keystore type.",
            identityProvider.getKeyStoreType(),
            identityProviderUpdated.getKeyStoreType()
        );
        Assert.assertEquals(
            "Invalid updated keystore path.",
            identityProvider.getKeyStorePath(),
            identityProviderUpdated.getKeyStorePath()
        );
        Assert.assertEquals(
            "Invalid updated keystore content.",
            identityProvider.getKeyStoreContent(),
            identityProviderUpdated.getKeyStoreContent()
        );
        Assert.assertEquals(
            "Invalid updated keystore password.",
            identityProvider.getKeyStorePassword(),
            identityProviderUpdated.getKeyStorePassword()
        );
        Assert.assertEquals(
            "Invalid updated keystore alias.",
            identityProvider.getKeyStoreAlias(),
            identityProviderUpdated.getKeyStoreAlias()
        );
        Assert.assertEquals("Invalid updated key password.", identityProvider.getKeyPassword(), identityProviderUpdated.getKeyPassword());
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
        Assert.assertTrue("Client registration provider with null SSL saved not found", optional.isPresent());

        final ClientRegistrationProvider clientRegistrationProviderSaved = optional.get();

        // Verify that null values are preserved
        Assert.assertNull("Truststore type should be null", clientRegistrationProviderSaved.getTrustStoreType());
        Assert.assertNull("Truststore path should be null", clientRegistrationProviderSaved.getTrustStorePath());
        Assert.assertNull("Truststore content should be null", clientRegistrationProviderSaved.getTrustStoreContent());
        Assert.assertNull("Truststore password should be null", clientRegistrationProviderSaved.getTrustStorePassword());
        Assert.assertNull("Keystore type should be null", clientRegistrationProviderSaved.getKeyStoreType());
        Assert.assertNull("Keystore path should be null", clientRegistrationProviderSaved.getKeyStorePath());
        Assert.assertNull("Keystore content should be null", clientRegistrationProviderSaved.getKeyStoreContent());
        Assert.assertNull("Keystore password should be null", clientRegistrationProviderSaved.getKeyStorePassword());
        Assert.assertNull("Keystore alias should be null", clientRegistrationProviderSaved.getKeyStoreAlias());
        Assert.assertNull("Key password should be null", clientRegistrationProviderSaved.getKeyPassword());
    }

    @Test
    public void shouldDelete() throws Exception {
        int nbClientRegistrationProvidersBeforeDeletion = clientRegistrationProviderRepository.findAll().size();
        clientRegistrationProviderRepository.delete("oidc3");
        int nbClientRegistrationProvidersAfterDeletion = clientRegistrationProviderRepository.findAll().size();

        Assert.assertEquals(nbClientRegistrationProvidersBeforeDeletion - 1, nbClientRegistrationProvidersAfterDeletion);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotUpdateUnknownClientRegistrationProvider() throws Exception {
        ClientRegistrationProvider unknownClientRegistrationProvider = new ClientRegistrationProvider();
        unknownClientRegistrationProvider.setId("unknown");
        clientRegistrationProviderRepository.update(unknownClientRegistrationProvider);
        fail("An unknown client registration provider should not be updated");
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotUpdateNull() throws Exception {
        clientRegistrationProviderRepository.update(null);
        fail("A null client registration provider should not be updated");
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

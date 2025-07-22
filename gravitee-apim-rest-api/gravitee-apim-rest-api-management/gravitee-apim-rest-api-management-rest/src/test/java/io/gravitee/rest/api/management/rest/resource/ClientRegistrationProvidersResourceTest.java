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
package io.gravitee.rest.api.management.rest.resource;

import static io.gravitee.common.http.HttpStatusCode.CREATED_201;
import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.gravitee.node.api.license.License;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.rest.api.model.configuration.application.registration.ClientRegistrationProviderEntity;
import io.gravitee.rest.api.model.configuration.application.registration.InitialAccessTokenType;
import io.gravitee.rest.api.model.configuration.application.registration.KeyStoreEntity;
import io.gravitee.rest.api.model.configuration.application.registration.NewClientRegistrationProviderEntity;
import io.gravitee.rest.api.model.configuration.application.registration.TrustStoreEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import javax.inject.Inject;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ClientRegistrationProvidersResourceTest extends AbstractResourceTest {

    @Inject
    private LicenseManager licenseManager;

    private License license;

    @Override
    protected String contextPath() {
        return "configuration/applications/registration/providers";
    }

    @Before
    public void init() {
        license = mock(License.class);
        when(licenseManager.getPlatformLicense()).thenReturn(license);
    }

    @Test
    public void should_allow_create_with_dcr_registration_feature() {
        when(license.isFeatureEnabled("apim-dcr-registration")).thenReturn(true);

        reset(clientRegistrationService);
        NewClientRegistrationProviderEntity newClientRegistrationProviderEntity = new NewClientRegistrationProviderEntity();
        newClientRegistrationProviderEntity.setName("my-client-registration-provider-name");
        newClientRegistrationProviderEntity.setDiscoveryEndpoint("my-client-registration-provider-discovery-endpoint");
        newClientRegistrationProviderEntity.setInitialAccessTokenType(InitialAccessTokenType.INITIAL_ACCESS_TOKEN);
        newClientRegistrationProviderEntity.setInitialAccessToken("my-client-registration-provider-initial-access-token");

        ClientRegistrationProviderEntity createdClientRegistrationProvider = new ClientRegistrationProviderEntity();
        createdClientRegistrationProvider.setId("my-client-registration-provider-id");
        when(clientRegistrationService.create(eq(GraviteeContext.getExecutionContext()), any()))
            .thenReturn(createdClientRegistrationProvider);

        final Response response = envTarget().request().post(Entity.json(newClientRegistrationProviderEntity));
        assertEquals(CREATED_201, response.getStatus());
        assertEquals(
            envTarget().path("my-client-registration-provider-id").getUri().toString(),
            response.getHeaders().getFirst(HttpHeaders.LOCATION)
        );
    }

    @Test
    public void should_forbid_create_without_dcr_registration_feature() {
        when(license.isFeatureEnabled("apim-dcr-registration")).thenReturn(false);

        NewClientRegistrationProviderEntity newClientRegistrationProviderEntity = new NewClientRegistrationProviderEntity();
        newClientRegistrationProviderEntity.setName("my-client-registration-provider-name");
        newClientRegistrationProviderEntity.setDiscoveryEndpoint("my-client-registration-provider-discovery-endpoint");
        newClientRegistrationProviderEntity.setInitialAccessTokenType(InitialAccessTokenType.INITIAL_ACCESS_TOKEN);
        newClientRegistrationProviderEntity.setInitialAccessToken("my-client-registration-provider-initial-access-token");
        final Response response = envTarget().request().post(Entity.json(newClientRegistrationProviderEntity));
        assertEquals(FORBIDDEN_403, response.getStatus());
    }

    @Test
    public void should_allow_create_with_dcr_registration_feature_truststore() {
        when(license.isFeatureEnabled("apim-dcr-registration")).thenReturn(true);

        reset(clientRegistrationService);
        NewClientRegistrationProviderEntity newClientRegistrationProviderEntity = new NewClientRegistrationProviderEntity();
        newClientRegistrationProviderEntity.setName("my-client-registration-provider-name");
        newClientRegistrationProviderEntity.setDiscoveryEndpoint("my-client-registration-provider-discovery-endpoint");
        newClientRegistrationProviderEntity.setInitialAccessTokenType(InitialAccessTokenType.INITIAL_ACCESS_TOKEN);
        newClientRegistrationProviderEntity.setInitialAccessToken("my-client-registration-provider-initial-access-token");

        TrustStoreEntity trustStoreEntity = new TrustStoreEntity();
        trustStoreEntity.setType(TrustStoreEntity.Type.PKCS12);
        trustStoreEntity.setContent("my-client-registration-provider-truststore-content");
        trustStoreEntity.setPassword("my-client-registration-provider-truststore-password");
        newClientRegistrationProviderEntity.setTrustStore(trustStoreEntity);

        ClientRegistrationProviderEntity createdClientRegistrationProvider = new ClientRegistrationProviderEntity();
        createdClientRegistrationProvider.setId("my-client-registration-provider-id");
        when(clientRegistrationService.create(eq(GraviteeContext.getExecutionContext()), any()))
            .thenReturn(createdClientRegistrationProvider);

        final Response response = envTarget().request().post(Entity.json(newClientRegistrationProviderEntity));
        assertEquals(CREATED_201, response.getStatus());
        assertEquals(
            envTarget().path("my-client-registration-provider-id").getUri().toString(),
            response.getHeaders().getFirst(HttpHeaders.LOCATION)
        );
    }

    @Test
    public void should_allow_create_with_dcr_registration_feature_keystore() {
        when(license.isFeatureEnabled("apim-dcr-registration")).thenReturn(true);

        reset(clientRegistrationService);
        NewClientRegistrationProviderEntity newClientRegistrationProviderEntity = new NewClientRegistrationProviderEntity();
        newClientRegistrationProviderEntity.setName("my-client-registration-provider-name");
        newClientRegistrationProviderEntity.setDiscoveryEndpoint("my-client-registration-provider-discovery-endpoint");
        newClientRegistrationProviderEntity.setInitialAccessTokenType(InitialAccessTokenType.INITIAL_ACCESS_TOKEN);
        newClientRegistrationProviderEntity.setInitialAccessToken("my-client-registration-provider-initial-access-token");

        TrustStoreEntity trustStoreEntity = new TrustStoreEntity();
        trustStoreEntity.setType(TrustStoreEntity.Type.PKCS12);
        trustStoreEntity.setContent("my-client-registration-provider-truststore-content");
        trustStoreEntity.setPassword("my-client-registration-provider-truststore-password");
        newClientRegistrationProviderEntity.setTrustStore(trustStoreEntity);

        KeyStoreEntity keyStoreEntity = new KeyStoreEntity();
        keyStoreEntity.setType(KeyStoreEntity.Type.PKCS12);
        keyStoreEntity.setContent("my-client-registration-provider-keystore-content");
        keyStoreEntity.setPassword("my-client-registration-provider-keystore-password");
        newClientRegistrationProviderEntity.setKeyStore(keyStoreEntity);

        ClientRegistrationProviderEntity createdClientRegistrationProvider = new ClientRegistrationProviderEntity();
        createdClientRegistrationProvider.setId("my-client-registration-provider-id");
        when(clientRegistrationService.create(eq(GraviteeContext.getExecutionContext()), any()))
            .thenReturn(createdClientRegistrationProvider);

        final Response response = envTarget().request().post(Entity.json(newClientRegistrationProviderEntity));
        assertEquals(CREATED_201, response.getStatus());
        assertEquals(
            envTarget().path("my-client-registration-provider-id").getUri().toString(),
            response.getHeaders().getFirst(HttpHeaders.LOCATION)
        );
    }
}

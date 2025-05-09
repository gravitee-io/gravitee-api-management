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
package io.gravitee.rest.api.portal.rest.resource;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.configuration.identity.IdentityProviderActivationEntity;
import io.gravitee.rest.api.model.configuration.identity.am.AMIdentityProviderEntity;
import io.gravitee.rest.api.model.configuration.identity.github.GitHubIdentityProviderEntity;
import io.gravitee.rest.api.model.configuration.identity.google.GoogleIdentityProviderEntity;
import io.gravitee.rest.api.model.configuration.identity.oidc.OIDCIdentityProviderEntity;
import io.gravitee.rest.api.model.settings.PortalSettingsEntity;
import io.gravitee.rest.api.portal.rest.model.ConfigurationIdentitiesResponse;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import org.junit.jupiter.api.Test;
import org.mockito.internal.util.collections.Sets;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ConfigurationIdentitiesResourceTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "configuration/identities";
    }

    private static final String IDP_AUTHORIZATION_ENDPOINT = "my-idp-authorization-endpoint";
    private static final String IDP_CLIENT_ID = "my-idp-client-id";
    private static final String IDP_CLIENT_SECRET = "my-idp-client-secret";
    private static final String IDP_COLOR = "my-idp-color";
    private static final String IDP_DESCRIPTION = "my-idp-description";
    private static final String IDP_DISCOVERY_ENDPOINT = "my-idp-discovery-endpoint";
    private static final boolean IDP_EMAIL_REQUIRED = true;
    private static final String IDP_ID = "my-idp-id";
    private static final String IDP_NAME = "my-idp-name";
    private static final String IDP_SCOPE = "my-idp-scope";
    private static final String IDP_TOKEN_ENDPOINT = "my-idp-token-endpoint";
    private static final String IDP_TOKEN_INTROSPECTION_ENDPOINT = "my-idp-token-introspection-endpoint";
    private static final String IDP_USER_INFO_ENDPOINT = "my-idp-user-info-endpoint";
    private static final String IDP_USER_LOGOUT_ENDPOINT = "my-idp-user-logout-endpoint";

    private String serverUrl = "SERVER_URL";

    @Test
    public void shouldGetConfigurationIdentities() {
        resetAllMocks();

        IdentityProviderActivationEntity activatedIdp = new IdentityProviderActivationEntity();
        activatedIdp.setIdentityProvider(IDP_ID);

        doReturn(Sets.newSet(activatedIdp)).when(identityProviderActivationService).findAllByTarget(any());

        doReturn(
            Sets.newSet(
                mockAMIdentityProviderEntity(),
                mockGoogleIdentityProviderEntity(),
                mockGitHubIdentityProviderEntity(),
                mockOIDCIdentityProviderEntity()
            )
        )
            .when(socialIdentityProviderService)
            .findAll(eq(GraviteeContext.getExecutionContext()), any());

        PortalSettingsEntity configEntity = new PortalSettingsEntity();
        doReturn(configEntity).when(configService).getPortalSettings(GraviteeContext.getExecutionContext());

        final Response response = target().request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        verify(identityProviderMapper, times(4)).convert(any());
        verify(socialIdentityProviderService).findAll(eq(GraviteeContext.getExecutionContext()), any());

        ConfigurationIdentitiesResponse configurationIdentitiesResponse = response.readEntity(ConfigurationIdentitiesResponse.class);
        assertEquals(4, configurationIdentitiesResponse.getData().size());
    }

    private Object mockGitHubIdentityProviderEntity() {
        GitHubIdentityProviderEntity providerEntity = new GitHubIdentityProviderEntity();
        providerEntity.setClientId(IDP_CLIENT_ID);
        providerEntity.setClientSecret(IDP_CLIENT_SECRET);
        providerEntity.setDescription(IDP_DESCRIPTION);
        providerEntity.setEmailRequired(IDP_EMAIL_REQUIRED);
        providerEntity.setGroupMappings(new ArrayList<>());
        providerEntity.setId(IDP_ID);
        providerEntity.setName(IDP_NAME);
        providerEntity.setRoleMappings(new ArrayList<>());
        return providerEntity;
    }

    private Object mockGoogleIdentityProviderEntity() {
        GoogleIdentityProviderEntity providerEntity = new GoogleIdentityProviderEntity();
        providerEntity.setClientId(IDP_CLIENT_ID);
        providerEntity.setClientSecret(IDP_CLIENT_SECRET);
        providerEntity.setDescription(IDP_DESCRIPTION);
        providerEntity.setEmailRequired(IDP_EMAIL_REQUIRED);
        providerEntity.setGroupMappings(new ArrayList<>());
        providerEntity.setId(IDP_ID);
        providerEntity.setName(IDP_NAME);
        providerEntity.setRoleMappings(new ArrayList<>());
        return providerEntity;
    }

    private Object mockAMIdentityProviderEntity() {
        AMIdentityProviderEntity providerEntity = new AMIdentityProviderEntity(serverUrl);
        providerEntity.setClientId(IDP_CLIENT_ID);
        providerEntity.setClientSecret(IDP_CLIENT_SECRET);
        providerEntity.setColor(IDP_COLOR);
        providerEntity.setDescription(IDP_DESCRIPTION);
        providerEntity.setDiscoveryEndpoint(IDP_DISCOVERY_ENDPOINT);
        providerEntity.setEmailRequired(IDP_EMAIL_REQUIRED);
        providerEntity.setGroupMappings(new ArrayList<>());
        providerEntity.setId(IDP_ID);
        providerEntity.setName(IDP_NAME);
        providerEntity.setRoleMappings(new ArrayList<>());
        providerEntity.setScopes(Arrays.asList(IDP_SCOPE));
        providerEntity.setUserProfileMapping(new HashMap<>());
        return providerEntity;
    }

    private Object mockOIDCIdentityProviderEntity() {
        OIDCIdentityProviderEntity providerEntity = new OIDCIdentityProviderEntity();
        providerEntity.setAuthorizationEndpoint(IDP_AUTHORIZATION_ENDPOINT);
        providerEntity.setClientId(IDP_CLIENT_ID);
        providerEntity.setClientSecret(IDP_CLIENT_SECRET);
        providerEntity.setColor(IDP_COLOR);
        providerEntity.setDescription(IDP_DESCRIPTION);
        providerEntity.setDiscoveryEndpoint(IDP_DISCOVERY_ENDPOINT);
        providerEntity.setEmailRequired(IDP_EMAIL_REQUIRED);
        providerEntity.setGroupMappings(new ArrayList<>());
        providerEntity.setId(IDP_ID);
        providerEntity.setName(IDP_NAME);
        providerEntity.setRoleMappings(new ArrayList<>());
        providerEntity.setScopes(Arrays.asList(IDP_SCOPE));
        providerEntity.setTokenEndpoint(IDP_TOKEN_ENDPOINT);
        providerEntity.setTokenIntrospectionEndpoint(IDP_TOKEN_INTROSPECTION_ENDPOINT);
        providerEntity.setUserInfoEndpoint(IDP_USER_INFO_ENDPOINT);
        providerEntity.setUserLogoutEndpoint(IDP_USER_LOGOUT_ENDPOINT);
        providerEntity.setUserProfileMapping(new HashMap<>());
        return providerEntity;
    }
}

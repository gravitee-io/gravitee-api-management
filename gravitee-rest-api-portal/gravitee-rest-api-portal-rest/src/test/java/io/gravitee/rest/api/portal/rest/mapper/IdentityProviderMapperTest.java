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
package io.gravitee.rest.api.portal.rest.mapper;

import io.gravitee.rest.api.model.configuration.identity.am.AMIdentityProviderEntity;
import io.gravitee.rest.api.model.configuration.identity.github.GitHubIdentityProviderEntity;
import io.gravitee.rest.api.model.configuration.identity.google.GoogleIdentityProviderEntity;
import io.gravitee.rest.api.model.configuration.identity.oidc.OIDCIdentityProviderEntity;
import io.gravitee.rest.api.portal.rest.model.IdentityProvider;
import io.gravitee.rest.api.portal.rest.model.IdentityProviderType;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class IdentityProviderMapperTest {

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
    
    private IdentityProviderMapper identityProviderMapper = new IdentityProviderMapper();
    
    @Test
    public void testGraviteeIoAMProvider() {
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
        
        IdentityProvider idp = identityProviderMapper.convert(providerEntity);
        checkIdp(idp, IdentityProviderType.GRAVITEEIO_AM);
    }

    @Test
    public void testGithubProvider() {
        GitHubIdentityProviderEntity providerEntity = new GitHubIdentityProviderEntity();
        providerEntity.setClientId(IDP_CLIENT_ID);
        providerEntity.setClientSecret(IDP_CLIENT_SECRET);
        providerEntity.setDescription(IDP_DESCRIPTION);
        providerEntity.setEmailRequired(IDP_EMAIL_REQUIRED);
        providerEntity.setGroupMappings(new ArrayList<>());
        providerEntity.setId(IDP_ID);
        providerEntity.setName(IDP_NAME);
        providerEntity.setRoleMappings(new ArrayList<>());
        
        IdentityProvider idp = identityProviderMapper.convert(providerEntity);
        checkIdp(idp, IdentityProviderType.GITHUB);
    }
    
    @Test
    public void testGoogleProvider() {
        GoogleIdentityProviderEntity providerEntity = new GoogleIdentityProviderEntity();
        providerEntity.setClientId(IDP_CLIENT_ID);
        providerEntity.setClientSecret(IDP_CLIENT_SECRET);
        providerEntity.setDescription(IDP_DESCRIPTION);
        providerEntity.setEmailRequired(IDP_EMAIL_REQUIRED);
        providerEntity.setGroupMappings(new ArrayList<>());
        providerEntity.setId(IDP_ID);
        providerEntity.setName(IDP_NAME);
        providerEntity.setRoleMappings(new ArrayList<>());
        
        IdentityProvider idp = identityProviderMapper.convert(providerEntity);
        checkIdp(idp, IdentityProviderType.GOOGLE);
    }
    
    @Test
    public void testOIDCProvider() {
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
        
        IdentityProvider idp = identityProviderMapper.convert(providerEntity);
        checkIdp(idp, IdentityProviderType.OIDC);
    }
    
    private void checkIdp(IdentityProvider idp, IdentityProviderType type) {
        assertEquals(IDP_CLIENT_ID, idp.getClientId());
        assertEquals(IDP_DESCRIPTION, idp.getDescription());
        assertEquals(IDP_EMAIL_REQUIRED, idp.getEmailRequired().booleanValue());
        assertEquals(IDP_ID, idp.getId());
        assertEquals(IDP_NAME, idp.getName());
        assertEquals(type, idp.getType());
        
        switch (type) {
            case GRAVITEEIO_AM:
                assertEquals(serverUrl + "/oauth/authorize", idp.getAuthorizationEndpoint());
                assertEquals(IDP_COLOR, idp.getColor());
                assertNull(idp.getDisplay());
                assertEquals("perm_identity", idp.getIcon());
                assertNull(idp.getOptionalUrlParams());
                assertNull(idp.getRequiredUrlParams());
                assertEquals(Arrays.asList(IDP_SCOPE), idp.getScopes());
                assertEquals(serverUrl + "/oauth/introspect", idp.getTokenIntrospectionEndpoint());
                assertEquals(serverUrl + "/logout?target_url=", idp.getUserLogoutEndpoint());

                break;
                
            case GITHUB:
                assertEquals("https://github.com/login/oauth/authorize", idp.getAuthorizationEndpoint());
                assertNull(idp.getColor());
                assertNull(idp.getDisplay());
                assertEquals("github-circle", idp.getIcon());
                assertEquals(Arrays.asList("scope"), idp.getOptionalUrlParams());
                assertNull(idp.getRequiredUrlParams());
                assertEquals(Arrays.asList("user:email"), idp.getScopes());
                assertNull(idp.getTokenIntrospectionEndpoint());
                assertNull(idp.getUserLogoutEndpoint());

                break;
                
            case GOOGLE:
                assertEquals("https://accounts.google.com/o/oauth2/v2/auth", idp.getAuthorizationEndpoint());
                assertNull(idp.getColor());
                assertEquals("popup", idp.getDisplay());
                assertEquals("google-plus", idp.getIcon());
                assertEquals(Arrays.asList("display", "state"), idp.getOptionalUrlParams());
                assertEquals(Arrays.asList("scope"), idp.getRequiredUrlParams());
                assertEquals(Arrays.asList("profile", "email"), idp.getScopes());
                assertNull(idp.getTokenIntrospectionEndpoint());
                assertNull(idp.getUserLogoutEndpoint());

                break;
                
            case OIDC:
                assertEquals(IDP_AUTHORIZATION_ENDPOINT, idp.getAuthorizationEndpoint());
                assertEquals(IDP_COLOR, idp.getColor());
                assertNull(idp.getDisplay());
                assertEquals("perm_identity", idp.getIcon());
                assertNull(idp.getOptionalUrlParams());
                assertNull(idp.getRequiredUrlParams());
                assertEquals(Arrays.asList(IDP_SCOPE), idp.getScopes());
                assertEquals(IDP_TOKEN_INTROSPECTION_ENDPOINT, idp.getTokenIntrospectionEndpoint());
                assertEquals(IDP_USER_LOGOUT_ENDPOINT, idp.getUserLogoutEndpoint());

                break;
        }
    }
}

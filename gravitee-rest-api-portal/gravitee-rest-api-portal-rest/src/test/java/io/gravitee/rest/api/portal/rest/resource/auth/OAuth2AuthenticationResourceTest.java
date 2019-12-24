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
package io.gravitee.rest.api.portal.rest.resource.auth;

import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.JWTVerifyException;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.configuration.identity.GroupMappingEntity;
import io.gravitee.rest.api.model.configuration.identity.IdentityProviderType;
import io.gravitee.rest.api.model.configuration.identity.RoleMappingEntity;
import io.gravitee.rest.api.model.configuration.identity.SocialIdentityProviderEntity;
import io.gravitee.rest.api.portal.rest.model.Token;
import io.gravitee.rest.api.portal.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.service.exceptions.EmailRequiredException;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;

import javax.ws.rs.core.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static javax.ws.rs.client.Entity.form;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.*;

/**
 * @author Christophe LANNOY (chrislannoy.java at gmail.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class OAuth2AuthenticationResourceTest extends AbstractResourceTest {

    private final static String USER_SOURCE_OAUTH2 = "oauth2";

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort());

    protected String contextPath() {
        return "auth/oauth2/"+USER_SOURCE_OAUTH2;
    }
    private SocialIdentityProviderEntity identityProvider = null;

    @Before
    public void init() {
        identityProvider = new SocialIdentityProviderEntity() {
            @Override
            public String getId() {
                return USER_SOURCE_OAUTH2;
            }

            @Override
            public IdentityProviderType getType() {
                return IdentityProviderType.OIDC;
            }

            @Override
            public String getAuthorizationEndpoint() {
                return null;
            }

            @Override
            public String getTokenEndpoint() {
                return "http://localhost:" + wireMockRule.port() + "/token";
            }

            @Override
            public String getUserInfoEndpoint() {
                return "http://localhost:" + wireMockRule.port() + "/userinfo";
            }

            @Override
            public List<String> getRequiredUrlParams() {
                return null;
            }

            @Override
            public List<String> getOptionalUrlParams() {
                return null;
            }

            @Override
            public List<String> getScopes() {
                return null;
            }

            @Override
            public String getDisplay() {
                return null;
            }

            @Override
            public String getColor() {
                return null;
            }

            @Override
            public String getIcon() {
                return null;
            }

            @Override
            public String getClientSecret() {
                return "the_client_secret";
            }

            private Map<String, String> userProfileMapping =  new HashMap<>();
            @Override
            public Map<String, String> getUserProfileMapping() {
                return userProfileMapping;
            }

            private List<GroupMappingEntity> groupMappings = new ArrayList<>();
            @Override
            public List<GroupMappingEntity> getGroupMappings() {
                return groupMappings;
            }

            private List<RoleMappingEntity> roleMappings = new ArrayList<>();
            @Override
            public List<RoleMappingEntity> getRoleMappings() {
                return roleMappings;
            }

            @Override
            public boolean isEmailRequired() {
                return true;
            }
        };

        when(socialIdentityProviderService.findById(USER_SOURCE_OAUTH2)).thenReturn(identityProvider);
        cleanEnvironment();
        cleanRolesGroupMapping();
        reset(userService, groupService, roleService, membershipService);
    }

    private void cleanEnvironment() {
        identityProvider.getUserProfileMapping().clear();
    }

    private void cleanRolesGroupMapping() {
        identityProvider.getGroupMappings().clear();
        identityProvider.getRoleMappings().clear();
    }

    @Test
    public void shouldConnectUser() throws Exception {
        // -- MOCK
        //mock environment
        mockDefaultEnvironment();

        //mock oauth2 exchange authorisation code for access token
        mockExchangeAuthorizationCodeForAccessToken();

        //mock oauth2 user info call
        mockUserInfo(okJson(IOUtils.toString(read("/oauth2/json/user_info_response_body.json"), Charset.defaultCharset())));

        //mock DB find user by name
        UserEntity userEntity = mockUserEntity();
        userEntity.setId("janedoe@example.com");
        userEntity.setSource(USER_SOURCE_OAUTH2);
        userEntity.setSourceId("janedoe@example.com");
        userEntity.setPicture("http://example.com/janedoe/me.jpg");

        when(userService.createOrUpdateUserFromSocialIdentityProvider(eq(identityProvider), anyString())).thenReturn(userEntity);

        //mock DB user connect
        when(userService.connect(userEntity.getId())).thenReturn(userEntity);


        // -- CALL

        final MultivaluedMap<String, String> payload =
                createPayload("the_client_id","http://localhost/callback","CoDe");

        Response response = target().request().post(form(payload));

        // -- VERIFY
        verify(userService, times(1)).createOrUpdateUserFromSocialIdentityProvider(eq(identityProvider), anyString());
        verify(userService, times(1)).connect(userEntity.getSourceId());

        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        // verify response body
//        verifyUserInResponseBody(response);


        // verify jwt token
        verifyJwtToken(response);
    }

    private void verifyJwtToken(Response response) throws NoSuchAlgorithmException, InvalidKeyException, IOException, SignatureException, JWTVerifyException {
        Token responseToken = response.readEntity(Token.class);
        assertEquals("BEARER", responseToken.getTokenType().name());

        String jwt = responseToken.getToken();

        JWTVerifier jwtVerifier = new JWTVerifier("myJWT4Gr4v1t33_S3cr3t");

        Map<String, Object> mapJwt = jwtVerifier.verify(jwt);

        assertEquals("janedoe@example.com", mapJwt.get("sub"));

        assertEquals("Jane", mapJwt.get("firstname"));
        assertEquals("gravitee-management-auth", mapJwt.get("iss"));
        assertEquals("janedoe@example.com", mapJwt.get("sub"));
        assertEquals("janedoe@example.com", mapJwt.get("email"));
        assertEquals("Doe", mapJwt.get("lastname"));
    }

    private void verifyJwtTokenIsNotPresent(Response response) throws NoSuchAlgorithmException, InvalidKeyException, IOException, SignatureException, JWTVerifyException {
        assertNull(response.getCookies().get(HttpHeaders.AUTHORIZATION));
    }

    private MultivaluedMap<String, String> createPayload(String clientId, String redirectUri, String code) {
        final MultivaluedMap<String, String> payload = new MultivaluedHashMap<>();
        payload.add("client_id", clientId);
        payload.add("redirect_uri", redirectUri);
        payload.add("code", code);
        return payload;
    }

    private UserEntity mockUserEntity() {
        UserEntity createdUser = new UserEntity();
        createdUser.setId("janedoe@example.com");
        createdUser.setSource(USER_SOURCE_OAUTH2);
        createdUser.setSourceId("janedoe@example.com");
        createdUser.setLastname("Doe");
        createdUser.setFirstname("Jane");
        createdUser.setEmail("janedoe@example.com");
        createdUser.setPicture("http://example.com/janedoe/me.jpg");
        return createdUser;
    }

    @Test
    public void shouldNotConnectUserOn401UserInfo() throws Exception {

        // -- MOCK
        //mock environment
        mockDefaultEnvironment();

        //mock oauth2 exchange authorisation code for access token
        mockExchangeAuthorizationCodeForAccessToken();

        //mock oauth2 user info call
        mockUserInfo(WireMock.unauthorized().withBody(IOUtils.toString(read("/oauth2/json/user_info_401_response_body.json"), Charset.defaultCharset())));

        // -- CALL

        final MultivaluedMap<String, String> payload =
                createPayload("the_client_id","http://localhost/callback","CoDe");

        Response response = target().request().post(form(payload));

        // -- VERIFY
        verify(userService, times(0)).createOrUpdateUserFromSocialIdentityProvider(eq(identityProvider), anyString());
        verify(userService, times(0)).connect(anyString());

        assertEquals(HttpStatusCode.UNAUTHORIZED_401, response.getStatus());

        // verify jwt token not present

        assertFalse(response.getCookies().containsKey(HttpHeaders.AUTHORIZATION));

    }


    @Test
    public void shouldNotConnectUserWhenMissingMailInUserInfo() throws Exception {

        // -- MOCK
        //mock environment
        mockWrongEnvironment();

        //mock oauth2 exchange authorisation code for access token
        mockExchangeAuthorizationCodeForAccessToken();

        //mock oauth2 user info call
        mockUserInfo(okJson(IOUtils.toString(read("/oauth2/json/user_info_response_body.json"), Charset.defaultCharset())));

        when(userService.createOrUpdateUserFromSocialIdentityProvider(refEq(identityProvider),anyString())).thenThrow(new EmailRequiredException(USER_NAME));
        // -- CALL

        final MultivaluedMap<String, String> payload =
                createPayload("the_client_id","http://localhost/callback","CoDe");

        Response response = target().request().post(form(payload));

        // -- VERIFY
        verify(userService, times(1)).createOrUpdateUserFromSocialIdentityProvider(eq(identityProvider), anyString());
        verify(userService, times(0)).connect(anyString());

        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());

        // verify jwt token not present
        assertFalse(response.getCookies().containsKey(HttpHeaders.AUTHORIZATION));
    }

    private void mockUserInfo(ResponseDefinitionBuilder responseDefinitionBuilder) throws IOException {
        stubFor(
                get("/userinfo")
                        .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON_TYPE.toString()))
                        .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer 2YotnFZFEjr1zCsicMWpAA"))
                        .willReturn(responseDefinitionBuilder));
    }

    private void mockExchangeAuthorizationCodeForAccessToken() throws IOException {
        String tokenRequestBody = ""
                + "code=CoDe&"
                + "grant_type=&"
                + "redirect_uri=http%3A%2F%2Flocalhost%2Fcallback&"
                + "client_secret=the_client_secret&"
                + "client_id=the_client_id&"
                + "code_verifier=";

        stubFor(
                post("/token")
                        .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON_TYPE.toString()))
                        .withRequestBody(equalTo(tokenRequestBody))
                    .willReturn(okJson(IOUtils.toString(read("/oauth2/json/token_response_body.json"), Charset.defaultCharset()))));
    }

    private void mockDefaultEnvironment() {
        identityProvider.getUserProfileMapping().put(SocialIdentityProviderEntity.UserProfile.ID, "email");
        identityProvider.getUserProfileMapping().put(SocialIdentityProviderEntity.UserProfile.SUB, "sub");
        identityProvider.getUserProfileMapping().put(SocialIdentityProviderEntity.UserProfile.FIRSTNAME, "given_name");
        identityProvider.getUserProfileMapping().put(SocialIdentityProviderEntity.UserProfile.LASTNAME, "family_name");
        identityProvider.getUserProfileMapping().put(SocialIdentityProviderEntity.UserProfile.EMAIL, "email");
        identityProvider.getUserProfileMapping().put(SocialIdentityProviderEntity.UserProfile.PICTURE, "picture");
    }

    private void mockWrongEnvironment() {
        mockDefaultEnvironment();
        identityProvider.getUserProfileMapping().put(SocialIdentityProviderEntity.UserProfile.EMAIL, "theEmail");
        identityProvider.getUserProfileMapping().put(SocialIdentityProviderEntity.UserProfile.ID, "theEmail");
    }

    private InputStream read(String resource) throws IOException {
        return this.getClass().getResourceAsStream(resource);
    }

    @Test
    public void shouldNotConnectNewUserWhenWrongELGroupsMapping() throws Exception {

        // -- MOCK
        //mock environment
        mockDefaultEnvironment();

        //mock oauth2 exchange authorisation code for access token
        mockExchangeAuthorizationCodeForAccessToken();

        //mock oauth2 user info call
        mockUserInfo(okJson(IOUtils.toString(read("/oauth2/json/user_info_response_body.json"), Charset.defaultCharset())));

        //mock DB find user by name
        when(userService.createOrUpdateUserFromSocialIdentityProvider(eq(identityProvider), anyString())).thenThrow(new SpelEvaluationException(SpelMessage.TYPE_CONVERSION_ERROR, "cannot convert from java.lang.String to boolean"));

        // -- CALL

        final MultivaluedMap<String, String> payload =
                createPayload("the_client_id","http://localhost/callback","CoDe");

        Response response = target().request().post(form(payload));

        // -- VERIFY
        verify(userService, times(1)).createOrUpdateUserFromSocialIdentityProvider(eq(identityProvider), anyString());
        verify(userService, times(0)).connect(anyString());

        assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR_500, response.getStatus());

        // verify jwt token
        verifyJwtTokenIsNotPresent(response);
    }

}

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
package io.gravitee.rest.api.management.rest.resource.auth;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static jakarta.ws.rs.client.Entity.form;
import static jakarta.ws.rs.client.Entity.json;
import static jakarta.ws.rs.client.Entity.text;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.el.exceptions.ExpressionEvaluationException;
import io.gravitee.rest.api.management.rest.model.ExchangePayloadEntity;
import io.gravitee.rest.api.management.rest.model.TokenEntity;
import io.gravitee.rest.api.management.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.configuration.identity.ClientAuthenticationMethod;
import io.gravitee.rest.api.model.configuration.identity.GroupMappingEntity;
import io.gravitee.rest.api.model.configuration.identity.IdentityProviderType;
import io.gravitee.rest.api.model.configuration.identity.RoleMappingEntity;
import io.gravitee.rest.api.model.configuration.identity.SocialIdentityProviderEntity;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.EmailRequiredException;
import io.gravitee.rest.api.service.exceptions.UserNotFoundException;
import io.vertx.core.json.JsonObject;
import jakarta.ws.rs.core.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.*;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Christophe LANNOY (chrislannoy.java at gmail.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class OAuth2AuthenticationResourceTest extends AbstractResourceTest {

    private static final String ORGANIZATION_ID = "organization-id";
    private static final String USER_SOURCE_OAUTH2 = "oauth2";

    private WireMockServer wireMockServer;

    private SocialIdentityProviderEntity identityProvider = null;

    @Override
    protected String contextPath() {
        return "auth/oauth2/" + USER_SOURCE_OAUTH2;
    }

    @BeforeEach
    public void init() {
        wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());

        identityProvider = new SocialIdentityProviderEntity() {
            private Map<String, String> userProfileMapping = new HashMap<>();
            private List<GroupMappingEntity> groupMappings = new ArrayList<>();
            private List<RoleMappingEntity> roleMappings = new ArrayList<>();

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
                return "http://localhost:" + wireMockServer.port() + "/token";
            }

            @Override
            public String getUserInfoEndpoint() {
                return "http://localhost:" + wireMockServer.port() + "/userinfo";
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
            public String getClientSecret() {
                return "the_client_secret";
            }

            @Override
            public Map<String, String> getUserProfileMapping() {
                return userProfileMapping;
            }

            @Override
            public List<GroupMappingEntity> getGroupMappings() {
                return groupMappings;
            }

            @Override
            public List<RoleMappingEntity> getRoleMappings() {
                return roleMappings;
            }

            @Override
            public boolean isEmailRequired() {
                return true;
            }

            @Override
            public String getTokenIntrospectionEndpoint() {
                return "http://localhost:" + wireMockServer.port() + "/introspect";
            }

            @Override
            public String getClientId() {
                return "the_client_id";
            }
        };

        when(socialIdentityProviderService.findById(eq(USER_SOURCE_OAUTH2), any())).thenReturn(identityProvider);
        cleanEnvironment();
        cleanRolesGroupMapping();
        reset(userService, groupService, roleService, membershipService);
    }

    @AfterEach
    public void cleanUp() {
        wireMockServer.stop();
    }

    private void cleanEnvironment() {
        identityProvider.getUserProfileMapping().clear();
    }

    private void cleanRolesGroupMapping() {
        identityProvider.getGroupMappings().clear();
        identityProvider.getRoleMappings().clear();
    }

    @Test
    public void shouldConnectExistingUser() throws Exception {
        // -- MOCK
        //mock environment
        mockDefaultEnvironment();

        //mock oauth2 exchange authorisation code for access token
        mockExchangeAuthorizationCodeForAccessToken();

        //mock oauth2 user info call
        mockUserInfo(okJson(IOUtils.toString(read("/oauth2/json/user_info_response_body.json"), Charset.defaultCharset())));

        //mock DB find user by name
        UserEntity userEntity = mockUserEntity();

        when(userService.createOrUpdateUserFromSocialIdentityProvider(any(), eq(identityProvider), any(), any(), any())).thenReturn(
            userEntity
        );

        //mock DB user connect
        when(userService.connect(any(), eq(userEntity.getId()))).thenReturn(userEntity);

        // -- CALL

        final MultivaluedMap<String, String> payload = createPayload("the_client_id", "http://localhost/callback", "CoDe", "StAtE");

        Response response = orgTarget().request().post(form(payload));

        // -- VERIFY
        verify(userService, times(1)).connect(any(), eq(userEntity.getSourceId()));

        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        // verify jwt token
        verifyJwtToken(response);
    }

    @Test
    public void should_exchange_token_by_query_param()
        throws IOException, NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        final String tokenToExchange = "MyToken";
        // Given
        //mock environment
        mockDefaultEnvironment();
        // mock introspect token
        mockIntrospectToken();
        //mock oauth2 user info call
        mockUserInfo(
            okJson(IOUtils.toString(read("/oauth2/json/user_info_response_body.json"), Charset.defaultCharset())),
            tokenToExchange
        );
        //mock DB find user by name
        UserEntity userEntity = mockUserEntity();
        //mock DB user connect
        when(userService.createOrUpdateUserFromSocialIdentityProvider(any(), eq(identityProvider), any(), any(), any())).thenReturn(
            userEntity
        );
        when(userService.connect(any(), eq("janedoe@example.com"))).thenReturn(userEntity);

        // When
        Response response = orgTarget().path("exchange").queryParam("token", tokenToExchange).request().post(json(null));

        // Then
        verify(userService, times(1)).connect(any(), eq(userEntity.getSourceId()));
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        verifyJwtToken(response);
    }

    @Test
    public void should_exchange_token_by_body() throws IOException, NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        final String tokenToExchange = "MyToken";
        // Given
        //mock environment
        mockDefaultEnvironment();
        // mock introspect token
        mockIntrospectToken();
        //mock oauth2 user info call
        mockUserInfo(
            okJson(IOUtils.toString(read("/oauth2/json/user_info_response_body.json"), Charset.defaultCharset())),
            tokenToExchange
        );
        //mock DB find user by name
        UserEntity userEntity = mockUserEntity();
        //mock DB user connect
        when(userService.createOrUpdateUserFromSocialIdentityProvider(any(), eq(identityProvider), any(), any(), any())).thenReturn(
            userEntity
        );
        when(userService.connect(any(), eq("janedoe@example.com"))).thenReturn(userEntity);

        // When
        ExchangePayloadEntity exchangePayloadEntity = new ExchangePayloadEntity();
        exchangePayloadEntity.setToken(tokenToExchange);
        Response response = orgTarget().path("exchange").request().post(json(exchangePayloadEntity));

        // Then
        verify(userService, times(1)).connect(any(), eq(userEntity.getSourceId()));
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        verifyJwtToken(response);
    }

    @Test
    public void should_send_client_credentials_in_token_request_body_when_no_auth_method_configured() throws Exception {
        // Given
        mockDefaultEnvironment();
        mockTokenEndpoint();
        mockUserInfo(okJson(IOUtils.toString(read("/oauth2/json/user_info_response_body.json"), Charset.defaultCharset())));
        mockConnectedUser();

        // When
        Response response = orgTarget().request().post(form(createPayload("the_client_id", "http://localhost/callback", "CoDe", "StAtE")));

        // Then
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        WireMock.verify(
            postRequestedFor(urlEqualTo("/token"))
                .withHeader(HttpHeaders.AUTHORIZATION, absent())
                .withRequestBody(containing("client_secret=the_client_secret"))
        );
    }

    @Test
    public void should_send_client_credentials_as_basic_auth_in_token_request_when_configured() throws Exception {
        // Given
        mockDefaultEnvironment();
        identityProvider.setTokenEndpointAuthMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC);
        mockTokenEndpoint();
        mockUserInfo(okJson(IOUtils.toString(read("/oauth2/json/user_info_response_body.json"), Charset.defaultCharset())));
        mockConnectedUser();

        // When
        Response response = orgTarget().request().post(form(createPayload("the_client_id", "http://localhost/callback", "CoDe", "StAtE")));

        // Then
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        WireMock.verify(
            postRequestedFor(urlEqualTo("/token"))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo(expectedBasicAuthorization()))
                .withRequestBody(notContaining("client_secret"))
        );
    }

    @Test
    public void should_authenticate_introspection_with_basic_auth_when_no_auth_method_configured() throws Exception {
        // Given
        mockDefaultEnvironment();
        mockIntrospectionEndpoint();
        mockUserInfo(okJson(IOUtils.toString(read("/oauth2/json/user_info_response_body.json"), Charset.defaultCharset())), "MyToken");
        mockConnectedUser();

        // When
        Response response = orgTarget().path("exchange").queryParam("token", "MyToken").request().post(json(null));

        // Then
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        WireMock.verify(
            postRequestedFor(urlEqualTo("/introspect"))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo(expectedBasicAuthorization()))
                .withRequestBody(notContaining("client_secret"))
        );
    }

    @Test
    public void should_send_client_credentials_in_introspection_body_when_configured() throws Exception {
        // Given
        mockDefaultEnvironment();
        identityProvider.setTokenEndpointAuthMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST);
        mockIntrospectionEndpoint();
        mockUserInfo(okJson(IOUtils.toString(read("/oauth2/json/user_info_response_body.json"), Charset.defaultCharset())), "MyToken");
        mockConnectedUser();

        // When
        Response response = orgTarget().path("exchange").queryParam("token", "MyToken").request().post(json(null));

        // Then
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        WireMock.verify(
            postRequestedFor(urlEqualTo("/introspect"))
                .withHeader(HttpHeaders.AUTHORIZATION, absent())
                .withRequestBody(containing("client_secret=the_client_secret"))
                .withRequestBody(containing("client_id=the_client_id"))
        );
    }

    /** Carries detail a caller must never see, so relaying the provider's raw body fails these tests. */
    private static final String PROVIDER_REJECTION_BODY =
        "{\"error\":\"invalid_client\",\"error_description\":\"client not found in realm internal-realm-7\",\"trace\":\"provider-node-3\"}";

    private void assertRelaysOnlyTheErrorCode(String body) {
        assertTrue(body.contains("invalid_client"), "the provider's own error code must reach the caller, got: " + body);
        assertFalse(body.contains("internal-realm-7"), "the provider's response detail must not be relayed, got: " + body);
        assertFalse(body.contains("provider-node-3"), "the provider's response detail must not be relayed, got: " + body);
        assertFalse(body.contains("error_description"), "the provider's response detail must not be relayed, got: " + body);
    }

    @Test
    public void should_report_invalid_client_on_token_request_instead_of_an_empty_unauthorized() throws Exception {
        // Given
        mockDefaultEnvironment();
        stubFor(post("/token").willReturn(aResponse().withStatus(HttpStatusCode.UNAUTHORIZED_401).withBody(PROVIDER_REJECTION_BODY)));

        // When
        Response response = orgTarget().request().post(form(createPayload("the_client_id", "http://localhost/callback", "CoDe", "StAtE")));

        // Then
        assertEquals(HttpStatusCode.UNAUTHORIZED_401, response.getStatus());
        String body = response.readEntity(String.class);
        assertRelaysOnlyTheErrorCode(body);
        assertTrue(
            body.contains("tokenEndpointAuthMethod"),
            "an invalid_client failure must name the client authentication method as the likely cause, got: " + body
        );
    }

    @Test
    public void should_report_invalid_client_on_introspection_instead_of_a_server_error() throws Exception {
        // Given
        mockDefaultEnvironment();
        stubFor(post("/introspect").willReturn(aResponse().withStatus(HttpStatusCode.UNAUTHORIZED_401).withBody(PROVIDER_REJECTION_BODY)));

        // When
        Response response = orgTarget().path("exchange").queryParam("token", "MyToken").request().post(json(null));

        // Then
        assertEquals(HttpStatusCode.UNAUTHORIZED_401, response.getStatus());
        String body = response.readEntity(String.class);
        assertRelaysOnlyTheErrorCode(body);
        assertTrue(
            body.contains("tokenEndpointAuthMethod"),
            "an invalid_client failure must name the client authentication method as the likely cause, got: " + body
        );
    }

    @Test
    public void should_not_suggest_the_client_authentication_method_for_an_unrelated_error() throws Exception {
        // Given
        mockDefaultEnvironment();
        stubFor(
            post("/token").willReturn(aResponse().withStatus(HttpStatusCode.BAD_REQUEST_400).withBody("{\"error\":\"invalid_grant\"}"))
        );

        // When
        Response response = orgTarget().request().post(form(createPayload("the_client_id", "http://localhost/callback", "CoDe", "StAtE")));

        // Then
        String body = response.readEntity(String.class);
        assertTrue(body.contains("invalid_grant"), "the provider's own error code must reach the caller, got: " + body);
        assertFalse(
            body.contains("tokenEndpointAuthMethod"),
            "the client authentication hint must be reserved for credential rejections, got: " + body
        );
    }

    @Test
    public void should_report_a_non_json_provider_response_without_relaying_it() throws Exception {
        // Given
        mockDefaultEnvironment();
        stubFor(
            post("/token").willReturn(
                aResponse().withStatus(HttpStatusCode.BAD_GATEWAY_502).withBody("<html><body>nginx: upstream 10.0.0.7 down</body></html>")
            )
        );

        // When
        Response response = orgTarget().request().post(form(createPayload("the_client_id", "http://localhost/callback", "CoDe", "StAtE")));

        // Then
        String body = response.readEntity(String.class);
        // A provider that is unreachable must not be reported as a credential problem
        assertTrue(body.contains("identity_provider_unavailable"), "an unreachable provider must be named as such, got: " + body);
        assertFalse(body.contains("10.0.0.7"), "the provider's response body must not be relayed, got: " + body);
        assertFalse(body.contains("tokenEndpointAuthMethod"), "an outage must not be blamed on the auth method, got: " + body);
    }

    private void mockTokenEndpoint() throws IOException {
        stubFor(
            post("/token").willReturn(okJson(IOUtils.toString(read("/oauth2/json/token_response_body.json"), Charset.defaultCharset())))
        );
    }

    private void mockIntrospectionEndpoint() {
        stubFor(post("/introspect").willReturn(okJson(new JsonObject().put("active", "true").toString())));
    }

    private void mockConnectedUser() {
        UserEntity userEntity = mockUserEntity();
        when(userService.createOrUpdateUserFromSocialIdentityProvider(any(), eq(identityProvider), any(), any(), any())).thenReturn(
            userEntity
        );
        when(userService.connect(any(), eq(userEntity.getId()))).thenReturn(userEntity);
    }

    private static String expectedBasicAuthorization() {
        return "Basic " + Base64.getEncoder().encodeToString("the_client_id:the_client_secret".getBytes(StandardCharsets.UTF_8));
    }

    private static void mockIntrospectToken() {
        String tokenRequestBody = "token=MyToken";
        JsonObject tokenResponseBody = new JsonObject().put("active", "true");
        stubFor(
            post("/introspect")
                .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON_TYPE.toString()))
                .withHeader(HttpHeaders.AUTHORIZATION, containing("Basic"))
                .withRequestBody(equalTo(tokenRequestBody))
                .willReturn(okJson(tokenResponseBody.toString()))
        );
    }

    private void verifyJwtToken(Response response)
        throws NoSuchAlgorithmException, InvalidKeyException, IOException, SignatureException, JWTVerificationException {
        TokenEntity responseToken = response.readEntity(TokenEntity.class);
        assertEquals("BEARER", responseToken.getType().name());

        String token = responseToken.getToken();

        Algorithm algorithm = Algorithm.HMAC256("myJWT4Gr4v1t33_S3cr3t");
        JWTVerifier jwtVerifier = JWT.require(algorithm).build();

        DecodedJWT jwt = jwtVerifier.verify(token);

        assertEquals(jwt.getSubject(), "janedoe@example.com");

        assertEquals(jwt.getClaim("firstname").asString(), "Jane");
        assertEquals(jwt.getClaim("iss").asString(), "gravitee-management-auth");
        assertEquals(jwt.getClaim("sub").asString(), "janedoe@example.com");
        assertEquals(jwt.getClaim("email").asString(), "janedoe@example.com");
        assertEquals(jwt.getClaim("lastname").asString(), "Doe");
        assertEquals(jwt.getClaim("org").asString(), "my-org");
    }

    private void verifyJwtTokenIsNotPresent(Response response)
        throws NoSuchAlgorithmException, InvalidKeyException, IOException, SignatureException, JWTVerificationException {
        assertNull(response.getCookies().get(HttpHeaders.AUTHORIZATION));
    }

    private MultivaluedMap<String, String> createPayload(String clientId, String redirectUri, String code, String state) {
        final MultivaluedMap<String, String> payload = new MultivaluedHashMap<>();
        payload.add("client_id", clientId);
        payload.add("redirect_uri", redirectUri);
        payload.add("code", code);
        return payload;
    }

    @Test
    public void shouldConnectNewUser() throws Exception {
        // -- MOCK
        //mock environment
        mockDefaultEnvironment();

        //mock oauth2 exchange authorisation code for access token
        mockExchangeAuthorizationCodeForAccessToken();

        //mock oauth2 user info call
        final String userInfo = IOUtils.toString(read("/oauth2/json/user_info_response_body.json"), Charset.defaultCharset());
        mockUserInfo(okJson(userInfo));

        //mock create user
        UserEntity createdUser = mockUserEntity();
        mockUserCreation(identityProvider, userInfo, createdUser);

        //mock DB user connect
        when(userService.createOrUpdateUserFromSocialIdentityProvider(any(), eq(identityProvider), any(), any(), any())).thenReturn(
            createdUser
        );
        when(userService.connect(any(), eq("janedoe@example.com"))).thenReturn(createdUser);

        // -- CALL

        final MultivaluedMap<String, String> payload = createPayload("the_client_id", "http://localhost/callback", "CoDe", "StAtE");

        Response response = orgTarget().request().post(form(payload));

        // -- VERIFY
        verify(userService, times(1)).createOrUpdateUserFromSocialIdentityProvider(any(), any(), any(), any(), any());
        verify(userService, times(1)).connect(any(), eq("janedoe@example.com"));

        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        // verify jwt token
        verifyJwtToken(response);
    }

    private UpdateUserEntity mockUpdateUserPicture(UserEntity user) {
        UpdateUserEntity updateUserEntity = new UpdateUserEntity();
        updateUserEntity.setPicture("http://example.com/janedoe/me.jpg");
        updateUserEntity.setFirstname("Jane");
        updateUserEntity.setLastname("Doe");
        user.setPicture("http://example.com/janedoe/me.jpg");

        when(userService.update(eq(GraviteeContext.getExecutionContext()), eq(user.getId()), refEq(updateUserEntity))).thenReturn(user);
        return updateUserEntity;
    }

    private void mockUserCreation(SocialIdentityProviderEntity socialIdentityProviderEntity, String userInfo, UserEntity createdUser) {
        when(
            userService.createOrUpdateUserFromSocialIdentityProvider(any(), refEq(socialIdentityProviderEntity), eq(userInfo), any(), any())
        ).thenReturn(createdUser);
    }

    private UserEntity mockUserEntity() {
        UserEntity createdUser = new UserEntity();
        createdUser.setId("janedoe@example.com");
        createdUser.setOrganizationId("my-org");
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
        mockUserInfo(
            WireMock.unauthorized().withBody(
                IOUtils.toString(read("/oauth2/json/user_info_401_response_body.json"), Charset.defaultCharset())
            )
        );

        // -- CALL

        final MultivaluedMap<String, String> payload = createPayload("the_client_id", "http://localhost/callback", "CoDe", "StAtE");

        Response response = orgTarget().request().post(form(payload));

        // -- VERIFY
        verify(userService, times(0)).createOrUpdateUserFromSocialIdentityProvider(any(), any(), any(), any(), any());
        verify(userService, times(0)).connect(any(), anyString());

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

        // mock processUser to throw EmailRequiredException
        when(userService.createOrUpdateUserFromSocialIdentityProvider(any(), any(), any(), any(), any())).thenThrow(
            new EmailRequiredException("email")
        );
        // -- CALL

        final MultivaluedMap<String, String> payload = createPayload("the_client_id", "http://localhost/callback", "CoDe", "StAtE");

        Response response = orgTarget().request().post(form(payload));

        // -- VERIFY
        verify(userService, times(1)).createOrUpdateUserFromSocialIdentityProvider(any(), any(), any(), any(), any());

        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
        verify(userService, times(0)).connect(any(), anyString());

        // verify jwt token not present
        assertFalse(response.getCookies().containsKey(HttpHeaders.AUTHORIZATION));
    }

    private void mockUserInfo(ResponseDefinitionBuilder responseDefinitionBuilder) throws IOException {
        mockUserInfo(responseDefinitionBuilder, "2YotnFZFEjr1zCsicMWpAA");
    }

    private void mockUserInfo(ResponseDefinitionBuilder responseDefinitionBuilder, String expectedBearer) throws IOException {
        stubFor(
            get("/userinfo")
                .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON_TYPE.toString()))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer " + expectedBearer))
                .willReturn(responseDefinitionBuilder)
        );
    }

    private void mockExchangeAuthorizationCodeForAccessToken() throws IOException {
        String tokenRequestBody =
            "code=CoDe&" +
            "grant_type=authorization_code&" +
            "redirect_uri=http%3A%2F%2Flocalhost%2Fcallback&" +
            "client_secret=the_client_secret&" +
            "client_id=the_client_id&" +
            "code_verifier=";

        stubFor(
            post("/token")
                .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON_TYPE.toString()))
                .withRequestBody(equalTo(tokenRequestBody))
                .willReturn(okJson(IOUtils.toString(read("/oauth2/json/token_response_body.json"), Charset.defaultCharset())))
        );
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
    public void shouldConnectNewUserWithGroupsMappingFromUserInfo() throws Exception {
        // -- MOCK
        //mock environment
        mockDefaultEnvironment();
        mockGroupsMapping();
        mockRolesMapping();

        //mock oauth2 exchange authorisation code for access token
        mockExchangeAuthorizationCodeForAccessToken();

        //mock oauth2 user info call
        final String userInfoBody = IOUtils.toString(read("/oauth2/json/user_info_response_body.json"), Charset.defaultCharset());
        mockUserInfo(okJson(userInfoBody));

        //mock DB find user by name
        when(userService.findBySource(ORGANIZATION_ID, USER_SOURCE_OAUTH2, "janedoe@example.com", false)).thenThrow(
            new UserNotFoundException("janedoe@example.com")
        );

        //mock create user
        UserEntity createdUser = mockUserEntity();
        mockUserCreation(identityProvider, userInfoBody, createdUser);

        //mock group search and association
        when(groupService.findById(GraviteeContext.getExecutionContext(), "Example group")).thenReturn(
            mockGroupEntity("group_id_1", "Example group")
        );
        when(groupService.findById(GraviteeContext.getExecutionContext(), "soft user")).thenReturn(
            mockGroupEntity("group_id_2", "soft user")
        );
        when(groupService.findById(GraviteeContext.getExecutionContext(), "Others")).thenReturn(mockGroupEntity("group_id_3", "Others"));
        when(groupService.findById(GraviteeContext.getExecutionContext(), "Api consumer")).thenReturn(
            mockGroupEntity("group_id_4", "Api consumer")
        );

        RoleEntity roleApiUser = mockRoleEntity(io.gravitee.rest.api.model.permissions.RoleScope.API, "USER");
        RoleEntity roleApplicationAdmin = mockRoleEntity(io.gravitee.rest.api.model.permissions.RoleScope.APPLICATION, "ADMIN");

        when(roleService.findDefaultRoleByScopes(ORGANIZATION_ID, RoleScope.API, RoleScope.APPLICATION)).thenReturn(
            Arrays.asList(roleApiUser, roleApplicationAdmin)
        );

        //mock DB update user picture
        UpdateUserEntity updateUserEntity = mockUpdateUserPicture(createdUser);

        //mock DB user connect
        when(userService.connect(any(), eq("janedoe@example.com"))).thenReturn(createdUser);

        // -- CALL

        final MultivaluedMap<String, String> payload = createPayload("the_client_id", "http://localhost/callback", "CoDe", "StAtE");

        Response response = orgTarget().request().post(form(payload));

        // -- VERIFY
        verify(userService, times(1)).createOrUpdateUserFromSocialIdentityProvider(any(), refEq(identityProvider), any(), any(), any());

        verify(userService, times(1)).connect(any(), eq("janedoe@example.com"));

        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        // verify response body
        //        verifyUserInResponseBody(response);

        // verify jwt token
        verifyJwtToken(response);
    }

    @Test
    public void shouldConnectNewUserWithNoMatchingGroupsMappingFromUserInfo() throws Exception {
        // -- MOCK
        //mock environment
        mockDefaultEnvironment();
        mockGroupsMapping();

        //mock oauth2 exchange authorisation code for access token
        mockExchangeAuthorizationCodeForAccessToken();

        //mock oauth2 user info call
        final String userInfo = IOUtils.toString(read("/oauth2/json/user_info_response_body_no_matching.json"), Charset.defaultCharset());
        mockUserInfo(okJson(userInfo));

        //mock DB find user by name
        when(userService.findBySource(ORGANIZATION_ID, USER_SOURCE_OAUTH2, "janedoe@example.com", false)).thenThrow(
            new UserNotFoundException("janedoe@example.com")
        );

        //mock create user
        UserEntity createdUser = mockUserEntity();
        mockUserCreation(identityProvider, userInfo, createdUser);

        //mock DB update user picture
        UpdateUserEntity updateUserEntity = mockUpdateUserPicture(createdUser);

        //mock DB user connect
        when(userService.connect(any(), eq("janedoe@example.com"))).thenReturn(createdUser);

        // -- CALL

        final MultivaluedMap<String, String> payload = createPayload("the_client_id", "http://localhost/callback", "CoDe", "StAtE");

        Response response = orgTarget().request().post(form(payload));

        // -- VERIFY
        verify(userService, times(1)).createOrUpdateUserFromSocialIdentityProvider(any(), refEq(identityProvider), any(), any(), any());

        verify(userService, times(1)).connect(any(), eq("janedoe@example.com"));

        //verify group creations
        verify(membershipService, times(0)).addRoleToMemberOnReference(
            eq(GraviteeContext.getExecutionContext()),
            any(MembershipService.MembershipReference.class),
            any(MembershipService.MembershipMember.class),
            any(MembershipService.MembershipRole.class)
        );

        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        // verify response body
        //        verifyUserInResponseBody(response);

        // verify jwt token
        verifyJwtToken(response);
    }

    @Test
    public void shouldConnectNewUserWithGroupsMappingFromUserInfoWhenGroupIsNotFound() throws Exception {
        // -- MOCK
        //mock environment
        mockDefaultEnvironment();
        mockGroupsMapping();

        //mock oauth2 exchange authorisation code for access token
        mockExchangeAuthorizationCodeForAccessToken();

        //mock oauth2 user info call
        final String userInfo = IOUtils.toString(read("/oauth2/json/user_info_response_body.json"), Charset.defaultCharset());
        mockUserInfo(okJson(userInfo));

        //mock DB find user by name
        when(userService.findBySource(ORGANIZATION_ID, USER_SOURCE_OAUTH2, "janedoe@example.com", false)).thenThrow(
            new UserNotFoundException("janedoe@example.com")
        );

        //mock group search and association
        when(groupService.findByName(GraviteeContext.getCurrentEnvironment(), "Example group")).thenReturn(Collections.emptyList());
        when(groupService.findByName(GraviteeContext.getCurrentEnvironment(), "soft user")).thenReturn(Collections.emptyList());
        when(groupService.findByName(GraviteeContext.getCurrentEnvironment(), "Others")).thenReturn(Collections.emptyList());
        when(groupService.findByName(GraviteeContext.getCurrentEnvironment(), "Api consumer")).thenReturn(Collections.emptyList());

        UserEntity createdUser = mockUserEntity();
        mockUserCreation(identityProvider, userInfo, createdUser);

        //mock DB user connect
        when(userService.connect(any(), eq(createdUser.getId()))).thenReturn(createdUser);

        // -- CALL

        final MultivaluedMap<String, String> payload = createPayload("the_client_id", "http://localhost/callback", "CoDe", "StAtE");

        Response response = orgTarget().request().post(form(payload));

        // -- VERIFY
        verify(userService, times(1)).createOrUpdateUserFromSocialIdentityProvider(any(), refEq(identityProvider), any(), any(), any());

        verify(userService, times(0)).update(eq(GraviteeContext.getExecutionContext()), any(String.class), any(UpdateUserEntity.class));
        verify(userService, times(1)).connect(any(), anyString());

        //verify group creations
        verify(membershipService, times(0)).addRoleToMemberOnReference(
            eq(GraviteeContext.getExecutionContext()),
            any(MembershipService.MembershipReference.class),
            any(MembershipService.MembershipMember.class),
            any(MembershipService.MembershipRole.class)
        );

        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        // verify jwt token
        verifyJwtToken(response);
    }

    @Test
    public void shouldNotConnectNewUserWhenWrongELGroupsMapping() throws Exception {
        // -- MOCK
        //mock environment
        mockDefaultEnvironment();
        mockWrongELGroupsMapping();

        //mock oauth2 exchange authorisation code for access token
        mockExchangeAuthorizationCodeForAccessToken();

        //mock oauth2 user info call
        mockUserInfo(okJson(IOUtils.toString(read("/oauth2/json/user_info_response_body.json"), Charset.defaultCharset())));

        when(userService.createOrUpdateUserFromSocialIdentityProvider(any(), any(), any(), any(), any())).thenThrow(
            new ExpressionEvaluationException("error")
        );

        // -- CALL

        final MultivaluedMap<String, String> payload = createPayload("the_client_id", "http://localhost/callback", "CoDe", "StAtE");

        Response response = orgTarget().request().post(form(payload));

        // -- VERIFY
        verify(userService, times(1)).createOrUpdateUserFromSocialIdentityProvider(any(), any(), any(), any(), any());
        verify(userService, times(0)).connect(any(), anyString());

        assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR_500, response.getStatus());

        // verify jwt token
        verifyJwtTokenIsNotPresent(response);
    }

    private RoleEntity mockRoleEntity(io.gravitee.rest.api.model.permissions.RoleScope scope, String name) {
        RoleEntity role = new RoleEntity();
        role.setId(scope.name() + ":" + name);
        role.setScope(scope);
        role.setName(name);
        return role;
    }

    private GroupEntity mockGroupEntity(String id, String name) {
        GroupEntity groupEntity = new GroupEntity();
        groupEntity.setId(id);
        groupEntity.setName(name);
        return groupEntity;
    }

    private MemberEntity mockMemberEntity() {
        return mock(MemberEntity.class);
    }

    private void mockGroupsMapping() {
        GroupMappingEntity condition1 = new GroupMappingEntity();
        condition1.setCondition(
            "{#jsonPath(#profile, '$.identity_provider_id') == 'idp_5' && #jsonPath(#profile, '$.job_id') != 'API_BREAKER'}"
        );
        condition1.setGroups(Arrays.asList("Example group", "soft user"));
        identityProvider.getGroupMappings().add(condition1);

        GroupMappingEntity condition2 = new GroupMappingEntity();
        condition2.setCondition("{#jsonPath(#profile, '$.identity_provider_id') == 'idp_6'}");
        condition2.setGroups(Collections.singletonList("Others"));
        identityProvider.getGroupMappings().add(condition2);

        GroupMappingEntity condition3 = new GroupMappingEntity();
        condition3.setCondition("{#jsonPath(#profile, '$.job_id') != 'API_BREAKER'}");
        condition3.setGroups(Collections.singletonList("Api consumer"));
        identityProvider.getGroupMappings().add(condition3);
    }

    private void mockRolesMapping() {
        RoleMappingEntity role1 = new RoleMappingEntity();
        role1.setCondition(
            "{#jsonPath(#profile, '$.identity_provider_id') == 'idp_5' && #jsonPath(#profile, '$.job_id') != 'API_BREAKER'}"
        );
        role1.setOrganizations(Collections.singletonList("USER"));
        identityProvider.getRoleMappings().add(role1);

        RoleMappingEntity role2 = new RoleMappingEntity();
        role2.setCondition("{#jsonPath(#profile, '$.identity_provider_id') == 'idp_6'}");
        role2.setOrganizations(Collections.singletonList("USER"));
        identityProvider.getRoleMappings().add(role2);

        RoleMappingEntity role3 = new RoleMappingEntity();
        role3.setCondition("{#jsonPath(#profile, '$.job_id') != 'API_BREAKER'}");
        role3.setOrganizations(Collections.singletonList("USER"));
        role3.setEnvironments(Collections.singletonMap("DEFAULT", Collections.singletonList("ADMIN")));
        identityProvider.getRoleMappings().add(role3);
    }

    private void mockWrongELGroupsMapping() {
        GroupMappingEntity condition1 = new GroupMappingEntity();
        condition1.setCondition("Some Soup");
        condition1.setGroups(Arrays.asList("Example group", "soft user"));
        identityProvider.getGroupMappings().add(condition1);

        GroupMappingEntity condition2 = new GroupMappingEntity();
        condition2.setCondition("{#jsonPath(#profile, '$.identity_provider_id') == 'idp_6'}");
        condition2.setGroups(Collections.singletonList("Others"));
        identityProvider.getGroupMappings().add(condition2);

        GroupMappingEntity condition3 = new GroupMappingEntity();
        condition3.setCondition("{#jsonPath(#profile, '$.job_id') != 'API_BREAKER'}");
        condition3.setGroups(Collections.singletonList("Api consumer"));
        identityProvider.getGroupMappings().add(condition3);
    }
}

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
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static jakarta.ws.rs.client.Entity.form;
import static jakarta.ws.rs.client.Entity.json;
import static jakarta.ws.rs.client.Entity.text;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.el.exceptions.ExpressionEvaluationException;
import io.gravitee.rest.api.management.rest.model.ExchangePayloadEntity;
import io.gravitee.rest.api.management.rest.model.TokenEntity;
import io.gravitee.rest.api.management.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.*;
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
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.*;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Christophe LANNOY (chrislannoy.java at gmail.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class OAuth2AuthenticationResourceTest extends AbstractResourceTest {

    private static final String ORGANIZATION_ID = "organization-id";
    private static final String USER_SOURCE_OAUTH2 = "oauth2";

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort());

    private SocialIdentityProviderEntity identityProvider = null;

    @Override
    protected String contextPath() {
        return "auth/oauth2/" + USER_SOURCE_OAUTH2;
    }

    @Before
    public void init() {
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
                return "http://localhost:" + wireMockRule.port() + "/introspect";
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

        when(userService.createOrUpdateUserFromSocialIdentityProvider(any(), eq(identityProvider), anyString())).thenReturn(userEntity);

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
        when(userService.createOrUpdateUserFromSocialIdentityProvider(any(), eq(identityProvider), any())).thenReturn(userEntity);
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
        when(userService.createOrUpdateUserFromSocialIdentityProvider(any(), eq(identityProvider), any())).thenReturn(userEntity);
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
        when(userService.createOrUpdateUserFromSocialIdentityProvider(any(), eq(identityProvider), any())).thenReturn(createdUser);
        when(userService.connect(any(), eq("janedoe@example.com"))).thenReturn(createdUser);

        // -- CALL

        final MultivaluedMap<String, String> payload = createPayload("the_client_id", "http://localhost/callback", "CoDe", "StAtE");

        Response response = orgTarget().request().post(form(payload));

        // -- VERIFY
        verify(userService, times(1)).createOrUpdateUserFromSocialIdentityProvider(any(), any(), any());
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
        when(userService.createOrUpdateUserFromSocialIdentityProvider(any(), refEq(socialIdentityProviderEntity), eq(userInfo))).thenReturn(
            createdUser
        );
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
        verify(userService, times(0)).createOrUpdateUserFromSocialIdentityProvider(any(), any(), any());
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
        when(userService.createOrUpdateUserFromSocialIdentityProvider(any(), any(), any())).thenThrow(new EmailRequiredException("email"));
        // -- CALL

        final MultivaluedMap<String, String> payload = createPayload("the_client_id", "http://localhost/callback", "CoDe", "StAtE");

        Response response = orgTarget().request().post(form(payload));

        // -- VERIFY
        verify(userService, times(1)).createOrUpdateUserFromSocialIdentityProvider(any(), any(), any());

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
        verify(userService, times(1)).createOrUpdateUserFromSocialIdentityProvider(any(), refEq(identityProvider), anyString());

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
        verify(userService, times(1)).createOrUpdateUserFromSocialIdentityProvider(any(), refEq(identityProvider), anyString());

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
        verify(userService, times(1)).createOrUpdateUserFromSocialIdentityProvider(any(), refEq(identityProvider), anyString());

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

        when(userService.createOrUpdateUserFromSocialIdentityProvider(any(), any(), any())).thenThrow(
            new ExpressionEvaluationException("error")
        );

        // -- CALL

        final MultivaluedMap<String, String> payload = createPayload("the_client_id", "http://localhost/callback", "CoDe", "StAtE");

        Response response = orgTarget().request().post(form(payload));

        // -- VERIFY
        verify(userService, times(1)).createOrUpdateUserFromSocialIdentityProvider(any(), any(), any());
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

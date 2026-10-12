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
package io.gravitee.apim.rest.api.mcp.security.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.installation.query_service.InstallationAccessQueryService;
import io.gravitee.apim.rest.api.mcp.security.filter.McpTokenAuthenticationFilter;
import io.gravitee.apim.rest.api.mcp.security.oauth.ClientCredentials;
import io.gravitee.apim.rest.api.mcp.security.oauth.IdentityProviderException;
import io.gravitee.apim.rest.api.mcp.security.oauth.IdentityProviderTokenIntrospector;
import io.gravitee.apim.rest.api.mcp.security.oauth.IntrospectedToken;
import io.gravitee.repository.management.api.UserRepository;
import io.gravitee.repository.management.model.Token;
import io.gravitee.repository.management.model.User;
import io.gravitee.rest.api.idp.api.authentication.UserDetails;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.configuration.identity.IdentityProviderActivationReferenceType;
import io.gravitee.rest.api.model.configuration.identity.oidc.OIDCIdentityProviderEntity;
import io.gravitee.rest.api.security.config.AbstractSecurityConfigurerAdapterTest;
import io.gravitee.rest.api.security.filter.TokenAuthenticationFilter;
import io.gravitee.rest.api.security.utils.AuthoritiesProvider;
import io.gravitee.rest.api.service.OrganizationService;
import io.gravitee.rest.api.service.SocialIdentityProviderService;
import io.gravitee.rest.api.service.TokenService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.JWTHelper.Claims;
import io.gravitee.rest.api.service.configuration.identity.IdentityProviderActivationService.ActivationTarget;
import io.gravitee.rest.api.service.exceptions.OrganizationNotFoundException;
import io.gravitee.rest.api.service.exceptions.TokenNotFoundException;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(
    classes = {
        McpSecurityConfigurerAdapter.class,
        McpOAuthConfiguration.class,
        AbstractSecurityConfigurerAdapterTest.TestConfig.class,
        McpSecurityConfigurerAdapterTest.TestBeans.class,
        McpSecurityConfigurerAdapterTest.Endpoint.class,
    }
)
@WebAppConfiguration
@ActiveProfiles("basic")
@TestPropertySource(
    properties = {
        "jwt.secret=" + McpSecurityConfigurerAdapterTest.JWT_SECRET,
        "http.api.mcp.authorization.organizations[0].orgId=acme",
        "http.api.mcp.authorization.organizations[0].identityProvider=keycloak-acme",
        "http.api.mcp.authorization.organizations[0].clientId=mcp-acme",
        "http.api.mcp.authorization.organizations[0].clientSecret=secret-acme",
    }
)
class McpSecurityConfigurerAdapterTest {

    static final String JWT_SECRET = "my-secret-for-tests";
    private static final String PAT = "a-personal-access-token";
    private static final String ACME_RESOURCE = "https://apim.example.com/mcp/organizations/acme";
    private static final String ACME_METADATA = ACME_RESOURCE + "/.well-known/oauth-protected-resource";
    private static final String ISSUER = "https://keycloak.example.com/realms/acme";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private AuthoritiesProvider authoritiesProvider;

    @Autowired
    private Endpoint endpoint;

    @Autowired
    private InstallationAccessQueryService installationAccessQueryService;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private SocialIdentityProviderService socialIdentityProviderService;

    @Autowired
    private IdentityProviderTokenIntrospector introspector;

    private final OIDCIdentityProviderEntity keycloak = new OIDCIdentityProviderEntity();

    @BeforeEach
    void setUp() throws Exception {
        endpoint.seen.set(null);
        when(authoritiesProvider.retrieveAuthorities(any())).thenReturn(Set.of());

        var token = new Token();
        token.setId("token-id");
        token.setName("mcp-demo");
        token.setReferenceId("user-id");
        // The mocks are singletons shared by the tests: stub without calling them, and start clean.
        Mockito.reset(tokenService);
        Mockito.doThrow(new TokenNotFoundException("unknown")).when(tokenService).findByToken(any());
        Mockito.doReturn(token).when(tokenService).findByToken(PAT);

        var user = new User();
        user.setId("user-id");
        user.setOrganizationId("acme");
        when(userRepository.findById("user-id")).thenReturn(Optional.of(user));
        var entity = new UserEntity();
        entity.setId("user-id");
        entity.setOrganizationId("acme");
        entity.setEmail("jane@acme.example");
        when(userService.findById(any(), eq("user-id"))).thenReturn(entity);

        when(installationAccessQueryService.getMcpAPIUrl(any())).thenReturn("https://apim.example.com/mcp");
        Mockito.reset(organizationService, introspector);
        Mockito.doThrow(new OrganizationNotFoundException("ghost")).when(organizationService).findById("ghost");
        keycloak.setId("keycloak-acme");
        keycloak.setTokenIntrospectionEndpoint("https://keycloak.example.com/introspect");
        keycloak.setDiscoveryEndpoint("https://keycloak.example.com/.well-known/openid-configuration");
        when(
            socialIdentityProviderService.findById(
                "keycloak-acme",
                new ActivationTarget("acme", IdentityProviderActivationReferenceType.ORGANIZATION)
            )
        ).thenReturn(keycloak);
        when(introspector.issuer(keycloak)).thenReturn(ISSUER);
    }

    @Test
    void should_challenge_for_a_bearer_token_when_no_credentials_are_sent() throws Exception {
        var response = mockMvc.perform(mcpRequest()).andExpect(status().isUnauthorized()).andReturn().getResponse();

        assertThat(response.getHeader(HttpHeaders.WWW_AUTHENTICATE)).isEqualTo("Bearer");
    }

    @Test
    void should_reject_basic_credentials() throws Exception {
        var basic = "Basic " + Base64.getEncoder().encodeToString("admin:admin".getBytes());

        mockMvc.perform(mcpRequest().header(HttpHeaders.AUTHORIZATION, basic)).andExpect(status().isUnauthorized());
    }

    @Test
    void should_reject_a_console_session_cookie() throws Exception {
        var sessionCookie = new Cookie(TokenAuthenticationFilter.AUTH_COOKIE_NAME, "Bearer%20" + aJwt("acme"));

        mockMvc.perform(mcpRequest().cookie(sessionCookie)).andExpect(status().isUnauthorized());
    }

    @Test
    void should_reject_an_invalid_token_with_a_challenge_and_no_cookie() throws Exception {
        var response = mockMvc
            .perform(mcpRequest().header(HttpHeaders.AUTHORIZATION, "Bearer not.a.jwt"))
            .andExpect(status().isUnauthorized())
            .andReturn()
            .getResponse();

        assertThat(response.getHeader(HttpHeaders.WWW_AUTHENTICATE)).isEqualTo("Bearer error=\"invalid_token\"");
        assertThat(response.getCookies()).isEmpty();
    }

    @Test
    void should_authenticate_a_jwt_with_the_organization_of_its_claim() throws Exception {
        mockMvc.perform(mcpRequest().header(HttpHeaders.AUTHORIZATION, "Bearer " + aJwt("acme"))).andExpect(status().isOk());

        assertThat(endpoint.seen.get()).containsEntry("user", "user-id").containsEntry("organization", "acme");
    }

    @Test
    void should_authenticate_a_personal_access_token_without_an_organization_in_the_path() throws Exception {
        mockMvc.perform(mcpRequest().header(HttpHeaders.AUTHORIZATION, "Bearer " + PAT)).andExpect(status().isOk());

        assertThat(endpoint.seen.get())
            .containsEntry("user", "user-id")
            .containsEntry("organization", "acme")
            .containsEntry("source", "token")
            .containsEntry("sourceId", "mcp-demo");
    }

    @Test
    void should_reject_an_expired_personal_access_token() throws Exception {
        var expired = new Token();
        expired.setId("token-id");
        expired.setReferenceId("user-id");
        expired.setExpiresAt(new Date(System.currentTimeMillis() - 1000));
        Mockito.doReturn(expired).when(tokenService).findByToken(PAT);

        mockMvc.perform(mcpRequest().header(HttpHeaders.AUTHORIZATION, "Bearer " + PAT)).andExpect(status().isUnauthorized());
    }

    @Test
    void should_reject_an_unknown_personal_access_token() throws Exception {
        mockMvc.perform(mcpRequest().header(HttpHeaders.AUTHORIZATION, "Bearer another-token")).andExpect(status().isUnauthorized());
    }

    @Test
    void should_never_log_the_token_it_rejects() throws Exception {
        // As TokenServiceImpl does: the exception carries the value it could not find.
        Mockito.doThrow(new TokenNotFoundException("an-unknown-secret-value")).when(tokenService).findByToken("an-unknown-secret-value");
        var logger = (Logger) LoggerFactory.getLogger(McpTokenAuthenticationFilter.class);
        var appender = new ListAppender<ILoggingEvent>();
        appender.start();
        logger.addAppender(appender);
        try {
            mockMvc
                .perform(mcpRequest().header(HttpHeaders.AUTHORIZATION, "Bearer an-unknown-secret-value"))
                .andExpect(status().isUnauthorized());
        } finally {
            logger.detachAppender(appender);
        }

        assertThat(appender.list)
            .isNotEmpty()
            .noneMatch(event -> event.getFormattedMessage().contains("an-unknown-secret-value"));
    }

    @Test
    void should_not_require_a_csrf_token_from_a_browser_hosted_client() throws Exception {
        mockMvc
            .perform(
                mcpRequest()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + aJwt("acme"))
                    .header(HttpHeaders.ORIGIN, "https://client.example.com")
            )
            .andExpect(status().isOk());
    }

    @Test
    void should_serve_the_protected_resource_metadata_of_an_organization_without_credentials() throws Exception {
        var response = mockMvc
            .perform(get("/organizations/acme/.well-known/oauth-protected-resource"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(new ObjectMapper().readTree(response.getContentAsString())).isEqualTo(
            new ObjectMapper().readTree(
                "{\"resource\":\"" +
                    ACME_RESOURCE +
                    "\",\"authorization_servers\":[\"" +
                    ISSUER +
                    "\"],\"bearer_methods_supported\":[\"header\"]}"
            )
        );
    }

    @Test
    void should_answer_unavailable_when_the_identity_provider_cannot_tell_its_issuer() throws Exception {
        when(introspector.issuer(keycloak)).thenThrow(new IdentityProviderException("Discovery document answered 503"));

        mockMvc.perform(get("/organizations/acme/.well-known/oauth-protected-resource")).andExpect(status().isServiceUnavailable());
    }

    @Test
    void should_not_serve_metadata_for_an_organization_without_oauth() throws Exception {
        mockMvc.perform(get("/.well-known/oauth-protected-resource")).andExpect(status().isNotFound());
    }

    @Test
    void should_not_serve_an_organization_that_does_not_exist() throws Exception {
        mockMvc
            .perform(mcpRequest("/organizations/ghost").header(HttpHeaders.AUTHORIZATION, "Bearer " + PAT))
            .andExpect(status().isNotFound());
    }

    @Test
    void should_point_a_client_without_credentials_to_the_metadata() throws Exception {
        var response = mockMvc.perform(mcpRequest("/organizations/acme")).andExpect(status().isUnauthorized()).andReturn().getResponse();

        assertThat(response.getHeader(HttpHeaders.WWW_AUTHENTICATE)).isEqualTo("Bearer resource_metadata=\"" + ACME_METADATA + "\"");
    }

    @Test
    void should_point_a_client_with_a_rejected_token_to_the_metadata() throws Exception {
        when(
            introspector.introspect(keycloak, new ClientCredentials("mcp-acme", "secret-acme"), "a-rejected-token", ACME_RESOURCE)
        ).thenThrow(new IdentityProviderException("Identity provider keycloak-acme issued the token for another audience"));

        var response = mockMvc
            .perform(mcpRequest("/organizations/acme").header(HttpHeaders.AUTHORIZATION, "Bearer a-rejected-token"))
            .andExpect(status().isUnauthorized())
            .andReturn()
            .getResponse();

        assertThat(response.getHeader(HttpHeaders.WWW_AUTHENTICATE)).isEqualTo(
            "Bearer error=\"invalid_token\", resource_metadata=\"" + ACME_METADATA + "\""
        );
    }

    @Test
    void should_authenticate_a_token_of_the_identity_provider_as_the_apim_user_it_maps_to() throws Exception {
        givenTheIdentityProviderVouchesForJane("a-token-of-jane");

        mockMvc
            .perform(mcpRequest("/organizations/acme").header(HttpHeaders.AUTHORIZATION, "Bearer a-token-of-jane"))
            .andExpect(status().isOk());

        assertThat(endpoint.seen.get())
            .containsEntry("user", "user-jane")
            .containsEntry("organization", "acme")
            .containsEntry("source", "keycloak-acme")
            .containsEntry("sourceId", "jane");
    }

    @Test
    void should_introspect_a_token_once_while_it_is_valid() throws Exception {
        givenTheIdentityProviderVouchesForJane("another-token-of-jane");

        for (int call = 0; call < 2; call++) {
            mockMvc
                .perform(mcpRequest("/organizations/acme").header(HttpHeaders.AUTHORIZATION, "Bearer another-token-of-jane"))
                .andExpect(status().isOk());
        }

        verify(introspector, times(1)).introspect(any(), any(), any(), any());
    }

    @Test
    void should_authenticate_a_personal_access_token_on_the_url_of_its_organization_without_the_identity_provider() throws Exception {
        mockMvc.perform(mcpRequest("/organizations/acme").header(HttpHeaders.AUTHORIZATION, "Bearer " + PAT)).andExpect(status().isOk());

        assertThat(endpoint.seen.get()).containsEntry("user", "user-id").containsEntry("organization", "acme");
        verify(introspector, never()).introspect(any(), any(), any(), any());
    }

    @Test
    void should_not_send_an_apim_jwt_to_the_identity_provider() throws Exception {
        mockMvc
            .perform(mcpRequest("/organizations/acme").header(HttpHeaders.AUTHORIZATION, "Bearer " + aJwt("acme")))
            .andExpect(status().isOk());

        verify(introspector, never()).introspect(any(), any(), any(), any());
    }

    @Test
    void should_reject_a_token_on_the_url_of_another_organization() throws Exception {
        mockMvc
            .perform(mcpRequest("/organizations/globex").header(HttpHeaders.AUTHORIZATION, "Bearer " + PAT))
            .andExpect(status().isUnauthorized());
    }

    /** Each test its own token: validated identities outlive a test in the filter shared by the context. */
    private void givenTheIdentityProviderVouchesForJane(String token) throws Exception {
        var claims = new ObjectMapper().readTree("{\"active\":true,\"sub\":\"jane\"}");
        when(introspector.introspect(keycloak, new ClientCredentials("mcp-acme", "secret-acme"), token, ACME_RESOURCE)).thenReturn(
            new IntrospectedToken(Instant.now().plusSeconds(3600), claims)
        );
        var jane = new UserEntity();
        jane.setId("user-jane");
        jane.setOrganizationId("acme");
        jane.setSourceId("jane");
        when(
            userService.createOrUpdateUserFromSocialIdentityProvider(any(), eq(keycloak), eq(claims.toString()), eq(token), isNull())
        ).thenReturn(jane);
    }

    private static MockHttpServletRequestBuilder mcpRequest() {
        return mcpRequest("/");
    }

    private static MockHttpServletRequestBuilder mcpRequest(String path) {
        return post(path).contentType("application/json").content("{}");
    }

    private static String aJwt(String organizationId) {
        return JWT.create().withSubject("user-id").withClaim(Claims.ORG, organizationId).sign(Algorithm.HMAC256(JWT_SECRET));
    }

    @Configuration
    static class TestBeans {

        @Bean
        public UserRepository userRepository() {
            return Mockito.mock(UserRepository.class);
        }

        @Bean
        public OrganizationService organizationService() {
            return Mockito.mock(OrganizationService.class);
        }

        @Bean
        public SocialIdentityProviderService socialIdentityProviderService() {
            return Mockito.mock(SocialIdentityProviderService.class);
        }

        @Bean
        @Primary
        public IdentityProviderTokenIntrospector mockIntrospector() {
            return Mockito.mock(IdentityProviderTokenIntrospector.class);
        }
    }

    /** Records who the chain authenticated and which organization it bound to the thread. */
    @Configuration
    @RestController
    static class Endpoint {

        final AtomicReference<Map<String, String>> seen = new AtomicReference<>();

        @PostMapping(path = { "/", "/organizations/{organizationId}" })
        public void handle() {
            var user = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            seen.set(
                Map.of(
                    "user",
                    user.getUsername(),
                    "organization",
                    String.valueOf(GraviteeContext.getCurrentOrganization()),
                    "source",
                    String.valueOf(user.getSource()),
                    "sourceId",
                    String.valueOf(user.getSourceId())
                )
            );
        }
    }
}

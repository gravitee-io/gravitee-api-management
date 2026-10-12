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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import io.gravitee.repository.management.api.UserRepository;
import io.gravitee.repository.management.model.Token;
import io.gravitee.repository.management.model.User;
import io.gravitee.rest.api.idp.api.authentication.UserDetails;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.security.config.AbstractSecurityConfigurerAdapterTest;
import io.gravitee.rest.api.security.filter.TokenAuthenticationFilter;
import io.gravitee.rest.api.security.utils.AuthoritiesProvider;
import io.gravitee.rest.api.service.TokenService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.JWTHelper.Claims;
import io.gravitee.rest.api.service.exceptions.TokenNotFoundException;
import jakarta.servlet.http.Cookie;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
        AbstractSecurityConfigurerAdapterTest.TestConfig.class,
        McpSecurityConfigurerAdapterTest.TestBeans.class,
        McpSecurityConfigurerAdapterTest.Endpoint.class,
    }
)
@WebAppConfiguration
@ActiveProfiles("basic")
@TestPropertySource(properties = { "jwt.secret=" + McpSecurityConfigurerAdapterTest.JWT_SECRET })
class McpSecurityConfigurerAdapterTest {

    static final String JWT_SECRET = "my-secret-for-tests";
    private static final String PAT = "a-personal-access-token";

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
    void should_not_require_a_csrf_token_from_a_browser_hosted_client() throws Exception {
        mockMvc
            .perform(
                mcpRequest()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + aJwt("acme"))
                    .header(HttpHeaders.ORIGIN, "https://client.example.com")
            )
            .andExpect(status().isOk());
    }

    private static MockHttpServletRequestBuilder mcpRequest() {
        return post("/").contentType("application/json").content("{}");
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
    }

    /** Records who the chain authenticated and which organization it bound to the thread. */
    @Configuration
    @RestController
    static class Endpoint {

        final AtomicReference<Map<String, String>> seen = new AtomicReference<>();

        @PostMapping(path = "/")
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

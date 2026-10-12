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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.security.config.AbstractSecurityConfigurerAdapterTest;
import io.gravitee.rest.api.security.cookies.CookieGenerator;
import io.gravitee.rest.api.security.filter.TokenAuthenticationFilter;
import io.gravitee.rest.api.security.utils.AuthoritiesProvider;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.common.JWTHelper.Claims;
import jakarta.servlet.http.Cookie;
import java.util.Base64;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
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
        McpSecurityConfigurerAdapterTest.DummyController.class,
    }
)
@WebAppConfiguration
@ActiveProfiles("basic")
@TestPropertySource(properties = { "jwt.secret=" + McpSecurityConfigurerAdapterTest.JWT_SECRET })
class McpSecurityConfigurerAdapterTest {

    static final String JWT_SECRET = "my-secret-for-tests";
    private static final String ENDPOINT = "/organizations/DEFAULT/environments/DEFAULT";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    private AuthoritiesProvider authoritiesProvider;

    @Autowired
    private CookieGenerator cookieGenerator;

    @BeforeEach
    void setUp() {
        var environment = new EnvironmentEntity();
        environment.setId("DEFAULT");
        environment.setOrganizationId("DEFAULT");
        when(environmentService.findByOrgAndIdOrHrid("DEFAULT", "DEFAULT")).thenReturn(environment);
        when(authoritiesProvider.retrieveAuthorities(any())).thenReturn(Set.of());
        when(cookieGenerator.generate(any(), any())).thenReturn(new Cookie(TokenAuthenticationFilter.AUTH_COOKIE_NAME, ""));
    }

    @Test
    void should_challenge_for_a_bearer_token_when_no_credentials_are_sent() throws Exception {
        var response = mockMvc.perform(mcpRequest()).andExpect(status().isUnauthorized()).andReturn().getResponse();

        assertThat(response.getHeader(HttpHeaders.WWW_AUTHENTICATE)).isEqualTo("Bearer");
    }

    @Test
    void should_reject_basic_credentials() throws Exception {
        var basic = "Basic " + Base64.getEncoder().encodeToString("admin:admin".getBytes());
        var response = mockMvc
            .perform(mcpRequest().header(HttpHeaders.AUTHORIZATION, basic))
            .andExpect(status().isUnauthorized())
            .andReturn()
            .getResponse();

        assertThat(response.getHeader(HttpHeaders.WWW_AUTHENTICATE)).isEqualTo("Bearer");
    }

    @Test
    void should_reject_a_console_session_cookie() throws Exception {
        var sessionCookie = new Cookie(TokenAuthenticationFilter.AUTH_COOKIE_NAME, "Bearer%20" + aToken("DEFAULT"));
        mockMvc.perform(mcpRequest().cookie(sessionCookie)).andExpect(status().isUnauthorized());
    }

    @Test
    void should_reject_an_invalid_bearer_token() throws Exception {
        mockMvc.perform(mcpRequest().header(HttpHeaders.AUTHORIZATION, "Bearer not.a.jwt")).andExpect(status().isUnauthorized());
    }

    @Test
    void should_accept_a_valid_bearer_token() throws Exception {
        mockMvc.perform(mcpRequest().header(HttpHeaders.AUTHORIZATION, "Bearer " + aToken("DEFAULT"))).andExpect(status().isOk());
    }

    @Test
    void should_not_require_a_csrf_token_from_a_browser_hosted_client() throws Exception {
        mockMvc
            .perform(
                mcpRequest()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + aToken("DEFAULT"))
                    .header(HttpHeaders.ORIGIN, "https://client.example.com")
            )
            .andExpect(status().isOk());
    }

    @Test
    void should_forbid_a_token_issued_for_another_organization() throws Exception {
        mockMvc
            .perform(mcpRequest().header(HttpHeaders.AUTHORIZATION, "Bearer " + aToken("another-organization")))
            .andExpect(status().isForbidden());
    }

    private static MockHttpServletRequestBuilder mcpRequest() {
        return post(ENDPOINT).contentType("application/json").content("{}");
    }

    private static String aToken(String organizationId) {
        return JWT.create().withSubject("user-id").withClaim(Claims.ORG, organizationId).sign(Algorithm.HMAC256(JWT_SECRET));
    }

    @Configuration
    @RestController
    static class DummyController {

        @PostMapping(path = ENDPOINT)
        public void handle() {}
    }
}

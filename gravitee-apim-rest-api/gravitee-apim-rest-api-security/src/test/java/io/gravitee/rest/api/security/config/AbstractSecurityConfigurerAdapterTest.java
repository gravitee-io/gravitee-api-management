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
package io.gravitee.rest.api.security.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.access_point.query_service.AccessPointQueryService;
import io.gravitee.apim.core.installation.domain_service.InstallationTypeDomainService;
import io.gravitee.apim.core.installation.query_service.InstallationAccessQueryService;
import io.gravitee.common.event.EventManager;
import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.rest.api.idp.core.plugin.IdentityProviderManager;
import io.gravitee.rest.api.security.authentication.AuthenticationProviderManager;
import io.gravitee.rest.api.security.cookies.CookieGenerator;
import io.gravitee.rest.api.security.utils.AuthoritiesProvider;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.ReCaptchaService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.TokenService;
import io.gravitee.rest.api.service.UserService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { AbstractSecurityConfigurerAdapterTest.TestConfig.class })
@WebAppConfiguration
@ActiveProfiles("basic")
@TestPropertySource(
    properties = {
        "jwt.secret=my-secret-for-tests",
        "http.secureHeaders.xframe.enabled=true",
        "http.secureHeaders.xframe.action=SAMEORIGIN",
        "http.secureHeaders.xContentTypeOptions.enabled=true",
        "http.secureHeaders.csp.policy=frame-ancestors 'self';",
        "http.secureHeaders.referrerPolicy.policy=strict-origin-when-cross-origin",
        "http.secureHeaders.permissionsPolicy.policy=camera=(), microphone=(), geolocation=()",
        "http.secureHeaders.hsts.enabled=true",
        "http.secureHeaders.hsts.include-sub-domains=true",
        "http.secureHeaders.hsts.max-age=31536000",
        "http.secureHeaders.csrf.enabled=true",
    }
)
public abstract class AbstractSecurityConfigurerAdapterTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected Configuration graviteeConfiguration;

    @Autowired
    protected CookieGenerator cookieGenerator;

    @BeforeEach
    public void setUp() {
        when(graviteeConfiguration.getProperty("jwt.secret")).thenReturn("my-secret-for-tests");
        when(cookieGenerator.generate(eq("XSRF-TOKEN"), any(), eq(true))).thenReturn(new Cookie("XSRF-TOKEN", "some-csrf-token"));
    }

    @Test
    void should_configure_secure_headers() throws Exception {
        mockMvc
            .perform(get(getPath()).secure(true))
            .andExpect(header().string("X-Content-Type-Options", "nosniff"))
            .andExpect(header().string("X-Frame-Options", "SAMEORIGIN"))
            .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
            .andExpect(header().string("Permissions-Policy", "camera=(), microphone=(), geolocation=()"))
            .andExpect(header().string("Content-Security-Policy", "frame-ancestors 'self';"))
            .andExpect(header().string("Strict-Transport-Security", "max-age=31536000 ; includeSubDomains"))
            .andExpect(cookie().exists("XSRF-TOKEN"));
    }

    protected abstract String getPath();

    @org.springframework.context.annotation.Configuration
    @EnableWebMvc
    public static class TestConfig {

        @Bean
        public MockMvc mockMvc(WebApplicationContext wac) {
            return MockMvcBuilders.webAppContextSetup(wac).apply(springSecurity()).build();
        }

        @Bean
        public CookieGenerator cookieGenerator() {
            return mock(CookieGenerator.class);
        }

        @Bean
        public IdentityProviderManager identityProviderManager() {
            return mock(IdentityProviderManager.class);
        }

        @Bean
        public UserService userService() {
            return mock(UserService.class);
        }

        @Bean
        public TokenService tokenService() {
            return mock(TokenService.class);
        }

        @Bean
        public AuthoritiesProvider authoritiesProvider() {
            return mock(AuthoritiesProvider.class);
        }

        @Bean
        public InstallationAccessQueryService installationAccessQueryService() {
            return mock(InstallationAccessQueryService.class);
        }

        @Bean
        public InstallationTypeDomainService installationTypeDomainService() {
            return mock(InstallationTypeDomainService.class);
        }

        @Bean
        public AccessPointQueryService accessPointQueryService() {
            return mock(AccessPointQueryService.class);
        }

        @Bean
        public Configuration graviteeConfiguration() {
            return mock(Configuration.class);
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
            return mock(CorsConfigurationSource.class);
        }

        @Bean
        public ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        public EventManager eventManager() {
            return mock(EventManager.class);
        }

        @Bean
        public AuthenticationProviderManager authenticationProviderManager() {
            return mock(AuthenticationProviderManager.class);
        }

        @Bean
        public ReCaptchaService reCaptchaService() {
            return mock(ReCaptchaService.class);
        }

        @Bean
        public ParameterService parameterService() {
            return mock(ParameterService.class);
        }

        @Bean
        public EnvironmentService environmentService() {
            return mock(EnvironmentService.class);
        }

        @Bean
        public MembershipService membershipService() {
            return mock(MembershipService.class);
        }

        @Bean
        public RoleService roleService() {
            return mock(RoleService.class);
        }
    }
}

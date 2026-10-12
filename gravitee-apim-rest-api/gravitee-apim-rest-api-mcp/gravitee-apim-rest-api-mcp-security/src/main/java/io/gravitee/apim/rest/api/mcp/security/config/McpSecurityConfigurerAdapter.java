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

import static io.gravitee.rest.api.security.config.SecureHeadersConfigurer.hsts;

import io.gravitee.apim.core.access_point.query_service.AccessPointQueryService;
import io.gravitee.apim.core.installation.domain_service.InstallationTypeDomainService;
import io.gravitee.apim.rest.api.mcp.security.filter.BearerOnlyFilter;
import io.gravitee.rest.api.security.cookies.CookieGenerator;
import io.gravitee.rest.api.security.filter.GraviteeContextAuthorizationFilter;
import io.gravitee.rest.api.security.filter.GraviteeContextFilter;
import io.gravitee.rest.api.security.filter.TokenAuthenticationFilter;
import io.gravitee.rest.api.security.utils.AuthoritiesProvider;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.TokenService;
import io.gravitee.rest.api.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfFilter;

/**
 * @author GraviteeSource Team
 */
@Configuration
@Profile("basic")
@EnableWebSecurity
public class McpSecurityConfigurerAdapter {

    @Autowired
    private ConfigurableEnvironment environment;

    @Autowired
    private CookieGenerator cookieGenerator;

    @Autowired
    private UserService userService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private AuthoritiesProvider authoritiesProvider;

    @Autowired
    private AccessPointQueryService accessPointQueryService;

    @Autowired
    private InstallationTypeDomainService installationTypeDomainService;

    @Autowired
    private EnvironmentService environmentService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        final String jwtSecret = environment.getProperty("jwt.secret");
        if (jwtSecret == null || jwtSecret.isEmpty()) {
            throw new IllegalStateException("JWT secret is mandatory");
        }

        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        // The session cookie never authenticates on this mount (see BearerOnlyFilter), so there is no ambient credential to protect.
        http.csrf(AbstractHttpConfigurer::disable);
        http.authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated());
        http.exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(BearerOnlyFilter::challenge));
        hsts(http, environment);

        http.addFilterBefore(
            new TokenAuthenticationFilter(jwtSecret, cookieGenerator, userService, tokenService, authoritiesProvider),
            BasicAuthenticationFilter.class
        );
        http.addFilterBefore(new BearerOnlyFilter(), TokenAuthenticationFilter.class);
        http.addFilterBefore(
            new GraviteeContextFilter(installationTypeDomainService, accessPointQueryService, environmentService),
            CsrfFilter.class
        );
        http.addFilterAfter(new GraviteeContextAuthorizationFilter(), AuthorizationFilter.class);

        return http.build();
    }
}

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

import io.gravitee.apim.rest.api.mcp.security.filter.BearerOnlyFilter;
import io.gravitee.apim.rest.api.mcp.security.filter.McpTokenAuthenticationFilter;
import io.gravitee.repository.management.api.UserRepository;
import io.gravitee.rest.api.security.utils.AuthoritiesProvider;
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
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

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
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private AuthoritiesProvider authoritiesProvider;

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

        // The organization is the authenticated user's: no path segment to resolve, nothing to check it against.
        http.addFilterBefore(
            new McpTokenAuthenticationFilter(jwtSecret, tokenService, userRepository, userService, authoritiesProvider),
            BasicAuthenticationFilter.class
        );
        http.addFilterBefore(new BearerOnlyFilter(), McpTokenAuthenticationFilter.class);

        return http.build();
    }
}

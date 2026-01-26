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
package io.gravitee.rest.api.management.v2.mcp;

import io.gravitee.rest.api.security.cookies.CookieGenerator;
import io.gravitee.rest.api.security.filter.TokenAuthenticationFilter;
import io.gravitee.rest.api.security.utils.AuthoritiesProvider;
import io.gravitee.rest.api.service.TokenService;
import io.gravitee.rest.api.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Security configuration for MCP endpoints.
 * This is a simplified configuration that only handles JWT token authentication
 * without the GraviteeContext filters that require organization/environment context.
 *
 * @author GraviteeSource Team
 */
@Configuration
@Profile("basic")
@EnableWebSecurity
public class McpSecurityConfiguration {

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

    @Bean
    public SecurityFilterChain mcpSecurityFilterChain(HttpSecurity http) throws Exception {
        final String jwtSecret = environment.getProperty("jwt.secret");
        if (jwtSecret == null || jwtSecret.isEmpty()) {
            throw new IllegalStateException("JWT secret is mandatory");
        }

        http
            // Stateless session
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Disable CSRF for API
            .csrf(csrf -> csrf.disable())
            // Disable frame options
            .headers(headers -> headers.frameOptions(frame -> frame.disable()))
            // CORS preflight requests are allowed without authentication
            // All other requests require authentication
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "**").permitAll()
                .anyRequest().authenticated()
            )
            // Add JWT token filter (wrapped to skip OPTIONS requests)
            .addFilterBefore(
                createTokenFilterSkippingOptions(jwtSecret),
                //TODO:  Why in other config we don"t need wrapper
//                new TokenAuthenticationFilter(jwtSecret, cookieGenerator, userService, tokenService, authoritiesProvider),
                BasicAuthenticationFilter.class
            );

        return http.build();
    }

    /**
     * Creates a filter that wraps TokenAuthenticationFilter but skips OPTIONS requests.
     * This allows CORS preflight requests to pass through without authentication.
     */
    private OncePerRequestFilter createTokenFilterSkippingOptions(String jwtSecret) {
        final TokenAuthenticationFilter tokenFilter = new TokenAuthenticationFilter(
            jwtSecret, cookieGenerator, userService, tokenService, authoritiesProvider
        );

        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, IOException {
                // Skip authentication for OPTIONS requests (CORS preflight)
                if (HttpMethod.OPTIONS.matches(request.getMethod())) {
                    filterChain.doFilter(request, response);
                    return;
                }
                // For all other requests, apply token authentication
                tokenFilter.doFilter(request, response, filterChain);
            }
        };
    }
}

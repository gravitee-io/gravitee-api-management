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
package io.gravitee.management.security.config;

import io.gravitee.management.idp.api.IdentityProvider;
import io.gravitee.management.idp.api.authentication.AuthenticationProvider;
import io.gravitee.management.idp.core.plugin.IdentityProviderManager;
import io.gravitee.management.security.authentication.AuthenticationProviderManager;
import io.gravitee.management.security.cookies.JWTCookieGenerator;
import io.gravitee.management.security.filter.AuthenticationSuccessFilter;
import io.gravitee.management.security.filter.JWTAuthenticationFilter;
import io.gravitee.management.security.listener.AuthenticationSuccessListener;
import io.gravitee.management.service.MembershipService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.SecurityConfigurer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static io.gravitee.management.service.common.JWTHelper.DefaultValues.DEFAULT_JWT_EXPIRE_AFTER;
import static io.gravitee.management.service.common.JWTHelper.DefaultValues.DEFAULT_JWT_ISSUER;
import static java.util.Arrays.asList;


/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Configuration
@Profile("basic")
@EnableWebSecurity
public class BasicSecurityConfigurerAdapter extends WebSecurityConfigurerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(BasicSecurityConfigurerAdapter.class);

    @Autowired
    private ConfigurableEnvironment environment;

    @Autowired
    private JWTCookieGenerator jwtCookieGenerator;

    @Autowired
    private IdentityProviderManager identityProviderManager;

    @Autowired
    private AuthenticationProviderManager authenticationProviderManager;

    @Autowired
    private MembershipService membershipService;

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        LOGGER.info("Loading authentication identity providers for Basic authentication");
        List<io.gravitee.management.security.authentication.AuthenticationProvider> providers =
                authenticationProviderManager.getIdentityProviders()
                        .stream()
                        .filter(authenticationProvider -> !authenticationProvider.external())
                        .collect(Collectors.toList());

        for (io.gravitee.management.security.authentication.AuthenticationProvider provider : providers) {
            LOGGER.info("Loading authentication provider of type {} at position {}", provider.type(), provider.index());

            boolean found = false;
            Collection<IdentityProvider> identityProviders = identityProviderManager.getAll();
            for (IdentityProvider identityProvider : identityProviders) {
                if (identityProvider.type().equalsIgnoreCase(provider.type())) {
                    AuthenticationProvider authenticationProviderPlugin = identityProviderManager.loadIdentityProvider(
                            identityProvider.type(), provider.configuration());

                    if (authenticationProviderPlugin != null) {
                        Object authenticationProvider = authenticationProviderPlugin.configure();

                        if (authenticationProvider instanceof org.springframework.security.authentication.AuthenticationProvider) {
                            auth.authenticationProvider((org.springframework.security.authentication.AuthenticationProvider) authenticationProvider);
                        } else if (authenticationProvider instanceof SecurityConfigurer) {
                            auth.apply((SecurityConfigurer) authenticationProvider);
                        }
                        found = true;
                        break;
                    }
                }
            }

            if (!found) {
                LOGGER.error("No authentication provider found for type: {}", provider.type());
                throw new IllegalStateException("No authentication provider found for type: " + provider.type());
            }
        }
    }

    @Bean
    public AuthenticationSuccessListener authenticationSuccessListener() {
        return new AuthenticationSuccessListener();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        final CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOrigins(getPropertiesAsList("http.cors.allow-origin", "*"));
        config.setAllowedHeaders(getPropertiesAsList("http.cors.allow-headers", "Cache-Control, Pragma, Origin, Authorization, Content-Type, X-Requested-With, If-Match"));
        config.setAllowedMethods(getPropertiesAsList("http.cors.allow-methods", "OPTIONS, GET, POST, PUT, DELETE"));
        config.setExposedHeaders(getPropertiesAsList("http.cors.exposed-headers", "ETag"));
        config.setMaxAge(environment.getProperty("http.cors.max-age", Long.class, 1728000L));
        config.setExposedHeaders(Collections.singletonList("ETag"));

        final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private List<String> getPropertiesAsList(final String propertyKey, final String defaultValue) {
        String property = environment.getProperty(propertyKey);
        if (property == null) {
            property = defaultValue;
        }
        return asList(property.replaceAll("\\s+","").split(","));
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        final String jwtSecret = environment.getProperty("jwt.secret");
        if (jwtSecret == null || jwtSecret.isEmpty()) {
            throw new IllegalStateException("JWT secret is mandatory");
        }

        http
            .httpBasic()
                .realmName("Gravitee.io Management API")
            .and()
            .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
                .authorizeRequests()
                    .antMatchers(HttpMethod.OPTIONS, "**").permitAll()
                    .antMatchers(HttpMethod.POST, "/user/login").permitAll()
                    .antMatchers(HttpMethod.GET, "/user/").permitAll()
                    .antMatchers(HttpMethod.GET, "/user/**").authenticated()
                    .antMatchers(HttpMethod.POST, "/auth/**").permitAll()

                    // API requests
                    .antMatchers(HttpMethod.GET, "/apis/**").permitAll()
                    .antMatchers(HttpMethod.POST, "/apis").authenticated()
                    .antMatchers(HttpMethod.POST, "/apis/**").authenticated()
                    .antMatchers(HttpMethod.PUT, "/apis/**").authenticated()
                    .antMatchers(HttpMethod.DELETE, "/apis/**").authenticated()

                    // Application requests
                    .antMatchers(HttpMethod.POST, "/applications").authenticated()
                    .antMatchers(HttpMethod.POST, "/applications/**").authenticated()
                    .antMatchers(HttpMethod.PUT, "/applications/**").authenticated()
                    .antMatchers(HttpMethod.DELETE, "/applications/**").authenticated()

                    // Subscriptions
                    .antMatchers(HttpMethod.GET, "/subscriptions/**").authenticated()

                    // Instance requests
                    .antMatchers(HttpMethod.GET, "/instances/**").authenticated()

                    // Platform requests
                    .antMatchers(HttpMethod.GET, "/platform/**").authenticated()

                    // User management
                    .antMatchers(HttpMethod.POST, "/users").permitAll()
                    .antMatchers(HttpMethod.POST, "/users/register").permitAll()
                    .antMatchers(HttpMethod.GET, "/users").authenticated()
                    .antMatchers(HttpMethod.GET, "/users/**").authenticated()
                    .antMatchers(HttpMethod.PUT, "/users/**").authenticated()
                    .antMatchers(HttpMethod.DELETE, "/users/**").authenticated()

                    // Swagger
                    .antMatchers(HttpMethod.GET, "/swagger.json").permitAll()

                    // Configuration Groups
                    .antMatchers(HttpMethod.GET, "/configuration/groups/**").permitAll()

                    // Configuration Views
                    .antMatchers(HttpMethod.GET, "/configuration/views/**").permitAll()

                    // Configuration Tags
                    .antMatchers(HttpMethod.GET, "/configuration/tags/**").permitAll()

                    // Configuration Tenants
                    .antMatchers(HttpMethod.GET, "/configuration/tenants/**").permitAll()

                    // Configuration
                    .antMatchers("/configuration/**").authenticated()

                    // Portal
                    .antMatchers(HttpMethod.GET, "/portal/**").permitAll()
                    .antMatchers(HttpMethod.POST, "/portal/**").authenticated()
                    .antMatchers(HttpMethod.PUT, "/portal/**").authenticated()
                    .antMatchers(HttpMethod.DELETE, "/portal/**").authenticated()

                    // Search
                    .antMatchers(HttpMethod.GET, "/search/users").authenticated()

                    .anyRequest().authenticated()
            .and()
                .csrf()
                    .disable()
                .cors()
            .and()
                .addFilterBefore(new JWTAuthenticationFilter(jwtCookieGenerator, jwtSecret), BasicAuthenticationFilter.class)
                .addFilterAfter(new AuthenticationSuccessFilter(jwtCookieGenerator, jwtSecret, environment.getProperty("jwt.issuer", DEFAULT_JWT_ISSUER),
                                environment.getProperty("jwt.expire-after", Integer.class, DEFAULT_JWT_EXPIRE_AFTER), membershipService),
                        BasicAuthenticationFilter.class);
    }
}

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
package io.gravitee.management.security.config.basic;

import io.gravitee.management.providers.core.authentication.AuthenticationManager;
import io.gravitee.management.security.JWTCookieGenerator;
import io.gravitee.management.security.config.basic.filter.AuthenticationSuccessFilter;
import io.gravitee.management.security.config.basic.filter.CORSFilter;
import io.gravitee.management.security.config.basic.filter.JWTAuthenticationFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import javax.servlet.Filter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


/**
 * @author Titouan COMPIEGNE (titouan.compiegne at gravitee.io)
 * @author GraviteeSource Team
 */
@Configuration
@Profile("basic")
@EnableWebSecurity
public class BasicSecurityConfigurerAdapter extends WebSecurityConfigurerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(BasicSecurityConfigurerAdapter.class);

    private static final int DEFAULT_JWT_EXPIRE_AFTER = 604800;
    private static final String DEFAULT_JWT_ISSUER = "gravitee-management-auth";

    @Autowired
    private Environment environment;

    @Autowired
    private Collection<AuthenticationManager> authenticationManagers;

    @Autowired
    private JWTCookieGenerator jwtCookieGenerator;

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        LOGGER.info("Loading security providers for basic authentication");

        List<String> providers = getSecurityProviders();

        for (int idx = 0; idx < providers.size(); idx++) {
            String providerType = providers.get(idx);

            boolean found = false;
            for (AuthenticationManager authenticationManager : authenticationManagers) {
                if (authenticationManager.canHandle(providerType)) {
                    authenticationManager.configure(auth, idx);
                    found = true;
                    break;
                }
            }

            if (!found) {
                LOGGER.error("No authentication provider found for type: {}", providerType);
            }
        }
    }

    private List<String> getSecurityProviders() {
        LOGGER.debug("Looking for security provider...");
        List<String> providers = new ArrayList<>();

        boolean found = true;
        int idx = 0;

        while (found) {
            String type = environment.getProperty("security.providers[" + (idx++) + "].type");
            found = (type != null);
            if (found) {
                LOGGER.debug("\tSecurity type {} has been defined", type);
                providers.add(type);
            }
        }

        return providers;
    }

    /*
     * TODO : fix filter order between Jersey Filter (CORSResponseFilter) and
     * Spring Security Filter TODO : remove this filter or CORSResponseFilter
     * when the problem will be solved
     */
    @Bean
    public Filter corsFilter() {
        return new CORSFilter();
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
                    .antMatchers(HttpMethod.GET, "/user/**").permitAll()
                    // API requests
                    .antMatchers(HttpMethod.GET, "/apis/**").permitAll()
                    .antMatchers(HttpMethod.POST, "/apis/**").hasAnyAuthority("ADMIN", "API_PUBLISHER")
                    .antMatchers(HttpMethod.PUT, "/apis/**").hasAnyAuthority("ADMIN", "API_PUBLISHER")
                    .antMatchers(HttpMethod.DELETE, "/apis/**").hasAnyAuthority("ADMIN", "API_PUBLISHER")
                    // Application requests
                    .antMatchers(HttpMethod.POST, "/applications/**").hasAnyAuthority("ADMIN", "API_CONSUMER")
                    .antMatchers(HttpMethod.PUT, "/applications/**").hasAnyAuthority("ADMIN", "API_CONSUMER")
                    .antMatchers(HttpMethod.DELETE, "/applications/**").hasAnyAuthority("ADMIN", "API_CONSUMER")
                    // Instance requests
                    .antMatchers(HttpMethod.GET, "/instances/**").hasAuthority("ADMIN")
                    .anyRequest().authenticated()
            .and()
                .csrf()
                    .disable()
            .addFilterAfter(corsFilter(), AbstractPreAuthenticatedProcessingFilter.class)
            .addFilterBefore(new JWTAuthenticationFilter(jwtCookieGenerator, jwtSecret), BasicAuthenticationFilter.class)
            .addFilterAfter(new AuthenticationSuccessFilter(jwtCookieGenerator, jwtSecret, environment.getProperty("jwt.issuer", DEFAULT_JWT_ISSUER),
                            environment.getProperty("jwt.expire-after", Integer.class, DEFAULT_JWT_EXPIRE_AFTER)),
                    BasicAuthenticationFilter.class);
    }
}

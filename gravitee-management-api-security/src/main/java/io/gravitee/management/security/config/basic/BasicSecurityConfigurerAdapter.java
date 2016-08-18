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

import io.gravitee.management.idp.api.IdentityProvider;
import io.gravitee.management.idp.api.authentication.AuthenticationProvider;
import io.gravitee.management.idp.core.plugin.IdentityProviderManager;
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
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.SecurityConfigurer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import javax.servlet.Filter;
import java.util.*;


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
    private ConfigurableEnvironment environment;

    @Autowired
    private JWTCookieGenerator jwtCookieGenerator;

    @Autowired
    private IdentityProviderManager identityProviderManager;

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        LOGGER.info("Loading authentication identity providers for Basic authentication");
        List<String> providers = loadingAuthenticationIdentityProviders();

        for (int idx = 0; idx < providers.size(); idx++) {
            String providerType = providers.get(idx);
            LOGGER.info("Loading identity provider of type {} at position {}", providerType, idx);

            boolean found = false;
            Collection<IdentityProvider> identityProviders = identityProviderManager.getAll();
            for (IdentityProvider identityProvider : identityProviders) {
                if (identityProvider.type().equalsIgnoreCase(providerType)) {
                    AuthenticationProvider authenticationProviderPlugin = identityProviderManager.loadIdentityProvider(
                            identityProvider.type(), identityProviderProperties(idx));

                    if (authenticationProviderPlugin != null) {
                        Object authenticationProvider = authenticationProviderPlugin.configure();

                        if (authenticationProvider instanceof org.springframework.security.authentication.AuthenticationProvider) {
                            auth.authenticationProvider((org.springframework.security.authentication.AuthenticationProvider) authenticationProvider);
                        }
                        else if (authenticationProvider instanceof SecurityConfigurer) {
                            auth.apply((SecurityConfigurer) authenticationProvider);
                        }
                        found = true;
                        break;
                    }
                }
            }

            if (!found) {
                LOGGER.error("No authentication provider found for type: {}", providerType);
            }
        }
    }

    private Map<String, Object> identityProviderProperties(int idx) {
        String prefix = "security.providers[" + (idx++) + "].";
        Map<String, Object> properties = getPropertiesStartingWith(environment, prefix);
        Map<String, Object> unprefixedProperties = new HashMap<>(properties.size());
        properties.entrySet().stream().forEach(propEntry -> unprefixedProperties.put(
                propEntry.getKey().substring(prefix.length()), propEntry.getValue()));
        return unprefixedProperties;
    }

    private List<String> loadingAuthenticationIdentityProviders() {
        LOGGER.debug("Looking for authentication identity providers...");
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
                    .antMatchers(HttpMethod.POST, "/apis").hasAnyAuthority("ADMIN", "API_PUBLISHER")
                    .antMatchers(HttpMethod.POST, "/apis/**").authenticated()
                    .antMatchers(HttpMethod.PUT, "/apis/**").authenticated()
                    .antMatchers(HttpMethod.DELETE, "/apis/**").authenticated()

                    // Application requests
                    .antMatchers(HttpMethod.POST, "/applications").hasAnyAuthority("ADMIN", "API_CONSUMER")
                    .antMatchers(HttpMethod.POST, "/applications/**").authenticated()
                    .antMatchers(HttpMethod.PUT, "/applications/**").authenticated()
                    .antMatchers(HttpMethod.DELETE, "/applications/**").authenticated()

                    // Instance requests
                    .antMatchers(HttpMethod.GET, "/instances/**").hasAuthority("ADMIN")

                    // Platform requests
                    .antMatchers(HttpMethod.GET, "/platform/**").hasAuthority("ADMIN")

                    // User management
                    .antMatchers(HttpMethod.POST, "/users").permitAll()
                    .antMatchers(HttpMethod.GET, "/users").hasAnyAuthority("ADMIN")
                    .antMatchers(HttpMethod.GET, "/users/**").authenticated()
                    .antMatchers(HttpMethod.PUT, "/users/**").hasAnyAuthority("ADMIN")
                    .antMatchers(HttpMethod.DELETE, "/users/**").hasAnyAuthority("ADMIN")
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

    public static Map<String, Object> getPropertiesStartingWith(ConfigurableEnvironment aEnv, String aKeyPrefix) {
        Map<String,Object> result = new HashMap<>();
        Map<String,Object> map = getAllProperties( aEnv );

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();

            if (key.startsWith(aKeyPrefix)) {
                result.put(key, entry.getValue());
            }
        }

        return result;
    }

    public static Map<String,Object> getAllProperties(ConfigurableEnvironment aEnv) {
        Map<String,Object> result = new HashMap<>();
        aEnv.getPropertySources().forEach(ps -> addAll(result, getAllProperties(ps)));
        return result;
    }

    public static Map<String,Object> getAllProperties(PropertySource<?> aPropSource) {
        Map<String,Object> result = new HashMap<>();

        if (aPropSource instanceof CompositePropertySource) {
            CompositePropertySource cps = (CompositePropertySource) aPropSource;
            cps.getPropertySources().forEach(ps -> addAll(result, getAllProperties(ps)));
            return result;
        }

        if (aPropSource instanceof EnumerablePropertySource<?>) {
            EnumerablePropertySource<?> ps = (EnumerablePropertySource<?>) aPropSource;
            Arrays.asList(ps.getPropertyNames()).forEach(key -> result.put(key, ps.getProperty(key)));
            return result;
        }

        // note: Most descendants of PropertySource are EnumerablePropertySource. There are some
        // few others like JndiPropertySource or StubPropertySource
        LOGGER.debug( "Given PropertySource is instanceof " + aPropSource.getClass().getName()
                + " and cannot be iterated" );

        return result;
    }

    private static void addAll(Map<String, Object> aBase, Map<String, Object> aToBeAdded) {
        for (Map.Entry<String, Object> entry : aToBeAdded.entrySet()) {
            if (aBase.containsKey(entry.getKey())) {
                continue;
            }

            aBase.put(entry.getKey(), entry.getValue());
        }
    }
}

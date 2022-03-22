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
package io.gravitee.rest.api.management.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.event.EventManager;
import io.gravitee.rest.api.idp.api.IdentityProvider;
import io.gravitee.rest.api.idp.api.authentication.AuthenticationProvider;
import io.gravitee.rest.api.idp.core.plugin.IdentityProviderManager;
import io.gravitee.rest.api.security.authentication.AuthenticationProviderManager;
import io.gravitee.rest.api.security.authentication.GraviteeAuthenticationDetails;
import io.gravitee.rest.api.security.cookies.CookieGenerator;
import io.gravitee.rest.api.security.csrf.CookieCsrfSignedTokenRepository;
import io.gravitee.rest.api.security.csrf.CsrfRequestMatcher;
import io.gravitee.rest.api.security.filter.CsrfIncludeFilter;
import io.gravitee.rest.api.security.filter.RecaptchaFilter;
import io.gravitee.rest.api.security.filter.TokenAuthenticationFilter;
import io.gravitee.rest.api.security.listener.AuthenticationFailureListener;
import io.gravitee.rest.api.security.listener.AuthenticationSuccessListener;
import io.gravitee.rest.api.security.utils.AuthoritiesProvider;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.ReCaptchaService;
import io.gravitee.rest.api.service.TokenService;
import io.gravitee.rest.api.service.UserService;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.config.annotation.SecurityConfigurer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.web.cors.CorsConfigurationSource;

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
    private IdentityProviderManager identityProviderManager;

    @Autowired
    private AuthenticationProviderManager authenticationProviderManager;

    @Autowired
    private CookieGenerator cookieGenerator;

    @Autowired
    private ReCaptchaService reCaptchaService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private AuthoritiesProvider authoritiesProvider;

    @Autowired
    private ParameterService parameterService;

    @Autowired
    private EventManager eventManager;

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        LOGGER.info("--------------------------------------------------------------");
        LOGGER.info("Management API BasicSecurity Config");
        LOGGER.info("Loading authentication identity providers for Basic authentication");

        List<io.gravitee.rest.api.security.authentication.AuthenticationProvider> providers = authenticationProviderManager
            .getIdentityProviders()
            .stream()
            .filter(authenticationProvider -> !authenticationProvider.external())
            .collect(Collectors.toList());

        for (io.gravitee.rest.api.security.authentication.AuthenticationProvider provider : providers) {
            LOGGER.info("Loading authentication provider of type {} at position {}", provider.type(), provider.index());

            boolean found = false;
            Collection<IdentityProvider> identityProviders = identityProviderManager.getAll();
            for (IdentityProvider identityProvider : identityProviders) {
                if (identityProvider.type().equalsIgnoreCase(provider.type())) {
                    AuthenticationProvider authenticationProviderPlugin = identityProviderManager.loadIdentityProvider(
                        identityProvider.type(),
                        provider.configuration()
                    );

                    if (authenticationProviderPlugin != null) {
                        Object authenticationProvider = authenticationProviderPlugin.configure();

                        if (authenticationProvider instanceof org.springframework.security.authentication.AuthenticationProvider) {
                            auth.authenticationProvider(
                                (org.springframework.security.authentication.AuthenticationProvider) authenticationProvider
                            );
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
            }
        }
        LOGGER.info("--------------------------------------------------------------");
    }

    @Bean
    public AuthenticationSuccessListener authenticationSuccessListener() {
        return new AuthenticationSuccessListener();
    }

    @Bean
    public AuthenticationFailureListener authenticationFailureListener() {
        return new AuthenticationFailureListener();
    }

    @Bean
    public CookieCsrfSignedTokenRepository cookieCsrfSignedTokenRepository() {
        return new CookieCsrfSignedTokenRepository();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        return new GraviteeUrlBasedCorsConfigurationSource(parameterService, eventManager);
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        final String jwtSecret = environment.getProperty("jwt.secret");
        if (jwtSecret == null || jwtSecret.isEmpty()) {
            throw new IllegalStateException("JWT secret is mandatory");
        }

        //Warning if the secret is still the default one
        if ("myJWT4Gr4v1t33_S3cr3t".equals(jwtSecret)) {
            LOGGER.warn("");
            LOGGER.warn("##############################################################");
            LOGGER.warn("#                      SECURITY WARNING                      #");
            LOGGER.warn("##############################################################");
            LOGGER.warn("");
            LOGGER.warn("You still use the default jwt secret.");
            LOGGER.warn("This known secret can be used to impersonate anyone.");
            LOGGER.warn("Please change this value, or ask your administrator to do it !");
            LOGGER.warn("");
            LOGGER.warn("##############################################################");
            LOGGER.warn("");
        }

        authentication(http);
        session(http);
        authorizations(http);
        hsts(http);
        csrf(http);
        cors(http);

        http.addFilterBefore(
            new TokenAuthenticationFilter(jwtSecret, cookieGenerator, userService, tokenService, authoritiesProvider),
            BasicAuthenticationFilter.class
        );
        http.addFilterBefore(new RecaptchaFilter(reCaptchaService, objectMapper), TokenAuthenticationFilter.class);
    }

    private HttpSecurity authentication(HttpSecurity security) throws Exception {
        return security
            .httpBasic()
            .authenticationDetailsSource(authenticationDetailsSource())
            .realmName("Gravitee.io Management API")
            .and();
    }

    private HttpSecurity session(HttpSecurity security) throws Exception {
        return security.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and();
    }

    private HttpSecurity authorizations(HttpSecurity security) throws Exception {
        String uriOrgPrefix = "/organizations/**";
        String uriPrefix = uriOrgPrefix + "/environments/**";

        return security
            .authorizeRequests()
            /*
             * Swagger
             */
            // ---------------------------
            // TODO: DELETE THIS IN 3.18.0
            .antMatchers(HttpMethod.GET, "/swagger.json")
            .permitAll()
            // ---------------------------

            .antMatchers(HttpMethod.GET, "/openapi.*")
            .permitAll()
            .antMatchers(HttpMethod.OPTIONS, "**")
            .permitAll()
            /*
             * Global auth resources.
             */
            .antMatchers(HttpMethod.GET, "/auth/cockpit")
            .permitAll()
            /*
             * organizations resources
             */
            // Console resource
            .antMatchers(HttpMethod.GET, uriOrgPrefix + "/console/**")
            .permitAll()
            // Auth resource
            .antMatchers(HttpMethod.POST, uriOrgPrefix + "/auth/**")
            .permitAll()
            .antMatchers(HttpMethod.GET, uriOrgPrefix + "/social-identities")
            .permitAll()
            // Current user
            .antMatchers(HttpMethod.POST, uriOrgPrefix + "/user/login")
            .permitAll()
            .antMatchers(HttpMethod.GET, uriOrgPrefix + "/user/**")
            .authenticated()
            // Users management
            .antMatchers(HttpMethod.GET, uriOrgPrefix + "/users/custom-fields")
            .permitAll()
            .antMatchers(HttpMethod.POST, uriOrgPrefix + "/users/registration/**")
            .permitAll()
            .antMatchers(HttpMethod.POST, uriOrgPrefix + "/users/**/changePassword")
            .permitAll()
            .antMatchers(HttpMethod.GET, uriOrgPrefix + "/users")
            .authenticated()
            .antMatchers(HttpMethod.GET, uriOrgPrefix + "/users/**")
            .authenticated()
            .antMatchers(HttpMethod.PUT, uriOrgPrefix + "/users/**")
            .authenticated()
            .antMatchers(HttpMethod.DELETE, uriOrgPrefix + "/users/**")
            .authenticated()
            // Organization configuration
            .antMatchers(HttpMethod.GET, uriOrgPrefix + "/configuration/rolescopes/**")
            .permitAll()
            .antMatchers(HttpMethod.GET, uriOrgPrefix + "/configuration/custom-user-fields")
            .permitAll()
            .antMatchers(uriOrgPrefix + "/configuration/**")
            .authenticated()
            // Search for users
            .antMatchers(HttpMethod.GET, uriOrgPrefix + "/search/users")
            .authenticated()
            /*
             * environments resources
             */
            // DEPRECATED
            // Auth resource
            .antMatchers(HttpMethod.POST, uriPrefix + "/auth/**")
            .permitAll()
            // DEPRECATED
            // Current user
            .antMatchers(HttpMethod.POST, uriPrefix + "/user/login")
            .permitAll()
            .antMatchers(HttpMethod.GET, uriPrefix + "/user/**")
            .authenticated()
            // DEPRECATED
            // Users management
            .antMatchers(HttpMethod.GET, uriPrefix + "/users/custom-fields")
            .permitAll()
            .antMatchers(HttpMethod.POST, uriPrefix + "/users/registration/**")
            .permitAll()
            .antMatchers(HttpMethod.GET, uriPrefix + "/users")
            .authenticated()
            .antMatchers(HttpMethod.GET, uriPrefix + "/users/**")
            .authenticated()
            .antMatchers(HttpMethod.PUT, uriPrefix + "/users/**")
            .authenticated()
            .antMatchers(HttpMethod.DELETE, uriPrefix + "/users/**")
            .authenticated()
            // DEPRECATED
            // configuration
            .antMatchers(HttpMethod.GET, uriPrefix + "/configuration/rolescopes/**")
            .permitAll()
            // DEPRECATED
            // Search for users
            .antMatchers(HttpMethod.GET, uriPrefix + "/search/users")
            .authenticated()
            /*
             * environments resources
             */
            // API requests
            .antMatchers(HttpMethod.GET, uriPrefix + "/apis/hooks")
            .authenticated()
            .antMatchers(HttpMethod.GET, uriPrefix + "/apis/**")
            .authenticated()
            .antMatchers(HttpMethod.POST, uriPrefix + "/apis")
            .authenticated()
            .antMatchers(HttpMethod.POST, uriPrefix + "/apis/**")
            .authenticated()
            .antMatchers(HttpMethod.PUT, uriPrefix + "/apis/**")
            .authenticated()
            .antMatchers(HttpMethod.DELETE, uriPrefix + "/apis/**")
            .authenticated()
            // Application requests
            .antMatchers(HttpMethod.POST, uriPrefix + "/applications")
            .authenticated()
            .antMatchers(HttpMethod.POST, uriPrefix + "/applications/**")
            .authenticated()
            .antMatchers(HttpMethod.PUT, uriPrefix + "/applications/**")
            .authenticated()
            .antMatchers(HttpMethod.DELETE, uriPrefix + "/applications/**")
            .authenticated()
            // Subscriptions
            .antMatchers(HttpMethod.GET, uriPrefix + "/subscriptions/**")
            .authenticated()
            // Instance requests
            .antMatchers(HttpMethod.GET, uriPrefix + "/instances/**")
            .authenticated()
            // Platform requests
            .antMatchers(HttpMethod.GET, uriPrefix + "/platform/**")
            .authenticated()
            // Configuration Groups
            .antMatchers(HttpMethod.GET, uriPrefix + "/configuration/groups/**")
            .permitAll()
            // Configuration Categories
            .antMatchers(HttpMethod.GET, uriPrefix + "/configuration/categories/**")
            .permitAll()
            // Configuration Tags
            .antMatchers(HttpMethod.GET, uriPrefix + "/configuration/tags/**")
            .permitAll()
            // Configuration Tenants
            .antMatchers(HttpMethod.GET, uriPrefix + "/configuration/tenants/**")
            .permitAll()
            // Configuration
            .antMatchers(uriPrefix + "/configuration/**")
            .authenticated()
            // Entrypoints
            .antMatchers(HttpMethod.GET, uriPrefix + "/entrypoints/**")
            .permitAll()
            .anyRequest()
            .authenticated()
            .and();
    }

    private AuthenticationDetailsSource<HttpServletRequest, GraviteeAuthenticationDetails> authenticationDetailsSource() {
        return GraviteeAuthenticationDetails::new;
    }

    private HttpSecurity hsts(HttpSecurity security) throws Exception {
        HeadersConfigurer<HttpSecurity>.HstsConfig hstsConfig = security.headers().httpStrictTransportSecurity();

        Boolean hstsEnabled = environment.getProperty("http.hsts.enabled", Boolean.class, true);
        if (hstsEnabled) {
            return hstsConfig
                .includeSubDomains(environment.getProperty("http.hsts.include-sub-domains", Boolean.class, true))
                .maxAgeInSeconds(environment.getProperty("http.hsts.max-age", Long.class, 31536000L))
                .and()
                .and();
        }

        return hstsConfig.disable().and();
    }

    private HttpSecurity csrf(HttpSecurity security) throws Exception {
        if (environment.getProperty("http.csrf.enabled", Boolean.class, false)) {
            return security
                .csrf()
                .csrfTokenRepository(cookieCsrfSignedTokenRepository())
                .requireCsrfProtectionMatcher(new CsrfRequestMatcher())
                .and()
                .addFilterAfter(new CsrfIncludeFilter(), CsrfFilter.class);
        } else {
            // deepcode ignore DisablesCSRFProtection: CSRF Protection is disabled here to match configuration set by the user (via gravitee.yml)
            return security.csrf().disable();
        }
    }

    private HttpSecurity cors(HttpSecurity security) throws Exception {
        return security.cors().and();
    }
}

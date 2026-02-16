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
package io.gravitee.rest.api.management.v2.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.access_point.query_service.AccessPointQueryService;
import io.gravitee.apim.core.installation.domain_service.InstallationTypeDomainService;
import io.gravitee.apim.core.installation.query_service.InstallationAccessQueryService;
import io.gravitee.common.event.EventManager;
import io.gravitee.rest.api.idp.api.IdentityProvider;
import io.gravitee.rest.api.idp.api.authentication.AuthenticationProvider;
import io.gravitee.rest.api.idp.core.plugin.IdentityProviderManager;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.security.authentication.AuthenticationProviderManager;
import io.gravitee.rest.api.security.authentication.GraviteeAuthenticationDetails;
import io.gravitee.rest.api.security.config.SecureHeadersConfigurer;
import io.gravitee.rest.api.security.cookies.CookieGenerator;
import io.gravitee.rest.api.security.csrf.CookieCsrfSignedTokenRepository;
import io.gravitee.rest.api.security.filter.ContextualLoggingFilter;
import io.gravitee.rest.api.security.filter.GraviteeContextAuthorizationFilter;
import io.gravitee.rest.api.security.filter.GraviteeContextFilter;
import io.gravitee.rest.api.security.filter.RecaptchaFilter;
import io.gravitee.rest.api.security.filter.TokenAuthenticationFilter;
import io.gravitee.rest.api.security.listener.AuthenticationFailureListener;
import io.gravitee.rest.api.security.listener.AuthenticationSuccessListener;
import io.gravitee.rest.api.security.utils.AuthoritiesProvider;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.ReCaptchaService;
import io.gravitee.rest.api.service.TokenService;
import io.gravitee.rest.api.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@Configuration
@Profile("basic")
@EnableWebSecurity
public class BasicSecurityConfigurerAdapter implements SecureHeadersConfigurer {

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

    @Autowired
    private AccessPointQueryService accessPointQueryService;

    @Autowired
    private InstallationAccessQueryService installationAccessQueryService;

    @Autowired
    private InstallationTypeDomainService installationTypeDomainService;

    @Autowired
    private EnvironmentService environmentService;

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
        return new GraviteeUrlBasedCorsConfigurationSource(
            environment,
            parameterService,
            installationAccessQueryService,
            eventManager,
            ParameterReferenceType.ORGANIZATION
        );
    }

    /*
     * We don't want sonar to warn us about the hard coded jwt secret string as it is used to provide a warning
     * and encourage users to use a personal secret instead of the default one
     */
    @Bean
    @SuppressWarnings("java:S6418")
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        final String jwtSecret = environment.getProperty("jwt.secret");
        if (jwtSecret == null || jwtSecret.isEmpty()) {
            throw new IllegalStateException("JWT secret is mandatory");
        }

        //Warning if the secret is still the default one
        if ("myJWT4Gr4v1t33_S3cr3t".equals(jwtSecret)) {
            log.warn("");
            log.warn("##############################################################");
            log.warn("#                      SECURITY WARNING                      #");
            log.warn("##############################################################");
            log.warn("");
            log.warn("You still use the default jwt secret.");
            log.warn("This known secret can be used to impersonate anyone.");
            log.warn("Please change this value, or ask your administrator to do it !");
            log.warn("");
            log.warn("##############################################################");
            log.warn("");
        }

        authenticationManager(http);
        authentication(http);
        session(http);
        authorizations(http);
        cors(http);
        configure(http, environment, cookieCsrfSignedTokenRepository());

        http.addFilterBefore(
            new TokenAuthenticationFilter(jwtSecret, cookieGenerator, userService, tokenService, authoritiesProvider),
            BasicAuthenticationFilter.class
        );
        http.addFilterBefore(new RecaptchaFilter(reCaptchaService, objectMapper), TokenAuthenticationFilter.class);
        http.addFilterBefore(
            new GraviteeContextFilter(installationTypeDomainService, accessPointQueryService, environmentService),
            CorsFilter.class
        );
        http.addFilterAfter(new ContextualLoggingFilter(), GraviteeContextFilter.class);
        http.addFilterAfter(new GraviteeContextAuthorizationFilter(), AuthorizationFilter.class);

        return http.build();
    }

    private void authenticationManager(HttpSecurity http) throws Exception {
        final AuthenticationManagerBuilder auth = http.getSharedObject(AuthenticationManagerBuilder.class);

        log.info("--------------------------------------------------------------");
        log.info("Management API BasicSecurity Config");
        log.info("Loading authentication identity providers for Basic authentication");

        List<io.gravitee.rest.api.security.authentication.AuthenticationProvider> providers = authenticationProviderManager
            .getIdentityProviders()
            .stream()
            .filter(authenticationProvider -> !authenticationProvider.external())
            .collect(Collectors.toList());

        for (io.gravitee.rest.api.security.authentication.AuthenticationProvider provider : providers) {
            log.info("Loading authentication provider of type {} at position {}", provider.type(), provider.index());

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
                log.error("No authentication provider found for type: {}", provider.type());
            }
        }
        log.info("--------------------------------------------------------------");
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
            .authorizeHttpRequests()
            .requestMatchers(HttpMethod.GET, "/")
            .permitAll()
            .requestMatchers(HttpMethod.GET, "/index-*.html")
            .permitAll()
            .requestMatchers(HttpMethod.GET, "/openapi-*.yaml")
            .permitAll()
            .requestMatchers(HttpMethod.OPTIONS, "**")
            .permitAll()
            .requestMatchers(HttpMethod.GET, "/extensions/*/assets/**")
            .permitAll()
            .requestMatchers(HttpMethod.GET, "/extensions", "/extensions/")
            .permitAll()
            /*
             * Management UI resources.
             */
            .requestMatchers(HttpMethod.GET, "/ui/**")
            .permitAll()
            .requestMatchers(HttpMethod.GET, uriOrgPrefix + "/ui/**")
            .permitAll()
            // Any other request must be authenticated
            .anyRequest()
            .authenticated()
            .and();
    }

    private AuthenticationDetailsSource<HttpServletRequest, GraviteeAuthenticationDetails> authenticationDetailsSource() {
        return GraviteeAuthenticationDetails::new;
    }

    private HttpSecurity cors(HttpSecurity security) throws Exception {
        return security.cors().and();
    }
}

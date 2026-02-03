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

import io.gravitee.rest.api.security.csrf.CookieCsrfSignedTokenRepository;
import io.gravitee.rest.api.security.csrf.CsrfRequestMatcher;
import io.gravitee.rest.api.security.filter.CsrfIncludeFilter;
import java.util.Arrays;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

public interface SecureHeadersConfigurer {
    default void configure(HttpSecurity http, ConfigurableEnvironment environment, CookieCsrfSignedTokenRepository csrfTokenRepository)
        throws Exception {
        xframe(http, environment);
        csp(http, environment);
        xContentTypeOptions(http, environment);
        referrerPolicy(http, environment);
        permissionsPolicy(http, environment);
        hsts(http, environment);
        csrf(http, environment, csrfTokenRepository);
    }

    static void xframe(HttpSecurity security, ConfigurableEnvironment environment) throws Exception {
        boolean enabled = environment.getProperty("http.secureHeaders.xframe.enabled", Boolean.class, true);
        if (enabled) {
            String action = environment.getProperty("http.secureHeaders.xframe.action", "SAMEORIGIN");
            if ("SAMEORIGIN".equalsIgnoreCase(action)) {
                security.headers().frameOptions().sameOrigin();
            } else {
                security.headers().frameOptions().deny();
            }
        } else {
            security.headers().frameOptions().disable();
        }
    }

    static void csp(HttpSecurity security, ConfigurableEnvironment environment) throws Exception {
        String cspPolicy = environment.getProperty("http.secureHeaders.csp.policy");
        if (cspPolicy != null && !cspPolicy.isEmpty()) {
            security.headers().contentSecurityPolicy(policy -> policy.policyDirectives(cspPolicy));
        }
    }

    static void xContentTypeOptions(HttpSecurity security, ConfigurableEnvironment environment) throws Exception {
        boolean enabled = environment.getProperty("http.secureHeaders.xContentTypeOptions.enabled", Boolean.class, true);
        if (enabled) {
            security.headers().contentTypeOptions();
        }
    }

    static void referrerPolicy(HttpSecurity security, ConfigurableEnvironment environment) throws Exception {
        String policyString = environment.getProperty(
            "http.secureHeaders.referrerPolicy.policy",
            ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN.getPolicy()
        );
        var policy = Arrays.stream(ReferrerPolicyHeaderWriter.ReferrerPolicy.values())
            .filter(p -> p.getPolicy().equalsIgnoreCase(policyString))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Invalid referrer policy provided: '" + policyString + "'"));
        security.headers().referrerPolicy(policy);
    }

    static void permissionsPolicy(HttpSecurity security, ConfigurableEnvironment environment) throws Exception {
        String permissionsPolicy = environment.getProperty("http.secureHeaders.permissionsPolicy.policy");
        if (permissionsPolicy != null && !permissionsPolicy.isEmpty()) {
            security.headers().permissionsPolicy(policy -> policy.policy(permissionsPolicy));
        }
    }

    static void hsts(HttpSecurity security, ConfigurableEnvironment environment) throws Exception {
        HeadersConfigurer<HttpSecurity>.HstsConfig hstsConfig = security.headers().httpStrictTransportSecurity();

        Boolean hstsEnabled = environment.getProperty(
            "http.secureHeaders.hsts.enabled",
            Boolean.class,
            environment.getProperty("http.hsts.enabled", Boolean.class, true)
        );
        if (hstsEnabled) {
            hstsConfig
                .includeSubDomains(environment.getProperty("http.secureHeaders.hsts.include-sub-domains", Boolean.class, true))
                .maxAgeInSeconds(environment.getProperty("http.secureHeaders.hsts.max-age", Long.class, 31536000L));
        } else {
            hstsConfig.disable();
        }
    }

    static void csrf(HttpSecurity security, ConfigurableEnvironment environment, CookieCsrfSignedTokenRepository csrfTokenRepository)
        throws Exception {
        if (
            environment.getProperty(
                "http.secureHeaders.csrf.enabled",
                Boolean.class,
                environment.getProperty("http.csrf.enabled", Boolean.class, false)
            )
        ) {
            // Don't use deferred csrf (see https://docs.spring.io/spring-security/reference/5.8/migration/servlet/exploits.html#_i_need_to_opt_out_of_deferred_tokens_for_another_reason)
            final CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
            requestHandler.setCsrfRequestAttributeName(null);

            security
                .csrf(csrf ->
                    csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        .requireCsrfProtectionMatcher(new CsrfRequestMatcher())
                        .csrfTokenRequestHandler(requestHandler)
                        .sessionAuthenticationStrategy((authentication, request, response) -> {
                            // Force the csrf cookie to be pushed back in the response cookies to keep it across subsequent request.
                            csrfTokenRepository.saveToken((CsrfToken) request.getAttribute(CsrfToken.class.getName()), request, response);
                        })
                )
                .addFilterAfter(new CsrfIncludeFilter(), CsrfFilter.class);
        } else {
            // deepcode ignore DisablesCSRFProtection: CSRF Protection is disabled here to match configuration set by the user (via gravitee.yml)
            security.csrf(AbstractHttpConfigurer::disable);
        }
    }
}

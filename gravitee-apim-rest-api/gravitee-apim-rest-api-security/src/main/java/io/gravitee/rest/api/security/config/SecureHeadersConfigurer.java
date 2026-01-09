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

import java.util.Arrays;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

public interface SecureHeadersConfigurer {
    default void configure(HttpSecurity http, ConfigurableEnvironment environment) throws Exception {
        xframe(http, environment);
        csp(http, environment);
        xContentTypeOptions(http, environment);
        referrerPolicy(http, environment);
        permissionsPolicy(http, environment);
    }

    private void xframe(HttpSecurity security, ConfigurableEnvironment environment) throws Exception {
        boolean enabled = environment.getProperty("http.xframe.enabled", Boolean.class, true);
        if (enabled) {
            String action = environment.getProperty("http.xframe.action", "SAMEORIGIN");
            if ("SAMEORIGIN".equalsIgnoreCase(action)) {
                security.headers().frameOptions().sameOrigin();
            } else {
                security.headers().frameOptions().deny();
            }
        } else {
            security.headers().frameOptions().disable();
        }
    }

    private void csp(HttpSecurity security, ConfigurableEnvironment environment) throws Exception {
        String cspPolicy = environment.getProperty("http.csp.policy");
        if (cspPolicy != null && !cspPolicy.isEmpty()) {
            security.headers().contentSecurityPolicy(policy -> policy.policyDirectives(cspPolicy));
        }
    }

    private void xContentTypeOptions(HttpSecurity security, ConfigurableEnvironment environment) throws Exception {
        boolean enabled = environment.getProperty("http.xContentTypeOptions.enabled", Boolean.class, true);
        if (enabled) {
            security.headers().contentTypeOptions();
        }
    }

    private void referrerPolicy(HttpSecurity security, ConfigurableEnvironment environment) throws Exception {
        String policyString = environment.getProperty("http.referrerPolicy.policy");

        var policy = Arrays.stream(ReferrerPolicyHeaderWriter.ReferrerPolicy.values())
            .filter(p -> p.getPolicy().equalsIgnoreCase(policyString))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Invalid referrer policy provided: '" + policyString + "'"));
        security.headers().referrerPolicy(policy);
    }

    private void permissionsPolicy(HttpSecurity security, ConfigurableEnvironment environment) throws Exception {
        String permissionsPolicy = environment.getProperty("http.permissionsPolicy.policy");
        if (permissionsPolicy != null && !permissionsPolicy.isEmpty()) {
            security.headers().permissionsPolicy(policy -> policy.policy(permissionsPolicy));
        }
    }
}

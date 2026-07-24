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
package io.gravitee.rest.api.security.oidc;

import lombok.CustomLog;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

@CustomLog
public final class OidcIdTokenCookieCipherSecrets {

    public static final String DEDICATED_SECRET_PROPERTY = "oidc.id-token-cookie.secret";
    static final String JWT_SECRET_FALLBACK_PROPERTY = "jwt.secret";

    private OidcIdTokenCookieCipherSecrets() {}

    public static String resolveRequired(Environment environment) {
        String dedicatedSecret = environment.getProperty(DEDICATED_SECRET_PROPERTY);
        if (StringUtils.hasText(dedicatedSecret)) {
            return dedicatedSecret.trim();
        }

        String jwtSecret = environment.getProperty(JWT_SECRET_FALLBACK_PROPERTY);
        if (StringUtils.hasText(jwtSecret)) {
            log.warn(
                "Property '{}' is not set; falling back to '{}' for OIDC id_token cookie encryption. " +
                    "Prefer a dedicated secret so session signing and cookie encryption keys stay separate.",
                DEDICATED_SECRET_PROPERTY,
                JWT_SECRET_FALLBACK_PROPERTY
            );
            return jwtSecret.trim();
        }

        throw new IllegalStateException(
            "OIDC id_token cookie encryption secret is not configured. " +
                "Set '" +
                DEDICATED_SECRET_PROPERTY +
                "' (recommended) or '" +
                JWT_SECRET_FALLBACK_PROPERTY +
                "'."
        );
    }
}

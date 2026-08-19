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
package io.gravitee.rest.api.model.configuration.identity;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * How APIM authenticates itself when calling an identity provider's token and introspection endpoints. The values are
 * the OpenID Connect Discovery {@code token_endpoint_auth_method} names, so a provider's own documentation can be
 * copied across without translation.
 *
 * @author GraviteeSource Team
 */
@Schema(enumAsRef = true)
public enum ClientAuthenticationMethod {
    /** Credentials sent as HTTP Basic authentication, as {@code Authorization: Basic base64(clientId:clientSecret)}. */
    CLIENT_SECRET_BASIC("client_secret_basic"),

    /** Credentials sent as {@code client_id} and {@code client_secret} parameters in the form-encoded request body. */
    CLIENT_SECRET_POST("client_secret_post");

    private final String value;

    ClientAuthenticationMethod(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * Resolves a configured value, accepting either the OIDC name ({@code client_secret_basic}) or the enum constant
     * ({@code CLIENT_SECRET_BASIC}).
     *
     * @return the matching method, or {@code null} when the value is absent, blank or unrecognised. Callers decide what
     *         an unrecognised value means rather than having a login fail here on a configuration typo.
     */
    public static ClientAuthenticationMethod fromValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        for (ClientAuthenticationMethod method : values()) {
            if (method.value.equalsIgnoreCase(normalized) || method.name().equalsIgnoreCase(normalized)) {
                return method;
            }
        }
        return null;
    }
}

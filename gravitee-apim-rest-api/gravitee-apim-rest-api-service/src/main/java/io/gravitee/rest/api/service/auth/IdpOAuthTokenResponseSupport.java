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
package io.gravitee.rest.api.service.auth;

import java.util.Optional;

/**
 * Maps IdP OAuth2 token-endpoint values onto Gravitee login JSON, honouring expose.* installation flags.
 *
 * <p>When a flag is off, the field is sent as {@code ""} rather than omitted so OIDC clients that treat
 * {@code /auth/oauth2/{idp}} as RFC 6749 token-endpoint still receive a conformant shape.
 */
public final class IdpOAuthTokenResponseSupport {

    private IdpOAuthTokenResponseSupport() {}

    /**
     * @return empty when no {@code access_token} field should be written; otherwise the value to set (possibly {@code ""})
     */
    public static Optional<String> accessTokenField(String idpAccessToken, String idpIdToken, boolean exposeAccessToken) {
        if (idpAccessToken == null && idpIdToken == null) {
            return Optional.empty();
        }
        if (!exposeAccessToken) {
            return Optional.of("");
        }
        if (idpAccessToken == null) {
            return Optional.empty();
        }
        return Optional.of(idpAccessToken);
    }

    /**
     * @return empty when no {@code id_token} field should be written; otherwise the value to set (possibly {@code ""})
     */
    public static Optional<String> idTokenField(String idpIdToken, boolean exposeIdToken) {
        if (idpIdToken == null) {
            return Optional.empty();
        }
        if (!exposeIdToken) {
            return Optional.of("");
        }
        return Optional.of(idpIdToken);
    }
}

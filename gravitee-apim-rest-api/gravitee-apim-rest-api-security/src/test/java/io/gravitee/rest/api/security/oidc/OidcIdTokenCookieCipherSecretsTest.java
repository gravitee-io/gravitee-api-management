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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class OidcIdTokenCookieCipherSecretsTest {

    @Test
    void should_prefer_dedicated_secret_over_jwt_secret() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty(OidcIdTokenCookieCipherSecrets.DEDICATED_SECRET_PROPERTY, "dedicated-secret");
        environment.setProperty(OidcIdTokenCookieCipherSecrets.JWT_SECRET_FALLBACK_PROPERTY, "jwt-secret");

        assertThat(OidcIdTokenCookieCipherSecrets.resolveRequired(environment)).isEqualTo("dedicated-secret");
    }

    @Test
    void should_fall_back_to_jwt_secret_when_dedicated_secret_is_missing() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty(OidcIdTokenCookieCipherSecrets.JWT_SECRET_FALLBACK_PROPERTY, "jwt-secret");

        assertThat(OidcIdTokenCookieCipherSecrets.resolveRequired(environment)).isEqualTo("jwt-secret");
    }

    @Test
    void should_fail_when_no_secret_is_configured() {
        assertThatThrownBy(() -> OidcIdTokenCookieCipherSecrets.resolveRequired(new MockEnvironment()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining(OidcIdTokenCookieCipherSecrets.DEDICATED_SECRET_PROPERTY)
            .hasMessageContaining(OidcIdTokenCookieCipherSecrets.JWT_SECRET_FALLBACK_PROPERTY);
    }
}

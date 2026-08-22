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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * This is the single point where a provider's configured string becomes behaviour. An unrecognised value resolves to
 * null, which every caller reads as "use the endpoint default", so a regression here silently reverts every configured
 * provider to the behaviour the feature exists to change.
 *
 * @author GraviteeSource Team
 */
class ClientAuthenticationMethodTest {

    @ParameterizedTest
    @CsvSource(
        {
            "client_secret_basic, CLIENT_SECRET_BASIC",
            "client_secret_post, CLIENT_SECRET_POST",
            "CLIENT_SECRET_BASIC, CLIENT_SECRET_BASIC",
            "CLIENT_SECRET_POST, CLIENT_SECRET_POST",
            "Client_Secret_Basic, CLIENT_SECRET_BASIC",
        }
    )
    void should_resolve_the_oidc_name_and_the_enum_constant_in_any_case(String configured, ClientAuthenticationMethod expected) {
        assertThat(ClientAuthenticationMethod.fromValue(configured)).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = { "  client_secret_basic  ", "\tclient_secret_post\n" })
    void should_ignore_surrounding_whitespace(String configured) {
        assertThat(ClientAuthenticationMethod.fromValue(configured)).isNotNull();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "   " })
    void should_resolve_an_absent_value_to_null(String configured) {
        assertThat(ClientAuthenticationMethod.fromValue(configured)).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = { "client_secret_jwt", "none", "private_key_jwt", "basic", "client-secret-basic", "nonsense" })
    void should_resolve_an_unsupported_or_unknown_value_to_null(String configured) {
        assertThat(ClientAuthenticationMethod.fromValue(configured)).isNull();
    }

    @Test
    void should_expose_the_oidc_names_as_values() {
        assertThat(ClientAuthenticationMethod.CLIENT_SECRET_BASIC.getValue()).isEqualTo("client_secret_basic");
        assertThat(ClientAuthenticationMethod.CLIENT_SECRET_POST.getValue()).isEqualTo("client_secret_post");
    }
}

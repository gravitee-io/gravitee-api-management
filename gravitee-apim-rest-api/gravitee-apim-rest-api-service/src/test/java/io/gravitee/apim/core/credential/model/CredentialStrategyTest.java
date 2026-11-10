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
package io.gravitee.gamma.module.platform.core.credential.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.gamma.module.platform.core.credential.exception.InvalidCredentialException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class CredentialStrategyTest {

    @Test
    void should_list_the_secret_fields_of_each_strategy() {
        assertThat(CredentialStrategy.BEARER.secretFields()).containsExactly("token");
        assertThat(CredentialStrategy.API_KEY.secretFields()).containsExactly("value");
        assertThat(CredentialStrategy.OAUTH2.secretFields()).containsExactly("clientId", "clientSecret");
        assertThat(CredentialStrategy.BASIC.secretFields()).containsExactly("username", "password");
        assertThat(CredentialStrategy.SERVICE_ACCOUNT.secretFields()).containsExactly("serviceAccountKey");
    }

    @ParameterizedTest
    @EnumSource(value = CredentialStrategy.class, names = { "BEARER", "BASIC", "SERVICE_ACCOUNT" })
    void should_accept_no_settings(CredentialStrategy strategy) {
        assertThat(strategy.validateConfiguration(null)).isEmpty();
        assertThatThrownBy(() -> strategy.validateConfiguration(Map.of("audience", "x"))).isInstanceOf(InvalidCredentialException.class);
    }

    @Test
    void should_reject_a_partial_set_of_secret_fields_when_all_are_needed() {
        assertThatThrownBy(() -> CredentialStrategy.OAUTH2.requireAllSecretFields(Set.of("clientId")))
            .isInstanceOf(InvalidCredentialException.class)
            .hasMessageContaining("clientSecret");
    }

    @Nested
    class Api_key {

        @Test
        void should_keep_the_header_name() {
            assertThat(CredentialStrategy.API_KEY.validateConfiguration(Map.of("headerName", "X-Api-Key"))).isEqualTo(
                Map.of("headerName", "X-Api-Key")
            );
        }

        @Test
        void should_require_the_header_name() {
            assertThatThrownBy(() -> CredentialStrategy.API_KEY.validateConfiguration(Map.of()))
                .isInstanceOf(InvalidCredentialException.class)
                .hasMessageContaining("headerName");
        }

        @ParameterizedTest
        @ValueSource(strings = { "", "X Api Key", "X-Api-Key:", "Bad\nHeader" })
        void should_reject_a_header_name_that_is_not_a_token(String headerName) {
            assertThatThrownBy(() -> CredentialStrategy.API_KEY.validateConfiguration(Map.of("headerName", headerName))).isInstanceOf(
                InvalidCredentialException.class
            );
        }
    }

    @Nested
    class Oauth2 {

        @Test
        void should_keep_the_token_url_scopes_and_audience() {
            var settings = CredentialStrategy.OAUTH2.validateConfiguration(
                Map.of("tokenUrl", "https://login.example.com/token", "scopes", List.of("read", "write"), "audience", "api://llm")
            );

            assertThat(settings).isEqualTo(
                Map.of("tokenUrl", "https://login.example.com/token", "scopes", List.of("read", "write"), "audience", "api://llm")
            );
        }

        @Test
        void should_require_the_token_url() {
            assertThatThrownBy(() -> CredentialStrategy.OAUTH2.validateConfiguration(Map.of()))
                .isInstanceOf(InvalidCredentialException.class)
                .hasMessageContaining("tokenUrl");
        }

        @ParameterizedTest
        @ValueSource(strings = { "login.example.com/token", "ftp://login.example.com/token", "https://", "not a url" })
        void should_reject_a_token_url_that_is_not_http(String tokenUrl) {
            assertThatThrownBy(() -> CredentialStrategy.OAUTH2.validateConfiguration(Map.of("tokenUrl", tokenUrl))).isInstanceOf(
                InvalidCredentialException.class
            );
        }

        @ParameterizedTest
        @ValueSource(strings = { "https://alice:s3cr3t@login.example.com/token", "https://alice@login.example.com/token" })
        void should_reject_a_token_url_that_carries_a_user_or_password(String tokenUrl) {
            assertThatThrownBy(() -> CredentialStrategy.OAUTH2.validateConfiguration(Map.of("tokenUrl", tokenUrl)))
                .isInstanceOf(InvalidCredentialException.class)
                .hasMessageNotContaining("s3cr3t");
        }

        @Test
        void should_reject_scopes_that_are_not_a_list_of_text() {
            assertThatThrownBy(() ->
                CredentialStrategy.OAUTH2.validateConfiguration(Map.of("tokenUrl", "https://login.example.com/token", "scopes", "read"))
            ).isInstanceOf(InvalidCredentialException.class);
            assertThatThrownBy(() ->
                CredentialStrategy.OAUTH2.validateConfiguration(
                    Map.of("tokenUrl", "https://login.example.com/token", "scopes", List.of("read", 3))
                )
            ).isInstanceOf(InvalidCredentialException.class);
        }

        @Test
        void should_reject_an_unknown_setting() {
            assertThatThrownBy(() ->
                CredentialStrategy.OAUTH2.validateConfiguration(Map.of("tokenUrl", "https://login.example.com/token", "grantType", "x"))
            )
                .isInstanceOf(InvalidCredentialException.class)
                .hasMessageContaining("grantType");
        }
    }
}

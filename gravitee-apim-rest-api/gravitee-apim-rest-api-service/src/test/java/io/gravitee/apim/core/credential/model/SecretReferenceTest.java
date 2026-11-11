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
package io.gravitee.apim.core.credential.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.apim.core.credential.exception.InvalidCredentialException;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SecretReferenceTest {

    @Test
    void should_keep_an_external_reference_as_typed() {
        var reference = SecretReference.external("token", "/vault/secret/openai:token?renewable=true");

        assertThat(reference.reference()).isEqualTo("/vault/secret/openai:token?renewable=true");
        assertThat(reference.isManaged()).isFalse();
    }

    @Test
    void should_ask_the_gateway_to_renew_and_reload_a_managed_reference() {
        var reference = SecretReference.managed("/gravitee/credential.c1.token:value", "secret-1");

        assertThat(reference.reference()).isEqualTo("/gravitee/credential.c1.token:value?renewable=true&reloadOnChange=true");
        assertThat(reference.secretId()).isEqualTo("secret-1");
        assertThat(reference.isManaged()).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "vault/secret", "/vault/my secret", "/vault/{#context.attributes['k']}", "{#secrets.get('/vault/k')}" })
    void should_reject_an_invalid_external_reference(String reference) {
        assertThatThrownBy(() -> SecretReference.external("token", reference))
            .isInstanceOf(InvalidCredentialException.class)
            .hasMessageContaining("token");
    }

    @Test
    void should_reject_an_external_reference_longer_than_2048_characters() {
        assertThatThrownBy(() -> SecretReference.external("token", "/" + "a".repeat(2048))).isInstanceOf(InvalidCredentialException.class);
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "/gravitee/credential.other.token:value",
            "/GRAVITEE/credential.other.token:value?renewable=true",
            "/gravitee/credential.other.token",
            "/gravitee/credential.",
        }
    )
    void should_reject_a_reference_to_the_secret_of_a_credential(String reference) {
        assertThatThrownBy(() -> SecretReference.external("token", reference))
            .isInstanceOf(InvalidCredentialException.class)
            .hasMessageContaining("another credential");
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "/gravitee/\u0000credential.other.token:value",
            "/gravitee/\u0001credential.other.token:value",
            "/gravitee/\u001fcredential.other.token:value",
            "/gravitee/\u007fcredential.other.token:value",
            "/gravitee/\u00a0credential.other.token:value",
            "/gravitee/\u2028credential.other.token:value",
            "/vault/secret/my\u0001key:value",
        }
    )
    void should_reject_a_reference_with_a_control_or_unicode_space_character(String reference) {
        assertThatThrownBy(() -> SecretReference.external("token", reference))
            .isInstanceOf(InvalidCredentialException.class)
            .hasMessageContaining("no spaces");
    }

    @ParameterizedTest
    @ValueSource(strings = { "/gravitee/my-credential.token:value", "/vault/credential.other.token:value", "/gravitee/Credential.x:value" })
    void should_accept_a_reference_that_only_looks_like_a_credential_secret(String reference) {
        assertThat(SecretReference.external("token", reference).reference()).isEqualTo(reference);
    }

    @ParameterizedTest
    @CsvSource(
        {
            "/gravitee/my-secret:value,true",
            "/Gravitee/my-secret:value,true",
            "/graviteeio/my-secret:value,false",
            "/vault/gravitee/x:y,false",
            "/gravitee,false",
        }
    )
    void should_tell_whether_a_reference_names_the_gravitee_secret_store(String reference, boolean inGraviteeStore) {
        assertThat(SecretReference.external("token", reference).isInGraviteeStore()).isEqualTo(inGraviteeStore);
    }
}

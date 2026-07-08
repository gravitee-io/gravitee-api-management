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

import java.util.Optional;
import org.junit.jupiter.api.Test;

class OidcIdTokenCookieCipherTest {

    private final OidcIdTokenCookieCipher cipher = new OidcIdTokenCookieCipher("test-secret");

    @Test
    void should_round_trip_encrypt_and_decrypt() {
        Optional<String> encrypted = cipher.encrypt("some.id.token");

        assertThat(encrypted).isPresent();
        assertThat(encrypted.get()).doesNotContain("some.id.token");

        assertThat(cipher.decrypt(encrypted.get())).contains("some.id.token");
    }

    @Test
    void should_produce_different_ciphertext_for_same_input() {
        // IV is randomized per call, so two encryptions of the same token must differ
        Optional<String> first = cipher.encrypt("some.id.token");
        Optional<String> second = cipher.encrypt("some.id.token");

        assertThat(first).isPresent();
        assertThat(second).isPresent();
        assertThat(first.get()).isNotEqualTo(second.get());
    }

    @Test
    void should_return_empty_for_blank_or_null_input() {
        assertThat(cipher.encrypt(null)).isEmpty();
        assertThat(cipher.encrypt("")).isEmpty();
        assertThat(cipher.encrypt("   ")).isEmpty();

        assertThat(cipher.decrypt(null)).isEmpty();
        assertThat(cipher.decrypt("")).isEmpty();
    }

    @Test
    void should_fail_to_decrypt_with_different_secret() {
        Optional<String> encrypted = cipher.encrypt("some.id.token");
        assertThat(encrypted).isPresent();

        OidcIdTokenCookieCipher otherCipher = new OidcIdTokenCookieCipher("different-secret");
        assertThat(otherCipher.decrypt(encrypted.get())).isEmpty();
    }

    @Test
    void should_return_empty_for_garbage_input() {
        assertThat(cipher.decrypt("not-a-valid-encrypted-payload")).isEmpty();
    }
}

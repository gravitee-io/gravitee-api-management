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
package io.gravitee.repository.mongodb.management.internal.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SecretMongoTest {

    @Test
    void should_keep_the_encrypted_value_out_of_to_string() {
        var secret = new SecretMongo();
        secret.setId("secret-1");
        secret.setKey("anthropic-key");
        secret.setEncryptedValue("ciphertext-1");

        assertThat(secret.toString()).contains("secret-1", "anthropic-key").doesNotContain("ciphertext-1");
    }
}

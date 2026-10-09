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
package io.gravitee.apim.infra.json.jackson;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EncryptedPropertyAccessPatchTest {

    @Test
    void should_list_each_encrypted_property_as_an_access_operation_with_the_fingerprint_of_its_ciphertext() {
        var patch = EncryptedPropertyAccessPatch.of(Map.of("secret-b", "NEW-CIPHER", "secret-a", "CIPHER"));

        assertThat(patch).isEqualTo(
            """
            [{"op":"access","path":"/properties/secret-a","value":{"value":"<sha256:e555a71f0ce4ab12bc3de31adda7979c753fa3f9edd36e8cd8929d5bd4b7e906>","encrypted":true}},\
            {"op":"access","path":"/properties/secret-b","value":{"value":"<sha256:bc66d34feaf1dd7d7ecd90ba7834e5cb554ba533abff6cb0664c6cec6647ad57>","encrypted":true}}]"""
        );
    }

    @Test
    void should_escape_the_key_in_the_path() {
        var patch = EncryptedPropertyAccessPatch.of(Map.of("a/b~c", "CIPHER"));

        assertThat(patch).contains("\"path\":\"/properties/a~1b~0c\"");
    }

    @Test
    void should_record_only_the_encrypted_flag_of_a_property_without_ciphertext() {
        Map<String, String> ciphertextByKey = new HashMap<>();
        ciphertextByKey.put("secret", null);

        var patch = EncryptedPropertyAccessPatch.of(ciphertextByKey);

        assertThat(patch).isEqualTo(
            """
            [{"op":"access","path":"/properties/secret","value":{"encrypted":true}}]"""
        );
    }
}

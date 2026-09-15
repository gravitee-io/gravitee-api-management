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
package io.gravitee.gamma.definition.credential;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CredentialWireTest {

    private final ObjectMapper om = new ObjectMapper();

    @Test
    void should_round_trip_through_json() throws Exception {
        Credential c = Credential.builder()
            .id("credential-1")
            .environmentId("env-1")
            .encryptedSecret("ciphertext")
            .updatedAt("2026-09-14T00:00:00Z")
            .allowedApiIds(Set.of("api-1", "api-2"))
            .build();

        Credential back = om.readValue(om.writeValueAsString(c), Credential.class);

        assertThat(back).isEqualTo(c);
    }

    @Test
    void should_keep_the_encrypted_secret_out_of_toString() {
        Credential c = Credential.builder().id("credential-1").environmentId("env-1").encryptedSecret("ciphertext").build();

        assertThat(c.toString()).contains("credential-1").doesNotContain("ciphertext");
    }
}

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
package io.gravitee.gamma.module.platform.rest.resource.mapper.credential;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.gamma.module.platform.core.credential.model.Credential;
import io.gravitee.gamma.module.platform.core.credential.model.CredentialStrategy;
import io.gravitee.gamma.module.platform.core.credential.model.SecretReference;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class CredentialDtoMapperTest {

    @Test
    void should_write_every_timestamp_as_an_iso_8601_string_in_utc_through_the_host_mapper() throws Exception {
        var credential = Credential.builder()
            .id("credential-1")
            .organizationId("org-id")
            .environmentId("env-id")
            .name("OpenAI")
            .strategy(CredentialStrategy.BEARER)
            .ownerId("owner-id")
            .configuration(Map.of())
            .secretReferences(Map.of("token", new SecretReference("/vault/secret/openai:token", null)))
            .expiresAt(Instant.parse("2030-01-01T00:00:00Z"))
            .createdAt(Instant.parse("2026-11-10T12:13:39.859Z"))
            .updatedAt(Instant.parse("2026-11-10T12:14:50.823Z"))
            .rotatedAt(Instant.parse("2026-11-10T12:14:50.823Z"))
            .build();

        var json = new GraviteeMapper().readTree(new GraviteeMapper().writeValueAsString(CredentialDtoMapper.toResponse(credential)));

        assertThat(json.get("expiresAt").asText()).isEqualTo("2030-01-01T00:00:00Z");
        assertThat(json.get("createdAt").asText()).isEqualTo("2026-11-10T12:13:39.859Z");
        assertThat(json.get("updatedAt").asText()).isEqualTo("2026-11-10T12:14:50.823Z");
        assertThat(json.get("rotatedAt").asText()).isEqualTo("2026-11-10T12:14:50.823Z");
    }
}

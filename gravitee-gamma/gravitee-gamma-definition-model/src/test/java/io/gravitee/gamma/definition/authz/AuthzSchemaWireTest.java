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
package io.gravitee.gamma.definition.authz;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AuthzSchemaWireTest {

    private final ObjectMapper om = new ObjectMapper();

    @Test
    void a_schema_document_round_trips_through_json() throws Exception {
        AuthzSchema s = AuthzSchema.builder()
            .id("s1")
            .name("fleet schema")
            .schemaText("entity User;")
            .environmentId("env-1")
            .updatedAt("2026-08-24T00:00:00Z")
            .targetPdpIds(Set.of("api-a", "stock@eu"))
            .build();

        AuthzSchema back = om.readValue(om.writeValueAsString(s), AuthzSchema.class);

        assertThat(back).isEqualTo(s);
        assertThat(back.getTargetPdpIds()).containsExactlyInAnyOrder("api-a", "stock@eu");
    }

    @Test
    void targetPdpIds_absent_in_json_deserializes_to_null() throws Exception {
        AuthzSchema back = om.readValue("{\"id\":\"s1\",\"name\":\"n\",\"schemaText\":\"entity User;\"}", AuthzSchema.class);

        assertThat(back.getTargetPdpIds()).isNull();
    }

    @Test
    void the_schema_source_is_serialised_as_schemaText() throws Exception {
        AuthzSchema s = AuthzSchema.builder().id("s1").name("n").schemaText("entity User;").build();

        assertThat(om.writeValueAsString(s)).contains("\"schemaText\"");
    }
}

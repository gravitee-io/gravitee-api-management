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
import org.junit.jupiter.api.Test;

class AuthzPdpTest {

    @Test
    void round_trips_through_json_preserving_scope_id() throws Exception {
        ObjectMapper om = new ObjectMapper();
        AuthzPdp pdp = AuthzPdp.builder()
            .id("pdp-1")
            .name("default")
            .targetPdpId("api-a")
            .environmentId("env-1")
            .tag("eu")
            .updatedAt("2024-01-01T00:00:00Z")
            .build();
        String json = om.writeValueAsString(pdp);
        AuthzPdp back = om.readValue(json, AuthzPdp.class);
        assertThat(back.getId()).isEqualTo("pdp-1");
        assertThat(back.getName()).isEqualTo("default");
        assertThat(back.getTargetPdpId()).isEqualTo("api-a");
        assertThat(back.getEnvironmentId()).isEqualTo("env-1");
        assertThat(back.getTag()).isEqualTo("eu");
        assertThat(back.getUpdatedAt()).isEqualTo("2024-01-01T00:00:00Z");
    }

    @Test
    void tag_is_serialized_as_tag_and_nullable() throws Exception {
        ObjectMapper om = new ObjectMapper();
        AuthzPdp tagged = AuthzPdp.builder().id("pdp-1").name("n").targetPdpId("api-a").tag("eu").build();
        assertThat(om.writeValueAsString(tagged)).contains("\"tag\":\"eu\"");

        AuthzPdp back = om.readValue("{\"id\":\"pdp-1\",\"name\":\"n\",\"targetPdpId\":\"api-a\"}", AuthzPdp.class);
        assertThat(back.getTag()).isNull();
    }

    @Test
    void scope_id_is_serialized_as_targetPdpId() throws Exception {
        ObjectMapper om = new ObjectMapper();
        AuthzPdp pdp = AuthzPdp.builder().id("pdp-1").name("n").targetPdpId("api-a").build();
        String json = om.writeValueAsString(pdp);
        assertThat(json).contains("\"targetPdpId\":\"api-a\"");
        assertThat(json).doesNotContain("tenantId");
    }

    @Test
    void unknown_legacy_fields_are_ignored_when_mapper_configured() throws Exception {
        ObjectMapper om = new ObjectMapper().configure(
            com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
            false
        );
        AuthzPdp back = om.readValue("{\"id\":\"pdp-1\",\"name\":\"n\",\"targetPdpId\":\"api-a\",\"legacy\":\"x\"}", AuthzPdp.class);
        assertThat(back.getTargetPdpId()).isEqualTo("api-a");
    }
}

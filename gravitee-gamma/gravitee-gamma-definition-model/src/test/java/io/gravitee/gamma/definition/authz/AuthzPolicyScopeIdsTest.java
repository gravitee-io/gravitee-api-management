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

class AuthzPolicyTargetPdpIdsTest {

    @Test
    void targetPdpIds_round_trips_through_json() throws Exception {
        ObjectMapper om = new ObjectMapper();
        AuthzPolicy p = AuthzPolicy.builder()
            .id("p1")
            .name("n")
            .kind(AuthzPolicyKind.GLOBAL)
            .policyText("permit(principal, action, resource);")
            .environmentId("env-1")
            .targetPdpIds(Set.of("api-a", "*"))
            .build();
        String json = om.writeValueAsString(p);
        AuthzPolicy back = om.readValue(json, AuthzPolicy.class);
        assertThat(back.getTargetPdpIds()).containsExactlyInAnyOrder("api-a", "*");
    }

    @Test
    void targetPdpIds_absent_in_legacy_json_deserializes_to_null() throws Exception {
        ObjectMapper om = new ObjectMapper();
        AuthzPolicy back = om.readValue(
            "{\"id\":\"p1\",\"name\":\"n\",\"kind\":\"GLOBAL\",\"policyText\":\"permit(principal, action, resource);\"}",
            AuthzPolicy.class
        );
        assertThat(back.getTargetPdpIds()).isNull();
    }
}

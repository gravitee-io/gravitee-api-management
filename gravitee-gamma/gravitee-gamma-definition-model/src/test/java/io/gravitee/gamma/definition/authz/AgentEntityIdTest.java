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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AgentEntityIdTest {

    @Test
    void derives_agent_prefix_domain_and_name_uuid_of_domain_slash_client_id() {
        String domain = "domain-1";
        String clientId = "agent-client";

        String id = AgentEntityId.derive(domain, clientId);

        String expectedLeaf = UUID.nameUUIDFromBytes("domain-1/agent-client".getBytes(StandardCharsets.UTF_8)).toString();
        assertThat(id).isEqualTo("agent-identity.domain-1." + expectedLeaf);
    }

    @Test
    void produces_an_id_that_satisfies_the_entity_id_grammar() {
        // The leaf is a lowercase-hex UUID, so even a client_id that is otherwise illegal in the
        // grammar (mixed case, slashes, dots — e.g. a CIMD URL) yields a valid entity id.
        String id = AgentEntityId.derive("domain-1", "https://Issuer.Example.com/Agents/Bot-42");

        assertThat(Pattern.matches(AuthzEntityIdConstants.FORMAT_REGEX, id)).isTrue();
    }

    @Test
    void is_stable_for_the_same_domain_and_client_id() {
        assertThat(AgentEntityId.derive("d", "c")).isEqualTo(AgentEntityId.derive("d", "c"));
    }

    @Test
    void differs_when_the_domain_differs() {
        assertThat(AgentEntityId.derive("d1", "c")).isNotEqualTo(AgentEntityId.derive("d2", "c"));
    }

    @Test
    void differs_when_the_client_id_differs() {
        assertThat(AgentEntityId.derive("d", "c1")).isNotEqualTo(AgentEntityId.derive("d", "c2"));
    }

    @Test
    void rejects_a_null_or_blank_client_id() {
        assertThatThrownBy(() -> AgentEntityId.derive("d", null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AgentEntityId.derive("d", "  ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_a_null_or_blank_domain() {
        assertThatThrownBy(() -> AgentEntityId.derive(null, "c")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AgentEntityId.derive("  ", "c")).isInstanceOf(IllegalArgumentException.class);
    }
}

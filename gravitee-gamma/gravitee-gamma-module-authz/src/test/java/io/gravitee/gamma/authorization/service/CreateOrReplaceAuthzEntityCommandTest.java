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
package io.gravitee.gamma.authorization.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.gamma.authorization.domain.AuthzEntityKind;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CreateOrReplaceAuthzEntityCommandTest {

    private static CreateOrReplaceAuthzEntityCommand cmd(String entityId, AuthzEntityKind kind, String entityType, Map<String, Object> attrs) {
        return new CreateOrReplaceAuthzEntityCommand("env-1", entityId, kind, entityType, attrs, List.of(), "src");
    }

    @Test
    void derives_principal_type_from_kind_attribute_when_unset() {
        assertThat(cmd("alice", AuthzEntityKind.PRINCIPAL, null, Map.of("_kind", "user")).entityType()).isEqualTo("User");
        assertThat(cmd("eng", AuthzEntityKind.PRINCIPAL, null, Map.of("_kind", "group")).entityType()).isEqualTo("Group");
        assertThat(cmd("x", AuthzEntityKind.PRINCIPAL, null, Map.of("_kind", "agent-identity")).entityType()).isEqualTo("AgentIdentity");
    }

    @Test
    void derives_resource_type_from_entityid_prefix_when_unset() {
        assertThat(cmd("mcp.weather", AuthzEntityKind.RESOURCE, null, Map.of()).entityType()).isEqualTo("MCPServer");
        assertThat(cmd("api.payments", AuthzEntityKind.RESOURCE, null, Map.of()).entityType()).isEqualTo("API");
        assertThat(cmd("mcptool.search", AuthzEntityKind.RESOURCE, null, Map.of()).entityType()).isEqualTo("MCPTool");
    }

    @Test
    void keeps_an_explicitly_supplied_entityType() {
        assertThat(cmd("mcp.weather", AuthzEntityKind.RESOURCE, "CustomThing", Map.of()).entityType()).isEqualTo("CustomThing");
    }

    @Test
    void falls_back_to_kind_default_when_no_signal() {
        assertThat(cmd("alice", AuthzEntityKind.PRINCIPAL, null, Map.of()).entityType()).isEqualTo("Principal");
        assertThat(cmd("webhook.x", AuthzEntityKind.RESOURCE, null, Map.of()).entityType()).isEqualTo("Resource");
        assertThat(cmd("x", AuthzEntityKind.PRINCIPAL, null, Map.of("_kind", "dupa")).entityType()).isEqualTo("Principal");
    }

    @Test
    void prefers_kind_attribute_over_entityid_prefix() {
        assertThat(cmd("user.alice", AuthzEntityKind.PRINCIPAL, null, Map.of("_kind", "user")).entityType()).isEqualTo("User");
    }
}

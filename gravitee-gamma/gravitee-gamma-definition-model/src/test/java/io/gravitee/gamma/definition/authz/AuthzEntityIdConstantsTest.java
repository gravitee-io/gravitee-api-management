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

import org.junit.jupiter.api.Test;

class AuthzEntityIdConstantsTest {

    @Test
    void maps_principal_kind_hints_to_engine_types() {
        assertThat(AuthzEntityIdConstants.engineTypeForHint("user")).isEqualTo("User");
        assertThat(AuthzEntityIdConstants.engineTypeForHint("group")).isEqualTo("Group");
        assertThat(AuthzEntityIdConstants.engineTypeForHint("role")).isEqualTo("Role");
        assertThat(AuthzEntityIdConstants.engineTypeForHint("agent-identity")).isEqualTo("AgentIdentity");
    }

    @Test
    void maps_resource_prefixes_to_engine_types() {
        assertThat(AuthzEntityIdConstants.engineTypeForHint("mcp")).isEqualTo("MCPServer");
        assertThat(AuthzEntityIdConstants.engineTypeForHint("mcptool")).isEqualTo("MCPTool");
        assertThat(AuthzEntityIdConstants.engineTypeForHint("model")).isEqualTo("Model");
        assertThat(AuthzEntityIdConstants.engineTypeForHint("api")).isEqualTo("API");
        assertThat(AuthzEntityIdConstants.engineTypeForHint("agent")).isEqualTo("Agent");
    }

    @Test
    void accepts_aliases_and_is_case_insensitive() {
        assertThat(AuthzEntityIdConstants.engineTypeForHint("service-account")).isEqualTo("ServiceAccount");
        assertThat(AuthzEntityIdConstants.engineTypeForHint("LLM")).isEqualTo("Model");
        assertThat(AuthzEntityIdConstants.engineTypeForHint("MCPServer")).isEqualTo("MCPServer");
    }

    @Test
    void returns_null_for_unknown_or_blank_hint() {
        assertThat(AuthzEntityIdConstants.engineTypeForHint("dupa")).isNull();
        assertThat(AuthzEntityIdConstants.engineTypeForHint("")).isNull();
        assertThat(AuthzEntityIdConstants.engineTypeForHint(null)).isNull();
    }

    @Test
    void toEngineUid_builds_the_canonical_quoted_form() {
        assertThat(AuthzEntityIdConstants.toEngineUid("User", "alice")).isEqualTo("User::\"alice\"");
        assertThat(AuthzEntityIdConstants.toEngineUid("API", "api.payments")).isEqualTo("API::\"api.payments\"");
    }

    @Test
    void normalizeParentUid_requotes_unquoted_typed_uids() {
        assertThat(AuthzEntityIdConstants.normalizeParentUid("Group::engineering")).isEqualTo("Group::\"engineering\"");
        // Namespaced type: split on the LAST '::' so the type keeps its inner '::'.
        assertThat(AuthzEntityIdConstants.normalizeParentUid("myapp::Report::r1")).isEqualTo("myapp::Report::\"r1\"");
    }

    @Test
    void normalizeParentUid_leaves_canonical_bare_and_null_untouched() {
        assertThat(AuthzEntityIdConstants.normalizeParentUid("Group::\"engineering\"")).isEqualTo("Group::\"engineering\"");
        assertThat(AuthzEntityIdConstants.normalizeParentUid("bare-id")).isEqualTo("bare-id");
        assertThat(AuthzEntityIdConstants.normalizeParentUid(null)).isNull();
    }

    @Test
    void normalizeParentUid_does_not_manufacture_empty_component_uids() {
        // Malformed Type::/::id must pass through unchanged, not become Type::"" / ::"id".
        assertThat(AuthzEntityIdConstants.normalizeParentUid("Group::")).isEqualTo("Group::");
        assertThat(AuthzEntityIdConstants.normalizeParentUid("::engineering")).isEqualTo("::engineering");
    }
}

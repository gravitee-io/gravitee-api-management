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
package io.gravitee.gamma.definition.entityid;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class EntityIdTest {

    @Test
    void parses_on_the_first_dot_so_a_scoped_slug_keeps_its_parent() {
        EntityId id = EntityId.parse("mcp-tool.github.list_pull_request");
        assertThat(id.type()).isEqualTo("mcp-tool");
        assertThat(id.slug()).isEqualTo("github.list_pull_request");
        assertThat(id.lastSegment()).isEqualTo("list_pull_request");
        assertThat(id.value()).isEqualTo("mcp-tool.github.list_pull_request");
    }

    @Test
    void last_segment_of_a_top_level_id_is_the_whole_slug() {
        assertThat(EntityId.parse("mcp-server.github").lastSegment()).isEqualTo("github");
    }

    @Test
    void of_slugs_the_name() {
        assertThat(EntityId.of("model", "GPT-4o").value()).isEqualTo("model.gpt-4o");
    }

    @Test
    void scoped_builds_the_hierarchical_form() {
        EntityId id = EntityId.scoped("mcp-tool", "github", "list_pull_request");
        assertThat(id.value()).isEqualTo("mcp-tool.github.list_pull_request");
        assertThat(id.lastSegment()).isEqualTo("list_pull_request");
    }

    @Test
    void rejects_strings_without_a_usable_dot() {
        assertThatThrownBy(() -> EntityId.parse("github")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EntityId.parse(".github")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EntityId.parse("mcp-server.")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_a_name_that_produces_no_slug() {
        assertThatThrownBy(() -> EntityId.of("model", "...")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_segments_that_are_not_slugs() {
        assertThatThrownBy(() -> EntityId.parse("MCP.foo")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EntityId.parse("model.Foo Bar")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void is_valid_checks_the_full_format() {
        assertThat(EntityId.isValid("mcp-tool.github.list_pull_request")).isTrue();
        assertThat(EntityId.isValid("github")).isFalse();
        assertThat(EntityId.isValid("model.GPT")).isFalse();
        assertThat(EntityId.isValid(null)).isFalse();
    }

    @Test
    void serializes_to_the_string_form_and_back() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        EntityId id = EntityId.parse("mcp-tool.github.list_pull_request");
        String json = mapper.writeValueAsString(id);
        assertThat(json).isEqualTo("\"mcp-tool.github.list_pull_request\"");
        assertThat(mapper.readValue(json, EntityId.class)).isEqualTo(id);
    }
}

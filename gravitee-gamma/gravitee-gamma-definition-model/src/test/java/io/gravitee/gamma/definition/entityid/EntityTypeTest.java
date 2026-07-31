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

import org.junit.jupiter.api.Test;

class EntityTypeTest {

    @Test
    void name_seeded_builds_a_top_level_id() {
        assertThat(EntityType.nameSeeded("model").id("GPT-4o").value()).isEqualTo("model.gpt-4o");
    }

    @Test
    void scoped_builds_a_hierarchical_id() {
        assertThat(EntityType.scoped("mcp-tool", "mcp-server").id("github", "list_pull_request").value()).isEqualTo(
            "mcp-tool.github.list_pull_request"
        );
    }

    @Test
    void name_seeded_rejects_a_parent_slug() {
        assertThatThrownBy(() -> EntityType.nameSeeded("model").id("parent", "x")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void scoped_requires_a_parent_slug() {
        assertThatThrownBy(() -> EntityType.scoped("mcp-tool", "mcp-server").id("x")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_a_token_that_is_not_a_slug() {
        assertThatThrownBy(() -> EntityType.nameSeeded("MCP")).isInstanceOf(IllegalArgumentException.class);
    }
}

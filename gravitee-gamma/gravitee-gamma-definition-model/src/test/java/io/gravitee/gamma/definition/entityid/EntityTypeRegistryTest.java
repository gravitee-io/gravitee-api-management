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

import java.util.List;
import org.junit.jupiter.api.Test;

class EntityTypeRegistryTest {

    @Test
    void default_registry_is_valid_and_exposes_its_tokens() {
        assertThat(EntityTypeRegistry.DEFAULT.all())
            .extracting(EntityType::token)
            .contains("mcp-server", "mcp-tool", "mcp-proxy", "llm-proxy", "a2a-proxy", "agent-identity");
    }

    @Test
    void records_family_and_parent() {
        assertThat(EntityTypeRegistry.DEFAULT.find("mcp-tool")).hasValueSatisfying(type -> {
            assertThat(type.family()).isEqualTo(IdentityFamily.NAME_SEEDED);
            assertThat(type.parentToken()).isEqualTo("mcp-server");
            assertThat(type.isScoped()).isTrue();
        });
        assertThat(EntityTypeRegistry.DEFAULT.find("agent-identity")).hasValueSatisfying(type ->
            assertThat(type.family()).isEqualTo(IdentityFamily.RECOMPUTABLE)
        );
    }

    @Test
    void every_scoped_parent_is_registered() {
        for (EntityType type : EntityTypeRegistry.DEFAULT.all()) {
            if (type.isScoped()) {
                assertThat(EntityTypeRegistry.DEFAULT.contains(type.parentToken())).isTrue();
            }
        }
    }

    @Test
    void find_is_empty_for_unknown_token() {
        assertThat(EntityTypeRegistry.DEFAULT.find("mcp")).isEmpty();
    }

    @Test
    void resolve_parses_then_looks_up_the_type() {
        assertThat(EntityTypeRegistry.DEFAULT.resolve("mcp-tool.github.list_pull_request")).hasValueSatisfying(type -> {
            assertThat(type.token()).isEqualTo("mcp-tool");
            assertThat(type.parentToken()).isEqualTo("mcp-server");
        });
    }

    @Test
    void resolve_is_empty_for_an_unregistered_type() {
        assertThat(EntityTypeRegistry.DEFAULT.resolve("mcp.github")).isEmpty();
    }

    @Test
    void resolve_throws_on_a_malformed_id() {
        assertThatThrownBy(() -> EntityTypeRegistry.DEFAULT.resolve("github")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_duplicate_tokens() {
        assertThatThrownBy(() -> new EntityTypeRegistry(List.of(EntityType.nameSeeded("model"), EntityType.nameSeeded("model"))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("duplicate");
    }

    @Test
    void rejects_scoped_type_with_unknown_parent() {
        assertThatThrownBy(() -> new EntityTypeRegistry(List.of(EntityType.scoped("mcp-tool", "mcp-server"))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("unknown parent");
    }
}

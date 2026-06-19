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
package io.gravitee.gateway.reactive.core.context;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DefaultEntityResolverTest {

    private DefaultEntityResolver cut;

    @BeforeEach
    void init() {
        cut = new DefaultEntityResolver();
    }

    @Test
    void should_resolve_registered_entity_id() {
        cut.register("tool", "create_issue", "mcp.jira.tool.create_issue");

        assertThat(cut.resolve("tool", "create_issue")).isEqualTo("mcp.jira.tool.create_issue");
    }

    @Test
    void should_return_name_unchanged_when_kind_not_registered() {
        assertThat(cut.resolve("tool", "create_issue")).isEqualTo("create_issue");
    }

    @Test
    void should_return_name_unchanged_when_name_not_registered_for_kind() {
        cut.register("tool", "create_issue", "mcp.jira.tool.create_issue");

        assertThat(cut.resolve("tool", "delete_issue")).isEqualTo("delete_issue");
    }

    @Test
    void should_resolve_multiple_kinds_independently() {
        cut.register("tool", "create_issue", "mcp.jira.tool.create_issue");
        cut.register("resource", "issues", "mcp.jira.resource.issues");
        cut.register("model", "gpt-4o", "llm.openai-gpt-4o");

        assertThat(cut.resolve("tool", "create_issue")).isEqualTo("mcp.jira.tool.create_issue");
        assertThat(cut.resolve("resource", "issues")).isEqualTo("mcp.jira.resource.issues");
        assertThat(cut.resolve("model", "gpt-4o")).isEqualTo("llm.openai-gpt-4o");
    }

    @Test
    void should_allow_same_name_across_different_kinds() {
        cut.register("tool", "summary", "mcp.jira.tool.summary");
        cut.register("prompt", "summary", "mcp.jira.prompt.summary");

        assertThat(cut.resolve("tool", "summary")).isEqualTo("mcp.jira.tool.summary");
        assertThat(cut.resolve("prompt", "summary")).isEqualTo("mcp.jira.prompt.summary");
    }

    @Test
    void should_overwrite_on_duplicate_registration() {
        cut.register("tool", "create_issue", "mcp.jira.tool.create_issue");
        cut.register("tool", "create_issue", "mcp.jira-v2.tool.create_issue");

        assertThat(cut.resolve("tool", "create_issue")).isEqualTo("mcp.jira-v2.tool.create_issue");
    }
}

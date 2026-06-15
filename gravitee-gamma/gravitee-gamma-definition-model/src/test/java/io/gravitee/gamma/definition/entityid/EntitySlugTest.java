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

import org.junit.jupiter.api.Test;

class EntitySlugTest {

    @Test
    void lowercases_and_strips_accents() {
        assertThat(EntitySlug.toSlug("Crème Brûlée")).isEqualTo("creme-brulee");
    }

    @Test
    void drops_the_dot_from_the_allow_set() {
        assertThat(EntitySlug.toSlug("claude.ai")).isEqualTo("claude-ai");
        assertThat(EntitySlug.toSlug("gpt-3.5")).isEqualTo("gpt-3-5");
    }

    @Test
    void keeps_digits_underscore_and_dash() {
        assertThat(EntitySlug.toSlug("list_pull_requests")).isEqualTo("list_pull_requests");
        assertThat(EntitySlug.toSlug("gpt-4o")).isEqualTo("gpt-4o");
    }

    @Test
    void collapses_disallowed_runs_and_trims_dashes() {
        assertThat(EntitySlug.toSlug("  Hello, World!  ")).isEqualTo("hello-world");
    }

    @Test
    void collapses_repeated_separators() {
        assertThat(EntitySlug.toSlug("a -- b")).isEqualTo("a-b");
        assertThat(EntitySlug.toSlug("a---b")).isEqualTo("a-b");
    }

    @Test
    void returns_null_for_blank_or_empty_result() {
        assertThat(EntitySlug.toSlug(null)).isNull();
        assertThat(EntitySlug.toSlug("   ")).isNull();
        assertThat(EntitySlug.toSlug("...")).isNull();
    }

    @Test
    void or_slug_prefers_explicit_then_name() {
        assertThat(EntitySlug.orSlug("Explicit Id", "The Name")).isEqualTo("explicit-id");
        assertThat(EntitySlug.orSlug(null, "The Name")).isEqualTo("the-name");
    }

    @Test
    void or_keep_prefers_explicit_then_existing_then_name() {
        assertThat(EntitySlug.orKeep("New Id", "existing", "The Name")).isEqualTo("new-id");
        assertThat(EntitySlug.orKeep(null, "existing", "The Name")).isEqualTo("existing");
        assertThat(EntitySlug.orKeep(null, null, "The Name")).isEqualTo("the-name");
    }
}

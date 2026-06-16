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
package io.gravitee.apim.core.portal.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class NavigationPathTest {

    @Test
    void accepts_single_segment() {
        assertThat(new NavigationPath("/a", null).path()).isEqualTo("/a");
    }

    @Test
    void accepts_multi_segment() {
        assertThat(new NavigationPath("/a/b/c", null).path()).isEqualTo("/a/b/c");
    }

    @Test
    void strips_trailing_slash() {
        assertThat(new NavigationPath("/a/b/", null).path()).isEqualTo("/a/b");
    }

    @Test
    void descend_appends_child_segment() {
        assertThat(new NavigationPath("/docs", null).descend("getting-started").path()).isEqualTo("/docs/getting-started");
    }

    @Test
    void descend_from_empty_root_produces_leading_slash_path() {
        assertThat(new NavigationPath("", null).descend("docs").path()).isEqualTo("/docs");
    }

    @Test
    void descend_clears_display_name() {
        assertThat(new NavigationPath("/docs", "Documentation").descend("api").displayName()).isNull();
    }

    @Test
    void normalizes_segment_casing_and_spaces() {
        assertThat(new NavigationPath("/Docs/Getting Started", null).path()).isEqualTo("/docs/getting-started");
    }
}

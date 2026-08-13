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
package io.gravitee.gateway.reactive.reactor.path;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class RequestPathNormalizerTest {

    @Nested
    class Resolving_dot_segments {

        @ParameterizedTest
        @CsvSource(
            {
                "/alpha/api/../../beta/api/echo, /beta/api/echo",
                "/alpha/api/../beta, /alpha/beta",
                "/a/./b, /a/b",
                "/a/b/./, /a/b/",
                "/./a, /a",
            }
        )
        void should_resolve_plain_dot_segments(String path, String expected) {
            assertThat(RequestPathNormalizer.normalize(path)).isEqualTo(expected);
        }

        @ParameterizedTest
        @CsvSource(
            {
                "/alpha/api/%2e%2e/%2e%2e/beta/api/echo, /beta/api/echo",
                "/alpha/api/%2E%2E/%2E%2E/beta/api/echo, /beta/api/echo",
                "/alpha/api/.%2e/%2e./beta/api/echo, /beta/api/echo",
                "/a/%2e/b, /a/b",
            }
        )
        void should_resolve_percent_encoded_dot_segments(String path, String expected) {
            assertThat(RequestPathNormalizer.normalize(path)).isEqualTo(expected);
        }

        @Test
        void should_leave_a_directory_behind_when_the_path_ends_on_a_dot_segment() {
            assertThat(RequestPathNormalizer.normalize("/a/b/..")).isEqualTo("/a/");
        }

        @Test
        void should_discard_the_steps_that_would_climb_above_the_root() {
            // RFC 3986 §5.2.4 drops them rather than failing.
            assertThat(RequestPathNormalizer.normalize("/../../x")).isEqualTo("/x");
        }
    }

    @Nested
    class Leaving_the_rest_untouched {

        @ParameterizedTest
        @ValueSource(
            strings = {
                "/alpha/api/echo",
                "/file.txt",
                "/a/b.c/d",
                // An empty segment is a valid segment: merging slashes is a separate decision.
                "/a//b",
                // Encoded slashes belong to their own option, not to dot segment resolution.
                "/a/b%2Fc",
                // Encoded dots that do not spell a dot segment stay encoded.
                "/a/x%2ey/b",
            }
        )
        void should_return_the_very_same_instance_when_there_is_nothing_to_resolve(String path) {
            assertThat(RequestPathNormalizer.normalize(path)).isSameAs(path);
        }

        @Test
        void should_tolerate_a_null_path() {
            assertThat(RequestPathNormalizer.normalize(null)).isNull();
        }

        @Test
        void should_tolerate_an_empty_path() {
            assertThat(RequestPathNormalizer.normalize("")).isEmpty();
        }

        @Test
        void should_keep_the_query_string_out_of_its_business() {
            // normalize() only ever sees a path: the query is carried by uri(), untouched.
            assertThat(RequestPathNormalizer.normalize("/a/../b")).isEqualTo("/b");
        }
    }
}

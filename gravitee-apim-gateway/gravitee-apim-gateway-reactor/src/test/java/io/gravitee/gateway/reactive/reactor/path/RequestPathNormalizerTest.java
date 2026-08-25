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

        @ParameterizedTest
        @CsvSource(
            {
                "/a/../b/../c, /c",
                "/alpha/api/../../beta/api/../../gamma/api/echo, /gamma/api/echo",
                "/a/%2e%2e/b/../c, /c",
                "/a/b/../../c/d/../e, /c/e",
            }
        )
        void should_resolve_a_chain_of_dot_segments(String path, String expected) {
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
    class Decoding_percent_encoding {

        @ParameterizedTest
        @CsvSource({ "/a/%41%42, /a/AB", "/a/%7Etilde, /a/~tilde", "/a/%2Ddash, /a/-dash", "/a/%30%39, /a/09" })
        void should_decode_unreserved_characters(String path, String expected) {
            // RFC 3986 §6.2.2.2 makes them equivalent to their plain form. It is the same rule
            // that turns %2e into a dot, so it cannot be applied to the dot alone.
            assertThat(RequestPathNormalizer.normalize(path)).isEqualTo(expected);
        }

        @ParameterizedTest
        @ValueSource(strings = { "/a/b%2Fc", "/a/b%3Fc", "/a/b%23c", "/a/b%3Bc" })
        void should_leave_reserved_characters_encoded(String path) {
            // This is what keeps %2F from becoming a separator, and object keys intact.
            assertThat(RequestPathNormalizer.normalize(path)).isSameAs(path);
        }

        @ParameterizedTest
        @ValueSource(strings = { "/a%", "/a%2", "/a%zz", "/a/%2e%2", "/a/%g0" })
        void should_answer_null_on_a_malformed_percent_sequence(String path) {
            // No normalized form exists, so the caller has nothing to decide on and rejects.
            assertThat(RequestPathNormalizer.normalize(path)).isNull();
        }

        @Test
        void should_not_resolve_a_double_encoded_dot_segment() {
            // %252e decodes to %2e, not to a dot: one pass only, by design.
            assertThat(RequestPathNormalizer.normalize("/a/b%252e%252e/c")).isSameAs("/a/b%252e%252e/c");
        }
    }

    @Nested
    class Request_targets_that_are_not_paths {

        @Test
        void should_leave_the_asterisk_form_alone() {
            // OPTIONS * is legal (RFC 9110 §7.1) and is what proxies and health probes send. Run
            // through the general scan it would be answered 400 under REJECT — a legal request
            // refused — and rewritten to "/*" under NORMALIZE, then routed to whatever sits on the
            // root context path.
            assertThat(RequestPathNormalizer.needsNormalization("*")).isFalse();
            assertThat(RequestPathNormalizer.normalize("*")).isEqualTo("*");
        }

        @Test
        void should_leave_a_null_target_alone() {
            // path() is nullable in the Vert.x API, and an HTTP/2 stream with no :path pseudo-header
            // produces exactly that. Such a request reached the acceptor before this class existed.
            assertThat(RequestPathNormalizer.needsNormalization(null)).isFalse();
            assertThat(RequestPathNormalizer.normalize(null)).isNull();
        }

        @Test
        void should_still_force_a_leading_slash_on_an_ordinary_relative_path() {
            // The exemption above is for the two forms that are not paths, not for anything that
            // merely fails to start with a slash.
            assertThat(RequestPathNormalizer.needsNormalization("relative/path")).isTrue();
            assertThat(RequestPathNormalizer.normalize("relative/path")).isEqualTo("/relative/path");
        }
    }

    @Nested
    class Dot_segments_carrying_path_parameters {

        @ParameterizedTest
        @CsvSource(
            {
                "/alpha/api/..;/..;/beta/api/secret, /beta/api/secret",
                "/alpha/..;jsessionid=1/beta, /beta",
                "/alpha/.;/beta, /alpha/beta",
                "/alpha/..;/..;/beta, /beta",
            }
        )
        void should_resolve_them_the_way_a_servlet_container_will(String path, String expected) {
            // Given a receiver that strips ;params before resolving — Tomcat, Jetty, Spring.
            // When the gateway normalizes.
            // Then it lands on the same resource the receiver would, rather than authorizing one
            // and letting the other be served. This is the escalation, in one line.
            assertThat(RequestPathNormalizer.normalize(path)).isEqualTo(expected);
        }

        @Test
        void should_flag_them_so_that_reject_refuses_them_too() {
            assertThat(RequestPathNormalizer.needsNormalization("/alpha/api/..;/..;/beta/api/secret")).isTrue();
        }

        @Test
        void should_never_emit_the_shape_it_refuses_to_resolve() {
            // The dangerous one: %2e%2e; decodes to ..; and, before the strip, normalize produced a
            // traversal a servlet receiver resolves from an input that contained none literally.
            final String normalized = RequestPathNormalizer.normalize("/alpha/%2e%2e;/beta");

            assertThat(normalized).isEqualTo("/beta").doesNotContain("..");
        }

        @ParameterizedTest
        @ValueSource(strings = { "/orders;v=2", "/a/report.csv;charset=utf8", "/a/..b;x/c", "/a/b..;x/c" })
        void should_leave_ordinary_segments_alone(String path) {
            // Only a segment that is entirely dots before the ';' is a dot segment. Anything else
            // keeps its parameters, or REJECT would start refusing perfectly well formed traffic.
            assertThat(RequestPathNormalizer.normalize(path)).isSameAs(path);
        }
    }

    @Nested
    class Reading_percent_sequences {

        @ParameterizedTest
        @ValueSource(strings = { "/a%+41b", "/a%-41b", "/a%4+1b" })
        void should_refuse_a_sign_wherever_a_hexadecimal_digit_is_expected(String path) {
            // Integer.parseInt accepts a leading sign, Character.digit does not. While the two
            // implementations each had their own reading, %+41 was flagged by one and ignored by
            // the other — the single disagreement a brute force over the input space could find.
            assertThat(RequestPathNormalizer.normalize(path)).isNull();
            assertThat(RequestPathNormalizer.needsNormalization(path)).isTrue();
        }

        @Test
        void should_refuse_a_non_ascii_digit_rather_than_reading_it_as_a_number() {
            // Character.digit is Unicode-aware: it reads the Arabic-Indic '٢' as 2, which turned
            // %٢e%٢e into a dot segment the gateway resolved and no receiver ever would — routing
            // to an API the raw path never named.
            assertThat(RequestPathNormalizer.normalize("/alpha/%٢e%٢e/beta")).isNull();
        }
    }

    @Nested
    class Beyond_the_specification {

        @ParameterizedTest
        @CsvSource({ "/a//b, /a/b", "/a///b, /a/b", "//a/b, /a/b" })
        void should_merge_duplicate_slashes(String path, String expected) {
            // Not in RFC 3986. Inherited from Vert.x and kept so the gateway agrees with the
            // rest of the stack, nginx included.
            assertThat(RequestPathNormalizer.normalize(path)).isEqualTo(expected);
        }

        @ParameterizedTest
        @CsvSource({ "relative/path, /relative/path", "a, /a" })
        void should_force_a_leading_slash(String path, String expected) {
            assertThat(RequestPathNormalizer.normalize(path)).isEqualTo(expected);
        }
    }

    @Nested
    class Leaving_the_rest_untouched {

        @ParameterizedTest
        @ValueSource(strings = { "/alpha/api/echo", "/file.txt", "/a/b.c/d", "/v1/orders/12345.json", "/" })
        void should_return_the_very_same_instance_when_there_is_nothing_to_resolve(String path) {
            assertThat(RequestPathNormalizer.normalize(path)).isSameAs(path);
        }

        @Test
        void should_tolerate_a_null_path() {
            assertThat(RequestPathNormalizer.normalize(null)).isNull();
        }

        @Test
        void should_answer_the_root_for_an_empty_path() {
            assertThat(RequestPathNormalizer.normalize("")).isEqualTo("/");
        }
    }
}

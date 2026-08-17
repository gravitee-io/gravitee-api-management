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
package io.gravitee.gateway.reactor.handler.index;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.gateway.reactor.handler.DefaultHttpAcceptor;
import io.gravitee.gateway.reactor.handler.HttpAcceptor;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PathSegmentTrieTest {

    private final PathSegmentTrie trie = new PathSegmentTrie();

    private HttpAcceptor index(String path) {
        HttpAcceptor acceptor = new DefaultHttpAcceptor(path);
        trie.add(acceptor.path(), acceptor);
        return acceptor;
    }

    private HttpAcceptor index(String path, List<String> serverIds) {
        HttpAcceptor acceptor = new DefaultHttpAcceptor(null, path, serverIds);
        trie.add(acceptor.path(), acceptor);
        return acceptor;
    }

    private HttpAcceptor resolve(String path) {
        return trie.resolveShortest(null, path, null);
    }

    @Nested
    class Prefix_matching {

        @Test
        void should_match_a_request_deeper_than_the_context_path() {
            // Given
            HttpAcceptor acceptor = index("/store");

            // When / Then
            assertThat(resolve("/store/v1/orders")).isSameAs(acceptor);
        }

        @Test
        void should_match_the_context_path_itself_without_trailing_slash() {
            // Given
            HttpAcceptor acceptor = index("/store");

            // When / Then
            assertThat(resolve("/store")).isSameAs(acceptor);
        }

        @Test
        void should_match_the_context_path_itself_with_trailing_slash() {
            // Given
            HttpAcceptor acceptor = index("/store");

            // When / Then
            assertThat(resolve("/store/")).isSameAs(acceptor);
        }

        @Test
        void should_not_match_a_request_sharing_only_a_partial_segment() {
            // Given
            index("/store");

            // When / Then
            assertThat(resolve("/storefront")).isNull();
        }

        @Test
        void should_not_match_a_sibling_segment() {
            // Given
            index("/a/b");

            // When / Then
            assertThat(resolve("/a/bc")).isNull();
        }
    }

    @Nested
    class Root_acceptor {

        @Test
        void should_match_any_request() {
            // Given
            HttpAcceptor acceptor = index("/");

            // When / Then
            assertThat(resolve("/")).isSameAs(acceptor);
            assertThat(resolve("/anything")).isSameAs(acceptor);
            assertThat(resolve("/deeply/nested/path")).isSameAs(acceptor);
        }

        @Test
        void should_win_over_a_longer_context_path_when_resolving_the_shortest() {
            // Given
            HttpAcceptor root = index("/");
            index("/store");

            // When / Then
            assertThat(resolve("/store/v1")).isSameAs(root);
        }
    }

    @Nested
    class Degenerate_request_paths {

        @Test
        void should_not_match_an_empty_path() {
            // Given
            index("/");

            // When / Then
            assertThat(resolve("")).isNull();
        }

        @Test
        void should_not_match_a_path_without_a_leading_slash() {
            // Given
            index("/store");

            // When / Then
            assertThat(resolve("store/v1")).isNull();
        }

        @Test
        void should_stop_at_a_duplicated_slash_the_context_path_does_not_declare() {
            // Given
            index("/a/b");

            // When / Then
            assertThat(resolve("/a//b")).isNull();
        }

        @Test
        void should_still_match_a_shorter_context_path_across_a_duplicated_slash() {
            // Given
            HttpAcceptor acceptor = index("/a");

            // When / Then
            assertThat(resolve("/a//b")).isSameAs(acceptor);
        }
    }

    @Nested
    class Precedence {

        @Test
        void should_return_the_shortest_context_path() {
            // Given
            HttpAcceptor shallow = index("/a");
            index("/a/b");

            // When / Then
            assertThat(trie.resolveShortest(null, "/a/b/c", null)).isSameAs(shallow);
        }

        @Test
        void should_return_the_longest_context_path() {
            // Given
            index("/a");
            HttpAcceptor deep = index("/a/b");

            // When / Then
            assertThat(trie.resolveLongest(null, "/a/b/c", null)).isSameAs(deep);
        }

        @Test
        void should_fall_through_to_a_deeper_node_when_the_shallower_one_rejects_the_server() {
            // Given
            index("/a", List.of("server-1"));
            HttpAcceptor deep = index("/a/b");

            // When / Then
            assertThat(trie.resolveShortest(null, "/a/b/c", "server-2")).isSameAs(deep);
        }

        @Test
        void should_keep_the_shallower_node_when_the_deeper_one_rejects_the_server() {
            // Given
            HttpAcceptor shallow = index("/a");
            index("/a/b", List.of("server-1"));

            // When / Then
            assertThat(trie.resolveLongest(null, "/a/b/c", "server-2")).isSameAs(shallow);
        }

        @Test
        void should_return_the_first_registered_acceptor_when_two_share_the_same_path() {
            // Given
            HttpAcceptor first = index("/a", List.of("server-1"));
            index("/a", List.of("server-1"));

            // When / Then
            assertThat(trie.resolveShortest(null, "/a/b", "server-1")).isSameAs(first);
        }
    }

    @Nested
    class Many_siblings {

        @Test
        void should_resolve_among_thousands_of_sibling_context_paths() {
            // Given
            HttpAcceptor target = null;
            for (int i = 0; i < 5_000; i++) {
                HttpAcceptor acceptor = index("/api-" + i);
                if (i == 4_999) {
                    target = acceptor;
                }
            }

            // When / Then
            assertThat(trie.size()).isEqualTo(5_000);
            assertThat(resolve("/api-4999/resource")).isSameAs(target);
            assertThat(resolve("/api-5000")).isNull();
        }
    }
}

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

import io.gravitee.gateway.reactor.handler.HttpAcceptor;
import io.gravitee.gateway.reactor.handler.OverlappingHttpAcceptor;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ReverseHostTrieTest {

    private final ReverseHostTrie trie = new ReverseHostTrie();

    private HttpAcceptor index(String host, String path) {
        OverlappingHttpAcceptor acceptor = new OverlappingHttpAcceptor(host, path);
        trie.computeIfAbsent(acceptor.host()).add(acceptor.path(), acceptor);
        return acceptor;
    }

    private HttpAcceptor resolve(String host, String path) {
        return trie.resolve(host.toLowerCase(), host, path, null);
    }

    @Nested
    class Exact_host {

        @Test
        void should_match_the_declared_host() {
            // Given
            HttpAcceptor acceptor = index("api.acme.com", "/store");

            // When / Then
            assertThat(resolve("api.acme.com", "/store/v1")).isSameAs(acceptor);
        }

        @Test
        void should_match_regardless_of_the_request_host_case() {
            // Given
            HttpAcceptor acceptor = index("api.acme.com", "/store");

            // When / Then
            assertThat(resolve("API.Acme.COM", "/store/v1")).isSameAs(acceptor);
        }

        @Test
        void should_not_match_a_different_host() {
            // Given
            index("api.acme.com", "/store");

            // When / Then
            assertThat(resolve("api.other.com", "/store/v1")).isNull();
        }
    }

    @Nested
    class Wildcard_host {

        @Test
        void should_match_any_subdomain() {
            // Given
            HttpAcceptor acceptor = index("*.acme.com", "/store");

            // When / Then
            assertThat(resolve("api.acme.com", "/store/v1")).isSameAs(acceptor);
            assertThat(resolve("deep.nested.acme.com", "/store/v1")).isSameAs(acceptor);
        }

        @Test
        void should_not_match_the_bare_domain() {
            // Given
            index("*.acme.com", "/store");

            // When / Then
            assertThat(resolve("acme.com", "/store/v1")).isNull();
        }

        @Test
        void should_match_a_bare_suffix_when_the_wildcard_carries_no_dot() {
            // Given
            HttpAcceptor acceptor = index("*acme.com", "/store");

            // When / Then
            assertThat(resolve("myacme.com", "/store/v1")).isSameAs(acceptor);
        }

        @Test
        void should_stay_case_sensitive_as_the_raw_ends_with_predicate_is() {
            // Given
            index("*.ACME.com", "/store");

            // When / Then
            assertThat(resolve("api.acme.com", "/store/v1")).isNull();
        }
    }

    @Nested
    class Precedence {

        @Test
        void should_prefer_the_longest_matching_suffix() {
            // Given
            index("*.acme.com", "/store");
            HttpAcceptor specific = index("*.bar.acme.com", "/store");

            // When / Then
            assertThat(resolve("api.bar.acme.com", "/store/v1")).isSameAs(specific);
        }

        @Test
        void should_prefer_the_exact_host_over_a_wildcard() {
            // Given
            index("*.acme.com", "/store");
            HttpAcceptor exact = index("api.acme.com", "/store");

            // When / Then
            assertThat(resolve("api.acme.com", "/store/v1")).isSameAs(exact);
        }

        @Test
        void should_fall_back_to_a_shorter_suffix_when_the_longest_has_no_matching_path() {
            // Given
            HttpAcceptor loose = index("*.acme.com", "/store");
            index("*.bar.acme.com", "/other");

            // When / Then
            assertThat(resolve("api.bar.acme.com", "/store/v1")).isSameAs(loose);
        }

        @Test
        void should_prefer_the_longest_path_within_a_host_bucket() {
            // Given
            index("*.acme.com", "/store");
            HttpAcceptor deep = index("*.acme.com", "/store/v1");

            // When / Then
            assertThat(resolve("api.acme.com", "/store/v1/orders")).isSameAs(deep);
        }
    }

    @Nested
    class Many_hosts {

        @Test
        void should_resolve_among_thousands_of_host_buckets() {
            // Given
            HttpAcceptor target = null;
            for (int i = 0; i < 5_000; i++) {
                HttpAcceptor acceptor = index("tenant-" + i + ".acme.com", "/store");
                if (i == 4_999) {
                    target = acceptor;
                }
            }

            // When / Then
            assertThat(trie.size()).isEqualTo(5_000);
            assertThat(resolve("tenant-4999.acme.com", "/store/v1")).isSameAs(target);
            assertThat(resolve("tenant-5000.acme.com", "/store/v1")).isNull();
        }
    }
}

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
package io.gravitee.apim.core.api.domain_service;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.definition.model.Cors;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class BrowserCallPermissionTest {

    private static final String PORTAL = "https://portal.example.com";
    private static final List<String> CHAT_HEADERS = List.of("Authorization", "Content-Type");

    private static Cors cors(Set<String> origins, Set<String> methods, Set<String> headers) {
        return Cors.builder()
            .enabled(true)
            .accessControlAllowOrigin(origins)
            .accessControlAllowMethods(methods)
            .accessControlAllowHeaders(headers)
            .build();
    }

    private static boolean allows(Cors cors) {
        return BrowserCallPermission.allows(cors, PORTAL, "POST", CHAT_HEADERS);
    }

    @Test
    void should_allow_the_origin_method_and_headers_it_lists() {
        assertThat(
            allows(cors(Set.of("https://app.example.com", PORTAL), Set.of("GET", "POST"), Set.of("Authorization", "Content-Type")))
        ).isTrue();
    }

    @Test
    void should_refuse_without_cors() {
        assertThat(allows(null)).isFalse();
    }

    @Test
    void should_refuse_when_cors_is_disabled() {
        var disabled = cors(Set.of(PORTAL), Set.of("POST"), Set.of("Authorization", "Content-Type"));
        disabled.setEnabled(false);

        assertThat(allows(disabled)).isFalse();
    }

    @Test
    void should_refuse_an_origin_it_does_not_list() {
        assertThat(allows(cors(Set.of("https://app.example.com"), Set.of("POST"), Set.of("Authorization", "Content-Type")))).isFalse();
    }

    @Test
    void should_allow_any_origin_with_a_wildcard() {
        assertThat(allows(cors(Set.of("*"), Set.of("POST"), Set.of("Authorization", "Content-Type")))).isTrue();
    }

    @Test
    void should_match_the_origin_ignoring_case_and_a_trailing_slash() {
        assertThat(allows(cors(Set.of("HTTPS://Portal.Example.com/"), Set.of("POST"), Set.of("Authorization", "Content-Type")))).isTrue();
    }

    @Test
    void should_allow_an_origin_matching_a_regex_entry() {
        assertThat(allows(cors(Set.of("https://(.*).example.com"), Set.of("POST"), Set.of("Authorization", "Content-Type")))).isTrue();
    }

    @Test
    void should_use_the_compiled_origin_patterns_when_present() {
        var withPatterns = cors(Set.of("https://app.example.com"), Set.of("POST"), Set.of("Authorization", "Content-Type"));
        withPatterns.setAccessControlAllowOriginRegex(Set.of(Pattern.compile("https://portal\\..*")));

        assertThat(allows(withPatterns)).isTrue();
    }

    @Test
    void should_ignore_an_invalid_regex_entry() {
        assertThat(allows(cors(Set.of("https://(portal.example.com"), Set.of("POST"), Set.of("Authorization", "Content-Type")))).isFalse();
    }

    @Test
    void should_refuse_when_the_method_is_missing() {
        assertThat(allows(cors(Set.of(PORTAL), Set.of("GET"), Set.of("Authorization", "Content-Type")))).isFalse();
    }

    @Test
    void should_allow_any_method_with_a_wildcard() {
        assertThat(allows(cors(Set.of(PORTAL), Set.of("*"), Set.of("Authorization", "Content-Type")))).isTrue();
    }

    @Test
    void should_refuse_when_a_header_is_missing() {
        assertThat(allows(cors(Set.of(PORTAL), Set.of("POST"), Set.of("Content-Type")))).isFalse();
    }

    @Test
    void should_match_headers_ignoring_case() {
        assertThat(allows(cors(Set.of(PORTAL), Set.of("POST"), Set.of("authorization", "content-type")))).isTrue();
    }

    @Test
    void should_allow_any_header_with_a_wildcard() {
        assertThat(allows(cors(Set.of(PORTAL), Set.of("POST"), Set.of("*")))).isTrue();
    }

    @Test
    void should_refuse_when_lists_are_null() {
        assertThat(allows(cors(null, null, null))).isFalse();
    }
}

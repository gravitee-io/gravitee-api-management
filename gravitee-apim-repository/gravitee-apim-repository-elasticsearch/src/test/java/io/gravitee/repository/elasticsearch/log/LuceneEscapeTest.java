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
package io.gravitee.repository.elasticsearch.log;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LuceneEscapeTest {

    @Test
    void should_not_alter_simple_status_filter() {
        String result = ElasticLogRepository.escapeForJsonLucene("status:200");

        assertThat(result).isEqualTo("status:200");
    }

    @Test
    void should_preserve_lucene_grouping_parentheses_for_multi_status_filter() {
        String result = ElasticLogRepository.escapeForJsonLucene("status:(200 OR 400)");

        // Parentheses must NOT be escaped — they are Lucene grouping syntax
        assertThat(result).isEqualTo("status:(200 OR 400)");
    }

    @Test
    void should_preserve_grouping_parentheses_with_multiple_filter_types() {
        String result = ElasticLogRepository.escapeForJsonLucene("status:(200 OR 400) AND method:(GET OR POST)");

        assertThat(result).isEqualTo("status:(200 OR 400) AND method:(GET OR POST)");
    }

    @Test
    void should_escape_forward_slashes() {
        String result = ElasticLogRepository.escapeForJsonLucene("uri:/api/test");

        assertThat(result).isEqualTo("uri:\\\\/api\\\\/test");
    }

    @Test
    void should_quadruple_backslashes() {
        String result = ElasticLogRepository.escapeForJsonLucene("path:C\\Users\\test");

        assertThat(result).isEqualTo("path:C\\\\\\\\Users\\\\\\\\test");
    }

    @Test
    void should_escape_slashes_and_preserve_grouping_parentheses() {
        String input = "status:200 AND custom.userAgent:RMel/1.0.0 (Ubuntu)";

        String result = ElasticLogRepository.escapeForJsonLucene(input);

        // forward slash is escaped so Lucene treats it as literal, not a regex delimiter
        assertThat(result).contains("RMel\\\\/1.0.0");
        assertThat(result).contains("status:200");
        assertThat(result).contains("custom.userAgent:");

        // parentheses are intentionally left unescaped to preserve Lucene grouping syntax
        assertThat(result).contains("(Ubuntu)");
        assertThat(result).doesNotContain("\\\\(");
    }

    @Test
    void should_preserve_grouping_while_escaping_slashes() {
        String result = ElasticLogRepository.escapeForJsonLucene("status:(200 OR 400) AND uri:/api/test*");

        assertThat(result).startsWith("status:(200 OR 400)");
        assertThat(result).doesNotContain("uri:/");
    }

    // --- safeEscapeReplacement ---

    @Test
    void should_replace_filter_with_escaped_form_when_filter_has_no_double_quote() {
        String filter = "uri:/api/test";
        String json = "{\"query\":\"" + filter + "\"}";

        String result = ElasticLogRepository.safeEscapeReplacement(json, filter);

        assertThat(result).isEqualTo("{\"query\":\"uri:\\\\/api\\\\/test\"}");
    }

    @Test
    void should_skip_replacement_when_filter_contains_double_quote() {
        // legacy portal wraps API UUIDs in backslash-escaped quotes: api:(\"uuid\")
        String filter = "api:(\\\"bad3b39c-7d97-4ab4-93b3-9c7d970ab4ca\\\")";
        String json = "{\"query\":\"" + filter + "\"}";

        String result = ElasticLogRepository.safeEscapeReplacement(json, filter);

        // bypass — backslash quadrupling would corrupt the \" JSON escape sequences (APIM-13402)
        assertThat(result).isEqualTo(json);
    }

    @Test
    void should_skip_replacement_when_filter_with_double_quote_also_has_forward_slash() {
        // caller contract: when filter contains ", caller must pre-escape / and \ in the quoted span
        String filter = "uri:(\\\"/api/v1\\\")";
        String json = "{\"query\":\"" + filter + "\"}";

        String result = ElasticLogRepository.safeEscapeReplacement(json, filter);

        assertThat(result).isEqualTo(json);
    }

    @Test
    void should_no_op_when_filter_is_empty() {
        String json = "{\"query\":\"\"}";

        String result = ElasticLogRepository.safeEscapeReplacement(json, "");

        assertThat(result).isEqualTo(json);
    }

    @Test
    void should_no_op_when_filter_is_null() {
        String json = "{\"query\":\"\"}";

        String result = ElasticLogRepository.safeEscapeReplacement(json, null);

        assertThat(result).isEqualTo(json);
    }
}

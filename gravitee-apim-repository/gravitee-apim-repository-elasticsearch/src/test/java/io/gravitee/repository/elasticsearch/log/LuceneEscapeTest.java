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

        assertThat(result).doesNotContain("RMel/1.0.0");
        assertThat(result).contains("status:200");
        assertThat(result).contains("custom.userAgent:");
        assertThat(result).contains("(Ubuntu)");
    }

    @Test
    void should_preserve_grouping_while_escaping_slashes() {
        String result = ElasticLogRepository.escapeForJsonLucene("status:(200 OR 400) AND uri:/api/test*");

        assertThat(result).startsWith("status:(200 OR 400)");
        assertThat(result).doesNotContain("uri:/");
    }
}

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

import io.gravitee.repository.analytics.query.QueryFilter;
import java.lang.reflect.Method;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.junit.jupiter.api.Test;

public class LuceneEscapeTest {

    @Test
    public void compareImplementationWithStandardParser() {
        // Original APIM-12955 regression test
        String input = "status:200 AND custom.userAgent:RMel/1.0.0 (Ubuntu)";

        String luceneStandard = QueryParser.escape(input);

        // escapeFilterValues splits field:value and only escapes values
        String result = invokeEscapeFilterValues(input);

        assertThat(result).contains("RMel\\\\/1.0.0");
        assertThat(result).contains("\\\\(Ubuntu\\\\)");

        // NO REGRESSION: field names preserved
        assertThat(result).contains("status:200");
        assertThat(result).contains("custom.userAgent:");

        assertThat(luceneStandard).contains("status\\:200");
    }

    @Test
    public void escapeFilterValues_preservesFieldNames() {
        String result = invokeEscapeFilterValues("status:200 AND api:some-uuid");
        assertThat(result).isEqualTo("status:200 AND api:some\\\\-uuid");
    }

    @Test
    public void escapeFilterValues_escapesSlashInValue() {
        String result = invokeEscapeFilterValues("custom.userAgent:RMel/1.0.0");
        assertThat(result).isEqualTo("custom.userAgent:RMel\\\\/1.0.0");
    }

    @Test
    public void escapeFilterValues_escapesParensInValue() {
        String result = invokeEscapeFilterValues("custom.userAgent:App (v2)");
        assertThat(result).isEqualTo("custom.userAgent:App \\\\(v2\\\\)");
    }

    @Test
    public void escapeValueForLuceneJson_noSpecialChars() {
        String result = invokeEscapeValue("searchterm");
        assertThat(result).isEqualTo("searchterm");
    }

    @Test
    public void escapeValueForLuceneJson_escapesParens() {
        String result = invokeEscapeValue("error(500)");
        assertThat(result).isEqualTo("error\\\\(500\\\\)");
    }

    @Test
    public void escapeValueForLuceneJson_escapesSlash() {
        String result = invokeEscapeValue("path/to/resource");
        assertThat(result).isEqualTo("path\\\\/to\\\\/resource");
    }

    @Test
    public void escapeValueForLuceneJson_doesNotEscapeDollarOrAt() {
        // $ and @ are not Lucene special chars
        String result = invokeEscapeValue("$99 user@example.com");
        assertThat(result).isEqualTo("$99 user@example.com");
    }

    @Test
    public void escapeValueForLuceneJson_escapesTilde() {
        String result = invokeEscapeValue("fuzzy~2");
        assertThat(result).isEqualTo("fuzzy\\\\~2");
    }

    @Test
    public void escapeValueForLuceneJson_idempotent_preEscapedParens() {
        // Raw-client 1-backslash pre-escape: \( → \\(
        String result = invokeEscapeValue("RMel\\(Ubuntu\\)");
        assertThat(result).isEqualTo("RMel\\\\(Ubuntu\\\\)");
    }

    @Test
    public void escapeValueForLuceneJson_idempotent_preEscapedTilde() {
        // Raw-client 1-backslash pre-escape: \~ → \\~
        String result = invokeEscapeValue("fuzzy\\~2");
        assertThat(result).isEqualTo("fuzzy\\\\~2");
    }

    @Test
    public void escapeValueForLuceneJson_idempotent_preEscapedSlash() {
        // Raw-client 1-backslash pre-escape: \/ → \\/
        String result = invokeEscapeValue("path\\/to");
        assertThat(result).isEqualTo("path\\\\/to");
    }

    @Test
    public void escapeValueForLuceneJson_idempotent_consolePreEscapedParens() {
        // Console 2-backslash convention: \\( → \\( (pass through unchanged)
        // Browser sends %5C%5C%28 → Java runtime: \\(
        String result = invokeEscapeValue("RMel\\\\(Ubuntu\\\\)");
        assertThat(result).isEqualTo("RMel\\\\(Ubuntu\\\\)");
    }

    @Test
    public void escapeValueForLuceneJson_idempotent_consolePreEscapedTilde() {
        // Console 2-backslash convention: \\~ → \\~ (pass through unchanged)
        String result = invokeEscapeValue("fuzzy\\\\~2");
        assertThat(result).isEqualTo("fuzzy\\\\~2");
    }

    @Test
    public void escapeValueForLuceneJson_idempotent_consolePreEscapedSlash() {
        // Console 2-backslash convention: \\/ → \\/ (pass through unchanged)
        String result = invokeEscapeValue("path\\\\/to");
        assertThat(result).isEqualTo("path\\\\/to");
    }

    @Test
    public void escapeFilterValues_doesNotEscapeBodyWildcardFieldPattern() {
        // \\*.body is the Lucene wildcard field pattern for body/payload searches (APIM-13030 regression guard).
        // escapeFilterValues must not corrupt the leading backslashes — only the value is escaped.
        String result = invokeEscapeFilterValues("\\\\*.body:application/json");
        assertThat(result).isEqualTo("\\\\*.body:application\\\\/json");
    }

    @Test
    public void escapeFilterValues_handlesOrSubclauses() {
        // OR-separated sub-clauses (e.g. _id:x OR _id:y generated from logIdsQuery)
        String result = invokeEscapeFilterValues("custom.path:a/b OR custom.path:c/d");
        assertThat(result).isEqualTo("custom.path:a\\\\/b OR custom.path:c\\\\/d");
    }

    // --- getQuery() unit tests (live path) ---

    @Test
    public void getQuery_returnsNullForNullFilter() {
        assertThat(new ElasticLogRepository().getQuery(null, true)).isNull();
    }

    @Test
    public void getQuery_routesBodyToWildcardField() {
        // APIM-13030: body filter must produce \\*.body:value, not a corrupted field pattern
        String result = new ElasticLogRepository().getQuery(new QueryFilter("body:hello"), true);
        assertThat(result).isEqualTo("\\\\*.body:hello");
    }

    @Test
    public void getQuery_escapesSpecialCharsInBodyValue() {
        String result = new ElasticLogRepository().getQuery(new QueryFilter("body:test/path"), true);
        assertThat(result).isEqualTo("\\\\*.body:test\\\\/path");
    }

    @Test
    public void getQuery_excludesBodyClauseWhenLogFalse() {
        // non-body query (log=false) must strip body filter and keep only non-body clauses
        String result = new ElasticLogRepository().getQuery(new QueryFilter("status:200 AND body:test"), false);
        assertThat(result).isEqualTo("status:200");
    }

    @Test
    public void getQuery_excludesNonBodyClauseWhenLogTrue() {
        // body query (log=true) must strip non-body filters and keep only body clause
        String result = new ElasticLogRepository().getQuery(new QueryFilter("status:200 AND body:hello"), true);
        assertThat(result).isEqualTo("\\\\*.body:hello");
    }

    @Test
    public void getQuery_handlesValueWithColon() {
        // value containing colons (e.g. URL) — indexOf(':') must not truncate after the first colon
        String result = new ElasticLogRepository().getQuery(new QueryFilter("body:http://example.com:8080"), true);
        assertThat(result).isEqualTo("\\\\*.body:http\\\\:\\\\/\\\\/example.com\\\\:8080");
    }

    @Test
    public void getQuery_doesNotEscapeWildcards() {
        // * and ? are intentional Lucene wildcards — the console uses body:*term* for contains-search
        String result = new ElasticLogRepository().getQuery(new QueryFilter("body:*hello*"), true);
        assertThat(result).isEqualTo("\\\\*.body:*hello*");
    }

    private String invokeEscapeFilterValues(String filter) {
        try {
            Method method = ElasticLogRepository.class.getDeclaredMethod("escapeFilterValues", String.class);
            method.setAccessible(true);
            return (String) method.invoke(new ElasticLogRepository(), filter);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String invokeEscapeValue(String value) {
        try {
            Method method = ElasticLogRepository.class.getDeclaredMethod("escapeValueForLuceneJson", String.class);
            method.setAccessible(true);
            return (String) method.invoke(null, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

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
        assertThat(result).isEqualTo("status:200 AND api:some-uuid");
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

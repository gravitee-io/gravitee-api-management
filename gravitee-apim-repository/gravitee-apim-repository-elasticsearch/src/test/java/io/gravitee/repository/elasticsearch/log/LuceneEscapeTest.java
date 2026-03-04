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

import org.apache.lucene.queryparser.classic.QueryParser;
import org.junit.jupiter.api.Test;

public class LuceneEscapeTest {

    @Test
    public void shouldEscapeToxicUserAgent() {
        String toxicInput = "custom.userAgentMelodi:RMel/1.0.0 (Ubuntu)";

        String escaped = QueryParser.escape(toxicInput);
        String result = escaped.replace("\\", "\\\\");

        assertThat(result).contains("RMel\\\\/1.0.0");
        assertThat(result).contains("\\\\(Ubuntu\\\\)");
        System.out.println("JSON String: " + result);
    }
}

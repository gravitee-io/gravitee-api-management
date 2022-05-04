/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.reporter.elasticsearch.indexer.name;

import static org.junit.Assert.assertEquals;

import io.gravitee.reporter.elasticsearch.config.ReporterConfiguration;
import java.time.Instant;
import org.junit.Test;

public class PerTypeIndexNameGeneratorTest {

    private PerTypeIndexNameGenerator indexNameGenerator = new PerTypeIndexNameGenerator();

    @Test
    public void generate_should_generate_index_name_with_type_and_no_date() {
        indexNameGenerator.configuration = new ReporterConfiguration();
        indexNameGenerator.configuration.setIndexName("indexName");
        indexNameGenerator.initialize();

        String indexName = indexNameGenerator.generate("indextype", Instant.parse("2018-04-28T18:35:24.00Z"));

        assertEquals(indexName, "indexName-indextype");
    }
}

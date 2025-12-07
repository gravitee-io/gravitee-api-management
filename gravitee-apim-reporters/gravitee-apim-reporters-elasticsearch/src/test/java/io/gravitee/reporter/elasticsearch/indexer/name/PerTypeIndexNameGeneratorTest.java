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
package io.gravitee.reporter.elasticsearch.indexer.name;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.reporter.elasticsearch.config.ReporterConfiguration;
import io.gravitee.reporter.elasticsearch.indexer.PerTypeAndDateIndexNameGenerator;
import io.gravitee.reporter.elasticsearch.indexer.PerTypeIndexNameGenerator;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PerTypeIndexNameGeneratorTest {

    private PerTypeIndexNameGenerator cut;

    @BeforeEach
    public void beforeEach() {
        ReporterConfiguration configuration = new ReporterConfiguration();
        configuration.setIndexName("indexName");
        cut = new PerTypeIndexNameGenerator(configuration);
    }

    @Test
    public void generate_should_generate_index_name_with_type_and_no_date() {
        String indexName = cut.generate("indextype", Instant.parse("2018-04-28T18:35:24.00Z"));
        assertThat(indexName).isEqualTo("indexName-indextype");
    }
}

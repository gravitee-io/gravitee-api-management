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
package io.gravitee.reporter.elasticsearch;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.elasticsearch.version.ElasticsearchInfo;
import io.gravitee.elasticsearch.version.Version;
import io.gravitee.reporter.elasticsearch.config.ReporterConfiguration;
import io.gravitee.reporter.elasticsearch.indexer.es7.ES7BulkIndexer;
import io.gravitee.reporter.elasticsearch.indexer.es8.ES8BulkIndexer;
import io.gravitee.reporter.elasticsearch.indexer.name.PerTypeAndDateIndexNameGenerator;
import io.gravitee.reporter.elasticsearch.indexer.name.PerTypeIndexNameGenerator;
import io.gravitee.reporter.elasticsearch.mapping.es7.ES7IndexPreparer;
import io.gravitee.reporter.elasticsearch.mapping.es8.ES8IndexPreparer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = UnitTestConfiguration.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BeanRegisterTest {

    @Autowired
    ApplicationContext applicationContext;

    ElasticsearchInfo elasticsearchInfo;

    BeanRegister register;

    @BeforeEach
    void setUp() {
        register = new BeanRegister(applicationContext);
    }

    @Nested
    class ElasticSearch7 {

        @Test
        void should_instantiate_beans_for_elasticsearch_7_daily_mode() {
            var reporterConfiguration = new ReporterConfiguration();
            reporterConfiguration.setIndexMode("daily");

            register.registerBeans(elasticsearchInfo("7.17.8"), reporterConfiguration);

            assertThat(applicationContext.getBean("indexer")).isInstanceOf(ES7BulkIndexer.class);
            assertThat(applicationContext.getBean("indexPreparer")).isInstanceOf(ES7IndexPreparer.class);
            assertThat(applicationContext.getBean("indexNameGenerator")).isInstanceOf(PerTypeAndDateIndexNameGenerator.class);
        }

        @Test
        void should_instantiate_beans_for_elasticsearch_7_ilm_mode() {
            var reporterConfiguration = new ReporterConfiguration();
            reporterConfiguration.setIndexMode("ilm");

            register.registerBeans(elasticsearchInfo("7.17.8"), reporterConfiguration);

            assertThat(applicationContext.getBean("indexer")).isInstanceOf(ES7BulkIndexer.class);
            assertThat(applicationContext.getBean("indexPreparer")).isInstanceOf(ES7IndexPreparer.class);
            assertThat(applicationContext.getBean("indexNameGenerator")).isInstanceOf(PerTypeIndexNameGenerator.class);
        }
    }

    @Nested
    class ElasticSearch8 {

        @Test
        void should_instantiate_beans_for_elasticsearch_8_daily_mode() {
            var reporterConfiguration = new ReporterConfiguration();
            reporterConfiguration.setIndexMode("daily");

            register.registerBeans(elasticsearchInfo("8.5.2"), reporterConfiguration);

            assertThat(applicationContext.getBean("indexer")).isInstanceOf(ES8BulkIndexer.class);
            assertThat(applicationContext.getBean("indexPreparer")).isInstanceOf(ES8IndexPreparer.class);
            assertThat(applicationContext.getBean("indexNameGenerator")).isInstanceOf(PerTypeAndDateIndexNameGenerator.class);
        }

        @Test
        void should_instantiate_beans_for_elasticsearch_8_ilm_mode() {
            var reporterConfiguration = new ReporterConfiguration();
            reporterConfiguration.setIndexMode("ilm");

            register.registerBeans(elasticsearchInfo("8.5.2"), reporterConfiguration);

            assertThat(applicationContext.getBean("indexer")).isInstanceOf(ES8BulkIndexer.class);
            assertThat(applicationContext.getBean("indexPreparer")).isInstanceOf(ES8IndexPreparer.class);
            assertThat(applicationContext.getBean("indexNameGenerator")).isInstanceOf(PerTypeIndexNameGenerator.class);
        }
    }

    @Nested
    class OpenSearch {

        @DisplayName("should instantiate beans for OpenSearch daily mode")
        @ParameterizedTest(name = "with version {0}")
        @ValueSource(strings = { "1.0", "2.6" })
        void should_instantiate_beans_for_opensearch_daily_mode(String version) {
            var reporterConfiguration = new ReporterConfiguration();
            reporterConfiguration.setIndexMode("daily");

            register.registerBeans(opensearchInfo(version), reporterConfiguration);

            assertThat(applicationContext.getBean("indexer")).isInstanceOf(ES7BulkIndexer.class);
            assertThat(applicationContext.getBean("indexPreparer")).isInstanceOf(ES7IndexPreparer.class);
            assertThat(applicationContext.getBean("indexNameGenerator")).isInstanceOf(PerTypeAndDateIndexNameGenerator.class);
        }

        @DisplayName("should instantiate beans for OpenSearch ilm mode")
        @ParameterizedTest(name = "with version {0}")
        @ValueSource(strings = { "1.0.0", "2.6" })
        void should_instantiate_beans_for_opensearch_ilm_mode(String version) {
            var reporterConfiguration = new ReporterConfiguration();
            reporterConfiguration.setIndexMode("ilm");

            register.registerBeans(opensearchInfo(version), reporterConfiguration);

            assertThat(applicationContext.getBean("indexer")).isInstanceOf(ES7BulkIndexer.class);
            assertThat(applicationContext.getBean("indexPreparer")).isInstanceOf(ES7IndexPreparer.class);
            assertThat(applicationContext.getBean("indexNameGenerator")).isInstanceOf(PerTypeIndexNameGenerator.class);
        }
    }

    ElasticsearchInfo elasticsearchInfo(String versionNumber) {
        return elasticsearchInfo("", versionNumber);
    }

    ElasticsearchInfo opensearchInfo(String versionNumber) {
        return elasticsearchInfo("opensearch", versionNumber);
    }

    ElasticsearchInfo elasticsearchInfo(String distribution, String versionNumber) {
        var version = new Version();
        version.setDistribution(distribution);
        version.setNumber(versionNumber);

        elasticsearchInfo = new ElasticsearchInfo();
        elasticsearchInfo.setVersion(version);

        return elasticsearchInfo;
    }
}

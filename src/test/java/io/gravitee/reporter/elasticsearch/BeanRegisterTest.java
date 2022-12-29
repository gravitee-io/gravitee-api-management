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
package io.gravitee.reporter.elasticsearch;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.elasticsearch.version.ElasticsearchInfo;
import io.gravitee.elasticsearch.version.Version;
import io.gravitee.reporter.elasticsearch.config.ReporterConfiguration;
import io.gravitee.reporter.elasticsearch.indexer.es5.ES5BulkIndexer;
import io.gravitee.reporter.elasticsearch.indexer.es6.ES6BulkIndexer;
import io.gravitee.reporter.elasticsearch.indexer.es7.ES7BulkIndexer;
import io.gravitee.reporter.elasticsearch.indexer.name.MultiTypeIndexNameGenerator;
import io.gravitee.reporter.elasticsearch.indexer.name.PerTypeAndDateIndexNameGenerator;
import io.gravitee.reporter.elasticsearch.indexer.name.PerTypeIndexNameGenerator;
import io.gravitee.reporter.elasticsearch.mapping.es5.ES5MultiTypeIndexPreparer;
import io.gravitee.reporter.elasticsearch.mapping.es5.ES5PerTypeIndexPreparer;
import io.gravitee.reporter.elasticsearch.mapping.es6.ES6IndexPreparer;
import io.gravitee.reporter.elasticsearch.mapping.es7.ES7IndexPreparer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
    class ElasticSearch5 {

        @Test
        void should_instantiate_beans_for_elasticsearch_5_per_type_index() {
            var reporterConfiguration = new ReporterConfiguration();
            reporterConfiguration.setPerTypeIndex(true);

            register.registerBeans(elasticsearchInfo("5.6.16"), reporterConfiguration);

            assertThat(applicationContext.getBean("indexer")).isInstanceOf(ES5BulkIndexer.class);
            assertThat(applicationContext.getBean("indexPreparer")).isInstanceOf(ES5PerTypeIndexPreparer.class);
            assertThat(applicationContext.getBean("indexNameGenerator")).isInstanceOf(PerTypeAndDateIndexNameGenerator.class);
        }

        @Test
        void should_instantiate_beans_for_elasticsearch_5_multi_type_index() {
            var reporterConfiguration = new ReporterConfiguration();
            reporterConfiguration.setPerTypeIndex(false);

            register.registerBeans(elasticsearchInfo("5.6.16"), reporterConfiguration);

            assertThat(applicationContext.getBean("indexer")).isInstanceOf(ES5BulkIndexer.class);
            assertThat(applicationContext.getBean("indexPreparer")).isInstanceOf(ES5MultiTypeIndexPreparer.class);
            assertThat(applicationContext.getBean("indexNameGenerator")).isInstanceOf(MultiTypeIndexNameGenerator.class);
        }
    }

    @Nested
    class ElasticSearch6 {

        @Test
        void should_instantiate_beans_for_elasticsearch_6_daily_mode() {
            var reporterConfiguration = new ReporterConfiguration();
            reporterConfiguration.setIndexMode("daily");

            register.registerBeans(elasticsearchInfo("6.7.2"), reporterConfiguration);

            assertThat(applicationContext.getBean("indexer")).isInstanceOf(ES6BulkIndexer.class);
            assertThat(applicationContext.getBean("indexPreparer")).isInstanceOf(ES6IndexPreparer.class);
            assertThat(applicationContext.getBean("indexNameGenerator")).isInstanceOf(PerTypeAndDateIndexNameGenerator.class);
        }

        @Test
        void should_instantiate_beans_for_elasticsearch_6_ilm_mode() {
            var reporterConfiguration = new ReporterConfiguration();
            reporterConfiguration.setIndexMode("ilm");

            register.registerBeans(elasticsearchInfo("6.6.16"), reporterConfiguration);

            assertThat(applicationContext.getBean("indexer")).isInstanceOf(ES6BulkIndexer.class);
            assertThat(applicationContext.getBean("indexPreparer")).isInstanceOf(ES6IndexPreparer.class);
            assertThat(applicationContext.getBean("indexNameGenerator")).isInstanceOf(PerTypeIndexNameGenerator.class);
        }
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
    class OpenSearch1 {

        @Test
        void should_instantiate_beans_for_opensearch1_daily_mode() {
            var reporterConfiguration = new ReporterConfiguration();
            reporterConfiguration.setIndexMode("daily");

            register.registerBeans(opensearchInfo("1.0"), reporterConfiguration);

            assertThat(applicationContext.getBean("indexer")).isInstanceOf(ES7BulkIndexer.class);
            assertThat(applicationContext.getBean("indexPreparer")).isInstanceOf(ES7IndexPreparer.class);
            assertThat(applicationContext.getBean("indexNameGenerator")).isInstanceOf(PerTypeAndDateIndexNameGenerator.class);
        }

        @Test
        void should_instantiate_beans_for_opensearch1_ilm_mode() {
            var reporterConfiguration = new ReporterConfiguration();
            reporterConfiguration.setIndexMode("ilm");

            register.registerBeans(opensearchInfo("1.0"), reporterConfiguration);

            assertThat(applicationContext.getBean("indexer")).isInstanceOf(ES7BulkIndexer.class);
            assertThat(applicationContext.getBean("indexPreparer")).isInstanceOf(ES7IndexPreparer.class);
            assertThat(applicationContext.getBean("indexNameGenerator")).isInstanceOf(PerTypeIndexNameGenerator.class);
        }
    }

    @ParameterizedTest
    @CsvSource(value = { "elastic:8.0.0", "opensearch:2.12.7" }, delimiter = ':')
    void should_ignore_unsupported_version(String distribution, String version) {
        register.registerBeans(elasticsearchInfo(distribution, version), new ReporterConfiguration());

        assertThat(applicationContext.getBeanDefinitionNames()).doesNotContain("indexer", "indexPreparer", "indexNameGenerator");
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

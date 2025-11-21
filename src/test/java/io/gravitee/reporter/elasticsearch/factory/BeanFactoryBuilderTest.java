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
package io.gravitee.reporter.elasticsearch.factory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.elasticsearch.client.Client;
import io.gravitee.elasticsearch.version.ElasticsearchInfo;
import io.gravitee.elasticsearch.version.Version;
import io.gravitee.reporter.common.formatter.FormatterFactoryConfiguration;
import io.gravitee.reporter.elasticsearch.config.PipelineConfiguration;
import io.gravitee.reporter.elasticsearch.config.ReporterConfiguration;
import io.gravitee.reporter.elasticsearch.indexer.IndexNameGenerator;
import io.gravitee.reporter.elasticsearch.indexer.PerTypeAndDateIndexNameGenerator;
import io.gravitee.reporter.elasticsearch.indexer.PerTypeIndexNameGenerator;
import io.gravitee.reporter.elasticsearch.mapping.es7.ES7IndexPreparer;
import io.gravitee.reporter.elasticsearch.mapping.es8.ES8IndexPreparer;
import io.gravitee.reporter.elasticsearch.mapping.opensearch.OpenSearchIndexPreparer;
import io.reactivex.rxjava3.core.Single;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class BeanFactoryBuilderTest {

    @Mock
    Client client;

    @Nested
    class ElasticSearch7 {

        @Test
        void should_instantiate_beans_for_elasticsearch_7_daily_mode() {
            var reporterConfiguration = new ReporterConfiguration();
            reporterConfiguration.setIndexMode("daily");
            when(client.getInfo()).thenReturn(Single.just(elasticsearchInfo("7.17.8")));
            BeanFactory beanFactory = BeanFactoryBuilder.buildFactory(client);

            assertThat(beanFactory).isNotNull();
            assertThat(beanFactory.createIndexNameGenerator(reporterConfiguration)).isInstanceOf(PerTypeAndDateIndexNameGenerator.class);
            assertThat(beanFactory.createIndexPreparer(reporterConfiguration, null, null, null)).isInstanceOf(ES7IndexPreparer.class);
        }

        @Test
        void should_instantiate_beans_for_elasticsearch_7_ilm_mode() {
            var reporterConfiguration = new ReporterConfiguration();
            reporterConfiguration.setIndexMode("ilm");
            when(client.getInfo()).thenReturn(Single.just(elasticsearchInfo("7.17.8")));
            BeanFactory beanFactory = BeanFactoryBuilder.buildFactory(client);

            assertThat(beanFactory).isNotNull();
            assertThat(beanFactory.createIndexNameGenerator(reporterConfiguration)).isInstanceOf(PerTypeIndexNameGenerator.class);
            assertThat(beanFactory.createIndexPreparer(reporterConfiguration, null, null, null)).isInstanceOf(ES7IndexPreparer.class);
        }
    }

    @Nested
    class ElasticSearch8 {

        @Test
        void should_instantiate_beans_for_elasticsearch_8_daily_mode() {
            var reporterConfiguration = new ReporterConfiguration();
            reporterConfiguration.setIndexMode("daily");
            when(client.getInfo()).thenReturn(Single.just(elasticsearchInfo("8.5.2")));
            BeanFactory beanFactory = BeanFactoryBuilder.buildFactory(client);

            assertThat(beanFactory).isNotNull();
            assertThat(beanFactory.createIndexNameGenerator(reporterConfiguration)).isInstanceOf(PerTypeAndDateIndexNameGenerator.class);
            assertThat(beanFactory.createIndexPreparer(reporterConfiguration, null, null, null)).isInstanceOf(ES8IndexPreparer.class);
        }

        @Test
        void should_instantiate_beans_for_elasticsearch_8_ilm_mode() {
            var reporterConfiguration = new ReporterConfiguration();
            reporterConfiguration.setIndexMode("ilm");

            when(client.getInfo()).thenReturn(Single.just(elasticsearchInfo("8.5.2")));
            BeanFactory beanFactory = BeanFactoryBuilder.buildFactory(client);

            assertThat(beanFactory).isNotNull();
            assertThat(beanFactory.createIndexNameGenerator(reporterConfiguration)).isInstanceOf(PerTypeIndexNameGenerator.class);
            assertThat(beanFactory.createIndexPreparer(reporterConfiguration, null, null, null)).isInstanceOf(ES8IndexPreparer.class);
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
            when(client.getInfo()).thenReturn(Single.just(opensearchInfo(version)));
            BeanFactory beanFactory = BeanFactoryBuilder.buildFactory(client);

            assertThat(beanFactory).isNotNull();
            assertThat(beanFactory.createIndexNameGenerator(reporterConfiguration)).isInstanceOf(PerTypeAndDateIndexNameGenerator.class);
            assertThat(beanFactory.createIndexPreparer(reporterConfiguration, null, null, null)).isInstanceOf(
                OpenSearchIndexPreparer.class
            );
        }

        @DisplayName("should instantiate beans for OpenSearch ilm mode")
        @ParameterizedTest(name = "with version {0}")
        @ValueSource(strings = { "1.0.0", "2.6" })
        void should_instantiate_beans_for_opensearch_ilm_mode(String version) {
            var reporterConfiguration = new ReporterConfiguration();
            reporterConfiguration.setIndexMode("ilm");

            when(client.getInfo()).thenReturn(Single.just(opensearchInfo(version)));
            BeanFactory beanFactory = BeanFactoryBuilder.buildFactory(client);

            assertThat(beanFactory).isNotNull();
            assertThat(beanFactory.createIndexNameGenerator(reporterConfiguration)).isInstanceOf(PerTypeIndexNameGenerator.class);
            assertThat(beanFactory.createIndexPreparer(reporterConfiguration, null, null, null)).isInstanceOf(
                OpenSearchIndexPreparer.class
            );
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

        ElasticsearchInfo elasticsearchInfo = new ElasticsearchInfo();
        elasticsearchInfo.setVersion(version);

        return elasticsearchInfo;
    }
}

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
package io.gravitee.apim.reporter.elasticsearch.factory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.gravitee.apim.reporter.elasticsearch.config.ReporterConfiguration;
import io.gravitee.apim.reporter.elasticsearch.mapping.datastream.DataStreamIndexPreparer;
import io.gravitee.elasticsearch.client.Client;
import io.gravitee.elasticsearch.version.ElasticsearchInfo;
import io.gravitee.elasticsearch.version.Version;
import io.reactivex.rxjava3.core.Single;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
class BeanFactoryBuilderDataStreamTest {

    private static final String ELASTICSEARCH = "elasticsearch";
    private static final String OPENSEARCH = "opensearch";

    @Mock
    Client client;

    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();
    private final ReporterConfiguration configuration = new ReporterConfiguration();

    @BeforeEach
    void setUp() {
        logs.start();
        builderLogger().addAppender(logs);
        configuration.setIndexMode("data_stream");
    }

    @AfterEach
    void tearDown() {
        builderLogger().detachAppender(logs);
    }

    @Nested
    class SupportedClusters {

        @ParameterizedTest(name = "{0} {1}")
        @CsvSource(
            {
                "elasticsearch, 7.9.0",
                "elasticsearch, 7.17.28",
                "elasticsearch, 8.13.4",
                "elasticsearch, 9.2.1",
                "opensearch, 1.3.20",
                "opensearch, 2.19.1",
            }
        )
        void should_write_data_streams(String distribution, String number) {
            givenCluster(distribution, number);

            final BeanFactory beanFactory = BeanFactoryBuilder.buildFactory(client, configuration);

            assertThat(beanFactory).isInstanceOf(DataStreamBeanFactory.class);
            assertThat(beanFactory.createIndexPreparer(configuration, null, null, null)).isInstanceOf(DataStreamIndexPreparer.class);
            assertThat(messages(Level.ERROR)).isEmpty();
            assertThat(summary()).contains("flavor [classic]").contains("index mode [data_stream]");
        }

        @Test
        void should_accept_the_data_stream_index_mode_whatever_its_case() {
            configuration.setIndexMode(" Data_Stream ");
            givenCluster(ELASTICSEARCH, "8.17.2");

            assertThat(BeanFactoryBuilder.buildFactory(client, configuration)).isInstanceOf(DataStreamBeanFactory.class);
        }

        @ParameterizedTest(name = "index_mode {0}")
        @ValueSource(strings = { "daily", "ilm" })
        void should_keep_the_classic_beans_for_the_other_index_modes(String indexMode) {
            configuration.setIndexMode(indexMode);
            configuration.setDataRetentionRequest("30d");
            givenCluster(ELASTICSEARCH, "7.8.1");

            assertThat(BeanFactoryBuilder.buildFactory(client, configuration)).isNotNull().isNotInstanceOf(DataStreamBeanFactory.class);
            assertThat(messages(Level.ERROR)).isEmpty();
            assertThat(messages(Level.WARN)).isEmpty();
        }
    }

    @Nested
    class Refusals {

        @ParameterizedTest(name = "elasticsearch {0}")
        @ValueSource(strings = { "7.0.0", "7.8.1" })
        void should_refuse_an_elasticsearch_older_than_7_9(String number) {
            givenCluster(ELASTICSEARCH, number);

            assertThat(BeanFactoryBuilder.buildFactory(client, configuration)).isNull();
            assertThat(messages(Level.ERROR))
                .singleElement()
                .satisfies(message ->
                    assertThat(message)
                        .contains("'reporters.elasticsearch.index_mode'")
                        .contains("data_stream")
                        .contains("7.9")
                        .contains(number)
                        .contains("The Elasticsearch reporter is disabled.")
                );
            assertThat(messages(Level.INFO)).noneMatch(message -> message.startsWith("Elasticsearch reporter: "));
        }

        @ParameterizedTest(name = "elasticsearch {0}")
        @ValueSource(strings = { "7.17.28", "8.13.4" })
        void should_refuse_a_retention_on_an_elasticsearch_older_than_8_14(String number) {
            configuration.setDataRetentionLog("7d");
            givenCluster(ELASTICSEARCH, number);

            assertThat(BeanFactoryBuilder.buildFactory(client, configuration)).isNull();
            assertThat(messages(Level.ERROR))
                .singleElement()
                .satisfies(message ->
                    assertThat(message)
                        .contains("'reporters.elasticsearch.lifecycle.retention.log'")
                        .contains("8.14")
                        .contains(number)
                        .contains("'reporters.elasticsearch.lifecycle.policies.log'")
                );
        }

        @Test
        void should_refuse_a_lifecycle_policy_on_opensearch() {
            configuration.setIndexLifecyclePolicyRequest("request-policy");
            givenCluster(OPENSEARCH, "2.19.1");

            assertThat(BeanFactoryBuilder.buildFactory(client, configuration)).isNull();
            assertThat(messages(Level.ERROR))
                .singleElement()
                .satisfies(message ->
                    assertThat(message)
                        .contains("'reporters.elasticsearch.lifecycle.policies.request'")
                        .contains("OpenSearch")
                        .contains("ism_template")
                        .contains("[gravitee-request, gravitee-v4-metrics, gravitee-v4-message-metrics]")
                );
        }

        @Test
        void should_refuse_a_retention_on_opensearch() {
            configuration.setDataRetentionEventMetrics("90d");
            givenCluster(OPENSEARCH, "2.19.1");

            assertThat(BeanFactoryBuilder.buildFactory(client, configuration)).isNull();
            assertThat(messages(Level.ERROR))
                .singleElement()
                .satisfies(message ->
                    assertThat(message)
                        .contains("'reporters.elasticsearch.lifecycle.retention.event_metrics'")
                        .contains("OpenSearch")
                        .contains("ism_template")
                        .contains("[gravitee-event-metrics]")
                );
        }

        @Test
        void should_refuse_a_lifecycle_policy_and_a_retention_on_the_same_type() {
            configuration.setIndexLifecyclePolicyRequest("request-policy");
            configuration.setDataRetentionRequest("30d");
            givenCluster(ELASTICSEARCH, "8.17.2");

            assertThat(BeanFactoryBuilder.buildFactory(client, configuration)).isNull();
            assertThat(messages(Level.ERROR))
                .singleElement()
                .satisfies(message ->
                    assertThat(message)
                        .contains("'reporters.elasticsearch.lifecycle.policies.request'")
                        .contains("'reporters.elasticsearch.lifecycle.retention.request'")
                        .contains("[gravitee-request, gravitee-v4-metrics, gravitee-v4-message-metrics]")
                        .contains("The Elasticsearch reporter is disabled.")
                );
        }
    }

    @Nested
    class Warnings {

        @Test
        void should_warn_once_about_every_type_kept_forever() {
            givenCluster(ELASTICSEARCH, "8.17.2");

            BeanFactoryBuilder.buildFactory(client, configuration);

            assertThat(messages(Level.WARN))
                .singleElement()
                .satisfies(message ->
                    assertThat(message)
                        .contains(
                            "[gravitee-request, gravitee-v4-metrics, gravitee-v4-message-metrics, gravitee-log, gravitee-v4-log, gravitee-v4-message-log, gravitee-monitor, gravitee-health, gravitee-event-metrics, gravitee-decisions]"
                        )
                        .contains("never deleted")
                        .contains("'reporters.elasticsearch.lifecycle.policies.<key>'")
                        .contains("'reporters.elasticsearch.lifecycle.retention.<key>'")
                        .contains("for the keys [request, log, monitor, health, event_metrics, decisions]")
                );
        }

        @Test
        void should_leave_out_the_types_with_a_policy_or_a_retention() {
            configuration.setIndexLifecyclePolicyRequest("request-policy");
            configuration.setDataRetentionLog("7d");
            configuration.setIndexLifecyclePolicyMonitor("monitor-policy");
            configuration.setIndexLifecyclePolicyHealth("health-policy");
            configuration.setDataRetentionEventMetrics("90d");
            givenCluster(ELASTICSEARCH, "8.17.2");

            BeanFactoryBuilder.buildFactory(client, configuration);

            assertThat(messages(Level.WARN))
                .singleElement()
                .satisfies(message -> assertThat(message).contains("[gravitee-decisions]").contains("for the keys [decisions]"));
        }

        @Test
        void should_only_suggest_a_policy_where_elasticsearch_has_no_retention() {
            givenCluster(ELASTICSEARCH, "8.13.4");

            BeanFactoryBuilder.buildFactory(client, configuration);

            assertThat(messages(Level.WARN))
                .singleElement()
                .satisfies(message ->
                    assertThat(message).contains("'reporters.elasticsearch.lifecycle.policies.<key>'").doesNotContain("retention")
                );
        }

        @Test
        void should_point_to_the_ism_template_on_opensearch() {
            givenCluster(OPENSEARCH, "2.19.1");

            BeanFactoryBuilder.buildFactory(client, configuration);

            assertThat(messages(Level.WARN))
                .singleElement()
                .satisfies(message ->
                    assertThat(message)
                        .contains("ism_template")
                        .contains(
                            "[gravitee-request, gravitee-v4-metrics, gravitee-v4-message-metrics, gravitee-log, gravitee-v4-log, gravitee-v4-message-log, gravitee-monitor, gravitee-health, gravitee-event-metrics, gravitee-decisions]"
                        )
                        .doesNotContain("*")
                        .doesNotContain("lifecycle.policies")
                        .doesNotContain("retention")
                );
        }
    }

    private void givenCluster(String distribution, String number) {
        final Version version = new Version();
        version.setDistribution(OPENSEARCH.equals(distribution) ? OPENSEARCH : null);
        version.setNumber(number);
        final ElasticsearchInfo info = new ElasticsearchInfo();
        info.setVersion(version);
        when(client.getInfo()).thenReturn(Single.just(info));
    }

    private String summary() {
        return messages(Level.INFO)
            .stream()
            .filter(message -> message.startsWith("Elasticsearch reporter: "))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No startup summary logged"));
    }

    private List<String> messages(Level level) {
        return logs.list
            .stream()
            .filter(event -> event.getLevel() == level)
            .map(ILoggingEvent::getFormattedMessage)
            .toList();
    }

    private static Logger builderLogger() {
        return (Logger) LoggerFactory.getLogger(BeanFactoryBuilder.class);
    }
}

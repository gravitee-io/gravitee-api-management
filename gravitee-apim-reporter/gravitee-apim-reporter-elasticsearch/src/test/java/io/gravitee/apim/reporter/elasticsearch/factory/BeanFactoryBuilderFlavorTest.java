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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.gravitee.apim.reporter.elasticsearch.config.ReporterConfiguration;
import io.gravitee.apim.reporter.elasticsearch.mapping.es8.ES8IndexPreparer;
import io.gravitee.elasticsearch.client.Client;
import io.gravitee.elasticsearch.version.ElasticsearchInfo;
import io.gravitee.elasticsearch.version.Version;
import io.reactivex.rxjava3.core.Single;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
class BeanFactoryBuilderFlavorTest {

    private static final String SERVERLESS_NOT_AVAILABLE = "Elastic Cloud Serverless index setup is not available";

    @Mock
    Client client;

    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();
    private final ReporterConfiguration configuration = new ReporterConfiguration();

    @BeforeEach
    void setUp() {
        logs.start();
        builderLogger().addAppender(logs);
    }

    @AfterEach
    void tearDown() {
        builderLogger().detachAppender(logs);
    }

    @Nested
    class Resolution {

        @ParameterizedTest(name = "build_flavor {0}")
        @NullSource
        @ValueSource(strings = { "default", "oss", "" })
        void should_detect_classic_when_the_build_flavor_is_not_serverless(String buildFlavor) {
            givenCluster(buildFlavor);

            final BeanFactory beanFactory = BeanFactoryBuilder.buildFactory(client, configuration);

            assertThat(beanFactory.createIndexPreparer(configuration, null, null, null)).isInstanceOf(ES8IndexPreparer.class);
            assertThat(summary()).contains("flavor [classic] (detected)");
        }

        @Test
        void should_detect_classic_on_opensearch() {
            final Version version = new Version();
            version.setDistribution("opensearch");
            version.setNumber("2.19.1");
            final ElasticsearchInfo info = new ElasticsearchInfo();
            info.setVersion(version);
            when(client.getInfo()).thenReturn(Single.just(info));

            assertThat(BeanFactoryBuilder.buildFactory(client, configuration)).isNotNull();
            assertThat(summary()).contains("flavor [classic] (detected)");
        }

        @Test
        void should_detect_serverless_when_the_build_flavor_is_serverless() {
            givenCluster("serverless");

            assertThat(BeanFactoryBuilder.buildFactory(client, configuration)).isNull();
            assertThat(summary()).contains("flavor [serverless] (detected)");
        }

        @ParameterizedTest(name = "build_flavor {0}")
        @ValueSource(strings = { "default", "serverless" })
        void should_detect_the_flavor_when_auto_is_configured(String buildFlavor) {
            configuration.setFlavor("auto");
            givenCluster(buildFlavor);

            BeanFactoryBuilder.buildFactory(client, configuration);

            assertThat(summary()).contains("(detected)");
        }

        @Test
        void should_use_the_classic_beans_when_classic_is_configured_on_serverless() {
            configuration.setFlavor("classic");
            givenCluster("serverless");

            final BeanFactory beanFactory = BeanFactoryBuilder.buildFactory(client, configuration);

            assertThat(beanFactory.createIndexPreparer(configuration, null, null, null)).isInstanceOf(ES8IndexPreparer.class);
            assertThat(summary()).contains("flavor [classic] (configured)");
        }

        @Test
        void should_resolve_serverless_when_serverless_is_configured_on_a_classic_cluster() {
            configuration.setFlavor("serverless");
            givenCluster("default");

            assertThat(BeanFactoryBuilder.buildFactory(client, configuration)).isNull();
            assertThat(summary()).contains("flavor [serverless] (configured)");
        }

        @Test
        void should_accept_the_flavor_whatever_its_case() {
            configuration.setFlavor("Classic");
            givenCluster("serverless");

            assertThat(BeanFactoryBuilder.buildFactory(client, configuration)).isNotNull();
        }

        @Test
        void should_refuse_an_unknown_flavor_without_calling_elasticsearch() {
            configuration.setFlavor("cloud");

            assertThat(BeanFactoryBuilder.buildFactory(client, configuration)).isNull();
            assertThat(messages(Level.ERROR))
                .singleElement()
                .satisfies(message ->
                    assertThat(message).contains("'reporters.elasticsearch.flavor'").contains("cloud").contains("auto, classic, serverless")
                );
            verify(client, never()).getInfo();
        }
    }

    @Nested
    class InterimServerlessBehaviour {

        @Test
        void should_stay_inactive_with_one_error_and_never_use_the_elasticsearch_8_beans() {
            givenCluster("serverless");

            assertThat(BeanFactoryBuilder.buildFactory(client, configuration)).isNull();
            assertThat(messages(Level.ERROR))
                .singleElement()
                .satisfies(message -> assertThat(message).contains(SERVERLESS_NOT_AVAILABLE));
        }

        @Test
        void should_accept_the_data_stream_index_mode() {
            configuration.setIndexMode("data_stream");
            givenCluster("serverless");

            assertThat(BeanFactoryBuilder.buildFactory(client, configuration)).isNull();
            assertThat(messages(Level.ERROR))
                .singleElement()
                .satisfies(message -> assertThat(message).contains(SERVERLESS_NOT_AVAILABLE));
        }
    }

    @Nested
    class Guardrails {

        @ParameterizedTest(name = "index_mode {0}")
        @ValueSource(strings = { "ilm", "daily", "ILM" })
        void should_refuse_an_explicit_ilm_or_daily_index_mode_on_serverless(String indexMode) {
            configuration.setIndexMode(indexMode);
            givenCluster("serverless");

            assertThat(BeanFactoryBuilder.buildFactory(client, configuration)).isNull();
            assertThat(messages(Level.ERROR))
                .singleElement()
                .satisfies(message -> assertThat(message).contains("'reporters.elasticsearch.index_mode'").contains(indexMode));
            assertThat(messages(Level.INFO)).noneMatch(message -> message.contains("flavor ["));
        }

        static Stream<Arguments> lifecyclePolicies() {
            return Stream.of(
                policy("request", ReporterConfiguration::setIndexLifecyclePolicyRequest),
                policy("monitor", ReporterConfiguration::setIndexLifecyclePolicyMonitor),
                policy("health", ReporterConfiguration::setIndexLifecyclePolicyHealth),
                policy("log", ReporterConfiguration::setIndexLifecyclePolicyLog),
                policy("event_metrics", ReporterConfiguration::setIndexLifecyclePolicyEventMetrics),
                policy("decisions", ReporterConfiguration::setIndexLifecyclePolicyDecisions)
            );
        }

        private static Arguments policy(String type, BiConsumer<ReporterConfiguration, String> setter) {
            return Arguments.of(type, setter);
        }

        @ParameterizedTest(name = "lifecycle.policies.{0}")
        @MethodSource("lifecyclePolicies")
        void should_refuse_a_lifecycle_policy_on_serverless(String type, BiConsumer<ReporterConfiguration, String> setter) {
            setter.accept(configuration, "my-policy");
            givenCluster("serverless");

            assertThat(BeanFactoryBuilder.buildFactory(client, configuration)).isNull();
            assertThat(messages(Level.ERROR))
                .singleElement()
                .satisfies(message -> assertThat(message).contains("'reporters.elasticsearch.lifecycle.policies." + type + "'"));
        }

        @ParameterizedTest(name = "flavor {0}")
        @NullSource
        @ValueSource(strings = { "classic" })
        void should_refuse_the_data_stream_index_mode_on_classic(String flavor) {
            configuration.setFlavor(flavor);
            configuration.setIndexMode("data_stream");
            givenCluster("default");

            assertThat(BeanFactoryBuilder.buildFactory(client, configuration)).isNull();
            assertThat(messages(Level.ERROR))
                .singleElement()
                .satisfies(message -> assertThat(message).contains("'reporters.elasticsearch.index_mode'").contains("data_stream"));
        }

        @Test
        void should_let_classic_bypass_the_serverless_rules() {
            configuration.setFlavor("classic");
            configuration.setIndexMode("ilm");
            configuration.setIndexLifecyclePolicyRequest("my-policy");
            givenCluster("serverless");

            assertThat(BeanFactoryBuilder.buildFactory(client, configuration)).isNotNull();
            assertThat(messages(Level.ERROR)).isEmpty();
        }
    }

    @Nested
    class StartupSummary {

        @Test
        void should_log_one_line_for_classic() {
            configuration.setIndexMode("ilm");
            configuration.setUsername("user");
            configuration.setPassword("secret");
            givenCluster("default");

            BeanFactoryBuilder.buildFactory(client, configuration);

            assertThat(summary()).isEqualTo(
                "Elasticsearch reporter: flavor [classic] (detected), authentication [basic], index mode [ilm], disabled by the flavor [none]"
            );
        }

        @Test
        void should_log_one_line_for_serverless() {
            configuration.setApiKey("encoded-key");
            givenCluster("serverless");

            BeanFactoryBuilder.buildFactory(client, configuration);

            assertThat(summary()).isEqualTo(
                "Elasticsearch reporter: flavor [serverless] (detected), authentication [api_key], index mode [data_stream], disabled by the flavor [index settings (number_of_shards, number_of_replicas, refresh_interval), lifecycle policies]"
            );
        }

        @Test
        void should_log_the_default_index_mode_and_no_authentication() {
            givenCluster("default");

            BeanFactoryBuilder.buildFactory(client, configuration);

            assertThat(summary()).contains("authentication [none]").contains("index mode [daily]");
        }

        @Test
        void should_not_log_the_summary_for_an_unsupported_version() {
            final ElasticsearchInfo info = info("default");
            info.getVersion().setNumber("6.8.23");
            when(client.getInfo()).thenReturn(Single.just(info));

            assertThat(BeanFactoryBuilder.buildFactory(client, configuration)).isNull();
            assertThat(messages(Level.INFO)).isEmpty();
        }
    }

    private void givenCluster(String buildFlavor) {
        when(client.getInfo()).thenReturn(Single.just(info(buildFlavor)));
    }

    private static ElasticsearchInfo info(String buildFlavor) {
        final Version version = new Version();
        version.setNumber("8.17.2");
        version.setBuildFlavor(buildFlavor);
        final ElasticsearchInfo info = new ElasticsearchInfo();
        info.setVersion(version);
        return info;
    }

    private String summary() {
        return messages(Level.INFO)
            .stream()
            .filter(message -> message.startsWith("Elasticsearch reporter: "))
            .reduce((first, second) -> {
                throw new AssertionError("More than one startup summary: " + first + " / " + second);
            })
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

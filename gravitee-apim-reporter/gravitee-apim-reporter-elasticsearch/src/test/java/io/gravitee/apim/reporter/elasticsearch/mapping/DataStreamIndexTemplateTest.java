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
package io.gravitee.apim.reporter.elasticsearch.mapping;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.apim.reporter.elasticsearch.config.PipelineConfiguration;
import io.gravitee.apim.reporter.elasticsearch.config.ReporterConfiguration;
import io.gravitee.apim.reporter.elasticsearch.mapping.datastream.DataStreamIndexPreparer;
import io.gravitee.apim.reporter.elasticsearch.mapping.datastream.DataStreamLifecycle;
import io.gravitee.apim.reporter.elasticsearch.mapping.es9.ES9IndexPreparer;
import io.gravitee.common.templating.FreeMarkerComponent;
import io.gravitee.elasticsearch.utils.Type;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DataStreamIndexTemplateTest {

    private static final ObjectMapper JSON = new ObjectMapper()
        .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
        .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);

    private static final DataStreamLifecycle ELASTICSEARCH_WITH_RETENTION = DataStreamLifecycle.elasticsearch(
        ReporterConfiguration.DEFAULT_INDEX_LIFECYCLE_POLICY_PROPERTY_NAME,
        true
    );
    private static final DataStreamLifecycle ELASTICSEARCH_WITHOUT_RETENTION = DataStreamLifecycle.elasticsearch(
        ReporterConfiguration.DEFAULT_INDEX_LIFECYCLE_POLICY_PROPERTY_NAME,
        false
    );
    private static final DataStreamLifecycle OPENSEARCH = DataStreamLifecycle.openSearch();

    static Stream<Type> types() {
        return Stream.of(Type.TYPES);
    }

    static Stream<Arguments> typesAndBackends() {
        return types().flatMap(type ->
            Stream.of(
                Arguments.of(type, ELASTICSEARCH_WITH_RETENTION),
                Arguments.of(type, ELASTICSEARCH_WITHOUT_RETENTION),
                Arguments.of(type, OPENSEARCH)
            )
        );
    }

    @Nested
    class Shape {

        @ParameterizedTest(name = "{0} {1}")
        @MethodSource("io.gravitee.apim.reporter.elasticsearch.mapping.DataStreamIndexTemplateTest#typesAndBackends")
        void should_declare_a_data_stream_named_exactly_after_the_index_prefix_and_the_type(Type type, DataStreamLifecycle lifecycle) {
            JsonNode template = template(type, new ReporterConfiguration(), lifecycle);

            assertThat(template.get("data_stream")).isInstanceOf(ObjectNode.class);
            assertThat(template.get("index_patterns")).isEqualTo(JSON.createArrayNode().add("gravitee-" + type.getType()));
        }

        @ParameterizedTest(name = "{0} {1}")
        @MethodSource("io.gravitee.apim.reporter.elasticsearch.mapping.DataStreamIndexTemplateTest#typesAndBackends")
        void should_never_refer_to_a_rollover_alias(Type type, DataStreamLifecycle lifecycle) {
            String template = preparer(withEveryPolicy(), lifecycle).generateIndexTemplate(type);

            assertThat(template).doesNotContain("rollover_alias");
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("io.gravitee.apim.reporter.elasticsearch.mapping.DataStreamIndexTemplateTest#types")
        void should_apply_the_configured_shard_replica_and_refresh_settings(Type type) {
            var configuration = new ReporterConfiguration();
            configuration.setNumberOfShards(3);
            configuration.setNumberOfReplicas(2);
            configuration.setRefreshInterval("10s");

            JsonNode settings = settings(template(type, configuration, ELASTICSEARCH_WITH_RETENTION));

            assertThat(settings.path("index.number_of_shards").asInt()).isEqualTo(3);
            assertThat(settings.path("index.number_of_replicas").asInt()).isEqualTo(2);
            assertThat(settings.path("index.refresh_interval").asText()).isEqualTo("10s");
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("io.gravitee.apim.reporter.elasticsearch.mapping.DataStreamIndexTemplateTest#types")
        void should_add_the_extended_settings_except_to_event_metrics_and_decisions(Type type) {
            var configuration = new ReporterConfiguration();
            configuration.setExtendedSettingsTemplate("extended-settings-test.ftl");

            JsonNode settings = settings(template(type, configuration, ELASTICSEARCH_WITH_RETENTION));

            assertThat(settings.has("index.mapping.total_fields.limit")).isEqualTo(type != Type.EVENT_METRICS && type != Type.DECISIONS);
        }
    }

    @Nested
    class Mappings {

        @ParameterizedTest(name = "{0} {1}")
        @MethodSource("io.gravitee.apim.reporter.elasticsearch.mapping.DataStreamIndexTemplateTest#typesAndBackends")
        void should_map_every_field_as_the_es9x_template_does(Type type, DataStreamLifecycle lifecycle) {
            var configuration = new ReporterConfiguration();

            assertThat(template(type, configuration, lifecycle).path("template").path("mappings"))
                .isNotEmpty()
                .isEqualTo(es9Template(type, configuration).path("template").path("mappings"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("io.gravitee.apim.reporter.elasticsearch.mapping.DataStreamIndexTemplateTest#types")
        void should_map_every_field_as_the_es9x_template_does_when_bodies_are_not_indexed(Type type) {
            var configuration = new ReporterConfiguration();
            configuration.setIndexBody(false);

            assertThat(template(type, configuration, OPENSEARCH).path("template").path("mappings")).isEqualTo(
                es9Template(type, configuration).path("template").path("mappings")
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("io.gravitee.apim.reporter.elasticsearch.mapping.DataStreamIndexTemplateTest#types")
        void should_define_the_analyzers_the_es9x_template_defines(Type type) {
            var configuration = new ReporterConfiguration();

            assertThat(settings(template(type, configuration, ELASTICSEARCH_WITH_RETENTION)).path("analysis")).isEqualTo(
                settings(es9Template(type, configuration)).path("analysis")
            );
        }
    }

    @Nested
    class Lifecycle {

        @Test
        void should_attach_the_ilm_policy_of_each_type_with_the_configured_property_name() {
            var configuration = withEveryPolicy();
            var lifecycle = DataStreamLifecycle.elasticsearch("index.custom.policy", true);

            assertThat(policies(configuration, lifecycle, "index.custom.policy")).containsExactly(
                "policy-request",
                "policy-monitor",
                "policy-health",
                "policy-log",
                "policy-log",
                "policy-request",
                "policy-log",
                "policy-request",
                "policy-event-metrics",
                "policy-decisions"
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("io.gravitee.apim.reporter.elasticsearch.mapping.DataStreamIndexTemplateTest#types")
        void should_attach_no_policy_on_opensearch(Type type) {
            // ISM ignores the policy_id index setting: an ISM policy reaches a data stream through its own ism_template.
            JsonNode settings = template(type, withEveryPolicy(), OPENSEARCH).path("template").path("settings");

            assertThat(settings.has("index.plugins.index_state_management.policy_id")).isFalse();
            assertThat(settings.has("index.lifecycle.name")).isFalse();
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("io.gravitee.apim.reporter.elasticsearch.mapping.DataStreamIndexTemplateTest#types")
        void should_write_the_retention_of_the_type_as_data_retention(Type type) {
            JsonNode template = template(type, withEveryRetention(), ELASTICSEARCH_WITH_RETENTION);

            assertThat(template.path("template").path("lifecycle").path("data_retention").asText()).isEqualTo(expectedRetention(type));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("io.gravitee.apim.reporter.elasticsearch.mapping.DataStreamIndexTemplateTest#types")
        void should_not_write_a_lifecycle_when_the_cluster_has_no_data_stream_lifecycle(Type type) {
            assertThat(template(type, withEveryRetention(), ELASTICSEARCH_WITHOUT_RETENTION).path("template").has("lifecycle")).isFalse();
            assertThat(template(type, withEveryRetention(), OPENSEARCH).path("template").has("lifecycle")).isFalse();
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("io.gravitee.apim.reporter.elasticsearch.mapping.DataStreamIndexTemplateTest#types")
        void should_set_no_lifecycle_when_nothing_is_configured(Type type) {
            String template = preparer(new ReporterConfiguration(), ELASTICSEARCH_WITH_RETENTION).generateIndexTemplate(type);

            assertThat(template).doesNotContain("lifecycle").doesNotContain("policy_id");
        }

        private String expectedRetention(Type type) {
            return switch (type) {
                case REQUEST, V4_METRICS, V4_MESSAGE_METRICS -> "30d";
                case LOG, V4_LOG, V4_MESSAGE_LOG -> "7d";
                case MONITOR -> "3d";
                case HEALTH_CHECK -> "14d";
                case EVENT_METRICS -> "90d";
                case DECISIONS -> "60d";
            };
        }

        private Stream<String> policies(ReporterConfiguration configuration, DataStreamLifecycle lifecycle, String propertyName) {
            return types().map(type -> settings(template(type, configuration, lifecycle)).path(propertyName).asText());
        }
    }

    private static ReporterConfiguration withEveryPolicy() {
        var configuration = new ReporterConfiguration();
        configuration.setIndexLifecyclePolicyRequest("policy-request");
        configuration.setIndexLifecyclePolicyHealth("policy-health");
        configuration.setIndexLifecyclePolicyMonitor("policy-monitor");
        configuration.setIndexLifecyclePolicyLog("policy-log");
        configuration.setIndexLifecyclePolicyEventMetrics("policy-event-metrics");
        configuration.setIndexLifecyclePolicyDecisions("policy-decisions");
        return configuration;
    }

    private static ReporterConfiguration withEveryRetention() {
        var configuration = new ReporterConfiguration();
        configuration.setDataRetentionRequest("30d");
        configuration.setDataRetentionHealth("14d");
        configuration.setDataRetentionMonitor("3d");
        configuration.setDataRetentionLog("7d");
        configuration.setDataRetentionEventMetrics("90d");
        configuration.setDataRetentionDecisions("60d");
        return configuration;
    }

    private static JsonNode settings(JsonNode template) {
        return template.path("template").path("settings");
    }

    @SneakyThrows
    private static JsonNode template(Type type, ReporterConfiguration configuration, DataStreamLifecycle lifecycle) {
        return JSON.readTree(preparer(configuration, lifecycle).generateIndexTemplate(type));
    }

    private static DataStreamIndexPreparer preparer(ReporterConfiguration configuration, DataStreamLifecycle lifecycle) {
        var freeMarkerComponent = freeMarkerComponent();
        return new DataStreamIndexPreparer(
            configuration,
            new PipelineConfiguration(freeMarkerComponent),
            freeMarkerComponent,
            null,
            lifecycle
        );
    }

    @SneakyThrows
    private static JsonNode es9Template(Type type, ReporterConfiguration configuration) {
        var freeMarkerComponent = freeMarkerComponent();
        return JSON.readTree(
            new ES9IndexPreparer(
                configuration,
                new PipelineConfiguration(freeMarkerComponent),
                freeMarkerComponent,
                null
            ).generateIndexTemplate(type)
        );
    }

    private static FreeMarkerComponent freeMarkerComponent() {
        return FreeMarkerComponent.builder()
            .classLoader(DataStreamIndexTemplateTest.class.getClassLoader())
            .classLoaderTemplateBase("freemarker")
            .build();
    }
}

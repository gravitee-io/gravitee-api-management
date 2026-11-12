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
import io.gravitee.apim.reporter.elasticsearch.mapping.es9.ES9IndexPreparer;
import io.gravitee.apim.reporter.elasticsearch.mapping.serverless.ServerlessIndexPreparer;
import io.gravitee.common.templating.FreeMarkerComponent;
import io.gravitee.elasticsearch.utils.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * The serverless templates are published in the es-index-mappings distribution for Elastic Cloud Serverless,
 * which rejects the shard settings and manages refresh, lifecycle and retention itself.
 */
class ServerlessIndexTemplateTest {

    private static final ObjectMapper JSON = new ObjectMapper()
        .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
        .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);

    static Stream<Type> types() {
        return Stream.of(Type.TYPES);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("types")
    void should_declare_a_data_stream(Type type) {
        assertThat(serverlessTemplate(type, configurationWithInfrastructureSettings()).get("data_stream")).isInstanceOf(ObjectNode.class);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("types")
    void should_carry_no_shard_replica_or_refresh_setting(Type type) {
        JsonNode settings = serverlessTemplate(type, configurationWithInfrastructureSettings()).path("template").path("settings");

        assertThat(fieldNames(settings)).noneMatch(
            name -> name.contains("number_of_shards") || name.contains("number_of_replicas") || name.contains("refresh_interval")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("types")
    void should_carry_no_lifecycle_or_retention(Type type) {
        String template = serverlessPreparer(configurationWithInfrastructureSettings()).generateIndexTemplate(type);

        assertThat(template).doesNotContain("lifecycle").doesNotContain("data_retention").doesNotContain("rollover_alias");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("types")
    void should_only_set_the_analysis_settings_the_mappings_need(Type type) {
        JsonNode settings = serverlessTemplate(type, configurationWithInfrastructureSettings()).path("template").path("settings");
        JsonNode es9Settings = es9Template(type, new ReporterConfiguration()).path("template").path("settings");

        assertThat(fieldNames(settings)).isSubsetOf("analysis");
        assertThat(settings.path("analysis")).isEqualTo(es9Settings.path("analysis"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("types")
    void should_map_every_field_as_the_es9x_template_does(Type type) {
        var configuration = new ReporterConfiguration();

        assertThat(serverlessTemplate(type, configuration).path("template").path("mappings"))
            .isNotEmpty()
            .isEqualTo(es9Template(type, configuration).path("template").path("mappings"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("types")
    void should_map_every_field_as_the_es9x_template_does_when_bodies_are_not_indexed(Type type) {
        var configuration = new ReporterConfiguration();
        configuration.setIndexBody(false);

        assertThat(serverlessTemplate(type, configuration).path("template").path("mappings")).isEqualTo(
            es9Template(type, configuration).path("template").path("mappings")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("types")
    void should_match_the_data_stream_named_after_the_index_prefix_and_the_type(Type type) {
        assertThat(serverlessTemplate(type, new ReporterConfiguration()).get("index_patterns")).isEqualTo(
            JSON.createArrayNode().add("gravitee-" + type.getType() + "*")
        );
    }

    /** Every setting the classic templates render, so that none of them can leak into the serverless ones. */
    private static ReporterConfiguration configurationWithInfrastructureSettings() {
        var configuration = new ReporterConfiguration();
        configuration.setNumberOfShards(3);
        configuration.setNumberOfReplicas(2);
        configuration.setRefreshInterval("10s");
        configuration.setIndexLifecyclePolicyRequest("policy-request");
        configuration.setIndexLifecyclePolicyHealth("policy-health");
        configuration.setIndexLifecyclePolicyMonitor("policy-monitor");
        configuration.setIndexLifecyclePolicyLog("policy-log");
        configuration.setIndexLifecyclePolicyEventMetrics("policy-event-metrics");
        configuration.setIndexLifecyclePolicyDecisions("policy-decisions");
        return configuration;
    }

    private static List<String> fieldNames(JsonNode node) {
        List<String> names = new ArrayList<>();
        node.fieldNames().forEachRemaining(names::add);
        return names;
    }

    @SneakyThrows
    private static JsonNode serverlessTemplate(Type type, ReporterConfiguration configuration) {
        return JSON.readTree(serverlessPreparer(configuration).generateIndexTemplate(type));
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

    private static ServerlessIndexPreparer serverlessPreparer(ReporterConfiguration configuration) {
        var freeMarkerComponent = freeMarkerComponent();
        return new ServerlessIndexPreparer(configuration, new PipelineConfiguration(freeMarkerComponent), freeMarkerComponent, null);
    }

    private static FreeMarkerComponent freeMarkerComponent() {
        return FreeMarkerComponent.builder()
            .classLoader(ServerlessIndexTemplateTest.class.getClassLoader())
            .classLoaderTemplateBase("freemarker")
            .build();
    }
}

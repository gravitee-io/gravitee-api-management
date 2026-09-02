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

import io.gravitee.apim.reporter.elasticsearch.config.PipelineConfiguration;
import io.gravitee.apim.reporter.elasticsearch.config.ReporterConfiguration;
import io.gravitee.apim.reporter.elasticsearch.mapping.es7.ES7IndexPreparer;
import io.gravitee.apim.reporter.elasticsearch.mapping.es8.ES8IndexPreparer;
import io.gravitee.apim.reporter.elasticsearch.mapping.es9.ES9IndexPreparer;
import io.gravitee.apim.reporter.elasticsearch.mapping.opensearch.OpenSearchIndexPreparer;
import io.gravitee.common.templating.FreeMarkerComponent;
import io.gravitee.elasticsearch.utils.Type;
import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Asserts how the log templates map captured request/response bodies, across every template tree and every
 * log type that carries a body.
 *
 * <p>Bodies are the most expensive thing the reporter indexes — a gateway capturing 256KB payloads hands
 * Elasticsearch up to four analysed bodies per request — and the only query that ever reads them is a
 * {@code query_string} prefix search. Positions and norms are therefore paid for and never used, which is
 * what these tests pin down.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class LogBodyMappingTest {

    /**
     * Where each log type keeps a captured payload, as a parent object property and the field under it.
     */
    private record BodyPath(String parent, String field) {}

    static Stream<Arguments> trees_and_log_types() {
        return Stream.of("es7x", "es8x", "es9x", "opensearch").flatMap(tree ->
            Stream.of(Type.LOG, Type.V4_LOG, Type.V4_MESSAGE_LOG).map(type -> Arguments.of(tree, type))
        );
    }

    /**
     * The analyzer each tree already picked for a given log type, which this change must leave alone. es7x
     * is the odd one out: it maps v4 bodies with the Elasticsearch default while every other body in every
     * other tree goes through {@code gravitee_body_analyzer}.
     */
    private static String expectedAnalyzer(String tree, Type type) {
        boolean elasticsearchDefault = "es7x".equals(tree) && type != Type.LOG;
        return elasticsearchDefault ? null : "gravitee_body_analyzer";
    }

    @ParameterizedTest(name = "{0} {1} indexes bodies without positions or norms")
    @MethodSource("trees_and_log_types")
    void should_index_bodies_for_prefix_search_only(String tree, Type type) {
        var bodyFields = bodyFieldsOf(render(tree, type, new ReporterConfiguration()), type);

        assertThat(bodyFields).allSatisfy(body ->
            assertThat(body).containsEntry("type", "text").containsEntry("index_options", "docs").containsEntry("norms", false)
        );
    }

    @ParameterizedTest(name = "{0} {1} keeps indexing bodies searchable with the analyzer it already used")
    @MethodSource("trees_and_log_types")
    void should_keep_bodies_searchable_with_their_existing_analyzer(String tree, Type type) {
        var bodyFields = bodyFieldsOf(render(tree, type, new ReporterConfiguration()), type);

        assertThat(bodyFields).allSatisfy(body -> {
            assertThat(body).doesNotContainKey("index");
            if (expectedAnalyzer(tree, type) == null) {
                assertThat(body).doesNotContainKey("analyzer");
            } else {
                assertThat(body).containsEntry("analyzer", expectedAnalyzer(tree, type));
            }
        });
    }

    @ParameterizedTest(name = "{0} {1} leaves bodies unindexed when body indexing is off")
    @MethodSource("trees_and_log_types")
    void should_not_index_bodies_at_all_when_disabled(String tree, Type type) {
        var configuration = new ReporterConfiguration();
        configuration.setIndexBody(false);

        var bodyFields = bodyFieldsOf(render(tree, type, configuration), type);

        // Elasticsearch rejects a mapping that sets analyzer, index_options or norms on an unindexed field,
        // so turning indexing off has to drop all three rather than merely add "index": false next to them.
        assertThat(bodyFields).allSatisfy(body ->
            assertThat(body)
                .containsEntry("type", "text")
                .containsEntry("index", false)
                .doesNotContainKey("analyzer")
                .doesNotContainKey("index_options")
                .doesNotContainKey("norms")
        );
    }

    @ParameterizedTest(name = "{0} {1} maps every captured payload")
    @MethodSource("trees_and_log_types")
    void should_map_every_captured_payload(String tree, Type type) {
        assertThat(bodyFieldsOf(render(tree, type, new ReporterConfiguration()), type)).hasSameSizeAs(bodyPathsOf(type));
    }

    private static String render(String tree, Type type, ReporterConfiguration configuration) {
        var freeMarkerComponent = FreeMarkerComponent.builder()
            .classLoader(LogBodyMappingTest.class.getClassLoader())
            .classLoaderTemplateBase("freemarker")
            .build();
        var pipelineConfiguration = new PipelineConfiguration(freeMarkerComponent);

        // No client: generateIndexTemplate only renders, it never talks to the cluster.
        AbstractIndexPreparer preparer = switch (tree) {
            case "es7x" -> new ES7IndexPreparer(configuration, pipelineConfiguration, freeMarkerComponent, null);
            case "es8x" -> new ES8IndexPreparer(configuration, pipelineConfiguration, freeMarkerComponent, null);
            case "es9x" -> new ES9IndexPreparer(configuration, pipelineConfiguration, freeMarkerComponent, null);
            case "opensearch" -> new OpenSearchIndexPreparer(configuration, pipelineConfiguration, freeMarkerComponent, null);
            default -> throw new IllegalArgumentException("Unknown template tree: " + tree);
        };

        return preparer.generateIndexTemplate(type);
    }

    /**
     * Reads the body field mappings out of a rendered template. es7x puts {@code mappings} at the root while
     * the composable-template trees nest it under {@code template}, so both shapes are accepted.
     */
    private static List<Map<String, Object>> bodyFieldsOf(String renderedTemplate, Type type) {
        var root = new JsonObject(renderedTemplate);
        var mappings = root.containsKey("mappings")
            ? root.getJsonObject("mappings")
            : root.getJsonObject("template").getJsonObject("mappings");
        var properties = mappings.getJsonObject("properties");

        return bodyPathsOf(type)
            .stream()
            .map(path -> properties.getJsonObject(path.parent()).getJsonObject("properties").getJsonObject(path.field()).getMap())
            .toList();
    }

    private static List<BodyPath> bodyPathsOf(Type type) {
        return switch (type) {
            case LOG -> List.of(
                new BodyPath("client-request", "body"),
                new BodyPath("client-response", "body"),
                new BodyPath("proxy-request", "body"),
                new BodyPath("proxy-response", "body")
            );
            case V4_LOG -> List.of(
                new BodyPath("entrypoint-request", "body"),
                new BodyPath("entrypoint-response", "body"),
                new BodyPath("endpoint-request", "body"),
                new BodyPath("endpoint-response", "body")
            );
            case V4_MESSAGE_LOG -> List.of(new BodyPath("message", "payload"));
            default -> throw new IllegalArgumentException("Type carries no body: " + type);
        };
    }
}

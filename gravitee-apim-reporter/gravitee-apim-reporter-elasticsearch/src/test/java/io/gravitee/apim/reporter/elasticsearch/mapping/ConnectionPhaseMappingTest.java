/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
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
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Readers exclude the document a message API writes when a connection opens with a term filter on
 * {@code connection-phase}, so the field is a keyword on both connection indices of every template tree.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ConnectionPhaseMappingTest {

    @ParameterizedTest(name = "{0} {1} maps connection-phase as a keyword")
    @CsvSource(
        {
            "es7x, V4_METRICS",
            "es7x, V4_LOG",
            "es8x, V4_METRICS",
            "es8x, V4_LOG",
            "es9x, V4_METRICS",
            "es9x, V4_LOG",
            "opensearch, V4_METRICS",
            "opensearch, V4_LOG",
        }
    )
    void should_map_connection_phase_as_a_keyword(String tree, Type type) {
        var connectionPhase = propertiesOf(tree, type).getJsonObject("connection-phase");

        assertThat(connectionPhase).as("%s %s declares connection-phase", tree, type).isNotNull();
        assertThat(connectionPhase.getMap()).containsEntry("type", "keyword");
    }

    /** es7x puts {@code mappings} at the root while the composable-template trees nest it under {@code template}. */
    private static JsonObject propertiesOf(String tree, Type type) {
        var root = new JsonObject(render(tree, type));
        var mappings = root.containsKey("mappings")
            ? root.getJsonObject("mappings")
            : root.getJsonObject("template").getJsonObject("mappings");

        return mappings.getJsonObject("properties");
    }

    private static String render(String tree, Type type) {
        var freeMarkerComponent = FreeMarkerComponent.builder()
            .classLoader(ConnectionPhaseMappingTest.class.getClassLoader())
            .classLoaderTemplateBase("freemarker")
            .build();
        var pipelineConfiguration = new PipelineConfiguration(freeMarkerComponent);
        var configuration = new ReporterConfiguration();

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
}

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
import java.util.Map;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Pins how the v4-metrics templates map the request path, across every template tree.
 *
 * <p>The analytics engine reads the HTTP path dimension — filter and facet alike — from
 * {@code path-info.keyword} (APIM-15072). No template ever declared {@code path-info}, so the field existed
 * only through Elasticsearch dynamic mapping, whose default for a string caps the keyword sub-field at
 * {@code ignore_above: 256}. A longer path was therefore absent from every filter match and every facet
 * bucket, silently — the same failure mode APIM-15072 had just fixed, one level down.
 *
 * <p>The sub-field is part of the contract: the resolver queries {@code path-info.keyword}, and a search
 * spans indices created on both sides of a template change, so the queryable path has to stay identical.
 * The always-empty {@code path} that used to sit next to it is gone; this pins that it stays gone.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class V4MetricsPathMappingTest {

    @ParameterizedTest(name = "{0} declares path-info with the keyword sub-field the engine queries")
    @ValueSource(strings = { "es7x", "es8x", "es9x", "opensearch" })
    void should_declare_path_info_with_a_keyword_sub_field(String tree) {
        assertThat(pathInfoKeywordOf(tree)).containsEntry("type", "keyword");
    }

    @ParameterizedTest(name = "{0} indexes a path of any length into path-info.keyword")
    @ValueSource(strings = { "es7x", "es8x", "es9x", "opensearch" })
    void should_not_cap_the_indexed_path_length(String tree) {
        // ignore_above drops the value from the sub-field altogether, so a long path stops matching the
        // HTTP_PATH filter and disappears from its facet buckets, with nothing reported.
        //
        // The opposite call from IndexTemplateTest#should_bound_additional_keyword_metrics_..., which bounds
        // additional-metrics.keyword_* precisely so one oversized value cannot break indexing. Both hold:
        // that one guards a client-controlled value with no other bound, whereas a path arrives on the
        // request line, capped by maxInitialLineLength (4096 by default). Past Lucene's 32766-byte term
        // limit Elasticsearch rejects the whole document, but `uri` on that same document is an uncapped
        // keyword and always at least as long, so the record was already lost — a cap here would buy
        // nothing and would only hide long paths from the filter and the facet.
        assertThat(pathInfoKeywordOf(tree)).doesNotContainKey("ignore_above");
    }

    @ParameterizedTest(name = "{0} does not declare the path field v4 never writes")
    @ValueSource(strings = { "es7x", "es8x", "es9x", "opensearch" })
    void should_not_declare_the_unwritten_path_field(String tree) {
        // v4 reports the path after the context path as path-info; `path` belongs to the v2 request index.
        // Declared here but empty on every document, it read as a usable dimension and cost APIM-15072 to
        // disprove.
        //
        // Removing it is not entirely free, and the case that occurs daily is the gravitee-v4-metrics-*
        // wildcard itself, spanning indices created before and after this change for the whole retention
        // window. Measured on Elasticsearch 8.17.2 across one such pair: a term filter and a terms
        // aggregation are unchanged (both already returned nothing), while a sort degrades to a 200 with
        // fewer hits — the shard whose mapping lacks the field fails, and the search reports it as a
        // partial result. Nothing in APIM sorts on this field, so the exposure was weighed and accepted;
        // a direct consumer of the indices that does sort on it wants unmapped_type.
        assertThat(propertiesOf(tree).getMap()).doesNotContainKey("path");
    }

    private static Map<String, Object> pathInfoKeywordOf(String tree) {
        var pathInfo = propertiesOf(tree).getJsonObject("path-info");
        assertThat(pathInfo).as("%s declares path-info", tree).isNotNull();
        var fields = pathInfo.getJsonObject("fields");
        assertThat(fields).as("%s gives path-info a sub-field", tree).isNotNull();
        var keyword = fields.getJsonObject("keyword");
        assertThat(keyword).as("%s names that sub-field keyword", tree).isNotNull();
        return keyword.getMap();
    }

    /**
     * Reads the v4-metrics field mappings out of a rendered template. es7x puts {@code mappings} at the root
     * while the composable-template trees nest it under {@code template}, so both shapes are accepted.
     */
    private static JsonObject propertiesOf(String tree) {
        var root = new JsonObject(render(tree));
        var mappings = root.containsKey("mappings")
            ? root.getJsonObject("mappings")
            : root.getJsonObject("template").getJsonObject("mappings");

        return mappings.getJsonObject("properties");
    }

    private static String render(String tree) {
        var freeMarkerComponent = FreeMarkerComponent.builder()
            .classLoader(V4MetricsPathMappingTest.class.getClassLoader())
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

        return preparer.generateIndexTemplate(Type.V4_METRICS);
    }
}

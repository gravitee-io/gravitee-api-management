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

import io.gravitee.apim.reporter.elasticsearch.IntegrationTestConfiguration;
import io.gravitee.apim.reporter.elasticsearch.config.PipelineConfiguration;
import io.gravitee.apim.reporter.elasticsearch.config.ReporterConfiguration;
import io.gravitee.apim.reporter.elasticsearch.mapping.es9.ES9IndexPreparer;
import io.gravitee.common.templating.FreeMarkerComponent;
import io.gravitee.elasticsearch.client.Client;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * Drives the v4-metrics template against a real Elasticsearch container to prove what an operator actually
 * gets from the HTTP path dimension: a long request path stays filterable and stays in the facet buckets.
 *
 * <p>That is the behaviour the declaration exists for. Left to dynamic mapping, {@code path-info} is a
 * {@code text} field whose {@code keyword} sub-field carries the Elasticsearch default {@code
 * ignore_above: 256}, so a longer path is not indexed into it: the {@code HTTP_PATH} filter stops matching
 * and the {@code HTTP_PATH} facet loses the bucket, silently. Both queries below are the shapes
 * {@code HTTPFieldResolver} produces, over {@code path-info.keyword}.
 *
 * <p>Scope: what the templates render is asserted by {@link V4MetricsPathMappingTest}, which needs no
 * container.
 */
@SpringJUnitConfig(IntegrationTestConfiguration.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class V4MetricsPathIndexingIntegrationTest {

    /** Comfortably past the 256 the default dynamic mapping would have capped the sub-field at. */
    private static final String LONG_PATH = "/orders/" + "a".repeat(300);

    private static final String PATH_KEYWORD = "path-info.keyword";

    @Autowired
    private Client client;

    @Autowired
    private ReporterConfiguration configuration;

    @Autowired
    private FreeMarkerComponent freeMarkerComponent;

    @Autowired
    private PipelineConfiguration pipelineConfiguration;

    @Test
    void should_match_a_long_path_on_the_http_path_filter() {
        String index = prepareAndIndexOneMetric("gravitee-long-path-filter");

        assertThat(termHits(index, LONG_PATH)).isOne();
    }

    @Test
    void should_bucket_a_long_path_in_the_http_path_facet() {
        String index = prepareAndIndexOneMetric("gravitee-long-path-facet");

        assertThat(facetKeys(index)).containsExactly(LONG_PATH);
    }

    /**
     * Puts every index template through the reporter's own preparer, then indexes a single v4 metric into an
     * index the {@code -v4-metrics} template pattern covers. The bulk is refreshed so the searches that
     * follow see it without the test having to wait on {@code refresh_interval}.
     */
    private String prepareAndIndexOneMetric(String indexName) {
        configuration.setIndexName(indexName);

        new ES9IndexPreparer(configuration, pipelineConfiguration, freeMarkerComponent, client)
            .prepare()
            .test()
            .awaitDone(60, TimeUnit.SECONDS)
            .assertComplete();

        String index = indexName + "-v4-metrics-test";
        var document = JsonObject.of(
            "@timestamp",
            "2026-09-08T10:00:00.000Z",
            "api-id",
            "an-api",
            "request-id",
            "a-request",
            "uri",
            LONG_PATH + "?page=2",
            "path-info",
            LONG_PATH
        );
        var bulk = Buffer.buffer(JsonObject.of("index", JsonObject.of("_index", index)).encode() + "\n" + document.encode() + "\n");

        var response = client.bulk(bulk, true).blockingGet();
        assertThat(response.getErrors()).as("bulk indexing into %s reported errors: %s", index, response.getItems()).isFalse();

        return index;
    }

    /** The shape the analytics engine builds for an {@code EQ} filter on {@code HTTP_PATH}. */
    private long termHits(String index, String path) {
        var query = JsonObject.of("query", JsonObject.of("term", JsonObject.of(PATH_KEYWORD, path)));

        return client.search(index, null, query.encode()).blockingGet().getSearchHits().getTotal().getValue();
    }

    /** The shape the analytics engine builds for the {@code HTTP_PATH} facet. */
    private List<String> facetKeys(String index) {
        var query = JsonObject.of(
            "size",
            0,
            "aggs",
            JsonObject.of("paths", JsonObject.of("terms", JsonObject.of("field", PATH_KEYWORD, "size", 10)))
        );

        return client
            .search(index, null, query.encode())
            .blockingGet()
            .getAggregations()
            .get("paths")
            .getBuckets()
            .stream()
            .map(bucket -> bucket.get("key").asText())
            .toList();
    }
}

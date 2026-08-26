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
import io.gravitee.elasticsearch.model.SearchHit;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * Drives the v4 log template against a real Elasticsearch container, in both body-indexing modes, to prove
 * what an operator actually gets: the cluster accepts the rendered mapping, the Logs UI payload search still
 * matches when body indexing is on, and switching it off stops the matching without hiding the body itself.
 *
 * <p>That last point is the whole trade: turning payload search off is only an acceptable answer to an
 * overloaded cluster if the Logs UI detail view keeps showing the payload, which it reads from
 * {@code _source} rather than from the inverted index.
 *
 * <p>Scope: what the templates render is asserted by {@link LogBodyMappingTest}, which needs no container.
 */
@SpringJUnitConfig(IntegrationTestConfiguration.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class LogBodyIndexingIntegrationTest {

    private static final String NEEDLE = "gravitee-needle-42";

    @Autowired
    private Client client;

    @Autowired
    private ReporterConfiguration configuration;

    @Autowired
    private FreeMarkerComponent freeMarkerComponent;

    @Autowired
    private PipelineConfiguration pipelineConfiguration;

    @Test
    void should_match_the_payload_search_when_bodies_are_indexed() {
        String index = prepareAndIndexOneLog("gravitee-body-indexed", true);

        assertThat(payloadSearchHits(index, NEEDLE)).isOne();
    }

    @Test
    void should_not_match_the_payload_search_when_body_indexing_is_off() {
        String index = prepareAndIndexOneLog("gravitee-body-unindexed", false);

        assertThat(payloadSearchHits(index, NEEDLE)).isZero();
    }

    @Test
    void should_still_return_the_body_to_the_logs_ui_when_body_indexing_is_off() {
        String index = prepareAndIndexOneLog("gravitee-body-unindexed-source", false);

        var hits = searchAll(index);

        assertThat(hits.getFirst().getSource().path("entrypoint-response").path("body").asText()).contains(NEEDLE);
    }

    /**
     * Puts every index template through the reporter's own preparer, then indexes a single v4 log document
     * into an index the {@code -v4-log} template pattern covers. The bulk is refreshed so the search that
     * follows sees it without the test having to wait on {@code refresh_interval}.
     */
    private String prepareAndIndexOneLog(String indexName, boolean indexBody) {
        configuration.setIndexName(indexName);
        configuration.setIndexBody(indexBody);

        new ES9IndexPreparer(configuration, pipelineConfiguration, freeMarkerComponent, client)
            .prepare()
            .test()
            .awaitDone(60, TimeUnit.SECONDS)
            .assertComplete();

        String index = indexName + "-v4-log-test";
        // gravitee_body_analyzer splits on whitespace only, so the needle has to sit in a payload as its own
        // whitespace-delimited word for the prefix query to reach it — exactly the constraint an operator
        // hits when searching payloads today, and unrelated to how the field is indexed.
        var document = JsonObject.of(
            "@timestamp",
            "2026-08-26T10:00:00.000Z",
            "api-id",
            "an-api",
            "request-id",
            "a-request",
            "entrypoint-request",
            JsonObject.of("body", "{\"note\":\"placed " + NEEDLE + " for review\"}"),
            "entrypoint-response",
            JsonObject.of("body", "{\"note\":\"accepted " + NEEDLE + " ok\"}")
        );
        var bulk = Buffer.buffer(JsonObject.of("index", JsonObject.of("_index", index)).encode() + "\n" + document.encode() + "\n");

        var response = client.bulk(bulk, true).blockingGet();
        assertThat(response.getErrors()).as("bulk indexing into %s reported errors: %s", index, response.getItems()).isFalse();

        return index;
    }

    /**
     * The query the Logs UI issues for "search in payloads" — see
     * {@code SearchConnectionLogDetailQueryAdapter#addBodyTextFilter} in the Elasticsearch repository, which
     * owns this shape. A prefix {@code query_string} over every {@code *.body} field, nothing more.
     */
    private long payloadSearchHits(String index, String bodyText) {
        var query = JsonObject.of(
            "query",
            JsonObject.of(
                "bool",
                JsonObject.of("must", JsonArray.of(JsonObject.of("query_string", JsonObject.of("query", "\\*.body:" + bodyText + "*"))))
            )
        );

        return client.search(index, null, query.encode()).blockingGet().getSearchHits().getTotal().getValue();
    }

    private List<SearchHit> searchAll(String index) {
        return client
            .search(index, null, JsonObject.of("query", JsonObject.of("match_all", JsonObject.of())).encode())
            .blockingGet()
            .getSearchHits()
            .getHits();
    }
}

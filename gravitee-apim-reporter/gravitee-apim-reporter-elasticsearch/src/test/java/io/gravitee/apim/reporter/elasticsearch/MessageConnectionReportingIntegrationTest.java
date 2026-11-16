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
package io.gravitee.apim.reporter.elasticsearch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.reporter.elasticsearch.config.PipelineConfiguration;
import io.gravitee.apim.reporter.elasticsearch.config.ReporterConfiguration;
import io.gravitee.common.templating.FreeMarkerComponent;
import io.gravitee.elasticsearch.client.Client;
import io.gravitee.elasticsearch.model.SearchHit;
import io.gravitee.node.api.Node;
import io.gravitee.reporter.api.v4.log.Log;
import io.gravitee.reporter.api.v4.metric.Metrics;
import io.vertx.core.json.JsonObject;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * A message API reports a connection twice, at opening and at end, with the same instances completed in place in
 * between. Both must land as documents of their own, the opening as it was when reported.
 */
@SpringJUnitConfig(IntegrationTestConfiguration.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class MessageConnectionReportingIntegrationTest {

    @Autowired
    private Client client;

    @Autowired
    private ReporterConfiguration configuration;

    @Autowired
    private PipelineConfiguration pipelineConfiguration;

    @Autowired
    private FreeMarkerComponent freeMarkerComponent;

    private ElasticsearchReporter reporter;

    @BeforeEach
    void setUp() throws Exception {
        configuration.setFlushInterval(1L);
        Node node = mock(Node.class);
        when(node.id()).thenReturn("gateway-id");
        reporter = new ElasticsearchReporter(node, configuration, pipelineConfiguration, freeMarkerComponent, client);
        reporter.start();
        waitUntilReady();
    }

    @AfterEach
    void tearDown() throws Exception {
        reporter.stop();
    }

    @Test
    void should_write_the_opening_and_the_end_of_a_message_connection_as_two_documents() {
        String requestId = UUID.randomUUID().toString();
        Log log = Log.builder().timestamp(Instant.now().toEpochMilli()).requestId(requestId).apiId("api").apiName("api").build();
        Metrics metrics = Metrics.builder()
            .timestamp(Instant.now().toEpochMilli())
            .remoteAddress("127.0.0.1")
            .requestId(requestId)
            .apiId("api")
            .apiType("message")
            .log(log)
            .build();

        // The message reactor reports a copy at opening; the platform reporter completes the instances at the end.
        reporter.report(metrics.toBuilder().build());
        reporter.report(copyOf(log));
        metrics.setRequestEnded(true);
        metrics.setGatewayResponseTimeMs(3000);
        log.setRequestEnded(true);
        reporter.report(metrics);
        reporter.report(log);

        List<SearchHit> metricsDocuments = awaitDocuments("v4-metrics", requestId, 2);
        assertThat(metricsDocuments)
            .extracting(SearchHit::getId, hit -> hit.getSource().path("request-ended").asText(), hit -> phase(hit))
            .containsExactlyInAnyOrder(tuple(requestId + ":open", "false", "open"), tuple(requestId, "true", "end"));
        assertThat(metricsDocuments)
            .filteredOn(hit -> hit.getId().equals(requestId + ":open"))
            .extracting(hit -> hit.getSource().path("gateway-response-time-ms").asLong())
            .containsExactly(0L);

        List<SearchHit> logDocuments = awaitDocuments("v4-log", requestId, 2);
        assertThat(logDocuments)
            .extracting(SearchHit::getId, hit -> hit.getSource().path("request-ended").asText(), hit -> phase(hit))
            .containsExactlyInAnyOrder(tuple(requestId + ":open", "false", "open"), tuple(requestId, "true", ""));
    }

    @Test
    void should_write_a_proxy_request_once_under_its_request_id() {
        String requestId = UUID.randomUUID().toString();
        Metrics metrics = Metrics.builder()
            .timestamp(Instant.now().toEpochMilli())
            .remoteAddress("127.0.0.1")
            .requestId(requestId)
            .apiId("api")
            .apiType("proxy")
            .requestEnded(true)
            .build();

        reporter.report(metrics);

        assertThat(awaitDocuments("v4-metrics", requestId, 1))
            .extracting(SearchHit::getId, this::phase)
            .containsExactly(tuple(requestId, ""));
    }

    private static Log copyOf(Log log) {
        return Log.builder()
            .timestamp(log.getTimestamp())
            .requestId(log.getRequestId())
            .apiId(log.getApiId())
            .apiName(log.getApiName())
            .build();
    }

    private String phase(SearchHit hit) {
        return hit.getSource().path("connection-phase").asText();
    }

    /** The reporter drops what it is given before its templates are in place, so wait for a probe to land. */
    private void waitUntilReady() {
        String probe = "probe-" + UUID.randomUUID();
        await()
            .atMost(Duration.ofSeconds(60))
            .pollInterval(Duration.ofSeconds(1))
            .ignoreExceptions()
            .until(() -> {
                reporter.report(
                    Metrics.builder()
                        .timestamp(Instant.now().toEpochMilli())
                        .remoteAddress("127.0.0.1")
                        .requestId(probe)
                        .apiType("proxy")
                        .requestEnded(true)
                        .build()
                );
                return count("v4-metrics", probe) > 0;
            });
    }

    private List<SearchHit> awaitDocuments(String type, String requestId, int expected) {
        await()
            .atMost(Duration.ofSeconds(30))
            .ignoreExceptions()
            .until(() -> count(type, requestId) >= expected);
        return search(type, requestId);
    }

    private long count(String type, String requestId) {
        return search(type, requestId).size();
    }

    private List<SearchHit> search(String type, String requestId) {
        var query = JsonObject.of("size", 10, "query", JsonObject.of("term", JsonObject.of("request-id", requestId)));
        return client
            .search(configuration.getIndexName() + "-" + type + "-*", null, query.encode())
            .blockingGet()
            .getSearchHits()
            .getHits();
    }
}

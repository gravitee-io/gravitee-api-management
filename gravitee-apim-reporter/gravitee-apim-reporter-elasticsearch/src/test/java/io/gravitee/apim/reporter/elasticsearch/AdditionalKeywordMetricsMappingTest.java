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
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.reporter.elasticsearch.config.PipelineConfiguration;
import io.gravitee.apim.reporter.elasticsearch.config.ReporterConfiguration;
import io.gravitee.common.templating.FreeMarkerComponent;
import io.gravitee.elasticsearch.client.Client;
import io.gravitee.node.api.Node;
import io.gravitee.reporter.api.v4.metric.AdditionalMetric;
import io.gravitee.reporter.api.v4.metric.Metrics;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.TestScheduler;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

/**
 * A {@code keyword_*} additional metric is a keyword whatever its value looks like. Elasticsearch detects a date in a
 * string such as {@code 2025-11-25} before matching dynamic templates by type, so a rule limited to strings let such a
 * metric be mapped as a date: values were then stored as dates, and a value that is not one (two versions joined with a
 * comma) got the whole document rejected.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { AdditionalKeywordMetricsMappingTest.TestConfig.class })
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AdditionalKeywordMetricsMappingTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String VERSION_METRIC = "keyword_mcp-proxy_protocol-version";
    private static final String VERSIONS_METRIC = "keyword_mcp-proxy_supported-protocol-versions";

    @Autowired
    private ElasticsearchReporter reporter;

    @Autowired
    private ElasticsearchContainer elasticsearch;

    /** Created once the reporter has put it into Elasticsearch; documents are dropped until then. */
    private static PipelineConfiguration pipelineConfiguration;

    private final HttpClient http = HttpClient.newHttpClient();
    private TestScheduler testScheduler;

    @Configuration
    @Import(IntegrationTestConfiguration.class)
    public static class TestConfig {

        /**
         * Set up so documents are really indexed, which the other reporter tests do not check: with an ingest plugin, so
         * the pipeline is created (without one, the reporter has no pipeline to name and drops every document), and with
         * a node that has an id.
         */
        @Bean
        public ElasticsearchReporter reporter(
            final Node node,
            final Client client,
            final ReporterConfiguration reporterConfiguration,
            final FreeMarkerComponent freeMarkerComponent
        ) {
            // The shared test node is a bare mock; every document names the gateway that reported it.
            when(node.id()).thenReturn("test-gateway");
            when(node.hostname()).thenReturn("localhost");
            pipelineConfiguration = new PipelineConfiguration("user_agent", null, freeMarkerComponent);
            return new ElasticsearchReporter(node, reporterConfiguration, pipelineConfiguration, freeMarkerComponent, client);
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        testScheduler = new TestScheduler();
        RxJavaPlugins.setComputationSchedulerHandler(ignore -> testScheduler);
        reporter.start();
        for (int attempt = 0; attempt < 100 && pipelineConfiguration.getPipeline() == null; attempt++) {
            testScheduler.advanceTimeBy(1, TimeUnit.SECONDS);
            Thread.sleep(100);
        }
        assertThat(pipelineConfiguration.getPipeline()).as("ingest pipeline created").isNotNull();
    }

    @AfterEach
    void tearDown() throws Exception {
        reporter.stop();
        RxJavaPlugins.reset();
    }

    @Test
    void should_keep_date_like_keyword_metrics_as_keywords_and_index_every_document() throws Exception {
        String api = UUID.randomUUID().toString();

        // The first value looks like a date, so it is the one that decides the mapping of each field.
        report(api, "2025-11-25", "2025-11-25");
        report(api, "2026-07-28", "2025-11-25,2026-07-28");

        assertThat(count(api)).as("the document whose versions are not a date is indexed too").isEqualTo(2);
        assertThat(fieldType(VERSION_METRIC)).isEqualTo("keyword");
        assertThat(fieldType(VERSIONS_METRIC)).isEqualTo("keyword");
        assertThat(count(api, VERSIONS_METRIC, "2025-11-25,2026-07-28")).isEqualTo(1);
        assertThat(count(api, VERSION_METRIC, "2025-11-25")).isEqualTo(1);
    }

    private void report(String api, String version, String supportedVersions) throws InterruptedException {
        Metrics metrics = Metrics.builder()
            .timestamp(Instant.now().toEpochMilli())
            .requestId(UUID.randomUUID().toString())
            .transactionId(UUID.randomUUID().toString())
            .apiId(api)
            .remoteAddress("127.0.0.1")
            .additionalMetrics(
                Set.of(
                    new AdditionalMetric.KeywordMetric(VERSION_METRIC, version),
                    new AdditionalMetric.KeywordMetric(VERSIONS_METRIC, supportedVersions)
                )
            )
            .build();
        var reported = Completable.fromRunnable(() -> reporter.report(metrics)).test();
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        reported.await();
        reported.assertNoErrors();
    }

    private String fieldType(String metric) throws Exception {
        JsonNode mappings = get("/gravitee-v4-metrics*/_mapping/field/additional-metrics." + metric);
        JsonNode field = mappings.elements().next().path("mappings").path("additional-metrics." + metric).path("mapping").path(metric);
        return field.path("type").asText();
    }

    /** Documents of {@code api}, optionally with {@code metric} = {@code value}; waits until the bulk is searchable. */
    private long count(String api, String metric, String value) throws Exception {
        String filter = metric == null ? "" : ",{\"term\":{\"additional-metrics.%s\":\"%s\"}}".formatted(metric, value);
        String query = "{\"query\":{\"bool\":{\"filter\":[{\"term\":{\"api-id\":\"%s\"}}%s]}}}".formatted(api, filter);
        long found = 0;
        for (int attempt = 0; attempt < 50 && found == 0; attempt++) {
            // Documents are formatted on another thread before they reach the bulk, which flushes on the test clock.
            testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
            post("/gravitee-v4-metrics*/_refresh", "");
            found = post("/gravitee-v4-metrics*/_count", query).path("count").asLong();
            if (found == 0) {
                Thread.sleep(100);
            }
        }
        return found;
    }

    private long count(String api) throws Exception {
        long previous = -1;
        long current = count(api, null, null);
        // The bulk may still be landing: wait until the count settles.
        while (current != previous) {
            Thread.sleep(300);
            previous = current;
            current = count(api, null, null);
        }
        return current;
    }

    private JsonNode get(String path) throws Exception {
        return send(HttpRequest.newBuilder(uri(path)).GET().build());
    }

    private JsonNode post(String path, String body) throws Exception {
        return send(
            HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build()
        );
    }

    private URI uri(String path) {
        return URI.create("http://" + elasticsearch.getHttpHostAddress() + path);
    }

    private JsonNode send(HttpRequest request) throws Exception {
        return MAPPER.readTree(http.send(request, HttpResponse.BodyHandlers.ofString()).body());
    }
}

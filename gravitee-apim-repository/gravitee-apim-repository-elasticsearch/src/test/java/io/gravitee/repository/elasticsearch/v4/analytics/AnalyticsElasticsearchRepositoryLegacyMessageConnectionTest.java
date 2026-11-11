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
package io.gravitee.repository.elasticsearch.v4.analytics;

import static io.gravitee.definition.model.DefinitionVersion.V4;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import io.gravitee.elasticsearch.client.Client;
import io.gravitee.repository.analytics.engine.api.metric.Measure;
import io.gravitee.repository.analytics.engine.api.metric.Metric;
import io.gravitee.repository.analytics.engine.api.query.Filter;
import io.gravitee.repository.analytics.engine.api.query.MeasuresQuery;
import io.gravitee.repository.analytics.engine.api.query.MetricMeasuresQuery;
import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.elasticsearch.AbstractElasticsearchRepositoryTest;
import io.gravitee.repository.elasticsearch.TimeProvider;
import io.gravitee.repository.elasticsearch.configuration.RepositoryConfiguration;
import io.gravitee.repository.elasticsearch.v4.log.MetricsElasticsearchRepository;
import io.gravitee.repository.log.v4.model.analytics.RequestResponseTimeQueryCriteria;
import io.gravitee.repository.log.v4.model.analytics.SearchTermId;
import io.gravitee.repository.log.v4.model.analytics.StatsQuery;
import io.gravitee.repository.log.v4.model.analytics.TimeRange;
import io.gravitee.repository.log.v4.model.connection.Metrics;
import io.gravitee.repository.log.v4.model.connection.MetricsQuery;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

/**
 * The day an upgraded gateway starts writing, the day's index already exists with the mapping of the previous
 * version: it holds connections written the old way, one document without connection-phase each, and the new
 * gateway adds connections as an opening and an end, with connection-phase mapped dynamically as text. Both must
 * read as they did, each connection once.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@TestPropertySource(properties = "reporters.elasticsearch.template_mapping.path=src/test/resources/freemarker-v4-analytics")
class AnalyticsElasticsearchRepositoryLegacyMessageConnectionTest extends AbstractElasticsearchRepositoryTest {

    private static final String MESSAGE_API = "c3d4e5f6-0000-4000-8000-0000000000e6";
    private static final String UPGRADE_DAY_INDEX = "gravitee-v4-metrics-upgrade-day";
    private static final QueryContext QUERY_CONTEXT = new QueryContext("org#1", "env#1");
    /** Far from the shared sample data, whose environment-wide assertions would otherwise count these documents. */
    private static final Instant DAY = TimeProvider.now().truncatedTo(ChronoUnit.DAYS).minus(Duration.ofDays(200));
    private static final Instant NEXT_DAY = DAY.plus(Duration.ofDays(1));

    @Autowired
    private AnalyticsElasticsearchRepository cut;

    @Autowired
    private MetricsElasticsearchRepository metricsRepository;

    @Autowired
    private Client client;

    @Autowired
    private Vertx vertx;

    @Autowired
    private RepositoryConfiguration configuration;

    private WebClient webClient;

    @BeforeEach
    void createTheUpgradeDayIndex() {
        webClient = WebClient.create(vertx);
        deleteTheUpgradeDayIndex();
        var dynamicallyMappedString = JsonObject.of(
            "type",
            "text",
            "fields",
            JsonObject.of("keyword", JsonObject.of("type", "keyword", "ignore_above", 256))
        );
        var created = send(
            HttpMethod.PUT,
            "/" + UPGRADE_DAY_INDEX,
            JsonObject.of("mappings", JsonObject.of("properties", JsonObject.of("connection-phase", dynamicallyMappedString)))
        );
        assertThat(created.statusCode()).as(created.bodyAsString()).isEqualTo(200);

        client
            .bulk(
                List.of(
                    document("legacy-ended", "legacy-ended", null, true, 2000),
                    document("legacy-open", "legacy-open", null, false, 0),
                    document("upgraded:open", "upgraded", "open", false, 0),
                    document("upgraded", "upgraded", "end", true, 4000)
                ),
                true
            )
            .blockingGet();
    }

    @AfterEach
    void deleteTheUpgradeDayIndex() {
        send(HttpMethod.DELETE, "/" + UPGRADE_DAY_INDEX, null);
    }

    @Test
    void should_map_the_connection_phase_as_text_like_a_dynamic_mapping_does() {
        assertThat(client.getFieldTypes(UPGRADE_DAY_INDEX, "connection-phase").blockingGet()).containsExactlyInAnyOrder("text", "keyword");
    }

    @Test
    void should_count_each_connection_once_and_time_the_legacy_ones_as_before() {
        var result = cut.searchRequestResponseTimes(
            QUERY_CONTEXT,
            new RequestResponseTimeQueryCriteria(List.of(MESSAGE_API), DAY.toEpochMilli(), NEXT_DAY.toEpochMilli(), EnumSet.of(V4))
        );

        assertThat(result.getRequestsTotal()).isEqualTo(3L);
        assertThat(result.getResponseMinTime()).isEqualTo(0.0);
        assertThat(result.getResponseMaxTime()).isEqualTo(4000.0);
        assertThat(result.getResponseAvgTime()).isEqualTo(2000.0);
    }

    @Test
    void should_count_each_connection_once_in_stats() {
        var query = new StatsQuery(
            "gateway-response-time-ms",
            new SearchTermId(SearchTermId.SearchTerm.API, MESSAGE_API),
            new TimeRange(DAY, NEXT_DAY),
            Optional.empty()
        );

        assertThat(cut.searchStats(QUERY_CONTEXT, query)).hasValueSatisfying(stats -> {
            assertThat(stats.count()).isEqualTo(3.0f);
            assertThat(stats.sum()).isEqualTo(6000.0f);
        });
    }

    @Test
    void should_count_each_connection_once_in_the_engine() {
        var result = cut.searchHTTPMeasures(
            QUERY_CONTEXT,
            new MeasuresQuery(
                new io.gravitee.repository.analytics.engine.api.query.TimeRange(DAY, NEXT_DAY),
                List.of(new Filter(Filter.Name.API, Filter.Operator.IN, List.of(MESSAGE_API))),
                List.of(
                    new MetricMeasuresQuery(Metric.HTTP_REQUESTS, Set.of(Measure.COUNT)),
                    new MetricMeasuresQuery(Metric.HTTP_GATEWAY_RESPONSE_TIME, Set.of(Measure.AVG))
                )
            )
        );

        assertThat(result.measures())
            .filteredOn(metric -> metric.metric() == Metric.HTTP_REQUESTS)
            .singleElement()
            .satisfies(metric -> assertThat(metric.measures().get(Measure.COUNT).longValue()).isEqualTo(3L));
        assertThat(result.measures())
            .filteredOn(metric -> metric.metric() == Metric.HTTP_GATEWAY_RESPONSE_TIME)
            .singleElement()
            .satisfies(metric -> assertThat(metric.measures().get(Measure.AVG).doubleValue()).isEqualTo(2000.0));
    }

    @Test
    void should_list_each_connection_once_the_upgraded_one_by_its_end() {
        var result = metricsRepository.searchMetrics(
            QUERY_CONTEXT,
            MetricsQuery.builder()
                .page(1)
                .size(10)
                .filter(
                    MetricsQuery.Filter.builder().apiIds(Set.of(MESSAGE_API)).from(DAY.toEpochMilli()).to(NEXT_DAY.toEpochMilli()).build()
                )
                .build(),
            List.of(V4)
        );

        assertThat(result.total()).isEqualTo(3L);
        assertThat(result.data())
            .extracting(Metrics::getRequestId, Metrics::isRequestEnded)
            .containsExactlyInAnyOrder(tuple("legacy-ended", true), tuple("legacy-open", false), tuple("upgraded", true));
    }

    private Buffer document(String id, String requestId, String phase, boolean ended, long responseTime) {
        var source = JsonObject.of(
            "entrypoint-id",
            "http-get",
            "@timestamp",
            DAY.plus(Duration.ofHours(8)).atOffset(ZoneOffset.UTC).toString(),
            "request-id",
            requestId,
            "transaction-id",
            requestId,
            "api-id",
            MESSAGE_API,
            "uri",
            "/message-api",
            "request-ended",
            String.valueOf(ended),
            "status",
            200,
            "gateway-response-time-ms",
            responseTime
        );
        if (phase != null) {
            source.put("connection-phase", phase);
        }
        var action = JsonObject.of("index", JsonObject.of("_index", UPGRADE_DAY_INDEX, "_id", id));
        return Buffer.buffer(action.encode() + "\n" + source.encode() + "\n");
    }

    private HttpResponse<Buffer> send(HttpMethod method, String path, JsonObject body) {
        var request = webClient
            .requestAbs(method, configuration.getEndpoints().getFirst().getUrl() + path)
            .basicAuthentication(configuration.getUsername(), configuration.getPassword());
        Future<HttpResponse<Buffer>> response = body == null ? request.send() : request.sendJsonObject(body);
        return response.toCompletionStage().toCompletableFuture().join();
    }
}

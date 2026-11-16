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
package io.gravitee.apim.reporter.elasticsearch.mapping.datastream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleAbstractTypeResolver;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.gravitee.apim.reporter.common.bulk.compressor.CompressedBulk;
import io.gravitee.apim.reporter.common.formatter.FormatterFactory;
import io.gravitee.apim.reporter.common.formatter.Type;
import io.gravitee.apim.reporter.elasticsearch.bulk.ElasticBulkSender;
import io.gravitee.apim.reporter.elasticsearch.bulk.ElasticBulkTransformer;
import io.gravitee.apim.reporter.elasticsearch.config.PipelineConfiguration;
import io.gravitee.apim.reporter.elasticsearch.config.ReporterConfiguration;
import io.gravitee.apim.reporter.elasticsearch.factory.BeanFactory;
import io.gravitee.apim.reporter.elasticsearch.factory.BeanFactoryBuilder;
import io.gravitee.common.templating.FreeMarkerComponent;
import io.gravitee.elasticsearch.client.Client;
import io.gravitee.elasticsearch.version.Version;
import io.gravitee.gateway.api.http.DefaultHttpHeaders;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.node.api.Node;
import io.gravitee.reporter.api.Reportable;
import io.gravitee.reporter.api.health.EndpointStatus;
import io.gravitee.reporter.api.jackson.AdditionalMetricDeserialization;
import io.gravitee.reporter.api.v4.metric.AdditionalMetric;
import io.reactivex.rxjava3.core.Completable;
import io.vertx.core.buffer.Buffer;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Writes health reports in the data stream index mode through the beans the reporter builds, and reads back what the cluster
 * made of them. The subclasses run it against each distribution; {@code -Delasticsearch.version} and
 * {@code -Dopensearch.version} pick the version.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
abstract class AbstractDataStreamIntegrationTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String GIVEN = "/io/gravitee/reporter/common/formatter/given/";
    private static final ObjectMapper REPORTABLES = reportablesMapper();

    @Autowired
    private Client client;

    @Autowired
    private ReporterConfiguration clusterConfiguration;

    @Autowired
    private PipelineConfiguration pipelineConfiguration;

    @Autowired
    private FreeMarkerComponent freeMarkerComponent;

    private final String prefix = "data-stream-it-" + UUID.randomUUID().toString().substring(0, 8);
    private final HttpClient http = HttpClient.newHttpClient();
    private Version version;
    private ElasticBulkTransformer transformer;

    @BeforeEach
    void setUp() {
        version = client.getInfo().blockingGet().getVersion();
    }

    static Stream<Arguments> reportables() {
        return Stream.of(
            Arguments.of("http.Metrics", "metrics.json", "request", true),
            Arguments.of("log.Log", "log.json", "log", true),
            Arguments.of("health.EndpointStatus", "endpoint-status.json", "health", true),
            Arguments.of("monitor.Monitor", "monitor.json", "monitor", false),
            Arguments.of("v4.metric.Metrics", "v4/metrics.json", "v4-metrics", true),
            Arguments.of("v4.log.Log", "v4/log.json", "v4-log", true),
            Arguments.of("v4.metric.MessageMetrics", "v4/message-metrics.json", "v4-message-metrics", true),
            Arguments.of("v4.log.MessageLog", "v4/message-log.json", "v4-message-log", true),
            Arguments.of("v4.metric.event.ApiEventMetrics", "v4/api-event-metrics.json", "event-metrics", false),
            Arguments.of("v4.report.DecisionReport", "v4/decision-report.json", "decisions", false)
        );
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("reportables")
    void should_create_each_report_in_the_data_stream_of_its_type_once_per_id_even_when_the_bulk_is_sent_again(
        String className,
        String given,
        String type,
        boolean identified
    ) throws Exception {
        final ElasticBulkSender sender = prepare(configuration -> {});
        final CompressedBulk bulk = new CompressedBulk(transformer.transform(given(className, given)).transformed(), Map.of(type, 1));

        sender.send(bulk).blockingAwait();
        // A bulk sent again, as after a timeout, conflicts on every document with an id: none of them counts as failed.
        sender.send(bulk).blockingAwait();

        final String dataStream = prefix + "-" + type;
        send(request("/" + dataStream + "/_refresh").POST(HttpRequest.BodyPublishers.noBody()));
        final JsonNode hits = get("/" + dataStream + "/_search").path("hits");
        // Monitor, event metrics and decision documents have no id: Elasticsearch creates each of them again.
        assertThat(hits.path("total").path("value").asInt()).isEqualTo(identified ? 1 : 2);
        assertThat(hits.path("hits").get(0).path("_index").asText()).startsWith(".ds-" + dataStream);
    }

    @Test
    void should_back_the_type_with_a_data_stream_and_no_rollover_alias() throws Exception {
        prepare(configuration -> {})
            .send(bulkOf(healthReport()))
            .blockingAwait();

        final JsonNode dataStream = get("/_data_stream/" + healthDataStream()).path("data_streams").get(0);
        assertThat(dataStream.path("name").asText()).isEqualTo(healthDataStream());
        assertThat(dataStream.path("template").asText()).isEqualTo(healthDataStream());
        assertThat(status("/_alias/" + healthDataStream())).isEqualTo(404);
    }

    @Test
    void should_attach_the_ilm_policy_of_the_type_to_the_backing_index() throws Exception {
        assumeFalse(version.isOpenSearch(), "OpenSearch refuses the lifecycle policies in the data stream index mode");
        final String policy = prefix + "-health-policy";
        put("/_ilm/policy/" + policy, "{ \"policy\": { \"phases\": { \"hot\": { \"actions\": {} } } } }");
        prepare(configuration -> configuration.setIndexLifecyclePolicyHealth(policy))
            .send(bulkOf(healthReport()))
            .blockingAwait();

        final JsonNode indices = get("/" + healthDataStream() + "/_ilm/explain").path("indices");
        assertThat(indices.elements().next().path("policy").asText()).isEqualTo(policy);
    }

    @Test
    void should_let_an_ism_template_manage_the_data_stream_on_opensearch() throws Exception {
        assumeTrue(version.isOpenSearch(), "an ism_template is the OpenSearch way to manage a data stream");
        final String policy = prefix + "-health-policy";
        put(
            "/_plugins/_ism/policies/" + policy,
            "{ \"policy\": { \"description\": \"it\", \"default_state\": \"hot\", \"states\": [ { \"name\": \"hot\", \"actions\": [], \"transitions\": [] } ], \"ism_template\": [ { \"index_patterns\": [ \"" +
                healthDataStream() +
                "\" ], \"priority\": 100 } ] } }"
        );
        prepare(configuration -> {})
            .send(bulkOf(healthReport()))
            .blockingAwait();

        // ISM attaches the policy of a matching ism_template shortly after the backing index is created.
        await()
            .atMost(30, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                final JsonNode explain = get("/_plugins/_ism/explain/" + healthDataStream());
                assertThat(explain.path(explain.fieldNames().next()).path("policy_id").asText()).isEqualTo(policy);
            });
    }

    @Test
    void should_set_the_retention_of_the_type_on_the_data_stream() throws Exception {
        assumeTrue(version.canUseDataStreamLifecycle(), "the data stream lifecycle needs Elasticsearch 8.14 or later");
        prepare(configuration -> configuration.setDataRetentionHealth("7d"))
            .send(bulkOf(healthReport()))
            .blockingAwait();

        final JsonNode dataStream = get("/_data_stream/" + healthDataStream()).path("data_streams").get(0);
        assertThat(dataStream.path("lifecycle").path("data_retention").asText()).isEqualTo("7d");
    }

    @Test
    void should_refuse_the_data_streams_when_an_alias_left_by_the_ilm_mode_has_their_name() throws Exception {
        put("/" + healthDataStream() + "-000001", "{ \"aliases\": { \"" + healthDataStream() + "\": { \"is_write_index\": true } } }");

        indexPreparation(configuration())
            .test()
            .await()
            .assertError(error -> error.getMessage().contains("[" + healthDataStream() + "]"));
        assertThat(status("/_index_template/" + healthDataStream())).isEqualTo(404);
    }

    @Test
    void should_prepare_the_data_streams_again_once_they_exist() throws Exception {
        prepare(configuration -> {})
            .send(bulkOf(healthReport()))
            .blockingAwait();

        indexPreparation(configuration()).test().await().assertComplete();
    }

    private ElasticBulkSender prepare(final Consumer<ReporterConfiguration> customization) {
        final ReporterConfiguration configuration = configuration();
        customization.accept(configuration);
        final BeanFactory beanFactory = beanFactory(configuration);
        beanFactory.createIndexPreparer(configuration, pipelineConfiguration, freeMarkerComponent, client).prepare().blockingAwait();

        final Node node = mock(Node.class);
        when(node.id()).thenReturn("gateway-id");
        transformer = new ElasticBulkTransformer(
            new FormatterFactory(node, beanFactory.createFormatterFactoryConfiguration()).getFormatter(Type.ELASTICSEARCH),
            pipelineConfiguration,
            beanFactory.createIndexNameGenerator(configuration)
        );
        return new ElasticBulkSender(client);
    }

    private ReporterConfiguration configuration() {
        final ReporterConfiguration configuration = new ReporterConfiguration();
        configuration.setEndpoints(clusterConfiguration.getEndpoints());
        configuration.setUsername(clusterConfiguration.getUsername());
        configuration.setPassword(clusterConfiguration.getPassword());
        configuration.setIndexName(prefix);
        configuration.setIndexMode("data_stream");
        return configuration;
    }

    private BeanFactory beanFactory(final ReporterConfiguration configuration) {
        final BeanFactory beanFactory = BeanFactoryBuilder.buildFactory(client, configuration);
        assertThat(beanFactory).as("the reporter refused the configuration, see the logs").isNotNull();
        return beanFactory;
    }

    private Completable indexPreparation(final ReporterConfiguration configuration) {
        return beanFactory(configuration).createIndexPreparer(configuration, pipelineConfiguration, freeMarkerComponent, client).prepare();
    }

    private CompressedBulk bulkOf(final EndpointStatus status) {
        final Buffer document = transformer.transform(status).transformed();
        return new CompressedBulk(document, Map.of("health", 1));
    }

    private static EndpointStatus healthReport() {
        return EndpointStatus.forEndpoint("api-id", "endpoint").apiName("api").on(System.currentTimeMillis()).build();
    }

    private String healthDataStream() {
        return prefix + "-health";
    }

    private static ObjectMapper reportablesMapper() {
        final SimpleModule module = new SimpleModule();
        module.addDeserializer(AdditionalMetric.class, new AdditionalMetricDeserialization());
        module.setAbstractTypes(new SimpleAbstractTypeResolver().addMapping(HttpHeaders.class, DefaultHttpHeaders.class));
        return new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).registerModule(module);
    }

    /**
     * Reads a reportable the formatter tests of the reporter common module are given.
     */
    private static Reportable given(final String className, final String path) throws IOException, ClassNotFoundException {
        final Class<? extends Reportable> type = Class.forName("io.gravitee.reporter.api." + className).asSubclass(Reportable.class);
        try (InputStream input = AbstractDataStreamIntegrationTest.class.getResourceAsStream(GIVEN + path)) {
            assertThat(input).as("given reportable %s", path).isNotNull();
            return REPORTABLES.readValue(input, type);
        }
    }

    private JsonNode get(final String path) throws IOException, InterruptedException {
        final HttpResponse<String> response = send(request(path).GET());
        assertThat(response.statusCode()).as("GET %s: %s", path, response.body()).isEqualTo(200);
        return MAPPER.readTree(response.body());
    }

    private int status(final String path) throws IOException, InterruptedException {
        return send(request(path).GET()).statusCode();
    }

    private void put(final String path, final String body) throws IOException, InterruptedException {
        final HttpResponse<String> response = send(
            request(path).header("Content-Type", "application/json").PUT(HttpRequest.BodyPublishers.ofString(body))
        );
        assertThat(response.statusCode()).as("PUT %s: %s", path, response.body()).isBetween(200, 299);
    }

    private HttpResponse<String> send(final HttpRequest.Builder request) throws IOException, InterruptedException {
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private HttpRequest.Builder request(final String path) {
        final HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(clusterConfiguration.getEndpoints().get(0).getUrl() + path));
        if (clusterConfiguration.getUsername() != null) {
            final String credentials = clusterConfiguration.getUsername() + ":" + clusterConfiguration.getPassword();
            request.header("Authorization", "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8)));
        }
        return request;
    }
}

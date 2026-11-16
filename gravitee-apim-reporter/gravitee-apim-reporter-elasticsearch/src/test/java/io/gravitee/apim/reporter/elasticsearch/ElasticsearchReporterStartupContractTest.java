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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import io.gravitee.apim.reporter.elasticsearch.config.PipelineConfiguration;
import io.gravitee.apim.reporter.elasticsearch.config.ReporterConfiguration;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.common.templating.FreeMarkerComponent;
import io.gravitee.elasticsearch.client.Client;
import io.gravitee.node.api.Node;
import io.gravitee.reporter.api.http.Metrics;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.MapPropertySource;

/**
 * Freezes what the reporter sends to a classic Elasticsearch or OpenSearch cluster at startup: the setup calls and the exact
 * body of every template, initial index and pipeline it puts.
 *
 * <p>The expected files under {@code src/test/resources/startup-contract}, one per cluster and index mode, hold what classic
 * clusters receive. Run with {@code -Dstartup-contract.record=true} to record them again, then review the diff: any change
 * is a change of what classic clusters receive.
 */
class ElasticsearchReporterStartupContractTest {

    private static final Path CONTRACT_DIRECTORY = Path.of("src/test/resources/startup-contract");
    private static final boolean RECORD = Boolean.getBoolean("startup-contract.record");
    private static final String ACKNOWLEDGED = "{\"acknowledged\":true}";
    private static final String PIPELINE_PATH = "/_ingest/pipeline/gravitee_pipeline";
    private static final List<String> TYPES = List.of(
        "request",
        "monitor",
        "health",
        "log",
        "v4-log",
        "v4-metrics",
        "v4-message-log",
        "v4-message-metrics",
        "event-metrics",
        "decisions"
    );
    private static final List<String> DATA_STREAM_TYPES = List.of("event-metrics", "decisions");

    @RegisterExtension
    static WireMockExtension elasticsearch = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

    private AnnotationConfigApplicationContext context;
    private ElasticsearchReporter reporter;

    @Configuration
    @Import(UnitTestConfiguration.class)
    static class ReporterTestConfiguration {

        @Bean
        public Node node() {
            final Node node = mock(Node.class);
            when(node.id()).thenReturn("gateway-id");
            when(node.hostname()).thenReturn("gateway-host");
            return node;
        }

        @Bean
        public ElasticsearchReporter reporter(
            Node node,
            Client client,
            ReporterConfiguration reporterConfiguration,
            PipelineConfiguration pipelineConfiguration,
            FreeMarkerComponent freeMarkerComponent
        ) {
            return new ElasticsearchReporter(node, reporterConfiguration, pipelineConfiguration, freeMarkerComponent, client);
        }
    }

    enum Cluster {
        ES7("elasticsearch", "7.17.25"),
        ES8("elasticsearch", "8.17.2"),
        ES9("elasticsearch", "9.2.1"),
        OPENSEARCH1("opensearch", "1.3.20"),
        OPENSEARCH2("opensearch", "2.19.1"),
        OPENSEARCH3("opensearch", "3.1.0");

        private final String distribution;
        private final String number;

        Cluster(String distribution, String number) {
            this.distribution = distribution;
            this.number = number;
        }

        String info() {
            final String distributionField = "opensearch".equals(distribution) ? "\"distribution\":\"opensearch\"," : "";
            final String buildFlavor = "opensearch".equals(distribution) ? "" : "\"build_flavor\":\"default\",";
            return (
                "{\"name\":\"node\",\"cluster_name\":\"cluster\",\"version\":{" +
                distributionField +
                "\"number\":\"" +
                number +
                "\"," +
                buildFlavor +
                "\"lucene_version\":\"9.12.0\"},\"tagline\":\"The OpenSearch Project / You Know, for Search\"}"
            );
        }
    }

    enum Setup {
        // Without an index mode, classic clusters must receive exactly what they receive in daily mode.
        DEFAULT("daily", Map.of()),
        DAILY("daily", Map.of("reporters.elasticsearch.index_mode", "daily")),
        ILM("ilm", Map.of("reporters.elasticsearch.index_mode", "ilm")),
        ILM_WITH_POLICIES(
            "ilm-with-policies",
            Map.of(
                "reporters.elasticsearch.index_mode",
                "ilm",
                "reporters.elasticsearch.lifecycle.policies.request",
                "request-policy",
                "reporters.elasticsearch.lifecycle.policies.monitor",
                "monitor-policy",
                "reporters.elasticsearch.lifecycle.policies.health",
                "health-policy",
                "reporters.elasticsearch.lifecycle.policies.log",
                "log-policy",
                "reporters.elasticsearch.lifecycle.policies.event_metrics",
                "event-metrics-policy",
                "reporters.elasticsearch.lifecycle.policies.decisions",
                "decisions-policy"
            )
        );

        private final String fileName;
        private final Map<String, Object> properties;

        Setup(String fileName, Map<String, Object> properties) {
            this.fileName = fileName;
            this.properties = properties;
        }

        boolean isIlm() {
            return "ilm".equals(properties.get("reporters.elasticsearch.index_mode"));
        }

        String fileName() {
            return fileName;
        }
    }

    static Stream<Arguments> clustersAndSetups() {
        return Stream.of(Cluster.values()).flatMap(cluster -> Stream.of(Setup.values()).map(setup -> Arguments.of(cluster, setup)));
    }

    static Stream<Cluster> clusters() {
        return Stream.of(Cluster.values());
    }

    @AfterEach
    void tearDown() throws Exception {
        if (reporter != null) {
            reporter.stop();
        }
        if (context != null) {
            context.close();
        }
        elasticsearch.resetAll();
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("clustersAndSetups")
    void should_send_the_recorded_setup_calls_and_bodies(Cluster cluster, Setup setup) throws Exception {
        stubCluster(cluster, false);

        startReporterAndIndexOneDocument(setup.properties);

        final String contract = contract(setupCalls());
        final Path expectedFile = CONTRACT_DIRECTORY.resolve(cluster.name().toLowerCase() + "-" + setup.fileName() + ".txt");
        if (RECORD) {
            Files.createDirectories(CONTRACT_DIRECTORY);
            Files.writeString(expectedFile, contract);
        }
        assertThat(contract).isEqualTo(Files.readString(expectedFile));
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("clustersAndSetups")
    void should_order_the_setup_calls(Cluster cluster, Setup setup) throws Exception {
        stubCluster(cluster, false);

        startReporterAndIndexOneDocument(setup.properties);

        final List<String> calls = allCalls().stream().map(ElasticsearchReporterStartupContractTest::callLine).toList();
        final int firstPut = indexOfFirst(calls, call -> call.startsWith("PUT "));
        final int pipeline = calls.indexOf("PUT " + PIPELINE_PATH);
        final int firstBulk = indexOfFirst(calls, call -> call.startsWith("POST /_bulk"));
        assertThat(calls.get(0)).isEqualTo("GET /");
        assertThat(pipeline).isGreaterThan(firstPut).isLessThan(firstBulk);
        for (String type : TYPES) {
            final int template = indexOfFirst(calls, call -> call.startsWith("PUT ") && call.endsWith("/gravitee-" + type));
            assertThat(template).as("template of %s", type).isBetween(firstPut, pipeline);
            if (setup.isIlm() && !DATA_STREAM_TYPES.contains(type)) {
                final int alias = calls.indexOf("GET /_alias/gravitee-" + type);
                final int initialIndex = calls.indexOf("PUT /gravitee-" + type + "-000001");
                assertThat(alias).as("alias of %s", type).isGreaterThan(template);
                assertThat(initialIndex).as("initial index of %s", type).isGreaterThan(alias).isLessThan(pipeline);
            }
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("clusters")
    void should_leave_existing_ilm_indices_untouched(Cluster cluster) throws Exception {
        stubCluster(cluster, true);

        startReporterAndIndexOneDocument(Setup.ILM.properties);

        assertThat(allCalls().stream().map(ElasticsearchReporterStartupContractTest::callLine))
            .noneMatch(call -> call.matches("PUT /gravitee-.*-000001"))
            .noneMatch(call -> call.contains("_settings") || call.contains("_lifecycle") || call.contains("_ilm"));
    }

    private static void stubCluster(Cluster cluster, boolean aliasesExist) {
        elasticsearch.stubFor(get(urlEqualTo("/")).willReturn(okJson(cluster.info())));
        elasticsearch.stubFor(put(urlPathMatching("/_index_template/.*")).willReturn(okJson(ACKNOWLEDGED)));
        elasticsearch.stubFor(put(urlPathMatching("/_template/.*")).willReturn(okJson(ACKNOWLEDGED)));
        elasticsearch.stubFor(
            get(urlPathMatching("/_alias/.*")).willReturn(aliasesExist ? okJson("{\"gravitee-000001\":{}}") : aResponse().withStatus(404))
        );
        elasticsearch.stubFor(put(urlPathMatching("/gravitee-.*-000001")).willReturn(okJson(ACKNOWLEDGED)));
        elasticsearch.stubFor(put(urlPathEqualTo(PIPELINE_PATH)).willReturn(okJson(ACKNOWLEDGED)));
        elasticsearch.stubFor(post(urlPathEqualTo("/_bulk")).willReturn(okJson("{\"took\":1,\"errors\":false,\"items\":[]}")));
    }

    private void startReporterAndIndexOneDocument(Map<String, Object> setupProperties) throws Exception {
        final Map<String, Object> properties = new HashMap<>(setupProperties);
        properties.put("reporters.elasticsearch.endpoints[0]", elasticsearch.baseUrl());
        properties.put("reporters.elasticsearch.bulk.actions", 1);

        context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test", properties));
        context.register(ReporterTestConfiguration.class);
        context.refresh();

        reporter = context.getBean(ElasticsearchReporter.class);
        reporter.start();
        await()
            .atMost(Duration.ofSeconds(10))
            .until(() -> elasticsearch.findAll(putRequestedFor(urlPathEqualTo(PIPELINE_PATH))).size() == 1);
        await()
            .atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> {
                reporter.report(metrics());
                assertThat(elasticsearch.findAll(postRequestedFor(urlPathEqualTo("/_bulk")))).isNotEmpty();
            });
    }

    /** Every call in the order the stub received it (its journal lists the latest first), leaving out the client's periodic health checks after the first one. */
    private static List<LoggedRequest> allCalls() {
        final List<ServeEvent> events = new ArrayList<>(elasticsearch.getAllServeEvents());
        Collections.reverse(events);
        final List<LoggedRequest> calls = new ArrayList<>();
        boolean infoSeen = false;
        for (ServeEvent event : events) {
            final LoggedRequest request = event.getRequest();
            final boolean info = "GET /".equals(callLine(request));
            if (!info || !infoSeen) {
                calls.add(request);
            }
            infoSeen |= info;
        }
        return calls;
    }

    /** The calls that prepare the cluster: neither the version lookup nor the bulk requests. */
    private static List<LoggedRequest> setupCalls() {
        return allCalls()
            .stream()
            .filter(request -> !"GET /".equals(callLine(request)))
            .filter(request -> !callLine(request).startsWith("POST /_bulk"))
            .toList();
    }

    /** Each setup call, sorted, followed by its body as sent: the calls themselves run concurrently, in no fixed order. */
    private static String contract(List<LoggedRequest> setupCalls) {
        return setupCalls
            .stream()
            .sorted(Comparator.comparing(ElasticsearchReporterStartupContractTest::callLine))
            .map(call -> "### " + callLine(call) + "\n" + call.getBodyAsString() + "\n")
            .collect(Collectors.joining());
    }

    private static String callLine(LoggedRequest request) {
        return request.getMethod().getName() + " " + request.getUrl();
    }

    private static int indexOfFirst(List<String> calls, Predicate<String> predicate) {
        for (int index = 0; index < calls.size(); index++) {
            if (predicate.test(calls.get(index))) {
                return index;
            }
        }
        return -1;
    }

    private static Metrics metrics() {
        final Metrics metrics = Metrics.on(System.currentTimeMillis()).build();
        metrics.setRequestId(UUID.randomUUID().toString());
        metrics.setTransactionId(UUID.randomUUID().toString());
        metrics.setApi("api");
        metrics.setHttpMethod(HttpMethod.GET);
        metrics.setUri("/echo");
        metrics.setStatus(HttpStatusCode.OK_200);
        metrics.setRemoteAddress("10.0.0.1");
        metrics.setLocalAddress("10.0.0.2");
        return metrics;
    }
}

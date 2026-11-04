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
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import io.gravitee.apim.reporter.elasticsearch.config.PipelineConfiguration;
import io.gravitee.apim.reporter.elasticsearch.config.ReporterConfiguration;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.common.templating.FreeMarkerComponent;
import io.gravitee.elasticsearch.client.Client;
import io.gravitee.node.api.Node;
import io.gravitee.reporter.api.http.Metrics;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.TestScheduler;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.MapPropertySource;

/**
 * Startup of the reporter against a stub Elasticsearch whose setup calls (index templates, ILM aliases, ingest pipeline)
 * can be made to fail.
 */
class ElasticsearchReporterSetupTest {

    private static final String ES_INFO =
        "{\"name\":\"node\",\"cluster_name\":\"cluster\",\"version\":{\"number\":\"8.17.2\",\"build_flavor\":\"default\",\"lucene_version\":\"9.12.0\"},\"tagline\":\"You Know, for Search\"}";
    private static final String ACKNOWLEDGED = "{\"acknowledged\":true}";
    private static final String PIPELINE_PATH = "/_ingest/pipeline/gravitee_pipeline";
    private static final String REQUEST_TEMPLATE_PATH = "/_index_template/gravitee-request";

    @RegisterExtension
    static WireMockExtension elasticsearch = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();
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

    @BeforeEach
    void setUp() {
        logs.start();
        graviteeLogger().addAppender(logs);

        elasticsearch.stubFor(get(urlEqualTo("/")).willReturn(okJson(ES_INFO)));
        elasticsearch.stubFor(put(urlPathMatching("/_index_template/.*")).willReturn(okJson(ACKNOWLEDGED)));
        elasticsearch.stubFor(get(urlPathMatching("/_alias/.*")).willReturn(aResponse().withStatus(404)));
        elasticsearch.stubFor(put(urlPathMatching("/gravitee-.*-000001")).willReturn(okJson(ACKNOWLEDGED)));
        elasticsearch.stubFor(put(urlPathEqualTo(PIPELINE_PATH)).willReturn(okJson(ACKNOWLEDGED)));
        elasticsearch.stubFor(post(urlPathEqualTo("/_bulk")).willReturn(okJson("{\"took\":1,\"errors\":false,\"items\":[]}")));
    }

    @AfterEach
    void tearDown() throws Exception {
        if (reporter != null) {
            reporter.stop();
        }
        if (context != null) {
            context.close();
        }
        graviteeLogger().detachAppender(logs);
        RxJavaPlugins.reset();
    }

    @Test
    void should_index_with_the_pipeline_when_setup_succeeds() throws Exception {
        startReporter(Map.of());
        awaitReporterLog(Level.INFO, "Starting Elastic reporter engine... DONE");

        reporter.report(metrics());

        assertThat(awaitBulkBody()).contains("\"pipeline\" : \"gravitee_pipeline\"");
        assertNoNullPointerExceptionLogged();
    }

    @ParameterizedTest
    @ValueSource(ints = { 400, 403 })
    void should_index_without_the_pipeline_when_elasticsearch_rejects_it(int status) throws Exception {
        elasticsearch.stubFor(
            put(urlPathEqualTo(PIPELINE_PATH)).willReturn(aResponse().withStatus(status).withBody("{\"error\":\"rejected\"}"))
        );
        startReporter(Map.of());
        awaitReporterLog(Level.INFO, "Starting Elastic reporter engine... DONE");

        reporter.report(metrics());

        assertThat(awaitBulkBody()).doesNotContain("pipeline");
        assertThat(reporterMessages(Level.WARN))
            .singleElement()
            .satisfies(message ->
                assertThat(message).contains("gravitee_pipeline").contains("status[" + status + "]").contains("without the ingest pipeline")
            );
        assertNoNullPointerExceptionLogged();
    }

    @Test
    void should_index_without_the_pipeline_when_no_ingest_plugin_is_configured() throws Exception {
        startReporter(Map.of("reporters.elasticsearch.pipeline.plugins.ingest", ""));
        awaitReporterLog(Level.INFO, "Starting Elastic reporter engine... DONE");

        reporter.report(metrics());

        assertThat(awaitBulkBody()).doesNotContain("pipeline");
        elasticsearch.verify(0, putRequestedFor(urlPathEqualTo(PIPELINE_PATH)));
        assertThat(reporterMessages(Level.WARN))
            .singleElement()
            .satisfies(message ->
                assertThat(message).contains("reporters.elasticsearch.pipeline.plugins.ingest").contains("without the ingest pipeline")
            );
        assertNoNullPointerExceptionLogged();
    }

    @Test
    void should_drop_documents_and_log_one_error_when_an_index_template_is_rejected() throws Exception {
        final TestScheduler testScheduler = new TestScheduler();
        RxJavaPlugins.setComputationSchedulerHandler(ignore -> testScheduler);
        elasticsearch.stubFor(
            put(urlPathEqualTo(REQUEST_TEMPLATE_PATH)).willReturn(
                aResponse().withStatus(400).withBody("{\"error\":\"number_of_shards must be greater than 0\"}")
            )
        );
        startReporter(Map.of());
        awaitReporterLog(Level.ERROR, "until the gateway restarts");

        reporter.report(metrics());
        reporter.report(metrics());
        reporter.report(metrics());
        testScheduler.advanceTimeBy(1, TimeUnit.MINUTES);
        testScheduler.advanceTimeBy(1, TimeUnit.MINUTES);

        elasticsearch.verify(0, postRequestedFor(urlPathEqualTo("/_bulk")));
        elasticsearch.verify(0, putRequestedFor(urlPathEqualTo(PIPELINE_PATH)));
        assertThat(reporterMessages(Level.ERROR))
            .singleElement()
            .satisfies(message ->
                assertThat(message)
                    .contains("index template [gravitee-request]")
                    .contains("status[400]")
                    .contains("number_of_shards must be greater than 0")
                    .contains("until the gateway restarts")
            );
        assertThat(reporterMessages(Level.WARN))
            .singleElement()
            .satisfies(message -> assertThat(message).contains("3 documents"));
        assertNoNullPointerExceptionLogged();
    }

    @Test
    void should_log_one_error_when_every_index_template_is_rejected() throws Exception {
        final List<Throwable> undeliverableErrors = new CopyOnWriteArrayList<>();
        RxJavaPlugins.setErrorHandler(undeliverableErrors::add);
        elasticsearch.stubFor(
            put(urlPathMatching("/_index_template/.*")).willReturn(
                aResponse().withStatus(400).withBody("{\"error\":\"number_of_shards must be greater than 0\"}")
            )
        );
        startReporter(Map.of());
        awaitReporterLog(Level.ERROR, "until the gateway restarts");
        await()
            .during(Duration.ofSeconds(1))
            .atMost(Duration.ofSeconds(3))
            .until(() -> undeliverableErrors.isEmpty() && reporterMessages(Level.ERROR).size() == 1);

        assertThat(reporterMessages(Level.ERROR))
            .singleElement()
            .satisfies(message -> assertThat(message).contains("index template ["));
        assertThat(undeliverableErrors).isEmpty();
    }

    @Test
    void should_index_when_another_gateway_creates_the_ilm_alias_first() throws Exception {
        elasticsearch.stubFor(
            get(urlPathEqualTo("/_alias/gravitee-request"))
                .inScenario("alias race")
                .whenScenarioStateIs(STARTED)
                .willReturn(aResponse().withStatus(404))
        );
        elasticsearch.stubFor(
            put(urlPathEqualTo("/gravitee-request-000001"))
                .inScenario("alias race")
                .whenScenarioStateIs(STARTED)
                .willReturn(aResponse().withStatus(400).withBody("{\"error\":\"resource_already_exists_exception\"}"))
                .willSetStateTo("created by another gateway")
        );
        elasticsearch.stubFor(
            get(urlPathEqualTo("/_alias/gravitee-request"))
                .inScenario("alias race")
                .whenScenarioStateIs("created by another gateway")
                .willReturn(okJson("{\"gravitee-request-000001\":{\"aliases\":{\"gravitee-request\":{\"is_write_index\":true}}}}"))
        );
        startReporter(Map.of("reporters.elasticsearch.index_mode", "ilm"));
        awaitReporterLog(Level.INFO, "Starting Elastic reporter engine... DONE");

        reporter.report(metrics());

        assertThat(awaitBulkBody()).contains("\"pipeline\" : \"gravitee_pipeline\"");
        assertThat(reporterMessages(Level.ERROR)).isEmpty();
    }

    @Test
    void should_drop_documents_and_log_one_error_when_the_initial_ilm_index_is_rejected() throws Exception {
        elasticsearch.stubFor(
            put(urlPathEqualTo("/gravitee-request-000001")).willReturn(
                aResponse().withStatus(400).withBody("{\"error\":\"resource_already_exists_exception\"}")
            )
        );
        startReporter(Map.of("reporters.elasticsearch.index_mode", "ilm"));
        awaitReporterLog(Level.ERROR, "until the gateway restarts");

        reporter.report(metrics());

        elasticsearch.verify(0, postRequestedFor(urlPathEqualTo("/_bulk")));
        assertThat(reporterMessages(Level.ERROR))
            .singleElement()
            .satisfies(message ->
                assertThat(message)
                    .contains("gravitee-request-000001")
                    .contains("status[400]")
                    .contains("resource_already_exists_exception")
                    .contains("until the gateway restarts")
            );
        assertNoNullPointerExceptionLogged();
    }

    private void startReporter(Map<String, Object> properties) throws Exception {
        final Map<String, Object> allProperties = new HashMap<>();
        allProperties.put("reporters.elasticsearch.endpoints[0]", elasticsearch.baseUrl());
        allProperties.put("reporters.elasticsearch.bulk.actions", 1);
        allProperties.putAll(properties);

        context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test", allProperties));
        context.register(ReporterTestConfiguration.class);
        context.refresh();

        reporter = context.getBean(ElasticsearchReporter.class);
        reporter.start();
    }

    private static Metrics metrics() {
        final Metrics metrics = Metrics.on(System.currentTimeMillis()).build();
        metrics.setRequestId(UUID.randomUUID().toString());
        metrics.setTransactionId(UUID.randomUUID().toString());
        metrics.setApi("api");
        metrics.setApplication("application");
        metrics.setPlan("plan");
        metrics.setHttpMethod(HttpMethod.GET);
        metrics.setUri("/echo");
        metrics.setStatus(HttpStatusCode.OK_200);
        metrics.setRemoteAddress("10.0.0.1");
        metrics.setLocalAddress("10.0.0.2");
        metrics.setApiResponseTimeMs(50);
        metrics.setProxyResponseTimeMs(51);
        metrics.setProxyLatencyMs(1);
        return metrics;
    }

    private String awaitBulkBody() {
        await()
            .atMost(Duration.ofSeconds(10))
            .until(() -> !elasticsearch.findAll(postRequestedFor(urlPathEqualTo("/_bulk"))).isEmpty());
        final List<LoggedRequest> bulks = elasticsearch.findAll(postRequestedFor(urlPathEqualTo("/_bulk")));
        return bulks.get(0).getBodyAsString();
    }

    private void awaitReporterLog(Level level, String fragment) {
        await()
            .atMost(Duration.ofSeconds(10))
            .until(() ->
                reporterMessages(level)
                    .stream()
                    .anyMatch(message -> message.contains(fragment))
            );
    }

    /** Logs of the reporter itself, leaving out those of the Elasticsearch client, which logs every failed call on its own. */
    private List<String> reporterMessages(Level level) {
        return logs.list
            .stream()
            .filter(event -> event.getLevel() == level)
            .filter(event -> event.getLoggerName().startsWith("io.gravitee.apim.reporter.elasticsearch"))
            .map(ILoggingEvent::getFormattedMessage)
            .toList();
    }

    private void assertNoNullPointerExceptionLogged() {
        assertThat(logs.list)
            .noneMatch(
                event ->
                    event.getThrowableProxy() != null &&
                    NullPointerException.class.getName().equals(event.getThrowableProxy().getClassName())
            )
            .noneMatch(event -> event.getFormattedMessage().contains("Unable to format incoming reportable"));
    }

    private static Logger graviteeLogger() {
        return (Logger) LoggerFactory.getLogger("io.gravitee");
    }
}

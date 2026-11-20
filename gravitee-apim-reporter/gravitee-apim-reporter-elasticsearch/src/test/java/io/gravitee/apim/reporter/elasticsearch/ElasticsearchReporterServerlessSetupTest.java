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
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.gravitee.apim.reporter.elasticsearch.config.PipelineConfiguration;
import io.gravitee.apim.reporter.elasticsearch.config.ReporterConfiguration;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.common.templating.FreeMarkerComponent;
import io.gravitee.elasticsearch.client.Client;
import io.gravitee.node.api.Node;
import io.gravitee.reporter.api.http.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.TestScheduler;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.MapPropertySource;

/**
 * Startup of the reporter on Elastic Cloud Serverless, whose setup is retried until Elasticsearch accepts it, while the
 * gateway keeps serving.
 */
class ElasticsearchReporterServerlessSetupTest {

    private static final String SERVERLESS_INFO =
        "{\"name\":\"serverless\",\"cluster_name\":\"project\",\"version\":{\"number\":\"8.11.0\",\"build_flavor\":\"serverless\",\"lucene_version\":\"9.7.0\"},\"tagline\":\"You Know, for Search\"}";
    private static final String ACKNOWLEDGED = "{\"acknowledged\":true}";
    private static final String PIPELINE_PATH = "/_ingest/pipeline/gravitee_pipeline";
    private static final String REQUEST_TEMPLATE_PATH = "/_index_template/gravitee-request";
    private static final String FORBIDDEN =
        "{\"error\":{\"type\":\"security_exception\",\"reason\":\"action [indices:admin/index_template/put] is unauthorized\"},\"status\":403}";
    private static final String UNAUTHORIZED =
        "{\"error\":{\"type\":\"security_exception\",\"reason\":\"unable to authenticate with provided credentials\"},\"status\":401}";

    @RegisterExtension
    static WireMockExtension elasticsearch = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();
    private final TestScheduler testScheduler = new TestScheduler();
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
            FreeMarkerComponent freeMarkerComponent,
            SimpleMeterRegistry meterRegistry
        ) {
            return new ElasticsearchReporter(
                node,
                reporterConfiguration,
                pipelineConfiguration,
                freeMarkerComponent,
                client,
                new DroppedDocumentsMetric(meterRegistry)
            );
        }

        @Bean
        public SimpleMeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }

    @BeforeEach
    void setUp() {
        logs.start();
        graviteeLogger().addAppender(logs);
        RxJavaPlugins.setComputationSchedulerHandler(ignore -> testScheduler);

        elasticsearch.stubFor(get(urlEqualTo("/")).willReturn(okJson(SERVERLESS_INFO)));
        elasticsearch.stubFor(put(urlPathMatching("/_index_template/.*")).willReturn(okJson(ACKNOWLEDGED)));
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
    void should_retry_the_setup_with_a_growing_delay_until_elasticsearch_accepts_it() throws Exception {
        stubRequestTemplate(
            aResponse().withStatus(HttpStatusCode.SERVICE_UNAVAILABLE_503),
            aResponse().withStatus(HttpStatusCode.SERVICE_UNAVAILABLE_503),
            okJson(ACKNOWLEDGED)
        );
        startReporter();

        awaitRequestTemplateCalls(1);
        awaitReporterLog(Level.WARN, "retrying in 1 s");
        advanceUntil(() -> requestTemplateCalls() == 2);
        awaitReporterLog(Level.WARN, "retrying in 2 s");
        testScheduler.advanceTimeBy(1900, TimeUnit.MILLISECONDS);
        assertThat(requestTemplateCalls()).isEqualTo(2);
        advanceUntil(() -> requestTemplateCalls() == 3);
        awaitReporterLog(Level.INFO, "Starting Elastic reporter engine... DONE");

        assertThat(requestTemplateCalls()).isEqualTo(3);
        assertThat(reporterMessages(Level.ERROR)).isEmpty();
    }

    @Test
    void should_drop_and_count_the_documents_reported_before_the_setup_succeeds() throws Exception {
        stubRequestTemplate(aResponse().withStatus(HttpStatusCode.SERVICE_UNAVAILABLE_503), okJson(ACKNOWLEDGED));
        startReporter();
        awaitReporterLog(Level.WARN, "retrying in 1 s");

        reporter.report(metrics());
        reporter.report(metrics());
        advanceUntil(() -> requestTemplateCalls() == 2);
        awaitReporterLog(Level.INFO, "Starting Elastic reporter engine... DONE");
        testScheduler.advanceTimeBy(1, TimeUnit.MINUTES);

        assertThat(reporterMessages(Level.WARN)).anySatisfy(message ->
            assertThat(message).contains("2 documents dropped in the last minute while the Elasticsearch reporter was setting up")
        );
        assertThat(
            context
                .getBean(SimpleMeterRegistry.class)
                .get("reporter_elasticsearch_dropped_documents")
                .tag("reason", "setup")
                .tag("type", "request")
                .counter()
                .count()
        ).isEqualTo(2);
        reporter.report(metrics());
        await()
            .atMost(Duration.ofSeconds(10))
            .until(() -> elasticsearch.findAll(postRequestedFor(urlPathEqualTo("/_bulk"))).size() == 1);
    }

    @Test
    void should_count_the_documents_elasticsearch_rejects_in_the_dropped_documents_metric() throws Exception {
        elasticsearch.stubFor(
            post(urlPathEqualTo("/_bulk")).willReturn(
                okJson(
                    "{\"took\":1,\"errors\":true,\"items\":[{\"create\":{\"_index\":\"gravitee-request\",\"status\":400," +
                        "\"error\":{\"type\":\"mapper_parsing_exception\",\"reason\":\"failed to parse\"}}}]}"
                )
            )
        );
        startReporter();
        awaitReporterLog(Level.INFO, "Starting Elastic reporter engine... DONE");

        reporter.report(metrics());

        await()
            .atMost(Duration.ofSeconds(10))
            .until(() -> {
                final var counter = context
                    .getBean(SimpleMeterRegistry.class)
                    .find("reporter_elasticsearch_dropped_documents")
                    .tag("reason", "rejected")
                    .tag("type", "request")
                    .counter();
                return counter != null && counter.count() == 1;
            });
    }

    @Test
    void should_name_the_missing_privilege_and_keep_serving_when_the_key_cannot_write_templates() throws Exception {
        elasticsearch.stubFor(put(urlPathMatching("/_index_template/.*")).willReturn(aResponse().withStatus(403).withBody(FORBIDDEN)));
        startReporter();

        awaitReporterLog(Level.WARN, "manage_index_templates");
        reporter.report(metrics());
        advanceUntil(() -> hasReporterLog(Level.WARN, "retrying in 2 s"));

        assertThat(reporter.canHandle(metrics())).isTrue();
        elasticsearch.verify(0, postRequestedFor(urlPathEqualTo("/_bulk")));
        assertThat(reporterMessages(Level.ERROR)).isEmpty();
    }

    @Test
    void should_tell_that_elasticsearch_rejects_the_credentials_when_the_key_is_revoked_after_startup() throws Exception {
        elasticsearch.stubFor(put(urlPathMatching("/_index_template/.*")).willReturn(aResponse().withStatus(401).withBody(UNAUTHORIZED)));
        startReporter();

        awaitReporterLog(Level.WARN, "(Elasticsearch rejects the credentials)");

        assertThat(reporterMessages(Level.WARN)).noneMatch(message -> message.contains("lacks the"));
    }

    @Test
    void should_count_the_documents_reported_after_the_setup_stopped_on_an_unmanaged_template() throws Exception {
        elasticsearch.stubFor(
            get(urlPathEqualTo(REQUEST_TEMPLATE_PATH)).willReturn(
                okJson(
                    "{\"index_templates\":[{\"name\":\"gravitee-request\",\"index_template\":" +
                        "{\"index_patterns\":[\"gravitee-request*\"],\"data_stream\":{},\"priority\":500}}]}"
                )
            )
        );
        startReporter();
        awaitReporterLog(Level.ERROR, "[gravitee-request]");

        reporter.report(metrics());
        testScheduler.advanceTimeBy(1, TimeUnit.MINUTES);

        assertThat(droppedDocuments("setup", "request")).isEqualTo(1);
        assertThat(reporterMessages(Level.WARN)).anySatisfy(message ->
            assertThat(message).contains("disabled since its setup failed at startup: 1 documents dropped")
        );
    }

    @Test
    void should_stop_retrying_the_setup_once_the_reporter_stops() throws Exception {
        elasticsearch.stubFor(put(urlPathMatching("/_index_template/.*")).willReturn(aResponse().withStatus(403).withBody(FORBIDDEN)));
        startReporter();
        awaitReporterLog(Level.WARN, "retrying in 1 s");

        reporter.stop();
        final int templateCalls = elasticsearch.findAll(putRequestedFor(urlPathMatching("/_index_template/.*"))).size();
        await()
            .during(Duration.ofSeconds(2))
            .atMost(Duration.ofSeconds(5))
            .until(() -> {
                testScheduler.advanceTimeBy(1, TimeUnit.SECONDS);
                return elasticsearch.findAll(putRequestedFor(urlPathMatching("/_index_template/.*"))).size() == templateCalls;
            });

        assertThat(reporterMessages(Level.WARN)).noneMatch(message -> message.contains("retrying in 2 s"));
        assertThat(reporterMessages(Level.INFO)).noneMatch(message -> message.contains("Starting Elastic reporter engine... DONE"));
    }

    @Test
    void should_index_without_the_pipeline_when_serverless_rejects_it() throws Exception {
        elasticsearch.stubFor(put(urlPathEqualTo(PIPELINE_PATH)).willReturn(aResponse().withStatus(403).withBody(FORBIDDEN)));
        startReporter();
        awaitReporterLog(Level.INFO, "Starting Elastic reporter engine... DONE");

        reporter.report(metrics());

        await()
            .atMost(Duration.ofSeconds(10))
            .until(() -> !elasticsearch.findAll(postRequestedFor(urlPathEqualTo("/_bulk"))).isEmpty());
        assertThat(elasticsearch.findAll(postRequestedFor(urlPathEqualTo("/_bulk"))).get(0).getBodyAsString())
            .contains("\"create\"")
            .doesNotContain("pipeline");
        assertThat(reporterMessages(Level.WARN))
            .singleElement()
            .satisfies(message -> assertThat(message).contains("gravitee_pipeline").contains("without the ingest pipeline"));
    }

    /** The request template answers each response in turn, then keeps answering the last one. */
    private void stubRequestTemplate(ResponseDefinitionBuilder... responses) {
        for (int attempt = 0; attempt < responses.length; attempt++) {
            final String state = attempt == 0 ? STARTED : "attempt-" + attempt;
            final String next = attempt == responses.length - 1 ? state : "attempt-" + (attempt + 1);
            elasticsearch.stubFor(
                put(urlPathEqualTo(REQUEST_TEMPLATE_PATH))
                    .inScenario("request template")
                    .whenScenarioStateIs(state)
                    .willReturn(responses[attempt])
                    .willSetStateTo(next)
            );
        }
    }

    private void startReporter() throws Exception {
        final Map<String, Object> properties = new HashMap<>();
        properties.put("reporters.elasticsearch.endpoints[0]", elasticsearch.baseUrl());
        properties.put("reporters.elasticsearch.bulk.actions", 1);

        context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test", properties));
        context.register(ReporterTestConfiguration.class);
        context.refresh();

        reporter = context.getBean(ElasticsearchReporter.class);
        reporter.start();
    }

    private void awaitRequestTemplateCalls(int calls) {
        await()
            .atMost(Duration.ofSeconds(10))
            .until(() -> requestTemplateCalls() == calls);
    }

    private static int requestTemplateCalls() {
        return elasticsearch.findAll(putRequestedFor(urlPathEqualTo(REQUEST_TEMPLATE_PATH))).size();
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
        return metrics;
    }

    /**
     * Advances the virtual clock until the condition holds: a retry is logged just before its timer is scheduled, so a single
     * advance could land before the timer exists.
     */
    private void advanceUntil(BooleanSupplier condition) {
        await()
            .atMost(Duration.ofSeconds(10))
            .until(() -> {
                testScheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);
                return condition.getAsBoolean();
            });
    }

    private void awaitReporterLog(Level level, String fragment) {
        await()
            .atMost(Duration.ofSeconds(10))
            .until(() -> hasReporterLog(level, fragment));
    }

    private boolean hasReporterLog(Level level, String fragment) {
        return reporterMessages(level)
            .stream()
            .anyMatch(message -> message.contains(fragment));
    }

    private double droppedDocuments(String reason, String type) {
        return context
            .getBean(SimpleMeterRegistry.class)
            .get("reporter_elasticsearch_dropped_documents")
            .tag("reason", reason)
            .tag("type", type)
            .counter()
            .count();
    }

    private List<String> reporterMessages(Level level) {
        return logs.list
            .stream()
            .filter(event -> event.getLevel() == level)
            .filter(event -> event.getLoggerName().startsWith("io.gravitee.apim.reporter.elasticsearch"))
            .map(ILoggingEvent::getFormattedMessage)
            .toList();
    }

    private static Logger graviteeLogger() {
        return (Logger) LoggerFactory.getLogger("io.gravitee");
    }
}

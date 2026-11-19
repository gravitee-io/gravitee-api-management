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
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.TestScheduler;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
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
            FreeMarkerComponent freeMarkerComponent
        ) {
            return new ElasticsearchReporter(node, reporterConfiguration, pipelineConfiguration, freeMarkerComponent, client);
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
        testScheduler.advanceTimeBy(1, TimeUnit.SECONDS);
        awaitRequestTemplateCalls(2);
        awaitReporterLog(Level.WARN, "retrying in 2 s");
        testScheduler.advanceTimeBy(1, TimeUnit.SECONDS);
        assertThat(requestTemplateCalls()).isEqualTo(2);
        testScheduler.advanceTimeBy(1, TimeUnit.SECONDS);
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
        testScheduler.advanceTimeBy(1, TimeUnit.SECONDS);
        awaitReporterLog(Level.INFO, "Starting Elastic reporter engine... DONE");
        testScheduler.advanceTimeBy(1, TimeUnit.MINUTES);

        assertThat(reporterMessages(Level.WARN)).anySatisfy(message -> assertThat(message).contains("2 documents dropped"));
        reporter.report(metrics());
        await()
            .atMost(Duration.ofSeconds(10))
            .until(() -> elasticsearch.findAll(postRequestedFor(urlPathEqualTo("/_bulk"))).size() == 1);
    }

    @Test
    void should_name_the_missing_privilege_and_keep_serving_when_the_key_cannot_write_templates() throws Exception {
        elasticsearch.stubFor(put(urlPathMatching("/_index_template/.*")).willReturn(aResponse().withStatus(403).withBody(FORBIDDEN)));
        startReporter();

        awaitReporterLog(Level.WARN, "manage_index_templates");
        reporter.report(metrics());
        testScheduler.advanceTimeBy(1, TimeUnit.SECONDS);
        awaitReporterLog(Level.WARN, "retrying in 2 s");

        assertThat(reporter.canHandle(metrics())).isTrue();
        elasticsearch.verify(0, postRequestedFor(urlPathEqualTo("/_bulk")));
        assertThat(reporterMessages(Level.ERROR)).isEmpty();
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

    private void awaitReporterLog(Level level, String fragment) {
        await()
            .atMost(Duration.ofSeconds(10))
            .until(() ->
                reporterMessages(level)
                    .stream()
                    .anyMatch(message -> message.contains(fragment))
            );
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

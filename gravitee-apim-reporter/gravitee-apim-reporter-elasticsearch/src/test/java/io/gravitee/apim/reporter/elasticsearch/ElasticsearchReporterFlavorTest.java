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

import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.gravitee.apim.reporter.elasticsearch.config.PipelineConfiguration;
import io.gravitee.apim.reporter.elasticsearch.config.ReporterConfiguration;
import io.gravitee.common.templating.FreeMarkerComponent;
import io.gravitee.elasticsearch.client.Client;
import io.gravitee.node.api.Node;
import io.gravitee.reporter.api.http.Metrics;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
 * Startup of the reporter against a cluster that reports itself as Elastic Cloud Serverless.
 */
class ElasticsearchReporterFlavorTest {

    private static final String SERVERLESS_INFO =
        "{\"name\":\"serverless\",\"cluster_name\":\"project\",\"version\":{\"number\":\"8.11.0\",\"build_flavor\":\"serverless\",\"lucene_version\":\"9.7.0\"},\"tagline\":\"You Know, for Search\"}";

    @RegisterExtension
    static WireMockExtension elasticsearch = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();
    private AnnotationConfigApplicationContext context;
    private ElasticsearchReporter reporter;

    @Configuration
    @Import(UnitTestConfiguration.class)
    static class ReporterTestConfiguration {

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
        elasticsearch.stubFor(get(urlEqualTo("/")).willReturn(okJson(SERVERLESS_INFO)));
        elasticsearch.stubFor(put(urlPathMatching(".*")).willReturn(okJson("{\"acknowledged\":true}")));
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
    }

    @Test
    void should_stay_inactive_without_writing_anything_on_serverless() throws Exception {
        startReporter(Map.of());

        assertThat(reporter.canHandle(Metrics.on(System.currentTimeMillis()).build())).isFalse();
        assertThat(elasticsearch.findAll(anyRequestedFor(anyUrl())))
            .isNotEmpty()
            .allSatisfy(request -> assertThat(request.getMethod().getName() + " " + request.getUrl()).isEqualTo("GET /"));
        assertThat(messages(Level.ERROR))
            .singleElement()
            .satisfies(message -> assertThat(message).contains("Elastic Cloud Serverless index setup is not available"));
    }

    @Test
    void should_refuse_the_configured_index_mode_on_serverless_without_writing_anything() throws Exception {
        startReporter(Map.of("reporters.elasticsearch.index_mode", "ilm"));

        assertThat(reporter.canHandle(Metrics.on(System.currentTimeMillis()).build())).isFalse();
        elasticsearch.verify(0, putRequestedFor(anyUrl()));
        assertThat(messages(Level.ERROR))
            .singleElement()
            .satisfies(message -> assertThat(message).contains("'reporters.elasticsearch.index_mode'"));
    }

    @Test
    void should_prepare_the_indices_when_classic_is_configured_on_serverless() throws Exception {
        startReporter(Map.of("reporters.elasticsearch.flavor", "classic"));

        await()
            .atMost(Duration.ofSeconds(10))
            .until(() -> messages(Level.INFO).contains("Starting Elastic reporter engine... DONE"));
        assertThat(reporter.canHandle(Metrics.on(System.currentTimeMillis()).build())).isTrue();
        elasticsearch.verify(putRequestedFor(urlEqualTo("/_index_template/gravitee-request")));
        assertThat(messages(Level.INFO)).anySatisfy(message -> assertThat(message).contains("flavor [classic] (configured)"));
    }

    private void startReporter(Map<String, Object> properties) throws Exception {
        final Map<String, Object> allProperties = new HashMap<>(properties);
        allProperties.put("reporters.elasticsearch.endpoints[0]", elasticsearch.baseUrl());

        context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test", allProperties));
        context.register(ReporterTestConfiguration.class);
        context.refresh();

        reporter = context.getBean(ElasticsearchReporter.class);
        reporter.start();
    }

    private List<String> messages(Level level) {
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

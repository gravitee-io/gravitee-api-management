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
package io.gravitee.repository.elasticsearch.spring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.gravitee.repository.elasticsearch.configuration.Flavor;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.http.HttpServer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.MapPropertySource;

/**
 * Startup of the analytics repository against a stub cluster, depending on the flavor it reports and the one configured.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ElasticsearchRepositoryFlavorTest {

    private static final String PREFIX = "analytics.elasticsearch.";

    private Vertx vertx;
    private HttpServer elasticsearch;
    private volatile String buildFlavorField;
    private AnnotationConfigApplicationContext context;
    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();

    @Configuration
    @Import(ElasticsearchRepositoryConfiguration.class)
    static class RepositoryTestConfiguration {

        @Bean
        public io.vertx.core.Vertx vertx() {
            return io.vertx.core.Vertx.vertx();
        }
    }

    @BeforeEach
    void setUp() {
        logs.start();
        repositoryLogger().addAppender(logs);
        vertx = Vertx.vertx();
        elasticsearch = vertx
            .createHttpServer()
            .requestHandler(request -> {
                request
                    .response()
                    .putHeader("Content-Type", "application/json")
                    .end(
                        "{\"name\":\"node\",\"cluster_name\":\"cluster\",\"version\":{\"number\":\"8.17.2\"," +
                            buildFlavorField +
                            "\"lucene_version\":\"9.12.0\"}}"
                    )
                    .subscribe();
            })
            .rxListen(0)
            .blockingGet();
        givenBuildFlavor("default");
    }

    @AfterEach
    void tearDown() {
        if (context != null) {
            context.close();
        }
        vertx.rxClose().blockingAwait();
        repositoryLogger().detachAppender(logs);
    }

    @Test
    void should_detect_classic_when_the_build_flavor_is_not_serverless() {
        start(Map.of());

        assertThat(context.getBean(Flavor.class)).isEqualTo(Flavor.CLASSIC);
    }

    @Test
    void should_detect_classic_when_the_build_flavor_is_absent() {
        givenBuildFlavor(null);

        start(Map.of());

        assertThat(context.getBean(Flavor.class)).isEqualTo(Flavor.CLASSIC);
    }

    @Test
    void should_detect_serverless_when_the_build_flavor_is_serverless() {
        givenBuildFlavor("serverless");

        start(Map.of(PREFIX + "flavor", "auto"));

        assertThat(context.getBean(Flavor.class)).isEqualTo(Flavor.SERVERLESS);
    }

    @Test
    void should_use_classic_when_classic_is_configured_on_serverless() {
        givenBuildFlavor("serverless");

        start(Map.of(PREFIX + "flavor", "classic", PREFIX + "index_mode", "ilm"));

        assertThat(context.getBean(Flavor.class)).isEqualTo(Flavor.CLASSIC);
    }

    @Test
    void should_use_serverless_when_serverless_is_configured_on_a_classic_cluster() {
        start(Map.of(PREFIX + "flavor", "serverless"));

        assertThat(context.getBean(Flavor.class)).isEqualTo(Flavor.SERVERLESS);
    }

    @Test
    void should_accept_the_data_stream_index_mode_on_serverless() {
        givenBuildFlavor("serverless");

        start(Map.of(PREFIX + "index_mode", "data_stream"));

        assertThat(context.getBean(Flavor.class)).isEqualTo(Flavor.SERVERLESS);
    }

    @Test
    void should_refuse_to_start_with_an_unknown_flavor() {
        assertThatThrownBy(() -> start(Map.of(PREFIX + "flavor", "cloud")))
            .rootCause()
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("'analytics.elasticsearch.flavor'")
            .hasMessageContaining("cloud");
    }

    @ParameterizedTest
    @ValueSource(strings = { "ilm", "daily" })
    void should_start_and_warn_with_an_explicit_ilm_or_daily_index_mode_on_serverless(String indexMode) {
        givenBuildFlavor("serverless");

        start(Map.of(PREFIX + "index_mode", indexMode));

        assertThat(context.getBean(Flavor.class)).isEqualTo(Flavor.SERVERLESS);
        assertThat(warnings()).anySatisfy(warning ->
            assertThat(warning).contains("'analytics.elasticsearch.index_mode'").contains(indexMode)
        );
    }

    @Test
    void should_refuse_to_start_with_the_data_stream_index_mode_on_classic() {
        assertThatThrownBy(() -> start(Map.of(PREFIX + "index_mode", "data_stream")))
            .rootCause()
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("'analytics.elasticsearch.index_mode'")
            .hasMessageContaining("data_stream");
    }

    private List<String> warnings() {
        return logs.list
            .stream()
            .filter(event -> event.getLevel() == Level.WARN)
            .map(ILoggingEvent::getFormattedMessage)
            .toList();
    }

    private static Logger repositoryLogger() {
        return (Logger) LoggerFactory.getLogger("io.gravitee.repository.elasticsearch");
    }

    private void givenBuildFlavor(String buildFlavor) {
        buildFlavorField = buildFlavor == null ? "" : "\"build_flavor\":\"" + buildFlavor + "\",";
    }

    private void start(Map<String, Object> properties) {
        final Map<String, Object> allProperties = new HashMap<>(properties);
        allProperties.put(PREFIX + "endpoints[0]", "http://localhost:" + elasticsearch.actualPort());

        context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test", allProperties));
        context.register(RepositoryTestConfiguration.class);
        context.refresh();
    }
}

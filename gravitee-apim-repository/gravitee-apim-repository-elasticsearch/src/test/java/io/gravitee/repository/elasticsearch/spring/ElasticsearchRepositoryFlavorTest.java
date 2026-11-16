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
import io.gravitee.elasticsearch.index.IndexNameGenerator;
import io.gravitee.elasticsearch.utils.Type;
import io.gravitee.repository.elasticsearch.configuration.Flavor;
import io.gravitee.repository.elasticsearch.configuration.IndexMode;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.http.HttpServer;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
    private static final Map<String, String> PLACEHOLDERS = Map.of("orgId", "ORG-1", "envId", "env-1");
    private static final long FROM = Instant.parse("2026-11-10T00:00:00Z").toEpochMilli();
    private static final long TO = Instant.parse("2026-11-12T00:00:00Z").toEpochMilli();

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
    @ValueSource(strings = { "ilm", "daily", "weekly" })
    void should_read_data_streams_and_warn_that_another_index_mode_is_ignored_on_serverless(String indexMode) {
        givenBuildFlavor("serverless");

        start(Map.of(PREFIX + "index_mode", indexMode));

        final IndexNameGenerator indexNames = context.getBean(IndexNameGenerator.class);
        assertThat(context.getBean(Flavor.class)).isEqualTo(Flavor.SERVERLESS);
        assertThat(context.getBean(IndexMode.class)).isEqualTo(IndexMode.DATA_STREAM);
        assertThat(indexNames.getWildcardIndexName(PLACEHOLDERS, Type.V4_METRICS, null)).isEqualTo("gravitee-v4-metrics");
        assertThat(warnings()).anySatisfy(warning ->
            assertThat(warning).contains("'analytics.elasticsearch.index_mode'").contains(indexMode).contains("ignored")
        );
    }

    @Test
    void should_accept_the_data_stream_index_mode_on_classic() {
        start(Map.of(PREFIX + "index_mode", "data_stream"));

        assertThat(context.getBean(IndexMode.class)).isEqualTo(IndexMode.DATA_STREAM);
    }

    @ParameterizedTest
    @CsvSource({ "default, data_stream", "serverless, data_stream", "serverless, ''" })
    void should_read_data_streams_named_after_the_prefix_and_the_type(String buildFlavor, String indexMode) {
        givenBuildFlavor(buildFlavor);

        start(Map.of(PREFIX + "index_mode", indexMode));

        final IndexNameGenerator indexNames = context.getBean(IndexNameGenerator.class);
        assertThat(context.getBean(IndexMode.class)).isEqualTo(IndexMode.DATA_STREAM);
        assertThat(indexNames.getWildcardIndexName(PLACEHOLDERS, Type.V4_METRICS, null)).isEqualTo("gravitee-v4-metrics");
        assertThat(indexNames.getIndexName(PLACEHOLDERS, Type.REQUEST, FROM, TO, null)).isEqualTo("gravitee-request");
    }

    @Test
    void should_fill_the_organization_into_the_data_stream_name() {
        start(Map.of(PREFIX + "index_mode", "data_stream", PREFIX + "index", "logs-{orgId}"));

        final IndexNameGenerator indexNames = context.getBean(IndexNameGenerator.class);
        assertThat(indexNames.getWildcardIndexName(PLACEHOLDERS, Type.V4_METRICS, null)).isEqualTo("logs-org-1-v4-metrics");
    }

    @Test
    void should_keep_reading_daily_indices_when_the_index_mode_is_unset_on_classic() {
        start(Map.of());

        final IndexNameGenerator indexNames = context.getBean(IndexNameGenerator.class);
        assertThat(context.getBean(IndexMode.class)).isEqualTo(IndexMode.DAILY);
        assertThat(indexNames.getWildcardIndexName(PLACEHOLDERS, Type.V4_METRICS, null)).isEqualTo("gravitee-v4-metrics-*");
    }

    @Test
    void should_keep_reading_ilm_aliases_in_ilm_mode() {
        start(Map.of(PREFIX + "index_mode", "ilm"));

        final IndexNameGenerator indexNames = context.getBean(IndexNameGenerator.class);
        assertThat(context.getBean(IndexMode.class)).isEqualTo(IndexMode.ILM);
        assertThat(indexNames.getWildcardIndexName(PLACEHOLDERS, Type.V4_METRICS, null)).isEqualTo("gravitee-v4-metrics");
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
        // Left unset here means unset: the integration runs pass an index mode as a system property.
        allProperties.putIfAbsent(PREFIX + "index_mode", "");
        allProperties.put(PREFIX + "endpoints[0]", "http://localhost:" + elasticsearch.actualPort());

        context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test", allProperties));
        context.register(RepositoryTestConfiguration.class);
        context.refresh();
    }
}

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
package io.gravitee.repository.elasticsearch.otel.log;

import io.gravitee.elasticsearch.client.Client;
import io.gravitee.elasticsearch.client.http.HttpClient;
import io.gravitee.elasticsearch.client.http.HttpClientConfiguration;
import io.gravitee.elasticsearch.config.Endpoint;
import io.gravitee.repository.otel.log.api.OtelLogRepository;
import java.time.Duration;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * Test wiring for {@link io.gravitee.repository.otel.log.OtelLogRepositoryTest} against Elasticsearch.
 * <p>
 * Stands up an Elasticsearch container plus an OTel Collector container on a shared docker network — the
 * collector receives OTLP HTTP from {@link ElasticsearchOtelLogTestRepositoryInitializer} on both the
 * traces pipeline (the elasticsearch exporter promotes span events into the logs data stream when
 * {@code mapping.mode: otel} is set) and the logs pipeline (direct OTLP log records, modelling what
 * {@code gravitee-reporter-otel} ships at runtime). The production
 * {@link ElasticsearchOtelLogRepository} then queries ES through the same {@link HttpClient} pipeline
 * used at runtime.
 * <p>
 * The configured {@code logs_index} ({@code logs-apim.otel-test-org}) matches what the contract test's
 * {@code QueryContext("test-org", "test-env")} resolves through the {@code logs-apim.otel-{orgId}} template.
 *
 * @author GraviteeSource Team
 */
@Configuration
public class ElasticsearchOtelLogTestRepositoryConfiguration {

    static final String ES_NETWORK_ALIAS = "elasticsearch";
    static final int OTEL_COLLECTOR_OTLP_HTTP_PORT = 4318;
    private static final int ES_HTTP_PORT = 9200;
    private static final String ES_IMAGE = "docker.elastic.co/elasticsearch/elasticsearch:8.17.2";
    // The elasticsearch exporter exposes mapping.mode: otel from 0.116+. The snake_case OTel-native field
    // layout it produces (trace_id, span_id, attributes.*, resource.attributes.*, event_name, body.*) is
    // what ElasticsearchOtelLogRepository parses. Pin to a known-good recent build for reproducibility.
    private static final String OTEL_COLLECTOR_IMAGE = "otel/opentelemetry-collector-contrib:0.145.0";

    @Bean
    public Network otelLogTestNetwork() {
        return Network.newNetwork();
    }

    // No `Vertx` @Bean here on purpose. {@code AbstractRepositoryTest} component-scans every
    // {@code *TestRepository*} config in the module, so this config and the tracing IT config load into the
    // same Spring context. {@link io.gravitee.elasticsearch.client.http.HttpClient}'s
    // {@code @Autowired Vertx vertx} would be ambiguous if both configs registered one. We rely on the
    // single Vertx bean declared by the tracing IT config (or any other test that defines exactly one).

    @Bean(destroyMethod = "stop")
    public ElasticsearchContainer otelLogTestElasticsearchContainer(Network otelLogTestNetwork) {
        ElasticsearchContainer container = new ElasticsearchContainer(DockerImageName.parse(ES_IMAGE))
            .withEnv("xpack.security.enabled", "false")
            .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m")
            .withNetwork(otelLogTestNetwork)
            .withNetworkAliases(ES_NETWORK_ALIAS);
        container.setWaitStrategy(Wait.forLogMessage(".*started.*", 1).withStartupTimeout(Duration.ofSeconds(180)));
        container.start();
        return container;
    }

    @Bean(destroyMethod = "stop")
    public GenericContainer<?> otelLogTestOtelCollectorContainer(
        Network otelLogTestNetwork,
        ElasticsearchContainer otelLogTestElasticsearchContainer
    ) {
        // Depending on otelLogTestElasticsearchContainer ensures Spring starts ES first; the collector
        // retries during its own startup window if ES isn't yet accepting writes.
        GenericContainer<?> collector = new GenericContainer<>(DockerImageName.parse(OTEL_COLLECTOR_IMAGE))
            .withNetwork(otelLogTestNetwork)
            .withExposedPorts(OTEL_COLLECTOR_OTLP_HTTP_PORT)
            .withCopyFileToContainer(
                MountableFile.forClasspathResource("otel-collector-otel-logs-config.yaml"),
                "/etc/otelcol-contrib/config.yaml"
            )
            .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(60)));
        collector.start();
        return collector;
    }

    @Bean(destroyMethod = "")
    public Client otelLogTestElasticsearchClient(ElasticsearchContainer otelLogTestElasticsearchContainer) {
        HttpClientConfiguration clientConfiguration = new HttpClientConfiguration();
        clientConfiguration.setEndpoints(
            List.of(
                new Endpoint(
                    "http://" +
                        otelLogTestElasticsearchContainer.getHost() +
                        ":" +
                        otelLogTestElasticsearchContainer.getMappedPort(ES_HTTP_PORT)
                )
            )
        );
        clientConfiguration.setRequestTimeout(60_000L);
        return new HttpClient(clientConfiguration);
    }

    @Bean
    public OtelLogRepository otelLogRepository(Client otelLogTestElasticsearchClient) {
        // Matches the collector's logs_index (otel-collector-otel-logs-config.yaml) once {orgId} is resolved
        // against the contract test's QueryContext (Fixtures.ORG_ID = "test-org").
        return new ElasticsearchOtelLogRepository("logs-apim.otel-{orgId}", otelLogTestElasticsearchClient);
    }
}

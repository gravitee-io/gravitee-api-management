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
package io.gravitee.repository.elasticsearch.tracing;

import io.gravitee.elasticsearch.client.Client;
import io.gravitee.elasticsearch.client.http.HttpClient;
import io.gravitee.elasticsearch.client.http.HttpClientConfiguration;
import io.gravitee.elasticsearch.config.Endpoint;
import io.gravitee.repository.tracing.api.TracingRepository;
import io.vertx.rxjava3.core.Vertx;
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
 * Test wiring for {@link io.gravitee.repository.tracing.TracingRepositoryTest} against Elasticsearch.
 * <p>
 * Stands up an Elasticsearch container plus an OTel Collector container on a shared docker network — the collector
 * receives OTLP HTTP from {@link ElasticsearchTracingTestRepositoryInitializer} and writes spans into ES via the
 * {@code elasticsearch} exporter in {@code mapping.mode: otel}, mirroring the production ingestion path. The
 * production {@link ElasticsearchTracingRepository} then queries ES through the same {@link HttpClient} pipeline used
 * at runtime.
 * <p>
 * The configured {@code traces_index} ({@code traces-apim.otel-test-org}) matches what the contract test's
 * {@code QueryContext("test-org", "test-env")} resolves through the {@code traces-apim.otel-{orgId}} template.
 *
 * @author GraviteeSource Team
 */
@Configuration
public class ElasticsearchTracingTestRepositoryConfiguration {

    static final String ES_NETWORK_ALIAS = "elasticsearch";
    static final int OTEL_COLLECTOR_OTLP_HTTP_PORT = 4318;
    private static final int ES_HTTP_PORT = 9200;
    private static final String ES_IMAGE = "docker.elastic.co/elasticsearch/elasticsearch:8.17.2";
    // The elasticsearch exporter exposes mapping.mode: otel from 0.116+. The snake_case OTel-native field layout
    // it produces (trace_id, span_id, parent_span_id, attributes.*, resource.attributes.*, events[]) is what
    // ElasticsearchTracingRepository parses. Pin to a known-good recent build so the contract test is reproducible.
    private static final String OTEL_COLLECTOR_IMAGE = "otel/opentelemetry-collector-contrib:0.145.0";

    @Bean
    public Network tracingTestNetwork() {
        return Network.newNetwork();
    }

    @Bean
    public Vertx tracingTestVertx() {
        return Vertx.vertx();
    }

    @Bean(destroyMethod = "stop")
    public ElasticsearchContainer tracingTestElasticsearchContainer(Network tracingTestNetwork) {
        ElasticsearchContainer container = new ElasticsearchContainer(DockerImageName.parse(ES_IMAGE))
            .withEnv("xpack.security.enabled", "false")
            .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m")
            .withNetwork(tracingTestNetwork)
            .withNetworkAliases(ES_NETWORK_ALIAS);
        container.setWaitStrategy(Wait.forLogMessage(".*started.*", 1).withStartupTimeout(Duration.ofSeconds(180)));
        container.start();
        return container;
    }

    @Bean(destroyMethod = "stop")
    public GenericContainer<?> tracingTestOtelCollectorContainer(
        Network tracingTestNetwork,
        ElasticsearchContainer tracingTestElasticsearchContainer
    ) {
        // Depending on tracingTestElasticsearchContainer ensures Spring starts ES first; the collector retries during
        // its own startup window if ES isn't yet accepting writes.
        GenericContainer<?> collector = new GenericContainer<>(DockerImageName.parse(OTEL_COLLECTOR_IMAGE))
            .withNetwork(tracingTestNetwork)
            .withExposedPorts(OTEL_COLLECTOR_OTLP_HTTP_PORT)
            .withCopyFileToContainer(
                MountableFile.forClasspathResource("otel-collector-tracing-config.yaml"),
                "/etc/otelcol-contrib/config.yaml"
            )
            .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(60)));
        collector.start();
        return collector;
    }

    @Bean(destroyMethod = "")
    public Client tracingTestElasticsearchClient(ElasticsearchContainer tracingTestElasticsearchContainer) {
        HttpClientConfiguration clientConfiguration = new HttpClientConfiguration();
        clientConfiguration.setEndpoints(
            List.of(
                new Endpoint(
                    "http://" +
                        tracingTestElasticsearchContainer.getHost() +
                        ":" +
                        tracingTestElasticsearchContainer.getMappedPort(ES_HTTP_PORT)
                )
            )
        );
        clientConfiguration.setRequestTimeout(60_000L);
        return new HttpClient(clientConfiguration);
    }

    @Bean
    public TracingRepository tracingRepository(Client tracingTestElasticsearchClient) {
        // Matches the collector's traces_index (otel-collector-tracing-config.yaml) once {orgId} is resolved
        // against the contract test's QueryContext (Fixtures.ORG_ID = "test-org").
        return new ElasticsearchTracingRepository("traces-apim.otel-{orgId}", tracingTestElasticsearchClient);
    }
}

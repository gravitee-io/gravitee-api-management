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

import io.gravitee.apim.reporter.elasticsearch.config.ReporterConfiguration;
import io.gravitee.elasticsearch.config.Endpoint;
import java.util.Collections;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * Spring configuration of the integration tests that run against OpenSearch.
 *
 * <p>Like the Elasticsearch node of {@link IntegrationTestConfiguration}, the OpenSearch node is started once for the
 * whole JVM and shared by every context importing this class: the CI machine does not hold a third node. Ryuk removes
 * it when the JVM exits.
 */
@Configuration
@Import(UnitTestConfiguration.class)
public class OpenSearchIntegrationTestConfiguration {

    public static final String OPENSEARCH_DEFAULT_VERSION = "2.13.0";

    private static final GenericContainer<?> OPENSEARCH_CONTAINER = startContainer();

    @Bean
    public ReporterConfiguration configuration() {
        ReporterConfiguration configuration = new ReporterConfiguration();
        configuration.setEndpoints(
            Collections.singletonList(
                new Endpoint("http://" + OPENSEARCH_CONTAINER.getHost() + ":" + OPENSEARCH_CONTAINER.getMappedPort(9200))
            )
        );
        return configuration;
    }

    private static GenericContainer<?> startContainer() {
        final String version = System.getProperty("opensearch.version", OPENSEARCH_DEFAULT_VERSION);
        final GenericContainer<?> container = new GenericContainer<>(DockerImageName.parse("opensearchproject/opensearch:" + version))
            .withExposedPorts(9200)
            .withEnv("discovery.type", "single-node")
            .withEnv("DISABLE_SECURITY_PLUGIN", "true")
            .withEnv("DISABLE_INSTALL_DEMO_CONFIG", "true")
            // Left alone, the node sizes its heap at half the memory it can see, and it shares the CI
            // machine with the Elasticsearch node the other integration tests keep running.
            .withEnv("OPENSEARCH_JAVA_OPTS", "-Xms512m -Xmx512m")
            .waitingFor(Wait.forHttp("/").forStatusCode(200));
        container.start();
        return container;
    }
}

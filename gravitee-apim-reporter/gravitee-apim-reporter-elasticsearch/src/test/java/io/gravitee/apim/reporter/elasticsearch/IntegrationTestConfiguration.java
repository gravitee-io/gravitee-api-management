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
import org.testcontainers.elasticsearch.ElasticsearchContainer;

/**
 * Spring configuration used for testing purpose.
 *
 * @author Guillaume Waignier
 * @author Sebastien Devaux
 */
@Configuration
@Import(UnitTestConfiguration.class)
public class IntegrationTestConfiguration {

    public static final String ELASTICSEARCH_DEFAULT_VERSION = "9.2.1";
    public static final String CLUSTER_NAME = "gravitee_test";

    /**
     * One Elasticsearch node for the whole JVM, shared by every Spring context importing this configuration.
     *
     * <p>The Spring TestContext cache never closes a context between test classes, so a container declared
     * as a plain bean stays up until the JVM exits. Two contexts import this class — this one and the
     * {@code TestConfig} of {@code ElasticsearchReporterTest} — and each would keep its own node running,
     * alongside the OpenSearch node of {@code IndexPreparerIntegrationTest}. Three of them is more than the
     * CI machine holds, and the third one never finishes starting.
     *
     * <p>The bean below therefore declares no destroy method: closing the container with the first context
     * to be discarded would take it away from the other. Ryuk removes it when the JVM exits.
     */
    private static final ElasticsearchContainer ELASTICSEARCH_CONTAINER = startContainer();

    @Bean
    public ReporterConfiguration configuration(ElasticsearchContainer elasticSearchContainer) {
        ReporterConfiguration elasticConfiguration = new ReporterConfiguration();
        elasticConfiguration.setEndpoints(Collections.singletonList(new Endpoint("http://" + elasticSearchContainer.getHttpHostAddress())));
        elasticConfiguration.setUsername("elastic");
        elasticConfiguration.setPassword(ElasticsearchContainer.ELASTICSEARCH_DEFAULT_PASSWORD);
        return elasticConfiguration;
    }

    @Bean
    public ElasticsearchContainer elasticSearchContainer() {
        return ELASTICSEARCH_CONTAINER;
    }

    private static ElasticsearchContainer startContainer() {
        final String version = System.getProperty("elasticsearch.version", ELASTICSEARCH_DEFAULT_VERSION);
        final ElasticsearchContainer elasticsearchContainer = new ElasticsearchContainer(
            "docker.elastic.co/elasticsearch/elasticsearch:" + version
        );
        elasticsearchContainer.withEnv("cluster.name", CLUSTER_NAME);
        if (!version.startsWith("7")) {
            elasticsearchContainer.withEnv("xpack.security.enabled", "false");
        }
        elasticsearchContainer.start();
        return elasticsearchContainer;
    }
}

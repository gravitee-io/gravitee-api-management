/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.repository.elasticsearch.spring;

import io.gravitee.elasticsearch.client.Client;
import io.gravitee.elasticsearch.client.http.HttpClient;
import io.gravitee.elasticsearch.client.http.HttpClientConfiguration;
import io.gravitee.elasticsearch.config.Endpoint;
import io.gravitee.elasticsearch.templating.freemarker.FreeMarkerComponent;
import io.gravitee.repository.elasticsearch.configuration.RepositoryConfiguration;
import io.vertx.core.Vertx;
import java.util.Collections;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.ConfigurableEnvironment;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

/**
 * Spring configuration for the test.
 *
 * @author Guillaume Waignier
 * @author Sebastien Devaux
 */

@Configuration
@Import(ElasticsearchRepositoryConfiguration.class)
public class ElasticsearchRepositoryConfigurationTest {

    public static final String DEFAULT_ELASTICSEARCH_VERSION = "8.5.2";
    public static final String CLUSTER_NAME = "gravitee_test";

    @Value("${elasticsearch.version:" + DEFAULT_ELASTICSEARCH_VERSION + "}")
    private String elasticsearchVersion;

    @Bean
    public Vertx vertx() {
        return Vertx.vertx();
    }

    @Bean
    public DatabaseHydrator databaseHydrator(Client client, FreeMarkerComponent freeMarkerComponent) {
        return new DatabaseHydrator(client, freeMarkerComponent, elasticsearchVersion);
    }

    @Bean
    public Client client(RepositoryConfiguration repositoryConfiguration) throws InterruptedException {
        HttpClientConfiguration clientConfiguration = new HttpClientConfiguration();
        clientConfiguration.setEndpoints(repositoryConfiguration.getEndpoints());
        clientConfiguration.setUsername(repositoryConfiguration.getUsername());
        clientConfiguration.setPassword(repositoryConfiguration.getPassword());
        clientConfiguration.setRequestTimeout(60_000);
        return new HttpClient(clientConfiguration);
    }

    @Bean
    public RepositoryConfiguration repositoryConfiguration(
        ElasticsearchContainer elasticSearchContainer,
        ConfigurableEnvironment environment
    ) {
        RepositoryConfiguration elasticConfiguration = new RepositoryConfiguration();
        elasticConfiguration.setEndpoints(Collections.singletonList(new Endpoint("http://" + elasticSearchContainer.getHttpHostAddress())));
        elasticConfiguration.setUsername("elastic");
        elasticConfiguration.setPassword(ElasticsearchContainer.ELASTICSEARCH_DEFAULT_PASSWORD);
        return elasticConfiguration;
    }

    @Bean(destroyMethod = "close")
    public ElasticsearchContainer elasticSearchContainer() {
        final ElasticsearchContainer elasticsearchContainer = new ElasticsearchContainer(
            "docker.elastic.co/elasticsearch/elasticsearch:" + elasticsearchVersion
        );
        elasticsearchContainer.withEnv("cluster.name", CLUSTER_NAME);
        elasticsearchContainer.withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m");
        if (elasticsearchVersion.startsWith("8")) {
            elasticsearchContainer.withEnv("xpack.security.enabled", "false");
        }
        elasticsearchContainer.start();
        return elasticsearchContainer;
    }
}

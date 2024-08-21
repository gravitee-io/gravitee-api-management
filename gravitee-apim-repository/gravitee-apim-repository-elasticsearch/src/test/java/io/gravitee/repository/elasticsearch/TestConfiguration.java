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
package io.gravitee.repository.elasticsearch;

import io.gravitee.elasticsearch.client.Client;
import io.gravitee.elasticsearch.client.http.HttpClient;
import io.gravitee.elasticsearch.client.http.HttpClientConfiguration;
import io.gravitee.elasticsearch.config.Endpoint;
import io.gravitee.elasticsearch.templating.freemarker.FreeMarkerComponent;
import io.gravitee.repository.elasticsearch.configuration.RepositoryConfiguration;
import io.vertx.core.Vertx;
import java.util.Collections;
import org.opensearch.testcontainers.OpensearchContainer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.ConfigurableEnvironment;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

/**
 * Spring configuration for the test.
 *
 * @author Guillaume Waignier
 * @author Sebastien Devaux
 */
@Configuration
@Import(io.gravitee.repository.elasticsearch.spring.ElasticsearchRepositoryConfiguration.class)
public class TestConfiguration {

    public static final String DEFAULT_ELASTICSEARCH_VERSION = "8.8.0";
    public static final String DEFAULT_OPENSEARCH_VERSION = "2";
    private static final String DEFAULT_SEARCH_TYPE = "elasticsearch";

    public static final String CLUSTER_NAME = "gravitee_test";
    private boolean isElasticsearch = true;

    @Value("${search.type:" + DEFAULT_SEARCH_TYPE + "}")
    private String searchType;

    @Value("${elasticsearch.version:" + DEFAULT_ELASTICSEARCH_VERSION + "}")
    private String elasticsearchVersion;

    @Value("${opensearch.version:" + DEFAULT_OPENSEARCH_VERSION + "}")
    private String opensearchVersion;

    @Bean
    public Vertx vertx() {
        return Vertx.vertx();
    }

    @Bean
    public DatabaseHydrator databaseHydrator(Client client, FreeMarkerComponent freeMarkerComponent) {
        return new DatabaseHydrator(client, freeMarkerComponent, elasticsearchVersion);
    }

    @Bean
    public Client client(RepositoryConfiguration repositoryConfiguration) {
        HttpClientConfiguration clientConfiguration = new HttpClientConfiguration();
        clientConfiguration.setEndpoints(repositoryConfiguration.getEndpoints());
        clientConfiguration.setUsername(repositoryConfiguration.getUsername());
        clientConfiguration.setPassword(repositoryConfiguration.getPassword());
        clientConfiguration.setRequestTimeout(60_000);
        return new HttpClient(clientConfiguration);
    }

    @Bean
    public RepositoryConfiguration repositoryConfiguration(GenericContainer<?> container, ConfigurableEnvironment environment) {
        RepositoryConfiguration configuration = new RepositoryConfiguration();
        configuration.setEndpoints(
            Collections.singletonList(new Endpoint("http://" + container.getHost() + ":" + container.getMappedPort(9200)))
        );
        configuration.setUsername(isElasticsearch ? "elastic" : "admin");
        configuration.setPassword(isElasticsearch ? ElasticsearchContainer.ELASTICSEARCH_DEFAULT_PASSWORD : "admin");
        if (isElasticsearch && elasticsearchVersion.startsWith("5")) {
            environment.getSystemProperties().put("analytics.elasticsearch.index_per_type", "true");
        }
        return configuration;
    }

    @Bean(destroyMethod = "close")
    public GenericContainer<?> elasticSearchContainer() {
        isElasticsearch = DEFAULT_SEARCH_TYPE.equals(searchType);
        var container = isElasticsearch ? generateElasticsearchContainer() : generateOpenSearchContainer();
        container.withEnv("cluster.name", CLUSTER_NAME);
        container.withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m");
        container.start();
        return container;
    }

    private ElasticsearchContainer generateElasticsearchContainer() {
        final String dockerImage = "docker.elastic.co/elasticsearch/elasticsearch:" + elasticsearchVersion;
        final ElasticsearchContainer elasticsearchContainer = new ElasticsearchContainer(dockerImage);
        if (elasticsearchVersion.startsWith("8")) {
            elasticsearchContainer.withEnv("xpack.security.enabled", "false");
        }
        return elasticsearchContainer;
    }

    private OpensearchContainer generateOpenSearchContainer() {
        final String dockerImage = "opensearchproject/opensearch:" + opensearchVersion;
        OpensearchContainer opensearchContainer = new OpensearchContainer(dockerImage);
        if (opensearchVersion.startsWith("2")) {
            // https://github.com/opensearch-project/OpenSearch/issues/15169
            opensearchContainer.withEnv("search.max_aggregation_rewrite_filters", "0");
        }
        return opensearchContainer;
    }
}

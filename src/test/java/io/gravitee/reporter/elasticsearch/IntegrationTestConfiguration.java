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
package io.gravitee.reporter.elasticsearch;

import io.gravitee.elasticsearch.config.Endpoint;
import io.gravitee.reporter.elasticsearch.config.ReporterConfiguration;
import java.util.Collections;
import org.springframework.beans.factory.annotation.Value;
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

    public static final String ELASTICSEARCH_DEFAULT_VERSION = "7.17.8";
    public static final String CLUSTER_NAME = "gravitee_test";

    @Value("${elasticsearch.version:" + ELASTICSEARCH_DEFAULT_VERSION + "}")
    private String elasticsearchVersion;

    @Bean
    public ReporterConfiguration configuration(ElasticsearchContainer elasticSearchContainer) {
        ReporterConfiguration elasticConfiguration = new ReporterConfiguration();
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
        elasticsearchContainer.start();
        return elasticsearchContainer;
    }
}

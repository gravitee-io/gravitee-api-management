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
package io.gravitee.reporter.elasticsearch.spring;

import io.gravitee.elasticsearch.config.Endpoint;
import io.gravitee.node.api.Node;
import io.gravitee.reporter.elasticsearch.config.ReporterConfiguration;
import io.gravitee.reporter.elasticsearch.embedded.ElasticsearchNode;
import io.gravitee.reporter.elasticsearch.node.DummyNode;
import io.vertx.core.Vertx;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.Collections;

/**
 * Spring configuration used for testing purpose.
 * 
 * @author Guillaume Waignier
 * @author Sebastien Devaux
 */
@Configuration
@Import(ElasticsearchReporterConfiguration.class)
public class ElasticsearchReporterConfigurationTest {

    @Bean
    public Vertx vertx() {
        return Vertx.vertx();
    }

    @Bean
    public ReporterConfiguration configuration() {
        ReporterConfiguration elasticConfiguration = new ReporterConfiguration();
        elasticConfiguration.setEndpoints(Collections.singletonList(new Endpoint("http://localhost:" + elasticsearchNode().getHttpPort())));
//        elasticConfiguration.setIngestPlugins(Arrays.asList("geoip"));
        return elasticConfiguration;
    }

    @Bean
    public Node node() {
        return new DummyNode();
    }

    @Bean
    public ElasticsearchNode elasticsearchNode() {
        return new ElasticsearchNode();
    }
}

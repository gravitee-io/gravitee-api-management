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
import io.gravitee.elasticsearch.index.IndexNameGenerator;
import io.gravitee.elasticsearch.index.MultiTypeIndexNameGenerator;
import io.gravitee.elasticsearch.index.PerTypeIndexNameGenerator;
import io.gravitee.elasticsearch.templating.freemarker.FreeMarkerComponent;
import io.gravitee.repository.elasticsearch.analytics.spring.AnalyticsConfiguration;
import io.gravitee.repository.elasticsearch.configuration.RepositoryConfiguration;
import io.gravitee.repository.elasticsearch.healthcheck.spring.HealthCheckConfiguration;
import io.gravitee.repository.elasticsearch.log.spring.LogConfiguration;
import io.gravitee.repository.elasticsearch.monitoring.spring.MonitoringConfiguration;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.reactivex.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.concurrent.TimeUnit;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Guillaume Waignier (zenika)
 * @author Sebastien Devaux (zenika)
 * @author GraviteeSource Team
 */
@Configuration
@Import({
        AnalyticsConfiguration.class,
        HealthCheckConfiguration.class,
        LogConfiguration.class,
        MonitoringConfiguration.class
})
public class ElasticsearchRepositoryConfiguration {

    private final Logger logger = LoggerFactory.getLogger(ElasticsearchRepositoryConfiguration.class);

    @Bean
    public Vertx vertxRx(io.vertx.core.Vertx vertx) {
        return Vertx.newInstance(vertx);
    }

    @Bean
    public RepositoryConfiguration repositoryConfiguration() {
        return new RepositoryConfiguration();
    }

    @Bean
    public FreeMarkerComponent freeMarckerComponent() {
        return new FreeMarkerComponent();
    }
    
    @Bean
    public Client client(RepositoryConfiguration repositoryConfiguration) {
        HttpClientConfiguration clientConfiguration = new HttpClientConfiguration();
        clientConfiguration.setEndpoints(repositoryConfiguration.getEndpoints());
        clientConfiguration.setUsername(repositoryConfiguration.getUsername());
        clientConfiguration.setPassword(repositoryConfiguration.getPassword());
        return new HttpClient(clientConfiguration);
    }
    
    @Bean
    public IndexNameGenerator indexNameGenerator(RepositoryConfiguration repositoryConfiguration, Client client) {
        // Wait for a connection to ES and retry each 5 seconds
        Single<Integer> singleVersion = client.getVersion()
                .retryWhen(error -> error.flatMap(
                        throwable -> Observable.just(new Object()).delay(5, TimeUnit.SECONDS).toFlowable(BackpressureStrategy.LATEST)));

        singleVersion.subscribe();

        Integer version = singleVersion.blockingGet();

        if (version == 6 || repositoryConfiguration.isPerTypeIndex()) {
            return new PerTypeIndexNameGenerator(repositoryConfiguration.getIndexName());
        } else {
            return new MultiTypeIndexNameGenerator(repositoryConfiguration.getIndexName());
        }
    }
}

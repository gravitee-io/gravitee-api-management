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

import static java.lang.String.format;

import io.gravitee.elasticsearch.client.Client;
import io.gravitee.elasticsearch.exception.ElasticsearchException;
import io.gravitee.elasticsearch.index.ILMIndexNameGenerator;
import io.gravitee.elasticsearch.index.IndexNameGenerator;
import io.gravitee.elasticsearch.index.MultiTypeIndexNameGenerator;
import io.gravitee.elasticsearch.index.PerTypeIndexNameGenerator;
import io.gravitee.elasticsearch.templating.freemarker.FreeMarkerComponent;
import io.gravitee.elasticsearch.version.ElasticsearchInfo;
import io.gravitee.platform.repository.api.Scope;
import io.gravitee.repository.elasticsearch.analytics.spring.AnalyticsConfiguration;
import io.gravitee.repository.elasticsearch.client.ElasticsearchHttpClientFactory;
import io.gravitee.repository.elasticsearch.configuration.RepositoryConfiguration;
import io.gravitee.repository.elasticsearch.healthcheck.spring.HealthCheckConfiguration;
import io.gravitee.repository.elasticsearch.log.spring.LogConfiguration;
import io.gravitee.repository.elasticsearch.monitoring.spring.MonitoringConfiguration;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.vertx.rxjava3.core.Vertx;
import java.util.concurrent.TimeUnit;
import lombok.CustomLog;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Guillaume Waignier (zenika)
 * @author Sebastien Devaux (zenika)
 * @author GraviteeSource Team
 */
@Configuration
@Import(
    {
        AnalyticsConfiguration.class,
        HealthCheckConfiguration.class,
        LogConfiguration.class,
        io.gravitee.repository.elasticsearch.v4.log.spring.LogConfiguration.class,
        io.gravitee.repository.elasticsearch.v4.analytics.spring.AnalyticsConfiguration.class,
        io.gravitee.repository.elasticsearch.v4.healthcheck.spring.HealthCheckConfiguration.class,
        MonitoringConfiguration.class,
    }
)
@CustomLog
public class ElasticsearchRepositoryConfiguration {

    @Bean
    public Vertx vertxRx(io.vertx.core.Vertx vertx) {
        return Vertx.newInstance(vertx);
    }

    @Bean
    public RepositoryConfiguration repositoryConfiguration() {
        return new RepositoryConfiguration();
    }

    @Bean
    public FreeMarkerComponent freeMarkerComponent() {
        return new FreeMarkerComponent();
    }

    @Bean
    public Client client(Environment environment) {
        // RepositoryConfiguration still owns analytics' index-naming / ILM / retention concerns and is
        // injected into other @Beans below; the client wiring is now scope-prefixed (analytics.elasticsearch.*)
        // through the shared factory, mirroring how the OTEL_TRACES scope reads otel-traces.elasticsearch.*.
        return new ElasticsearchHttpClientFactory(environment, Scope.ANALYTICS.getName()).createHttpClient();
    }

    @Bean
    public IndexNameGenerator indexNameGenerator(RepositoryConfiguration repositoryConfiguration, ElasticsearchInfo info) {
        if (info.getVersion().canUseIlmManagedIndex() && repositoryConfiguration.isILMIndex()) {
            return new ILMIndexNameGenerator(repositoryConfiguration.getIndexName());
        } else if (!info.getVersion().canUseMultiTypeIndex() || repositoryConfiguration.isPerTypeIndex()) {
            return new PerTypeIndexNameGenerator(repositoryConfiguration.getIndexName());
        } else {
            return new MultiTypeIndexNameGenerator(repositoryConfiguration.getIndexName());
        }
    }

    @Bean
    public ElasticsearchInfo elasticsearchInfo(Client client) {
        // Wait for a connection to ES and retry each 5 seconds
        Single<ElasticsearchInfo> singleVersion = client
            .getInfo()
            .retryWhen(error ->
                error.flatMap(throwable -> Observable.just(new Object()).delay(5, TimeUnit.SECONDS).toFlowable(BackpressureStrategy.LATEST))
            );

        singleVersion.subscribe();

        final ElasticsearchInfo elasticsearchInfo = singleVersion.blockingGet();
        if (elasticsearchInfo.getStatus() != null && elasticsearchInfo.getStatus() != 200) {
            throw new ElasticsearchException(
                format("Status '%d', reason: %s", elasticsearchInfo.getStatus(), elasticsearchInfo.getError().getReason())
            );
        }

        return elasticsearchInfo;
    }
}

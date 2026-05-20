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
package io.gravitee.repository.elasticsearch.tracing.spring;

import io.gravitee.elasticsearch.client.Client;
import io.gravitee.platform.repository.api.Scope;
import io.gravitee.repository.elasticsearch.client.ElasticsearchHttpClientFactory;
import io.gravitee.repository.elasticsearch.spring.ElasticsearchRepositoryConfiguration;
import io.gravitee.repository.elasticsearch.tracing.ElasticsearchTracingRepository;
import io.gravitee.repository.tracing.api.TracingRepository;
import io.vertx.rxjava3.core.Vertx;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Spring configuration loaded for the {@code OTEL_TRACES} scope. Self-contained on purpose — the platform's
 * repository plugin handler creates one application context per scope and promotes its singletons into the parent
 * bean factory; if this class shared bean names with {@link ElasticsearchRepositoryConfiguration} (the
 * {@code ANALYTICS} config), starting MAPI with both scopes pointed at this plugin would fail with
 * "there is already object […] bound under bean name 'client'".
 * <p>
 * Connection settings (endpoints, credentials, SSL, proxy) and the index template live under the dedicated
 * {@code otel-traces.elasticsearch.*} property block — no fallback to {@code analytics.elasticsearch.*}, so
 * tracing can be enabled even when analytics targets a different repository (Mongo, JDBC, none). The actual
 * property reads happen inside {@link ElasticsearchHttpClientFactory}, scoped by
 * {@link Scope#OTEL_TRACES}.{@code getName()} — mirrors the per-scope factory pattern used by the Redis plugin
 * for its rate-limit and distributed-sync scopes.
 *
 * @author GraviteeSource Team
 */
@Configuration
public class TracingConfiguration {

    @Bean
    public Vertx tracingVertxRx(io.vertx.core.Vertx vertx) {
        return Vertx.newInstance(vertx);
    }

    @Bean
    public Client tracingClient(Environment environment) {
        return new ElasticsearchHttpClientFactory(environment, Scope.OTEL_TRACES.getName()).createHttpClient();
    }

    @Bean
    public TracingRepository tracingRepository(
        @Value("${otel-traces.elasticsearch.index:traces-apim.otel-{orgId}}") String indexTemplate,
        Client tracingClient
    ) {
        return new ElasticsearchTracingRepository(indexTemplate, tracingClient);
    }
}

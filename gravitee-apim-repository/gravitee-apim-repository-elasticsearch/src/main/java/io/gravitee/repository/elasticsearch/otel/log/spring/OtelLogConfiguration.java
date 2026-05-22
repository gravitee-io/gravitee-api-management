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
package io.gravitee.repository.elasticsearch.otel.log.spring;

import io.gravitee.elasticsearch.client.Client;
import io.gravitee.platform.repository.api.Scope;
import io.gravitee.repository.elasticsearch.client.ElasticsearchHttpClientFactory;
import io.gravitee.repository.elasticsearch.otel.log.ElasticsearchOtelLogRepository;
import io.gravitee.repository.elasticsearch.tracing.spring.TracingConfiguration;
import io.gravitee.repository.otel.log.api.OtelLogRepository;
import io.vertx.rxjava3.core.Vertx;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Spring configuration loaded for the {@code OTEL_LOGS} scope. Self-contained on purpose — same
 * cross-scope bean-isolation pattern as {@link TracingConfiguration}: the platform's repository plugin
 * handler creates one application context per scope and promotes its singletons into the parent bean
 * factory, so each scope must expose distinctly-named beans to avoid collisions when an operator wires
 * several scopes (analytics, otel-traces, otel-logs) through this plugin simultaneously.
 * <p>
 * Connection settings (endpoints, credentials, SSL, proxy) and the index template live under the
 * dedicated {@code otel-logs.elasticsearch.*} property block — no fallback to
 * {@code analytics.elasticsearch.*} or {@code otel-traces.elasticsearch.*}, so the OTel-log reader can
 * target a different ES cluster (or be enabled in deployments that don't use ES for analytics or
 * tracing). The actual property reads happen inside {@link ElasticsearchHttpClientFactory}, scoped by
 * {@link Scope#OTEL_LOGS}.{@code getName()} — mirrors the per-scope factory pattern used by the Redis
 * plugin for its rate-limit and distributed-sync scopes.
 *
 * @author GraviteeSource Team
 */
@Configuration
public class OtelLogConfiguration {

    @Bean
    public Vertx otelLogVertxRx(io.vertx.core.Vertx vertx) {
        return Vertx.newInstance(vertx);
    }

    @Bean
    public Client otelLogClient(Environment environment) {
        return new ElasticsearchHttpClientFactory(environment, Scope.OTEL_LOGS.getName()).createHttpClient();
    }

    @Bean
    public OtelLogRepository otelLogRepository(
        @Value("${otel-logs.elasticsearch.index:logs-apim.otel-{orgId}}") String indexTemplate,
        Client otelLogClient
    ) {
        return new ElasticsearchOtelLogRepository(indexTemplate, otelLogClient);
    }
}

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.gravitee.elasticsearch.client.Client;
import io.gravitee.repository.elasticsearch.otel.log.ElasticsearchOtelLogRepository;
import io.gravitee.repository.otel.log.api.OtelLogRepository;
import io.vertx.rxjava3.core.Vertx;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class OtelLogConfigurationTest {

    private final OtelLogConfiguration configuration = new OtelLogConfiguration();

    @Test
    void otelLogVertxRx_should_wrap_core_vertx_into_rxjava3_vertx() {
        io.vertx.core.Vertx coreVertx = io.vertx.core.Vertx.vertx();
        try {
            Vertx wrapped = configuration.otelLogVertxRx(coreVertx);

            assertThat(wrapped).isNotNull();
            assertThat(wrapped.getDelegate()).isSameAs(coreVertx);
        } finally {
            coreVertx.close();
        }
    }

    @Test
    void otelLogClient_should_build_es_client_from_environment_with_otel_logs_prefix() {
        // ElasticsearchHttpClientFactory reads <scope>.elasticsearch.* properties — with a bare Environment
        // it falls back to all defaults (localhost:9200, no auth, no SSL) and still returns a usable client.
        // We just verify wiring: the @Bean creates a non-null Client without any otel-logs.* property set.
        MockEnvironment environment = new MockEnvironment();

        Client client = configuration.otelLogClient(environment);

        assertThat(client).isNotNull();
    }

    @Test
    void otelLogRepository_should_construct_es_repository_with_index_template_and_client() {
        Client client = mock(Client.class);

        OtelLogRepository repository = configuration.otelLogRepository("logs-apim.otel-{orgId}", client);

        assertThat(repository).isInstanceOf(ElasticsearchOtelLogRepository.class);
    }
}

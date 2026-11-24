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
package io.gravitee.apim.reporter.elasticsearch.spring;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.reporter.elasticsearch.config.ReporterConfiguration;
import io.gravitee.elasticsearch.client.http.HttpClientConfiguration;
import io.gravitee.elasticsearch.client.http.HttpClientTrustStoreConfiguration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

class ElasticsearchReporterConfigurationTlsTest {

    @Test
    void should_verify_server_certificates_by_default() {
        final HttpClientConfiguration configuration = clientConfiguration(Map.of());

        assertThat(configuration.isTrustAll()).isFalse();
        assertThat(configuration.getTrustStoreConfig()).isNull();
    }

    @Test
    void should_trust_all_server_certificates_when_trust_all_is_enabled() {
        assertThat(clientConfiguration(Map.of("reporters.elasticsearch.ssl.trustAll", "true")).isTrustAll()).isTrue();
    }

    @Test
    void should_wire_the_truststore_when_its_type_is_set() {
        final HttpClientConfiguration configuration = clientConfiguration(
            Map.of(
                "reporters.elasticsearch.ssl.truststore.type",
                "pem",
                "reporters.elasticsearch.ssl.truststore.path",
                "/path/to/ca.pem",
                "reporters.elasticsearch.ssl.truststore.password",
                "unused"
            )
        );

        assertThat(configuration.getTrustStoreConfig())
            .extracting(
                HttpClientTrustStoreConfiguration::getType,
                HttpClientTrustStoreConfiguration::getPath,
                HttpClientTrustStoreConfiguration::getPassword
            )
            .containsExactly("pem", "/path/to/ca.pem", "unused");
    }

    private static HttpClientConfiguration clientConfiguration(Map<String, Object> properties) {
        try (var context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test", properties));
            context.registerBean(ReporterConfiguration.class);
            context.refresh();
            return ElasticsearchReporterConfiguration.clientConfiguration(context.getBean(ReporterConfiguration.class));
        }
    }
}

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
package io.gravitee.repository.elasticsearch.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.elasticsearch.config.Endpoint;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
public class RepositoryConfigurationTest {

    @Configuration
    @PropertySource(factory = YamlPropertySourceFactory.class, value = "classpath:configuration/gravitee.yml")
    static class ContextConfiguration {

        @Bean
        public RepositoryConfiguration repositoryConfiguration() {
            return new RepositoryConfiguration();
        }
    }

    @Autowired
    private RepositoryConfiguration configuration;

    @Test
    public void shouldHaveEndpoints() {
        assertThat(configuration.getEndpoints()).hasSize(1).extracting(Endpoint::getUrl).containsExactly("http://localhost:9200");
    }

    @Test
    public void shouldHaveCrossClusterMapping() {
        assertThat(configuration.hasCrossClusterMapping()).isTrue();
        assertThat(configuration.getCrossClusterMapping()).hasSize(2).containsEntry("tenant1", "cluster1");
    }

    @Test
    public void shouldHaveClientTimeout() {
        assertThat(configuration.getRequestTimeout()).isNotNull().isEqualTo(30000L);
    }

    @Test
    public void shouldHaveKeystoreCerts() {
        Assertions.assertThat(configuration.getSslPemCerts()).hasSize(2).containsExactly("cert1", "cert2");
    }

    @Test
    public void shouldHaveKeystoreKeys() {
        Assertions.assertThat(configuration.getSslPemKeys()).hasSize(1).containsExactly("unique-key");
    }

    @Test
    public void shouldHaveIndexSettings() {
        assertThat(configuration.getIndexName()).isEqualTo("gravitee");
        assertThat(configuration.isPerTypeIndex()).isFalse();
        assertThat(configuration.getIndexMode()).isEqualTo("daily");
        assertThat(configuration.isILMIndex()).isFalse();
    }

    @Test
    public void shouldHaveProxySettings() {
        assertThat(configuration.getProxyType()).isEqualTo("HTTP");

        assertThat(configuration.getProxyHttpHost()).isEqualTo("localhost");
        assertThat(configuration.getProxyHttpPort()).isEqualTo(3128);
        assertThat(configuration.getProxyHttpsHost()).isEqualTo("localhost");
        assertThat(configuration.getProxyHttpsPort()).isEqualTo(3128);

        assertThat(configuration.isProxyConfigured()).isFalse();
    }

    @Test
    public void shouldHaveSecuritySettings() {
        assertThat(configuration.getUsername()).isNull();
        assertThat(configuration.getPassword()).isNull();
    }

    @Test
    public void shouldHaveSslKeystoreSettings() {
        assertThat(configuration.getSslKeystoreType()).isNull();
        assertThat(configuration.getSslKeystore()).isNull();
        assertThat(configuration.getSslKeystorePassword()).isNull();
    }
}

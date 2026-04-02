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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

/**
 * Tests that the Elasticsearch RepositoryConfiguration correctly reads properties
 * from the new "repositories.analytics.elasticsearch.*" prefix when a
 * RepositoryAliasPropertySource is registered (simulating the gravitee-plugin behavior).
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
public class RepositoryConfigurationNewPrefixTest {

    @Configuration
    @PropertySource(factory = YamlPropertySourceFactory.class, value = "classpath:configuration/gravitee-repositories-prefix.yml")
    static class ContextConfiguration {

        @Autowired
        private Environment environment;

        @Bean
        public org.springframework.core.env.PropertySource<?> repositoryAliasPropertySource() {
            org.springframework.core.env.PropertySource<?> aliasSource = new org.springframework.core.env.PropertySource<Object>(
                "repositoryAlias"
            ) {
                @Override
                public Object getProperty(String name) {
                    if (name.startsWith("management.") || name.startsWith("analytics.") || name.startsWith("ratelimit.")) {
                        return environment.getProperty("repositories." + name);
                    }
                    return null;
                }
            };
            // Register with highest priority
            ((ConfigurableEnvironment) environment).getPropertySources().addFirst(aliasSource);
            return aliasSource;
        }

        @Bean
        public RepositoryConfiguration repositoryConfiguration() {
            return new RepositoryConfiguration();
        }
    }

    @Autowired
    private RepositoryConfiguration configuration;

    @Test
    public void should_read_endpoints_from_new_prefix() {
        assertThat(configuration.getEndpoints()).hasSize(1).extracting(Endpoint::getUrl).containsExactly("http://es-remote:9200");
    }

    @Test
    public void should_not_fallback_to_localhost_9200() {
        assertThat(configuration.getEndpoints()).extracting(Endpoint::getUrl).doesNotContain("http://localhost:9200");
    }

    @Test
    public void should_read_index_name_from_new_prefix() {
        assertThat(configuration.getIndexName()).isEqualTo("my-custom-index");
    }

    @Test
    public void should_read_index_mode_from_new_prefix() {
        assertThat(configuration.isILMIndex()).isTrue();
    }

    @Test
    public void should_read_security_credentials_from_new_prefix() {
        assertThat(configuration.getUsername()).isEqualTo("es_user");
        assertThat(configuration.getPassword()).isEqualTo("es_pass");
    }

    @Test
    public void should_read_http_timeout_from_new_prefix() {
        assertThat(configuration.getRequestTimeout()).isEqualTo(15000L);
    }

    @Test
    public void should_read_cross_cluster_mapping_from_new_prefix() {
        assertThat(configuration.hasCrossClusterMapping()).isTrue();
        assertThat(configuration.getCrossClusterMapping()).containsEntry("tenantA", "clusterA");
    }

    @Test
    public void should_read_ssl_pem_certs_from_new_prefix() {
        assertThat(configuration.getSslPemCerts()).hasSize(1).containsExactly("new-cert1");
    }

    @Test
    public void should_read_ssl_pem_keys_from_new_prefix() {
        assertThat(configuration.getSslPemKeys()).hasSize(1).containsExactly("new-key1");
    }

    @Test
    public void should_detect_proxy_configured_from_new_prefix() {
        assertThat(configuration.isProxyConfigured()).isTrue();
    }
}

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
package io.gravitee.repository.elasticsearch.client;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.elasticsearch.client.http.HttpClientConfiguration;
import io.gravitee.elasticsearch.client.http.HttpClientJksSslConfiguration;
import io.gravitee.elasticsearch.client.http.HttpClientPemSslConfiguration;
import io.gravitee.elasticsearch.client.http.HttpClientPfxSslConfiguration;
import io.gravitee.elasticsearch.config.Endpoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ElasticsearchHttpClientFactoryTest {

    private static final String SCOPE_NAME = "otel-traces";
    private static final String PROPERTY_PREFIX = SCOPE_NAME + ".elasticsearch.";

    private MockEnvironment environment;
    private ElasticsearchHttpClientFactory factory;

    @BeforeEach
    void setUp() {
        environment = new MockEnvironment();
        factory = new ElasticsearchHttpClientFactory(environment, SCOPE_NAME);
    }

    @Test
    void should_default_to_localhost_endpoint_when_none_configured() {
        // Local dev / unit tests shouldn't need a property file just to spin up a client — the factory
        // falls back to the canonical localhost endpoint when no indexed endpoints[] block was set.
        HttpClientConfiguration cfg = new TestElasticsearchHttpClientFactory(environment, SCOPE_NAME).buildHttpClientConfiguration();

        assertThat(cfg.getEndpoints()).extracting(Endpoint::getUrl).containsExactly("http://localhost:9200");
    }

    @Test
    void should_read_endpoint_credentials_and_timeout_from_scoped_property_prefix() {
        // The full property prefix is "<scopeName>.elasticsearch." — pin every connection setting against
        // it so a future rename of the suffix (e.g. "endpoints" → "hosts") fails this test loudly instead
        // of silently dropping credentials in production.
        environment.setProperty(PROPERTY_PREFIX + "endpoints[0]", "http://es-a:9200");
        environment.setProperty(PROPERTY_PREFIX + "endpoints[1]", "http://es-b:9200");
        environment.setProperty(PROPERTY_PREFIX + "security.username", "user");
        environment.setProperty(PROPERTY_PREFIX + "security.password", "pass");
        environment.setProperty(PROPERTY_PREFIX + "http.timeout", "7500");

        HttpClientConfiguration cfg = new TestElasticsearchHttpClientFactory(environment, SCOPE_NAME).buildHttpClientConfiguration();

        assertThat(cfg.getEndpoints()).extracting(Endpoint::getUrl).containsExactly("http://es-a:9200", "http://es-b:9200");
        assertThat(cfg.getUsername()).isEqualTo("user");
        assertThat(cfg.getPassword()).isEqualTo("pass");
        assertThat(cfg.getRequestTimeout()).isEqualTo(7500L);
    }

    @Test
    void should_wire_jks_ssl_when_keystore_type_is_jks() {
        // JKS branch — the factory must build the matching SslConfiguration subclass so the underlying
        // transport actually negotiates with the right keystore type.
        environment.setProperty(PROPERTY_PREFIX + "ssl.keystore.type", "JKS");
        environment.setProperty(PROPERTY_PREFIX + "ssl.keystore.path", "/path/to/keystore.jks");
        environment.setProperty(PROPERTY_PREFIX + "ssl.keystore.password", "jksPass");

        HttpClientConfiguration cfg = new TestElasticsearchHttpClientFactory(environment, SCOPE_NAME).buildHttpClientConfiguration();

        assertThat(cfg.getSslConfig()).isInstanceOf(HttpClientJksSslConfiguration.class);
    }

    @Test
    void should_wire_pfx_ssl_when_keystore_type_is_pfx() {
        environment.setProperty(PROPERTY_PREFIX + "ssl.keystore.type", "PFX");
        environment.setProperty(PROPERTY_PREFIX + "ssl.keystore.path", "/path/to/keystore.pfx");
        environment.setProperty(PROPERTY_PREFIX + "ssl.keystore.password", "pfxPass");

        HttpClientConfiguration cfg = new TestElasticsearchHttpClientFactory(environment, SCOPE_NAME).buildHttpClientConfiguration();

        assertThat(cfg.getSslConfig()).isInstanceOf(HttpClientPfxSslConfiguration.class);
    }

    @Test
    void should_wire_pem_ssl_with_indexed_cert_and_key_paths_when_keystore_type_is_pem() {
        // PEM keystores carry multiple cert / key paths via indexed property lists. Make sure both
        // entries flow through so a multi-cert deployment isn't silently truncated.
        environment.setProperty(PROPERTY_PREFIX + "ssl.keystore.type", "PEM");
        environment.setProperty(PROPERTY_PREFIX + "ssl.keystore.certs[0]", "/cert-a");
        environment.setProperty(PROPERTY_PREFIX + "ssl.keystore.certs[1]", "/cert-b");
        environment.setProperty(PROPERTY_PREFIX + "ssl.keystore.keys[0]", "/key-a");

        HttpClientConfiguration cfg = new TestElasticsearchHttpClientFactory(environment, SCOPE_NAME).buildHttpClientConfiguration();

        assertThat(cfg.getSslConfig()).isInstanceOf(HttpClientPemSslConfiguration.class);
    }

    @Test
    void should_leave_ssl_unset_when_keystore_type_is_absent() {
        // No keystore configured = plaintext HTTP. The factory must NOT default to a JKS/PEM placeholder,
        // otherwise transport negotiation fails before any request even leaves the gateway.
        HttpClientConfiguration cfg = new TestElasticsearchHttpClientFactory(environment, SCOPE_NAME).buildHttpClientConfiguration();

        assertThat(cfg.getSslConfig()).isNull();
    }

    @Test
    void should_leave_ssl_unset_when_keystore_type_is_unknown() {
        // Defensive: an unrecognised keystore type (config typo, future SSL type) falls through silently
        // rather than crashing — operator can fix the property without bouncing the gateway.
        environment.setProperty(PROPERTY_PREFIX + "ssl.keystore.type", "UNKNOWN");

        HttpClientConfiguration cfg = new TestElasticsearchHttpClientFactory(environment, SCOPE_NAME).buildHttpClientConfiguration();

        assertThat(cfg.getSslConfig()).isNull();
    }

    @Test
    void should_mark_proxy_configured_when_any_http_proxy_property_is_present() {
        // isProxyConfigured drives whether the proxy block is applied at all — if the operator declared
        // ANY proxy property under http.proxy.*, the client should honour it.
        environment.setProperty(PROPERTY_PREFIX + "http.proxy.http.host", "proxy.test");

        HttpClientConfiguration cfg = new TestElasticsearchHttpClientFactory(environment, SCOPE_NAME).buildHttpClientConfiguration();

        assertThat(cfg.isProxyConfigured()).isTrue();
        assertThat(cfg.getProxyHttpHost()).isEqualTo("proxy.test");
    }

    @Test
    void should_mark_proxy_unconfigured_when_no_proxy_property_was_set() {
        // No proxy properties → factory must not assume one. Otherwise traffic would route through the
        // default localhost:3128 even in operators' production deployments that don't proxy.
        HttpClientConfiguration cfg = new TestElasticsearchHttpClientFactory(environment, SCOPE_NAME).buildHttpClientConfiguration();

        assertThat(cfg.isProxyConfigured()).isFalse();
    }

    @Test
    void should_scope_property_prefix_per_factory_instance() {
        // Two factories with different scopes don't see each other's properties — the prefix isolation
        // is what lets analytics and OTEL_TRACES live side-by-side in the same Spring application context.
        environment.setProperty("analytics.elasticsearch.security.username", "analytics-user");
        environment.setProperty(PROPERTY_PREFIX + "security.username", "tracing-user");

        HttpClientConfiguration analyticsCfg = new TestElasticsearchHttpClientFactory(
            environment,
            "analytics"
        ).buildHttpClientConfiguration();
        HttpClientConfiguration tracingCfg = new TestElasticsearchHttpClientFactory(environment, SCOPE_NAME).buildHttpClientConfiguration();

        assertThat(analyticsCfg.getUsername()).isEqualTo("analytics-user");
        assertThat(tracingCfg.getUsername()).isEqualTo("tracing-user");
    }

    /**
     * Test-side subclass exposing the {@code protected buildHttpClientConfiguration()} so we can assert on
     * the configuration without instantiating a real {@link io.gravitee.elasticsearch.client.http.HttpClient}
     * (which would try to open Vert.x resources we'd then have to clean up).
     */
    private static class TestElasticsearchHttpClientFactory extends ElasticsearchHttpClientFactory {

        TestElasticsearchHttpClientFactory(org.springframework.core.env.Environment environment, String scopeName) {
            super(environment, scopeName);
        }

        @Override
        public HttpClientConfiguration buildHttpClientConfiguration() {
            return super.buildHttpClientConfiguration();
        }
    }
}

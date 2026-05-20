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

import io.gravitee.elasticsearch.client.http.ClientSslConfiguration;
import io.gravitee.elasticsearch.client.http.HttpClient;
import io.gravitee.elasticsearch.client.http.HttpClientConfiguration;
import io.gravitee.elasticsearch.client.http.HttpClientJksSslConfiguration;
import io.gravitee.elasticsearch.client.http.HttpClientPemSslConfiguration;
import io.gravitee.elasticsearch.client.http.HttpClientPfxSslConfiguration;
import io.gravitee.elasticsearch.config.Endpoint;
import java.util.ArrayList;
import java.util.List;
import lombok.CustomLog;
import org.springframework.core.env.Environment;

/**
 * Per-scope Elasticsearch {@link HttpClient} factory — mirrors the {@code RedisConnectionFactory} pattern:
 * instantiated once per Spring scope ({@code ANALYTICS}, {@code OTEL_TRACES}, …) with the scope name as the
 * property-prefix root, then reads every connection setting from {@link Environment} on demand. Keeps
 * scope wiring straightforward (a single 2-arg constructor instead of a 19-arg record + adapter), and lets
 * each scope's {@code @Configuration} declare exactly one ES-specific concern — the scope it lives in —
 * by passing {@link io.gravitee.platform.repository.api.Scope#getName()} as the prefix.
 * <p>
 * The full property prefix is {@code <scopeName>.elasticsearch.} — so {@code Scope.ANALYTICS.getName()}
 * yields {@code analytics.elasticsearch.*}, {@code Scope.OTEL_TRACES.getName()} yields
 * {@code otel-traces.elasticsearch.*}, and so on. Each scope's property block is independent — no
 * fallback across scopes, so an operator can wire one of them and leave the others on a different
 * backend (Mongo, JDBC, none).
 *
 * @author GraviteeSource Team
 */
@CustomLog
public class ElasticsearchHttpClientFactory {

    private static final String DEFAULT_ELASTICSEARCH_ENDPOINT = "http://localhost:9200";

    private final Environment environment;
    private final String propertyPrefix;

    public ElasticsearchHttpClientFactory(Environment environment, String scopeName) {
        this.environment = environment;
        this.propertyPrefix = scopeName + ".elasticsearch.";
    }

    public HttpClient createHttpClient() {
        return new HttpClient(buildHttpClientConfiguration());
    }

    /**
     * Exposed as {@code protected} so unit tests can assert on the intermediate {@link HttpClientConfiguration}
     * (endpoints, credentials, SSL config, proxy) without reflecting into the resulting client. Mirrors
     * {@code RedisConnectionFactory.buildRedisClientOptions()}.
     */
    protected HttpClientConfiguration buildHttpClientConfiguration() {
        HttpClientConfiguration cfg = new HttpClientConfiguration();
        cfg.setEndpoints(readEndpoints());
        cfg.setUsername(readProperty("security.username", String.class, null));
        cfg.setPassword(readProperty("security.password", String.class, null));
        cfg.setRequestTimeout(readProperty("http.timeout", Long.class, 10_000L));

        cfg.setProxyType(readProperty("http.proxy.type", String.class, "HTTP"));
        cfg.setProxyHttpHost(readProperty("http.proxy.http.host", String.class, System.getProperty("http.proxyHost", "localhost")));
        cfg.setProxyHttpPort(
            readProperty("http.proxy.http.port", int.class, Integer.parseInt(System.getProperty("http.proxyPort", "3128")))
        );
        cfg.setProxyHttpUsername(readProperty("http.proxy.http.username", String.class, null));
        cfg.setProxyHttpPassword(readProperty("http.proxy.http.password", String.class, null));
        cfg.setProxyHttpsHost(readProperty("http.proxy.https.host", String.class, System.getProperty("https.proxyHost", "localhost")));
        cfg.setProxyHttpsPort(
            readProperty("http.proxy.https.port", int.class, Integer.parseInt(System.getProperty("https.proxyPort", "3128")))
        );
        cfg.setProxyHttpsUsername(readProperty("http.proxy.https.username", String.class, null));
        cfg.setProxyHttpsPassword(readProperty("http.proxy.https.password", String.class, null));
        cfg.setProxyConfigured(isProxyConfigured());

        configureSsl(cfg);
        return cfg;
    }

    private void configureSsl(HttpClientConfiguration cfg) {
        String keystoreType = readProperty("ssl.keystore.type", String.class, null);
        if (keystoreType == null) {
            return;
        }
        if (keystoreType.equalsIgnoreCase(ClientSslConfiguration.JKS_KEYSTORE_TYPE)) {
            cfg.setSslConfig(
                new HttpClientJksSslConfiguration(
                    readProperty("ssl.keystore.path", String.class, null),
                    readProperty("ssl.keystore.password", String.class, null)
                )
            );
        } else if (keystoreType.equalsIgnoreCase(ClientSslConfiguration.PFX_KEYSTORE_TYPE)) {
            cfg.setSslConfig(
                new HttpClientPfxSslConfiguration(
                    readProperty("ssl.keystore.path", String.class, null),
                    readProperty("ssl.keystore.password", String.class, null)
                )
            );
        } else if (keystoreType.equalsIgnoreCase(ClientSslConfiguration.PEM_KEYSTORE_TYPE)) {
            cfg.setSslConfig(
                new HttpClientPemSslConfiguration(
                    readIndexedListProperty("ssl.keystore.certs"),
                    readIndexedListProperty("ssl.keystore.keys")
                )
            );
        }
    }

    /**
     * Resolves the ES endpoints from the {@code endpoints[N]} indexed property block, falling back to a
     * single default ({@value DEFAULT_ELASTICSEARCH_ENDPOINT}) when the operator didn't configure any —
     * matches the analytics convention so local dev / unit tests don't need a property file.
     */
    private List<Endpoint> readEndpoints() {
        List<Endpoint> endpoints = new ArrayList<>();
        for (int idx = 0; environment.containsProperty(propertyPrefix + "endpoints[" + idx + "]"); idx++) {
            endpoints.add(new Endpoint(readProperty("endpoints[" + idx + "]", String.class, null)));
        }
        if (endpoints.isEmpty()) {
            endpoints.add(new Endpoint(DEFAULT_ELASTICSEARCH_ENDPOINT));
        }
        return endpoints;
    }

    /**
     * Indexed list properties ({@code prop[0]=a, prop[1]=b, …}) with a fallback to the single-value form
     * for back-compat with the analytics convention.
     */
    private List<String> readIndexedListProperty(String suffix) {
        List<String> values = new ArrayList<>();
        for (int idx = 0; environment.containsProperty(propertyPrefix + suffix + "[" + idx + "]"); idx++) {
            values.add(readProperty(suffix + "[" + idx + "]", String.class, null));
        }
        if (values.isEmpty() && environment.containsProperty(propertyPrefix + suffix)) {
            values.add(readProperty(suffix, String.class, null));
        }
        return values;
    }

    /**
     * "Proxy is configured" iff any of the proxy properties under {@code http.proxy.*} was explicitly set
     * in the operator's config. Avoids the Spring-environment-walking that the analytics-only
     * {@code RepositoryConfiguration.isProxyConfigured()} did — same behaviour, but per-scope.
     */
    private boolean isProxyConfigured() {
        return (
            environment.containsProperty(propertyPrefix + "http.proxy.type") ||
            environment.containsProperty(propertyPrefix + "http.proxy.http.host") ||
            environment.containsProperty(propertyPrefix + "http.proxy.https.host") ||
            environment.containsProperty(propertyPrefix + "http.proxy.http.port") ||
            environment.containsProperty(propertyPrefix + "http.proxy.https.port")
        );
    }

    private <T> T readProperty(String name, Class<T> type, T defaultValue) {
        return environment.getProperty(propertyPrefix + name, type, defaultValue);
    }
}

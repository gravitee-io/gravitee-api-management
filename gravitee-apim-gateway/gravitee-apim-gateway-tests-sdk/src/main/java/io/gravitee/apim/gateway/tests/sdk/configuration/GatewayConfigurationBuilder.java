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
package io.gravitee.apim.gateway.tests.sdk.configuration;

import java.util.Objects;
import java.util.Properties;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GatewayConfigurationBuilder {

    private final Properties systemProperties;
    private final Properties yamlProperties;

    public record GatewayConfiguration(Properties systemProperties, Properties yamlProperties) {}

    public GatewayConfigurationBuilder(Properties systemProperties, Properties yamlProperties) {
        this.systemProperties = Objects.requireNonNull(systemProperties);
        this.yamlProperties = Objects.requireNonNull(yamlProperties);
    }

    public GatewayConfigurationBuilder(Properties systemProperties) {
        this(systemProperties, new Properties());
    }

    public static GatewayConfigurationBuilder emptyConfiguration() {
        return new GatewayConfigurationBuilder(new Properties());
    }

    public GatewayConfigurationBuilder setSystemProperty(String key, Object value) {
        this.systemProperties.put(key, value);
        return this;
    }

    public GatewayConfigurationBuilder setYamlProperty(String key, Object value) {
        this.yamlProperties.put(key, value);
        return this;
    }

    /**
     * @see #setSystemProperty(String, Object)
     */
    public GatewayConfigurationBuilder set(String key, Object value) {
        return this.setSystemProperty(key, value);
    }

    public GatewayConfiguration build() {
        return new GatewayConfiguration((Properties) this.systemProperties.clone(), (Properties) yamlProperties.clone());
    }

    public GatewayConfigurationBuilder v2EmulateV4Engine(String mode) {
        this.set("api.v2.emulateV4Engine.default", mode);
        return this;
    }

    public GatewayConfigurationBuilder httpSecured(boolean enabled) {
        this.set("http.secured", enabled);
        return this;
    }

    public GatewayConfigurationBuilder httpAlpn(boolean enabled) {
        this.set("http.alpn", enabled);
        return this;
    }

    public GatewayConfigurationBuilder httpSslKeystoreType(String type) {
        this.set("http.ssl.keystore.type", type);
        return this;
    }

    public GatewayConfigurationBuilder httpSslSecret(String secret) {
        return this.set("http.ssl.keystore.secret", secret);
    }

    public GatewayConfigurationBuilder configureTcpGateway(int port) {
        return this.set("tcp.enabled", true)
            .set("tcp.secured", true)
            .set("tcp.port", port)
            .set("tcp.instances", 1)
            .set("tcp.ssl.sni", true);
    }

    public GatewayConfigurationBuilder configureKafkaGateway(int port) {
        return this.set("kafka.enabled", true)
            .set("kafka.secured", true)
            .set("kafka.port", port)
            .set("kafka.instances", 1)
            .set("kafka.ssl.sni", true);
    }
}

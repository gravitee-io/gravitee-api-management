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

import java.util.Properties;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GatewayConfigurationBuilder {

    private final Properties properties;

    public GatewayConfigurationBuilder(Properties properties) {
        this.properties = properties;
    }

    public static GatewayConfigurationBuilder emptyConfiguration() {
        return new GatewayConfigurationBuilder(new Properties());
    }

    public GatewayConfigurationBuilder set(String key, Object value) {
        this.properties.put(key, value);
        return this;
    }

    public Properties build() {
        return properties;
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
}

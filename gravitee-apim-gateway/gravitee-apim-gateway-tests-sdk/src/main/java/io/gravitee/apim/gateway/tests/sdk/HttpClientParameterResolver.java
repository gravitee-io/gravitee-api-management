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
package io.gravitee.apim.gateway.tests.sdk;

import static io.gravitee.apim.gateway.tests.sdk.GatewayTestingExtension.GATEWAY_DYNAMIC_CONFIG_KEY;

import io.gravitee.apim.gateway.tests.sdk.parameters.GatewayDynamicConfig;
import io.gravitee.apim.gateway.tests.sdk.parameters.GatewayTestParameterResolver;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.junit5.ScopedObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.http.HttpClient;
import java.util.Objects;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HttpClientParameterResolver implements GatewayTestParameterResolver {

    @Override
    public boolean supports(ParameterContext parameterContext) {
        return parameterContext.getParameter().getType() == HttpClient.class;
    }

    @Override
    public Object resolve(ExtensionContext extensionContext, ParameterContext parameterContext, AbstractGatewayTest gatewayTest) {
        Vertx vertx = getStoredVertx(extensionContext);
        var gatewayConfig = getStoredGatewayConfig(extensionContext);

        final HttpClientOptions httpClientOptions = new HttpClientOptions().setDefaultHost("localhost");
        // configure a default port only if the port is obvious
        if (gatewayConfig.httpPorts().size() == 1) {
            httpClientOptions.setDefaultPort(gatewayConfig.httpPort());
        }
        gatewayTest.configureHttpClient(httpClientOptions, gatewayConfig, parameterContext);
        HttpClient httpClient = vertx.createHttpClient(httpClientOptions);
        gatewayTest.registerATearDownHandler("HTTP client", () -> httpClient.close().onErrorComplete().subscribe());
        return httpClient;
    }

    private Vertx getStoredVertx(ExtensionContext extensionContext) {
        ExtensionContext.Store store = extensionContext.getStore(ExtensionContext.Namespace.GLOBAL);
        ScopedObject scopedObject = store.get(VertxExtension.VERTX_INSTANCE_KEY, ScopedObject.class);
        Objects.requireNonNull(scopedObject, "A Vertx instance must exist, try adding the Vertx parameter as the first method argument");
        return (Vertx) scopedObject.get();
    }

    private GatewayDynamicConfig.GatewayDynamicConfigImpl getStoredGatewayConfig(ExtensionContext extensionContext) {
        if (
            extensionContext
                .getStore(ExtensionContext.Namespace.GLOBAL)
                .get(GATEWAY_DYNAMIC_CONFIG_KEY) instanceof GatewayDynamicConfig.GatewayDynamicConfigImpl config
        ) {
            return config;
        }
        throw new IllegalArgumentException("Gateway seems not deployed");
    }
}

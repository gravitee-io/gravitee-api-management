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
package io.gravitee.apim.plugin.apiservice.dynamicproperties.http.http;

import io.gravitee.apim.plugin.apiservice.dynamicproperties.http.HttpDynamicPropertiesServiceConfiguration;
import io.gravitee.apim.rest.api.common.apiservices.ManagementDeploymentContext;
import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.node.vertx.client.http.VertxHttpClientFactory;
import io.gravitee.node.vertx.client.http.VertxHttpClientOptions;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.http.HttpClient;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class HttpClientFactory {

    // Dummy {@link URLStreamHandler} implementation to avoid unknown protocol issue with default implementation (which knows how to handle only http and https protocol).
    public static final URLStreamHandler URL_HANDLER = new URLStreamHandler() {
        @Override
        protected URLConnection openConnection(URL u) {
            return null;
        }
    };
    private static final String HTTPS_SCHEME = "https";
    public static final int UNSECURE_PORT = 80;
    public static final int SECURE_PORT = 443;

    public static HttpClient createClient(
        ManagementDeploymentContext deploymentContext,
        HttpDynamicPropertiesServiceConfiguration configuration
    ) {
        return VertxHttpClientFactory
            .builder()
            .vertx(deploymentContext.getComponent(Vertx.class))
            .nodeConfiguration(deploymentContext.getComponent(Configuration.class))
            .defaultTarget(configuration.getUrl())
            .httpOptions(VertxHttpClientOptions.builder().maxConcurrentConnections(1).build())
            .build()
            .createHttpClient();
    }

    public static URL buildUrl(String uri) {
        try {
            return new URL(null, uri, URL_HANDLER);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Target [" + uri + "] is not valid");
        }
    }

    public static boolean isSecureProtocol(String protocol) {
        return HTTPS_SCHEME.equalsIgnoreCase(protocol);
    }

    public static int getPort(URL target, boolean isSecured) {
        final int defaultPort = isSecured ? SECURE_PORT : UNSECURE_PORT;
        return target.getPort() != -1 ? target.getPort() : defaultPort;
    }
}

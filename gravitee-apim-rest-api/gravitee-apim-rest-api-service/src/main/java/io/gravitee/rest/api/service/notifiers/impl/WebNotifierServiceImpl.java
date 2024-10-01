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
package io.gravitee.rest.api.service.notifiers.impl;

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.http.MediaType;
import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.notifiers.WebNotifierService;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.*;
import io.vertx.core.net.ProxyOptions;
import io.vertx.core.net.ProxyType;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class WebNotifierServiceImpl implements WebNotifierService {

    private final Logger LOGGER = LoggerFactory.getLogger(WebNotifierServiceImpl.class);

    private static final String HTTPS_SCHEME = "https";

    private final Configuration configuration;

    public WebNotifierServiceImpl(Configuration configuration) {
        this.configuration = configuration;
    }

    @Autowired
    private Vertx vertx;

    public void request(HttpMethod method, final String uri, final Map<String, String> headers, String body, boolean useSystemProxy) {
        if (uri == null || uri.isEmpty()) {
            LOGGER.error("Webhook Notifier configuration is empty");
            return;
        }

        CompletableFuture<Buffer> future = new CompletableFuture<>();
        URI requestUri = URI.create(uri);
        boolean ssl = HTTPS_SCHEME.equalsIgnoreCase(requestUri.getScheme());

        final HttpClientOptions clientOptions = new HttpClientOptions()
            .setSsl(ssl)
            .setTrustAll(true)
            .setMaxPoolSize(1)
            .setKeepAlive(false)
            .setTcpKeepAlive(false)
            .setConnectTimeout(httpClientTimeout());

        if (useSystemProxy) {
            ProxyOptions proxyOptions = new ProxyOptions();
            proxyOptions.setType(ProxyType.valueOf(httpClientProxyType()));
            if (HTTPS_SCHEME.equals(requestUri.getScheme())) {
                proxyOptions.setHost(httpClientProxyHttpsHost());
                proxyOptions.setPort(httpClientProxyHttpsPort());
                proxyOptions.setUsername(httpClientProxyHttpsUsername());
                proxyOptions.setPassword(httpClientProxyHttpsPassword());
            } else {
                proxyOptions.setHost(httpClientProxyHttpHost());
                proxyOptions.setPort(httpClientProxyHttpPort());
                proxyOptions.setUsername(httpClientProxyHttpUsername());
                proxyOptions.setPassword(httpClientProxyHttpPassword());
            }
            clientOptions.setProxyOptions(proxyOptions);
        }

        final HttpClient httpClient = vertx.createHttpClient(clientOptions);

        final int port = requestUri.getPort() != -1 ? requestUri.getPort() : (HTTPS_SCHEME.equals(requestUri.getScheme()) ? 443 : 80);

        RequestOptions options = new RequestOptions()
            .setMethod(io.vertx.core.http.HttpMethod.valueOf(method.name()))
            .setHost(requestUri.getHost())
            .setPort(port)
            .setURI(requestUri.toString())
            .setTimeout(httpClientTimeout());

        //headers
        options.putHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        options.putHeader(HttpHeaders.CONTENT_LENGTH, Integer.toString(body.getBytes(StandardCharsets.UTF_8).length));
        headers.forEach(options::putHeader);
        options.putHeader("X-Gravitee-Request-Id", UuidString.generateRandom());

        Future<HttpClientRequest> requestFuture = httpClient.request(options);

        requestFuture
            .onFailure(throwable -> {
                future.completeExceptionally(throwable);

                // Close client
                httpClient.close();
            })
            .onSuccess(request -> {
                request
                    .response(asyncResponse -> {
                        if (asyncResponse.failed()) {
                            future.completeExceptionally(asyncResponse.cause());

                            // Close client
                            httpClient.close();
                        } else {
                            HttpClientResponse response = asyncResponse.result();
                            LOGGER.debug("Web response status code : {}", response.statusCode());

                            if (isStatus2xx(response)) {
                                response.bodyHandler(buffer -> {
                                    future.complete(buffer);

                                    // Close client
                                    httpClient.close();
                                });
                            } else {
                                future.completeExceptionally(
                                    new TechnicalManagementException(
                                        " Error on url '" +
                                        uri +
                                        "'. Status code: " +
                                        response.statusCode() +
                                        ". Message: " +
                                        response.statusMessage(),
                                        null
                                    )
                                );
                            }
                        }
                    })
                    .exceptionHandler(throwable -> {
                        future.completeExceptionally(throwable);

                        // Close client
                        httpClient.close();
                    });

                request.end(body);
            });

        try {
            future.get();
        } catch (ExecutionException e) {
            LOGGER.error(e.getMessage(), e);
            throw new TechnicalManagementException(e.getMessage(), e);
        } catch (InterruptedException e) {
            LOGGER.error(e.getMessage(), e);
            Thread.currentThread().interrupt();
            throw new TechnicalManagementException(e.getMessage(), e);
        }
    }

    private static boolean isStatus2xx(HttpClientResponse httpResponse) {
        return httpResponse.statusCode() / 100 == 2;
    }

    private Integer httpClientTimeout() {
        return configuration.getProperty("httpClient.timeout", Integer.class, 10000);
    }

    private String httpClientProxyType() {
        return configuration.getProperty("httpClient.proxy.type", "HTTP");
    }

    private String httpClientProxyHttpHost() {
        return configuration.getProperty("httpClient.proxy.http.host", System.getProperty("http.proxyHost", "localhost"));
    }

    private Integer httpClientProxyHttpPort() {
        return configuration.getProperty(
            "httpClient.proxy.http.port",
            Integer.class,
            Integer.valueOf(System.getProperty("http.proxyPort", "3128"))
        );
    }

    private String httpClientProxyHttpUsername() {
        return configuration.getProperty("httpClient.proxy.http.username");
    }

    private String httpClientProxyHttpPassword() {
        return configuration.getProperty("httpClient.proxy.http.password");
    }

    private String httpClientProxyHttpsHost() {
        return configuration.getProperty("httpClient.proxy.https.host", System.getProperty("https.proxyHost", "localhost"));
    }

    private Integer httpClientProxyHttpsPort() {
        return configuration.getProperty(
            "httpClient.proxy.https.port",
            Integer.class,
            Integer.valueOf(System.getProperty("https.proxyPort", "3128"))
        );
    }

    private String httpClientProxyHttpsUsername() {
        return configuration.getProperty("httpClient.proxy.https.username");
    }

    private String httpClientProxyHttpsPassword() {
        return configuration.getProperty("httpClient.proxy.https.password");
    }
}

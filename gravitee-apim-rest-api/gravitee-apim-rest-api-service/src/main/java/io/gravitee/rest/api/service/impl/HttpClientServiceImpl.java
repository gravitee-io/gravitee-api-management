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
package io.gravitee.rest.api.service.impl;

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.common.http.MediaType;
import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.rest.api.service.HttpClientService;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.*;
import io.vertx.core.net.ProxyOptions;
import io.vertx.core.net.ProxyType;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class HttpClientServiceImpl extends AbstractService implements HttpClientService {

    private final Logger LOGGER = LoggerFactory.getLogger(HttpClientServiceImpl.class);

    private static final String HTTPS_SCHEME = "https";

    private final Configuration configuration;
    private final Vertx vertx;

    public HttpClientServiceImpl(Configuration configuration, Vertx vertx) {
        this.configuration = configuration;
        this.vertx = vertx;
    }

    @Override
    public HttpClient createHttpClient(String uriScheme, Boolean useSystemProxy) {
        boolean ssl = HTTPS_SCHEME.equalsIgnoreCase(uriScheme);

        final HttpClientOptions options = new HttpClientOptions()
            .setSsl(ssl)
            .setTrustAll(true)
            .setVerifyHost(false)
            .setMaxPoolSize(1)
            .setKeepAlive(false)
            .setTcpKeepAlive(false)
            .setConnectTimeout(httpClientTimeout());

        if ((useSystemProxy == Boolean.TRUE) || (useSystemProxy == null && this.isProxyConfigured())) {
            ProxyOptions proxyOptions = new ProxyOptions();
            proxyOptions.setType(ProxyType.valueOf(httpClientProxyType()));
            if (HTTPS_SCHEME.equals(uriScheme)) {
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
            options.setProxyOptions(proxyOptions);
        }

        return vertx.createHttpClient(options);
    }

    @Override
    public Buffer request(HttpMethod method, String uri, Map<String, String> headers, String body, Boolean useSystemProxy) {
        if (uri == null || uri.isEmpty()) {
            LOGGER.error("HttpClient configuration is empty");
            return null;
        }

        Promise<Buffer> promise = Promise.promise();

        URI requestUri = URI.create(uri);

        final HttpClient httpClient = this.createHttpClient(requestUri.getScheme(), useSystemProxy);

        final int port = requestUri.getPort() != -1 ? requestUri.getPort() : (HTTPS_SCHEME.equals(requestUri.getScheme()) ? 443 : 80);

        RequestOptions options = new RequestOptions()
            .setMethod(io.vertx.core.http.HttpMethod.valueOf(method.name()))
            .setHost(requestUri.getHost())
            .setPort(port)
            .setURI(requestUri.getPath())
            .setTimeout(httpClientTimeout());

        //headers
        if (headers != null) {
            headers.forEach(options::putHeader);
        }

        options.putHeader("X-Gravitee-Request-Id", UuidString.generateRandom().trim());

        if (body != null) {
            if (!options.getHeaders().contains(HttpHeaders.CONTENT_TYPE)) {
                options.putHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
            }
            options.putHeader(HttpHeaders.CONTENT_LENGTH, Integer.toString(body.getBytes().length));
        }

        Future<HttpClientRequest> requestFuture = httpClient.request(options);

        requestFuture
            .onFailure(
                new Handler<Throwable>() {
                    @Override
                    public void handle(Throwable throwable) {
                        promise.fail(throwable);

                        // Close client
                        httpClient.close();
                    }
                }
            )
            .onSuccess(
                new Handler<HttpClientRequest>() {
                    @Override
                    public void handle(HttpClientRequest request) {
                        request
                            .response(
                                new Handler<AsyncResult<HttpClientResponse>>() {
                                    @Override
                                    public void handle(AsyncResult<HttpClientResponse> asyncResponse) {
                                        if (asyncResponse.failed()) {
                                            promise.fail(asyncResponse.cause());

                                            // Close client
                                            httpClient.close();
                                        } else {
                                            HttpClientResponse response = asyncResponse.result();
                                            LOGGER.debug("Web response status code : {}", response.statusCode());

                                            if (response.statusCode() >= HttpStatusCode.OK_200 && response.statusCode() <= 299) {
                                                response.bodyHandler(buffer -> {
                                                    promise.complete(buffer);

                                                    // Close client
                                                    httpClient.close();
                                                });
                                            } else {
                                                response.bodyHandler(buffer ->
                                                    promise.fail(
                                                        new TechnicalManagementException(
                                                            " Error on url '" +
                                                                uri +
                                                                "'. Status code: " +
                                                                response.statusCode() +
                                                                ". Message: " +
                                                                buffer.toString(),
                                                            null
                                                        )
                                                    )
                                                );
                                            }
                                        }
                                    }
                                }
                            )
                            .exceptionHandler(
                                new Handler<Throwable>() {
                                    @Override
                                    public void handle(Throwable throwable) {
                                        promise.fail(throwable);

                                        // Close client
                                        httpClient.close();
                                    }
                                }
                            );

                        if (body != null) {
                            request.end(body);
                        } else {
                            request.end();
                        }
                    }
                }
            );

        try {
            return promise.future().toCompletionStage().toCompletableFuture().get();
        } catch (ExecutionException e) {
            throw new TechnicalManagementException(e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TechnicalManagementException(e.getMessage(), e);
        }
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

    private Boolean isProxyConfigured() {
        return Boolean.valueOf(System.getProperty("httpClient.proxy", "false"));
    }
}

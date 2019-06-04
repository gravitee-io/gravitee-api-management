/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
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
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.common.http.MediaType;
import io.gravitee.common.utils.UUID;
import io.gravitee.fetcher.api.FetcherException;
import io.gravitee.repository.management.model.GenericNotificationConfig;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.notification.Hook;
import io.gravitee.rest.api.service.notifiers.WebNotifierService;
import io.gravitee.rest.api.service.notifiers.WebhookNotifierService;
import io.gravitee.rest.api.service.vertx.VertxCompletableFuture;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.ProxyOptions;
import io.vertx.core.net.ProxyType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static io.gravitee.rest.api.service.notification.NotificationParamsBuilder.*;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com) 
 * @author GraviteeSource Team
 */
@Component
public class WebNotifierServiceImpl implements WebNotifierService {

    private final Logger LOGGER = LoggerFactory.getLogger(WebNotifierServiceImpl.class);

    private static final String HTTPS_SCHEME = "https";

    @Value("${httpClient.timeout:10000}")
    private int httpClientTimeout;
    @Value("${httpClient.proxy.type:HTTP}")
    private String httpClientProxyType;

    @Value("${httpClient.proxy.http.host:#{systemProperties['http.proxyHost'] ?: 'localhost'}}")
    private String httpClientProxyHttpHost;
    @Value("${httpClient.proxy.http.port:#{systemProperties['http.proxyPort'] ?: 3128}}")
    private int httpClientProxyHttpPort;
    @Value("${httpClient.proxy.http.username:#{null}}")
    private String httpClientProxyHttpUsername;
    @Value("${httpClient.proxy.http.password:#{null}}")
    private String httpClientProxyHttpPassword;

    @Value("${httpClient.proxy.https.host:#{systemProperties['https.proxyHost'] ?: 'localhost'}}")
    private String httpClientProxyHttpsHost;
    @Value("${httpClient.proxy.https.port:#{systemProperties['https.proxyPort'] ?: 3128}}")
    private int httpClientProxyHttpsPort;
    @Value("${httpClient.proxy.https.username:#{null}}")
    private String httpClientProxyHttpsUsername;
    @Value("${httpClient.proxy.https.password:#{null}}")
    private String httpClientProxyHttpsPassword;

    @Autowired
    private Vertx vertx;

    public void request(HttpMethod method, final String uri, final Map<String, String> headers, String body, boolean useSystemProxy) {
        if (uri == null || uri.isEmpty()) {
            LOGGER.error("Webhook Notifier configuration is empty");
            return;
        }

        CompletableFuture<Buffer> future = new VertxCompletableFuture<>(vertx);
        URI requestUri = URI.create(uri);
        boolean ssl = HTTPS_SCHEME.equalsIgnoreCase(requestUri.getScheme());

        final HttpClientOptions options = new HttpClientOptions()
                .setSsl(ssl)
                .setTrustAll(true)
                .setMaxPoolSize(1)
                .setKeepAlive(false)
                .setTcpKeepAlive(false)
                .setConnectTimeout(httpClientTimeout);

        if (useSystemProxy) {
            ProxyOptions proxyOptions = new ProxyOptions();
            proxyOptions.setType(ProxyType.valueOf(httpClientProxyType));
            if (HTTPS_SCHEME.equals(requestUri.getScheme())) {
                proxyOptions.setHost(httpClientProxyHttpsHost);
                proxyOptions.setPort(httpClientProxyHttpsPort);
                proxyOptions.setUsername(httpClientProxyHttpsUsername);
                proxyOptions.setPassword(httpClientProxyHttpsPassword);
            } else {
                proxyOptions.setHost(httpClientProxyHttpHost);
                proxyOptions.setPort(httpClientProxyHttpPort);
                proxyOptions.setUsername(httpClientProxyHttpUsername);
                proxyOptions.setPassword(httpClientProxyHttpPassword);
            }
            options.setProxyOptions(proxyOptions);
        }

        final HttpClient httpClient = vertx.createHttpClient(options);

        final int port = requestUri.getPort() != -1 ? requestUri.getPort() :
                (HTTPS_SCHEME.equals(requestUri.getScheme()) ? 443 : 80);

        HttpClientRequest request = httpClient.request(
                io.vertx.core.http.HttpMethod.valueOf(method.name()),
                port,
                requestUri.getHost(),
                requestUri.toString(),
                response -> LOGGER.debug("Web response status code : {}", response.statusCode())
        );
        request.setTimeout(httpClientTimeout);

        //headers
        request.putHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        request.putHeader(HttpHeaders.CONTENT_LENGTH, Integer.toString(body.length()));
        headers.forEach(request::putHeader);
        request.putHeader("X-Gravitee-Request-Id", UUID.toString(UUID.random()));
        request.write(body);

        request.handler(response -> {
            if (response.statusCode() == HttpStatusCode.OK_200) {
                response.bodyHandler(buffer -> {
                    future.complete(buffer);

                    // Close client
                    httpClient.close();
                });
            } else {
                future.completeExceptionally(new TechnicalManagementException(" Error on url '" + uri + "'. Status code: " + response.statusCode() + ". Message: " + response.statusMessage(), null));
            }
        });
        request.exceptionHandler(event -> {
            try {
                future.completeExceptionally(event);

                // Close client
                httpClient.close();
            } catch (IllegalStateException ise) {
                // Do not take care about exception when closing client
            }
        });

        request.end();

        try {
            future.get();
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error(e.getMessage(), e);
            throw new TechnicalManagementException(e.getMessage(), e);
        }
    }
}

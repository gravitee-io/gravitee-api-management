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
package io.gravitee.management.service.notifiers.impl;

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.common.http.MediaType;
import io.gravitee.common.utils.UUID;
import io.gravitee.fetcher.api.FetcherException;
import io.gravitee.management.model.*;
import io.gravitee.management.model.api.ApiEntity;
import io.gravitee.management.service.exceptions.TechnicalManagementException;
import io.gravitee.management.service.notification.Hook;
import io.gravitee.management.service.notifiers.WebNotifierService;
import io.gravitee.management.service.notifiers.WebhookNotifierService;
import io.gravitee.management.service.vertx.VertxCompletableFuture;
import io.gravitee.repository.management.model.GenericNotificationConfig;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static io.gravitee.management.service.notification.NotificationParamsBuilder.*;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com) 
 * @author GraviteeSource Team
 */
@Component
public class WebNotifierServiceImpl implements WebNotifierService {

    private final Logger LOGGER = LoggerFactory.getLogger(WebNotifierServiceImpl.class);

    private static final String HTTPS_SCHEME = "https";
    private static final int GLOBAL_TIMEOUT = 10_000;

    @Autowired
    private Vertx vertx;

    public void request(HttpMethod method, final String uri, final Map<String, String> headers, String body) {
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
                .setConnectTimeout(GLOBAL_TIMEOUT);

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
        request.setTimeout(GLOBAL_TIMEOUT);

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

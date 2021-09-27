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
package io.gravitee.rest.api.services.dynamicproperties.provider.http;

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.definition.model.services.dynamicproperty.DynamicPropertyService;
import io.gravitee.definition.model.services.dynamicproperty.http.HttpDynamicPropertyProviderConfiguration;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.utils.NodeUtils;
import io.gravitee.rest.api.service.HttpClientService;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.services.dynamicproperties.model.DynamicProperty;
import io.gravitee.rest.api.services.dynamicproperties.provider.Provider;
import io.gravitee.rest.api.services.dynamicproperties.provider.http.mapper.JoltMapper;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.*;
import java.net.URL;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HttpProvider implements Provider {

    private final Logger logger = LoggerFactory.getLogger(HttpProvider.class);

    private static final String HTTPS_SCHEME = "https";

    private final HttpDynamicPropertyProviderConfiguration dpConfiguration;

    private JoltMapper mapper;

    private HttpClientService httpClientService;

    private Node node;

    public HttpProvider(final DynamicPropertyService dpService) {
        Objects.requireNonNull(dpService, "Service must not be null");

        this.dpConfiguration = (HttpDynamicPropertyProviderConfiguration) dpService.getConfiguration();
        this.mapper = new JoltMapper(dpConfiguration.getSpecification());
    }

    @Override
    public CompletableFuture<Collection<DynamicProperty>> get() {
        Promise<Buffer> promise = Promise.promise();

        try {
            URL requestUrl = new URL(dpConfiguration.getUrl());

            final HttpClient httpClient = httpClientService.createHttpClient(requestUrl.getProtocol(), dpConfiguration.isUseSystemProxy());

            final int port = requestUrl.getPort() != -1 ? requestUrl.getPort() : (HTTPS_SCHEME.equals(requestUrl.getProtocol()) ? 443 : 80);

            String relativeUri = (requestUrl.getQuery() == null)
                ? requestUrl.getPath()
                : requestUrl.getPath() + '?' + requestUrl.getQuery();

            RequestOptions options = new RequestOptions()
                .setMethod(HttpMethod.valueOf(dpConfiguration.getMethod().name()))
                .setHost(requestUrl.getHost())
                .setPort(port)
                .setURI(relativeUri);

            //headers
            options.putHeader(HttpHeaders.USER_AGENT, NodeUtils.userAgent(node));
            options.putHeader("X-Gravitee-Request-Id", UuidString.generateRandom());

            if (dpConfiguration.getHeaders() != null) {
                dpConfiguration.getHeaders().forEach(httpHeader -> options.putHeader(httpHeader.getName(), httpHeader.getValue()));
            }

            httpClient
                .request(options)
                .onFailure(
                    new Handler<Throwable>() {
                        @Override
                        public void handle(Throwable event) {
                            promise.fail(event);

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
                                                final HttpClientResponse response = asyncResponse.result();

                                                if (response.statusCode() == HttpStatusCode.OK_200) {
                                                    response.bodyHandler(
                                                        buffer -> {
                                                            promise.complete(buffer);

                                                            // Close client
                                                            httpClient.close();
                                                        }
                                                    );
                                                } else {
                                                    promise.complete(null);

                                                    // Close client
                                                    httpClient.close();
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

                            if (!StringUtils.isEmpty(dpConfiguration.getBody())) {
                                request.end(dpConfiguration.getBody());
                            } else {
                                request.end();
                            }
                        }
                    }
                );
        } catch (Exception e) {
            promise.fail(e);
        }

        return promise
            .future()
            .map(
                new Function<Buffer, Collection<DynamicProperty>>() {
                    @Override
                    public Collection<DynamicProperty> apply(Buffer buffer) {
                        if (buffer == null) {
                            return null;
                        }
                        return mapper.map(buffer.toString());
                    }
                }
            )
            .toCompletionStage()
            .toCompletableFuture();
    }

    @Override
    public String name() {
        return "custom";
    }

    public void setMapper(JoltMapper mapper) {
        this.mapper = mapper;
    }

    public void setHttpClientService(HttpClientService httpClientService) {
        this.httpClientService = httpClientService;
    }

    public void setNode(Node node) {
        this.node = node;
    }
}

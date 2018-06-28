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
package io.gravitee.repository.bridge.client.http;

import io.gravitee.repository.bridge.client.utils.VertxCompletableFuture;
import io.gravitee.repository.exceptions.TechnicalException;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.CaseInsensitiveHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HttpRequest<T> {

    private final Logger logger = LoggerFactory.getLogger(HttpRequest.class);

    private final WebClient client;

    private final HttpMethod method;

    private final String url;

    private BodyCodec<T> codec;

    private MultiMap parameters = new CaseInsensitiveHeaders();

    private Vertx vertx;

    private HttpRequest(WebClient client, HttpMethod method, String url) {
        this.client = client;
        this.method = method;
        this.url = url;
    }

    public static <T> HttpRequest<T> post(WebClient client, String url) {
        return new HttpRequest<>(client, HttpMethod.POST, url);
    }

    public static <T> HttpRequest<T> get(WebClient client, String url) {
        return new HttpRequest<>(client, HttpMethod.GET, url);
    }

    public static <T> HttpRequest<T> delete(WebClient client, String url) {
        return new HttpRequest<>(client, HttpMethod.DELETE, url);
    }

    public static <T> HttpRequest<T> put(WebClient client, String url) {
        return new HttpRequest<>(client, HttpMethod.PUT, url);
    }

    public HttpRequest<T> addQueryParam(String key, String value) {
        this.parameters.add(key, value);

        return this;
    }

    public HttpRequest<T> bodyCodec(BodyCodec<T> codec) {
        this.codec = codec;

        return this;
    }

    public HttpRequest<T> vertx(Vertx vertx) {
        this.vertx = vertx;

        return this;
    }

    public HttpMethod method() {
        return method;
    }

    public String url() {
        return url;
    }

    public BodyCodec<T> codec() {
        return codec;
    }

    public MultiMap parameters() {
        return parameters;
    }

    public T send() {
        Future<T> future = Future.future();

        io.vertx.ext.web.client.HttpRequest<T> request = client
                .request(method, "/_bridge" + url)
                .as(codec);

        this.parameters.forEach(paramEntry -> request.addQueryParam(paramEntry.getKey(), paramEntry.getValue()));

        request
                .send(ar -> {
                    if (ar.succeeded()) {
                        HttpResponse<T> response = ar.result();
                        future.complete(response.body());
                    } else {
                        if (ar.cause() != null) {
                            logger.error("An error occurs while invoking the bridge server", ar.cause());
                        }
                        future.fail(new TechnicalException("An error occurs while invoking the bridge server", ar.cause()));
                    }
                });

        VertxCompletableFuture<T> completable = VertxCompletableFuture.from(vertx, future);
        try {
            return completable.get();
        } catch (Exception ex) {
            logger.error("Unexpected error", ex);
            return null;
        }
    }

    public T send(Object payload) {
        Future<T> future = Future.future();

        io.vertx.ext.web.client.HttpRequest<T> request = client
                .request(method, "/_bridge" + url)
                .as(codec);

        this.parameters.forEach(paramEntry -> request.addQueryParam(paramEntry.getKey(), paramEntry.getValue()));

        request
                .sendJson(payload, ar -> {
                    if (ar.succeeded()) {
                        HttpResponse<T> response = ar.result();
                        future.complete(response.body());
                    } else {
                        if (ar.cause() != null) {
                            logger.error("An error occurs while invoking the bridge server", ar.cause());
                        }
                        future.fail(new TechnicalException("An error occurs while invoking the bridge server", ar.cause()));
                    }
                });

        VertxCompletableFuture<T> completable = VertxCompletableFuture.from(vertx, future);
        try {
            return completable.get();
        } catch (Exception ex) {
            logger.error("Unexpected error", ex);
            return null;
        }
    }
}

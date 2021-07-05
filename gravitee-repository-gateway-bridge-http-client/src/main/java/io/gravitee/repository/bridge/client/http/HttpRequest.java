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

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.repository.exceptions.TechnicalException;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
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

    public Future<T> send() {
        return send(null);
    }

    public Future<T> send(Object payload) {
        Future<T> future = Future.future();

        io.vertx.ext.web.client.HttpRequest<T> request = client
                .request(method, url)
                .as(codec);

        this.parameters.forEach(paramEntry -> request.addQueryParam(paramEntry.getKey(), paramEntry.getValue()));

        logger.debug("Calling bridge server: method[{}] url[{}]", method, url);

        Handler<AsyncResult<HttpResponse<T>>> handler = event -> {
            if (event.succeeded()) {
                HttpResponse<T> response = event.result();
                // A 200 status response is always expected from the remote bridge. If it's not the case, we have to
                // consider that something wrong occurs.
                if (response.statusCode() == HttpStatusCode.OK_200) {
                    future.complete(response.body());
                } else {
                    future.fail(new TechnicalException("Unexpected response from the bridge server while calling " +
                            "url[" +  url + "] status [" + response.statusCode()+ "]"));
                }
            } else {
                future.fail(new TechnicalException("An error occurs while invoking the bridge server", event.cause()));
            }
        };

        if (payload != null) {
            request.sendJson(payload, handler);
        } else {
            request.send(handler);
        }

        return future;
    }
}

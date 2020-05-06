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
package io.gravitee.repository.bridge.client.management;

import io.gravitee.repository.bridge.client.http.HttpRequest;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractRepository {

    @Autowired
    protected WebClient client;

    @Autowired
    protected Vertx vertx;

    @Autowired
    private Environment environment;

    protected <T> HttpRequest<T> post(String url, BodyCodec<T> codec) {
        return HttpRequest.<T>post(client, url).bodyCodec(codec).vertx(vertx).env(environment);
    }

    protected <T> HttpRequest<T> get(String url, BodyCodec<T> codec) {
        return HttpRequest.<T>get(client, url).bodyCodec(codec).vertx(vertx).env(environment);
    }

    protected <T> HttpRequest<T> put(String url, BodyCodec<T> codec) {
        return HttpRequest.<T>put(client, url).bodyCodec(codec).vertx(vertx).env(environment);
    }

    protected <T> HttpRequest<T> delete(String url, BodyCodec<T> codec) {
        return HttpRequest.<T>delete(client, url).bodyCodec(codec).vertx(vertx).env(environment);
    }
}

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
import io.gravitee.repository.bridge.client.utils.BridgePath;
import io.gravitee.repository.exceptions.TechnicalException;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractRepository implements InitializingBean {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private WebClient client;

    @Autowired
    protected Vertx vertx;

    @Autowired
    private Environment environment;

    private String prefixPath;

    protected <T> HttpRequest<T> post(String url, BodyCodec<T> codec) {
        return HttpRequest.<T>post(client, prefixPath + url).bodyCodec(codec);
    }

    protected <T> HttpRequest<T> get(String url, BodyCodec<T> codec) {
        return HttpRequest.<T>get(client, prefixPath + url).bodyCodec(codec);
    }

    protected <T> HttpRequest<T> put(String url, BodyCodec<T> codec) {
        return HttpRequest.<T>put(client, prefixPath + url).bodyCodec(codec);
    }

    protected <T> HttpRequest<T> delete(String url, BodyCodec<T> codec) {
        return HttpRequest.<T>delete(client, prefixPath + url).bodyCodec(codec);
    }

    <T> T blockingGet(Promise<T> promise) throws TechnicalException {
        final CompletableFuture<T> future = promise.future().toCompletionStage().toCompletableFuture();

        try {
            return future.get(10, TimeUnit.SECONDS);
        } catch (Exception ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            logger.error("Unexpected error while invoking bridge: {}", ex.getMessage());
            throw new TechnicalException(ex);
        }
    }

    @Override
    public void afterPropertiesSet() {
        prefixPath = BridgePath.get(environment);
    }
}

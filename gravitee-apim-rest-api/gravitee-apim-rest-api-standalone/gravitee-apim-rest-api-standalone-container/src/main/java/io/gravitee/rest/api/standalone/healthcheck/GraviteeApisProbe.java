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
package io.gravitee.rest.api.standalone.healthcheck;

import io.gravitee.node.api.healthcheck.Probe;
import io.gravitee.node.api.healthcheck.Result;
import io.gravitee.rest.api.standalone.vertx.VertxCompletableFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import java.util.concurrent.CompletableFuture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 * HTTP Probe used to check the Management API itself.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GraviteeApisProbe implements Probe {

    @Value("${jetty.port:8083}")
    private int port;

    @Value("${jetty.host:localhost}")
    private String host;

    @Autowired
    private Vertx vertx;

    @Override
    public String id() {
        return "gravitee-apis";
    }

    @Override
    public CompletableFuture<Result> check() {
        Future<Result> future = Future.future();

        NetClientOptions options = new NetClientOptions().setConnectTimeout(500);
        NetClient client = vertx.createNetClient(options);

        client.connect(
            port,
            host,
            res -> {
                if (res.succeeded()) {
                    future.complete(Result.healthy());
                } else {
                    future.complete(Result.unhealthy(res.cause()));
                }

                client.close();
            }
        );

        return VertxCompletableFuture.from(vertx, future);
    }
}

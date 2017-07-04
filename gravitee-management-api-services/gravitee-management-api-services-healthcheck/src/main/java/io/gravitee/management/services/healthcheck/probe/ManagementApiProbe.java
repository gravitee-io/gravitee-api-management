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
package io.gravitee.management.services.healthcheck.probe;

import io.gravitee.management.services.healthcheck.Probe;
import io.gravitee.management.services.healthcheck.Result;
import io.gravitee.management.services.healthcheck.vertx.VertxCompletableFuture;
import io.vertx.core.Vertx;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.concurrent.CompletableFuture;

/**
 * HTTP Probe used to check the Management API itself.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ManagementApiProbe implements Probe {

    @Value("${http.port:8083}")
    private int port;

    @Autowired
    private Vertx vertx;

    @Override
    public String id() {
        return "management-api";
    }

    @Override
    public CompletableFuture<Result> check() {
        VertxCompletableFuture<Result> result = new VertxCompletableFuture<>(vertx);

        NetClientOptions options = new NetClientOptions().setConnectTimeout(500);
        NetClient client = vertx.createNetClient(options);

        client.connect(port, "localhost", res -> {
            if (res.succeeded()) {
                result.complete(Result.healthy());
            } else {
                result.complete(Result.unhealthy(res.cause()));
            }

            client.close();
        });

        return result;
    }
}

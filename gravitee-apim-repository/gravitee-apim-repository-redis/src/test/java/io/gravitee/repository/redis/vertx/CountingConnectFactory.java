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
package io.gravitee.repository.redis.vertx;

import io.gravitee.node.vertx.client.redis.VertxRedisClientFactory;
import io.gravitee.plugin.configurations.redis.RedisClientOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.RedisConnection;
import io.vertx.redis.client.Request;
import io.vertx.redis.client.Response;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Counts the clients created and closed and the connect attempts made, whether the client is created once or per attempt.
 */
final class CountingConnectFactory extends VertxRedisClientFactory {

    final AtomicInteger connects = new AtomicInteger();
    final AtomicInteger creates = new AtomicInteger();
    final AtomicInteger closes = new AtomicInteger();

    CountingConnectFactory(Vertx vertx) {
        super(vertx);
    }

    @Override
    public Redis createClient(RedisClientOptions options) {
        Redis delegate = super.createClient(options);
        creates.incrementAndGet();
        return new Redis() {
            @Override
            public Future<RedisConnection> connect() {
                connects.incrementAndGet();
                return delegate.connect();
            }

            @Override
            public Future<Void> close() {
                closes.incrementAndGet();
                return delegate.close();
            }

            @Override
            public Future<Response> send(Request command) {
                return delegate.send(command);
            }

            @Override
            public Future<List<Response>> batch(List<Request> commands) {
                return delegate.batch(commands);
            }
        };
    }
}

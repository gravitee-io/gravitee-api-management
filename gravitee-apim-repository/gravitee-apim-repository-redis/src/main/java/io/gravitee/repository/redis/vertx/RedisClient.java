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
package io.gravitee.repository.redis.vertx;

import io.gravitee.repository.redis.ratelimit.RedisRateLimitRepository;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.RedisAPI;
import io.vertx.redis.client.RedisConnection;
import io.vertx.redis.client.RedisOptions;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class RedisClient {

    private final Logger logger = LoggerFactory.getLogger(RedisClient.class);

    private static final String SCRIPT_LOAD_COMMAND = "LOAD";
    private static final String SCRIPT = "scripts/ratelimit.lua";

    private String scriptSha1;
    private final Vertx vertx;
    private final RedisOptions options;
    private RedisAPI redisAPI;

    public RedisClient(Vertx vertx, RedisOptions options) {
        this.vertx = vertx;
        this.options = options;

        createRedisClient().onFailure(t -> attemptReconnect(0));
    }

    public RedisAPI getRedisApi() {
        return redisAPI;
    }

    public String getScriptSha1() {
        return scriptSha1;
    }

    private Future<RedisConnection> createRedisClient() {
        Promise<RedisConnection> promise = Promise.promise();

        Redis
            .createClient(vertx, options)
            .connect()
            .onSuccess(
                conn -> {
                    // make sure to invalidate old connection if present
                    if (redisAPI != null) {
                        redisAPI.close();
                    }

                    this.redisAPI = RedisAPI.api(conn);

                    loadScript();

                    // make sure the client is reconnected on error
                    conn.exceptionHandler(
                        e ->
                            // attempt to reconnect,
                            // if there is an unrecoverable error
                            attemptReconnect(0)
                    );
                    // make sure the client is reconnected on connection close
                    conn.endHandler(endEvent -> attemptReconnect(0));

                    // allow further processing
                    promise.complete(conn);
                }
            )
            .onFailure(promise::fail);

        return promise.future();
    }

    private void loadScript() {
        try (InputStream stream = RedisRateLimitRepository.class.getClassLoader().getResourceAsStream(SCRIPT)) {
            this.redisAPI.script(
                    Arrays.asList(SCRIPT_LOAD_COMMAND, new String(stream.readAllBytes(), StandardCharsets.UTF_8)),
                    event -> {
                        if (event.succeeded()) {
                            scriptSha1 = event.result().toString();
                        }
                    }
                );
        } catch (Exception ex) {
            logger.error("Unexpected error while loading lua script to Redis", ex);
        }
    }

    /**
     * Attempt to reconnect
     */
    private void attemptReconnect(int retry) {
        // retry with backoff up to 10240 ms
        long backoff = (long) (Math.pow(2, Math.min(retry, 10)) * 10);
        vertx.setTimer(backoff, timer -> createRedisClient().onFailure(t -> attemptReconnect(retry + 1)));
    }
}

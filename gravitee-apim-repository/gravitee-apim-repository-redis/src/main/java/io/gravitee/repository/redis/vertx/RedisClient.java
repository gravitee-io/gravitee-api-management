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

import io.gravitee.repository.redis.ratelimit.RedisRateLimitRepository;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.RedisAPI;
import io.vertx.redis.client.RedisOptions;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class RedisClient {

    private static final String SCRIPT_LOAD_COMMAND = "LOAD";

    private final Vertx vertx;
    private final RedisOptions options;
    private final Map<String, String> scripts;
    private final Map<String, String> scriptsSha = new HashMap<>();
    private Future<RedisAPI> redisAPIFuture;

    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicBoolean connecting = new AtomicBoolean(false);
    private Redis redis;

    public RedisClient(final Vertx vertx, final RedisOptions options, final Map<String, String> scripts) {
        this.vertx = vertx;
        this.options = options;
        this.scripts = scripts;
        this.connect(0);
    }

    public boolean isConnected() {
        return connected.get();
    }

    private void connect(final int retry) {
        // Cleanup any already existing connection.
        if (redis != null) {
            redis.close();
            redis = null;
            connected.set(false);
        }

        if (connecting.compareAndSet(false, true)) {
            redis = Redis.createClient(vertx, options);
            this.redisAPIFuture =
                redis
                    .connect()
                    .onSuccess(conn -> {
                        log.debug("Connected to Redis");
                        // make sure the client is reconnected on error
                        conn.exceptionHandler(e ->
                            // attempt to reconnect,
                            // if there is an unrecoverable error
                            attemptReconnect(0)
                        );
                        // make sure the client is reconnected on connection close
                        conn.endHandler(v -> attemptReconnect(0));
                    })
                    .flatMap(redisConnection -> {
                        RedisAPI redisAPI = RedisAPI.api(redisConnection);
                        return loadScripts(redisAPI);
                    })
                    .onSuccess(redisAPI -> {
                        log.info("Redis is now ready to be used.");
                        connecting.set(false);
                        connected.set(true);
                    })
                    .timeout(options.getNetClientOptions().getConnectTimeout(), TimeUnit.MILLISECONDS)
                    .onFailure(t -> {
                        log.error("Unable to connect to Redis", t);
                        connected.set(false);
                        attemptReconnect(retry);
                    });
        }
    }

    /**
     * Attempt to reconnect
     */
    private void attemptReconnect(int retry) {
        connecting.set(false);

        // retry with backoff up to 10240 ms
        long backoff = (long) (Math.pow(2, Math.min(retry, 10)) * 10);
        vertx.setTimer(backoff, timer -> connect(retry + 1));
    }

    private Future<RedisAPI> loadScripts(final RedisAPI redisAPI) {
        if (scripts != null) {
            return Future
                .all(
                    scripts
                        .entrySet()
                        .stream()
                        .map(entry -> {
                            String key = entry.getKey();
                            String script = entry.getValue();
                            try (InputStream stream = RedisRateLimitRepository.class.getClassLoader().getResourceAsStream(script)) {
                                return redisAPI
                                    .script(Arrays.asList(SCRIPT_LOAD_COMMAND, new String(stream.readAllBytes(), StandardCharsets.UTF_8)))
                                    .onSuccess(response -> {
                                        log.debug("Lua script '{}' registered to Redis", script);
                                        scriptsSha.put(key, response.toString());
                                    })
                                    .mapEmpty();
                            } catch (Exception ex) {
                                return Future.failedFuture("Unexpected error while loading lua script");
                            }
                        })
                        .collect(Collectors.toList())
                )
                .map(v -> redisAPI);
        }
        return Future.succeededFuture(redisAPI);
    }

    public Future<RedisAPI> redisApi() {
        return redisAPIFuture;
    }

    public String scriptSha1(final String key) {
        return scriptsSha.get(key);
    }
}

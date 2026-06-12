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
import io.gravitee.repository.redis.ratelimit.RedisRateLimitRepository;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.RedisAPI;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import lombok.CustomLog;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
public class RedisClient {

    private static final String SCRIPT_LOAD_COMMAND = "LOAD";

    /**
     * Shared by callers without a Vert.x context (health checks, tests, blocking threads).
     */
    private static final int FALLBACK_LOOP_KEY = 0;

    private final Vertx vertx;
    private final VertxRedisClientFactory factory;
    private final RedisClientOptions clientOptions;
    private final Map<String, String> scripts;
    private final Map<String, String> scriptsSource = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, LoopRedis> loops = new ConcurrentHashMap<>();

    public RedisClient(
        final Vertx vertx,
        final VertxRedisClientFactory factory,
        final RedisClientOptions clientOptions,
        final Map<String, String> scripts
    ) {
        this.vertx = vertx;
        this.factory = factory;
        this.clientOptions = clientOptions;
        this.scripts = scripts;
        preloadScriptSources();
        vertx.runOnContext(v -> redisApi());
    }

    public boolean isConnected() {
        LoopRedis loop = loops.get(currentLoopKey());
        if (loop != null && loop.connected.get()) {
            return true;
        }
        return loops
            .values()
            .stream()
            .anyMatch(l -> l.connected.get());
    }

    public Future<RedisAPI> redisApi() {
        LoopRedis loop = resolveLoop();
        synchronized (loop) {
            if (loop.redisAPIFuture == null) {
                startConnectLoop(loop, 0);
            }
            if (loop.redisAPIFuture == null) {
                return Future.failedFuture("Redis connection is not available");
            }
            return loop.redisAPIFuture;
        }
    }

    public String scriptSha1(final String key) {
        LoopRedis loop = loops.get(currentLoopKey());
        if (loop != null) {
            String sha = loop.scriptsSha.get(key);
            if (sha != null) {
                return sha;
            }
        }
        return loops
            .values()
            .stream()
            .map(l -> l.scriptsSha.get(key))
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
    }

    public String scriptSource(final String key) {
        return scriptsSource.get(key);
    }

    private LoopRedis resolveLoop() {
        return loops.computeIfAbsent(currentLoopKey(), k -> new LoopRedis());
    }

    private static int currentLoopKey() {
        Context context = Vertx.currentContext();
        if (context == null) {
            return FALLBACK_LOOP_KEY;
        }
        return System.identityHashCode(context.owner());
    }

    private void startConnectLoop(final LoopRedis loop, final int retry) {
        if (loop.redis != null) {
            loop.redis.close();
            loop.redis = null;
            loop.connected.set(false);
            loop.redisAPIFuture = null;
        }

        if (!loop.connecting.compareAndSet(false, true)) {
            return;
        }

        loop.redis = factory.createClient(clientOptions);
        loop.redisAPIFuture = loop.redis
            .connect()
            .onSuccess(conn -> {
                log.debug("Connected to Redis on event loop {}", currentLoopKey());
                conn.exceptionHandler(e -> attemptReconnect(loop, 0));
                conn.endHandler(v -> attemptReconnect(loop, 0));
            })
            .flatMap(redisConnection -> loadScripts(RedisAPI.api(redisConnection), loop))
            .onSuccess(redisAPI -> {
                log.info("Redis is now ready to be used on event loop {}.", currentLoopKey());
                loop.connecting.set(false);
                loop.connected.set(true);
            })
            .timeout(clientOptions.getConnectTimeout(), TimeUnit.MILLISECONDS)
            .onFailure(t -> {
                log.error("Unable to connect to Redis on event loop {}", currentLoopKey(), t);
                loop.connected.set(false);
                loop.connecting.set(false);
                loop.redisAPIFuture = null;
                attemptReconnect(loop, retry);
            });
    }

    private void attemptReconnect(final LoopRedis loop, int retry) {
        loop.connecting.set(false);
        long backoff = (long) (Math.pow(2, Math.min(retry, 10)) * 10);
        vertx.setTimer(backoff, timer -> {
            synchronized (loop) {
                if (loop.connected.get()) {
                    return;
                }
                startConnectLoop(loop, retry + 1);
            }
        });
    }

    private void preloadScriptSources() {
        if (scripts == null) {
            return;
        }
        scripts.forEach((key, scriptPath) -> {
            try (InputStream stream = RedisRateLimitRepository.class.getClassLoader().getResourceAsStream(scriptPath)) {
                if (stream == null) {
                    throw new IllegalStateException("Lua script not found on classpath: " + scriptPath);
                }
                scriptsSource.put(key, new String(stream.readAllBytes(), StandardCharsets.UTF_8));
            } catch (Exception ex) {
                throw new IllegalStateException("Unexpected error while reading lua script '" + scriptPath + "'", ex);
            }
        });
    }

    private Future<RedisAPI> loadScripts(final RedisAPI redisAPI, final LoopRedis loop) {
        if (scripts == null || scripts.isEmpty()) {
            return Future.succeededFuture(redisAPI);
        }
        return Future.all(
            scripts
                .entrySet()
                .stream()
                .map(entry -> {
                    String key = entry.getKey();
                    String source = scriptsSource.get(key);
                    if (source == null) {
                        return Future.failedFuture(new IllegalStateException("Lua script source not loaded for key: " + key));
                    }
                    return redisAPI
                        .script(Arrays.asList(SCRIPT_LOAD_COMMAND, source))
                        .onSuccess(response -> {
                            log.debug("Lua script '{}' registered to Redis", entry.getValue());
                            loop.scriptsSha.put(key, response.toString());
                        })
                        .mapEmpty();
                })
                .collect(Collectors.toList())
        ).map(v -> redisAPI);
    }

    private static final class LoopRedis {

        private final AtomicBoolean connected = new AtomicBoolean(false);
        private final AtomicBoolean connecting = new AtomicBoolean(false);
        private final Map<String, String> scriptsSha = new ConcurrentHashMap<>();
        private volatile Redis redis;
        private volatile Future<RedisAPI> redisAPIFuture;
    }
}

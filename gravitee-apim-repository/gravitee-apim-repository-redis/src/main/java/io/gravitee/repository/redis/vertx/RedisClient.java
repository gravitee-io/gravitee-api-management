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
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.RedisAPI;
import io.vertx.redis.client.RedisOptions;
import java.io.InputStream;
import java.net.SocketException;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class RedisClient {

    private static final String SCRIPT_LOAD_COMMAND = "LOAD";

    /**
     * Shared by callers without a Vert.x context (health checks, tests, blocking threads).
     */
    private static final int FALLBACK_LOOP_KEY = 0;

    private final Vertx vertx;
    private final RedisOptions options;
    private final Map<String, String> scripts;
    private final Map<String, String> scriptsSource = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, LoopRedis> loops = new ConcurrentHashMap<>();

    public RedisClient(final Vertx vertx, final RedisOptions options, final Map<String, String> scripts) {
        this.vertx = vertx;
        this.options = options;
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
        synchronized (loop.monitor) {
            if (loop.redisAPIFuture == null) {
                startConnectLoop(loop, 0);
            }
            if (loop.redisAPIFuture == null) {
                return Future.failedFuture("Redis connection is not available");
            }
            return loop.redisAPIFuture;
        }
    }

    /**
     * Notifies the client that a Redis operation failed because the connection is no longer usable.
     * This covers cases where the TCP session is half-open and Vert.x connection handlers have not
     * fired yet, which was leaving {@code connected=true} and blocking automatic reconnection.
     */
    public void notifyConnectionFailure(final Throwable failure) {
        if (!isRecoverableConnectionFailure(failure)) {
            return;
        }
        final Context context = Vertx.currentContext();
        if (context == null) {
            log.debug("Ignoring Redis connection failure notification without a Vert.x context");
            return;
        }
        final LoopRedis loop = resolveLoop();
        context.runOnContext(v -> invalidateConnection(loop));
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
        final long connectionGeneration;
        synchronized (loop.monitor) {
            if (!loop.connecting.compareAndSet(false, true)) {
                return;
            }
            closeClient(loop);
            loop.connected.set(false);
            loop.redisAPIFuture = null;
            connectionGeneration = loop.generation.incrementAndGet();
        }

        final Redis redisClient = Redis.createClient(vertx, options);
        final Future<RedisAPI> connectFuture = redisClient
            .connect()
            .onSuccess(conn -> {
                log.debug("Connected to Redis on event loop {}", currentLoopKey());
                conn.exceptionHandler(e -> handleConnectionLost(loop, connectionGeneration));
                conn.endHandler(v -> handleConnectionLost(loop, connectionGeneration));
            })
            .flatMap(redisConnection -> loadScripts(RedisAPI.api(redisConnection), loop))
            .onSuccess(redisAPI -> {
                if (connectionGeneration != loop.generation.get()) {
                    return;
                }
                log.info("Redis is now ready to be used on event loop {}.", currentLoopKey());
                loop.connecting.set(false);
                loop.connected.set(true);
            })
            .timeout(options.getNetClientOptions().getConnectTimeout(), TimeUnit.MILLISECONDS)
            .onFailure(t -> {
                if (connectionGeneration != loop.generation.get()) {
                    return;
                }
                log.error("Unable to connect to Redis on event loop {}", currentLoopKey(), t);
                scheduleReconnectAfterFailure(loop, retry);
            });

        synchronized (loop.monitor) {
            if (connectionGeneration == loop.generation.get()) {
                loop.redis = redisClient;
                loop.redisAPIFuture = connectFuture;
            } else {
                loop.connecting.set(false);
                redisClient.close();
            }
        }
    }

    private void closeClient(final LoopRedis loop) {
        if (loop.redis == null) {
            return;
        }
        Redis clientToClose = loop.redis;
        loop.redis = null;
        clientToClose.close();
    }

    private void handleConnectionLost(final LoopRedis loop, final long connectionGeneration) {
        if (connectionGeneration != loop.generation.get()) {
            return;
        }

        synchronized (loop.monitor) {
            if (connectionGeneration != loop.generation.get()) {
                return;
            }
            loop.redisAPIFuture = null;
        }
        scheduleReconnectAfterFailure(loop, 0);
    }

    private void invalidateConnection(final LoopRedis loop) {
        synchronized (loop.monitor) {
            loop.generation.incrementAndGet();
            loop.redisAPIFuture = null;
            loop.connected.set(false);
            loop.connecting.set(false);
            closeClient(loop);
            if (!scheduleReconnectIfAbsent(loop)) {
                return;
            }
        }
        attemptReconnect(loop, 0);
    }

    private void scheduleReconnectAfterFailure(final LoopRedis loop, final int retry) {
        synchronized (loop.monitor) {
            loop.connected.set(false);
            loop.connecting.set(false);
            loop.redisAPIFuture = null;
            if (!scheduleReconnectIfAbsent(loop)) {
                return;
            }
        }
        attemptReconnect(loop, retry);
    }

    private boolean scheduleReconnectIfAbsent(final LoopRedis loop) {
        return loop.reconnectPending.compareAndSet(false, true);
    }

    private void attemptReconnect(final LoopRedis loop, int retry) {
        long backoff = (long) (Math.pow(2, Math.min(retry, 10)) * 10);
        vertx.setTimer(backoff, timer -> {
            synchronized (loop.monitor) {
                loop.reconnectPending.set(false);
                if (loop.connected.get() || loop.connecting.get()) {
                    return;
                }
                startConnectLoop(loop, retry + 1);
            }
        });
    }

    private static boolean isRecoverableConnectionFailure(final Throwable failure) {
        Throwable current = failure;
        while (current != null) {
            if (current instanceof ClosedChannelException || current instanceof SocketException) {
                return true;
            }
            final String message = current.getMessage();
            if (message != null) {
                final String normalized = message.toLowerCase();
                if (
                    normalized.contains("connection is closed") ||
                    normalized.contains("connection lost") ||
                    normalized.contains("connection reset") ||
                    normalized.contains("not connected") ||
                    normalized.contains("broken pipe")
                ) {
                    return true;
                }
            }
            final String simpleName = current.getClass().getSimpleName();
            if (simpleName.contains("NotConnected") || simpleName.contains("ClosedChannel")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
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

        private final Object monitor = new Object();
        private final AtomicBoolean connected = new AtomicBoolean(false);
        private final AtomicBoolean connecting = new AtomicBoolean(false);
        private final AtomicBoolean reconnectPending = new AtomicBoolean(false);
        private final AtomicLong generation = new AtomicLong(0);
        private final Map<String, String> scriptsSha = new ConcurrentHashMap<>();
        private volatile Redis redis;
        private volatile Future<RedisAPI> redisAPIFuture;
    }
}

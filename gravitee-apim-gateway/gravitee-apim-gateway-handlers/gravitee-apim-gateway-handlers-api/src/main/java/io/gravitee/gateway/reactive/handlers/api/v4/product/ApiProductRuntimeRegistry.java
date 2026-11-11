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
package io.gravitee.gateway.reactive.handlers.api.v4.product;

import io.gravitee.gateway.handlers.api.ReactableApiProduct;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Scheduler;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import lombok.CustomLog;

/**
 * Holds the runtime of each API Product deployed on this gateway, published as one immutable snapshot.
 *
 * <p>A new version is swapped in without a gap: its runtime is built and started, published, and only then is the
 * previous one retired. A retired runtime stops once the requests pinned to it have ended, or once the drain timeout
 * expires, which is logged.</p>
 */
@CustomLog
public class ApiProductRuntimeRegistry {

    static final long DRAIN_POLL_INTERVAL_MS = 100;

    private final Function<ReactableApiProduct, ApiProductRuntime> runtimeFactory;
    private final long drainTimeoutMs;
    private final Scheduler scheduler;
    private volatile Map<Key, ApiProductRuntime> runtimes = Map.of();

    public ApiProductRuntimeRegistry(
        Function<ReactableApiProduct, ApiProductRuntime> runtimeFactory,
        long drainTimeoutMs,
        Scheduler scheduler
    ) {
        this.runtimeFactory = runtimeFactory;
        this.drainTimeoutMs = drainTimeoutMs;
        this.scheduler = scheduler;
    }

    /**
     * Pins the published runtime of a product for one request, which must {@link ApiProductRuntime#release()} it.
     *
     * @return the runtime, or null when the product has none on this gateway
     */
    public ApiProductRuntime acquire(String apiProductId, String environmentId) {
        Key key = new Key(apiProductId, environmentId);
        while (true) {
            ApiProductRuntime runtime = runtimes.get(key);
            if (runtime == null || runtime.tryAcquire()) {
                return runtime;
            }
            // Retired between the read and the pin: its replacement is already published.
        }
    }

    /**
     * Swaps in the runtime of a product version.
     *
     * @throws IllegalStateException when it cannot be built or started: what it started is stopped and the previous
     * runtime keeps serving
     */
    public synchronized void deploy(ReactableApiProduct product) {
        ApiProductRuntime runtime = null;
        try {
            runtime = runtimeFactory.apply(product);
            runtime.start();
        } catch (Exception e) {
            if (runtime != null) {
                stop(runtime, 0);
            }
            throw new IllegalStateException(
                "Unable to start the runtime of API Product [" + product.getId() + "] in environment [" + product.getEnvironmentId() + "]",
                e
            );
        }
        Map<Key, ApiProductRuntime> next = new HashMap<>(runtimes);
        ApiProductRuntime previous = next.put(new Key(product.getId(), product.getEnvironmentId()), runtime);
        runtimes = Map.copyOf(next);
        if (previous != null) {
            retire(previous);
        }
    }

    public synchronized void remove(String apiProductId, String environmentId) {
        Map<Key, ApiProductRuntime> next = new HashMap<>(runtimes);
        ApiProductRuntime previous = next.remove(new Key(apiProductId, environmentId));
        if (previous != null) {
            runtimes = Map.copyOf(next);
            retire(previous);
        }
    }

    /** Stops every runtime right away, as the gateway shuts down. */
    public synchronized void clear() {
        Map<Key, ApiProductRuntime> stopping = runtimes;
        runtimes = Map.of();
        stopping
            .values()
            .forEach(runtime -> {
                runtime.retire();
                stop(runtime, 0);
            });
    }

    private void retire(ApiProductRuntime runtime) {
        runtime.retire();
        AtomicInteger lastSeenInFlight = new AtomicInteger();
        Flowable.interval(0, DRAIN_POLL_INTERVAL_MS, TimeUnit.MILLISECONDS, scheduler)
            .takeWhile(tick -> {
                int inFlight = runtime.inFlight();
                lastSeenInFlight.set(inFlight);
                return inFlight > 0 && tick * DRAIN_POLL_INTERVAL_MS < drainTimeoutMs;
            })
            .ignoreElements()
            .doFinally(() -> stop(runtime, lastSeenInFlight.get()))
            .subscribe();
    }

    private void stop(ApiProductRuntime runtime, int stillInFlight) {
        String apiProductId = runtime.product().getId();
        if (stillInFlight > 0) {
            log.warn(
                "Stopping the previous runtime of API Product [{}] while {} request(s) still use it: waited {} ms for them",
                apiProductId,
                stillInFlight,
                drainTimeoutMs
            );
        }
        try {
            runtime.stop();
        } catch (Exception e) {
            log.error("Unable to stop a runtime of API Product [{}]", apiProductId, e);
        }
    }

    private record Key(String apiProductId, String environmentId) {}
}

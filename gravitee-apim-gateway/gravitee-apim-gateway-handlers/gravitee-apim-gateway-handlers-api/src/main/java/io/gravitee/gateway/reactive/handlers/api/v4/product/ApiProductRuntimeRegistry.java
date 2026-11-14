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
import io.gravitee.gateway.reactive.handlers.api.security.plan.HttpSecurityPlan;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Scheduler;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
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

    private final ApiProductRuntimeFactory runtimeFactory;
    private final long drainTimeoutMs;
    private final Scheduler scheduler;
    private final AtomicReference<Snapshot> snapshot = new AtomicReference<>(Snapshot.EMPTY);

    public ApiProductRuntimeRegistry(ApiProductRuntimeFactory runtimeFactory, long drainTimeoutMs, Scheduler scheduler) {
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
            ApiProductRuntime runtime = snapshot.get().runtimes().get(key);
            if (runtime == null || runtime.tryAcquire()) {
                return runtime;
            }
            // Retired between the read and the pin: its replacement is already published.
        }
    }

    /**
     * Pins, for one request, the published runtimes of every product listing an API, all from the same snapshot. Each
     * must be {@link ApiProductRuntime#release() released}.
     */
    public MemberApiRuntimes acquireForApi(String apiId, String environmentId) {
        Key key = new Key(apiId, environmentId);
        while (true) {
            MemberApiRuntimes runtimes = snapshot.get().byApi().getOrDefault(key, MemberApiRuntimes.NONE);
            if (runtimes.tryAcquireAll()) {
                return runtimes;
            }
            // One was retired between the read and the pin: its replacement is already published.
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
        ApiProductRuntime publishedBefore = snapshot.get().runtimes().get(new Key(product.getId(), product.getEnvironmentId()));
        try {
            runtime = runtimeFactory.create(product, publishedBefore);
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
        Map<Key, ApiProductRuntime> next = new LinkedHashMap<>(snapshot.get().runtimes());
        Key key = new Key(product.getId(), product.getEnvironmentId());
        ApiProductRuntime previous = next.remove(key);
        next.put(key, runtime);
        snapshot.set(Snapshot.of(next));
        if (previous != null) {
            retire(previous);
        }
    }

    public synchronized void remove(String apiProductId, String environmentId) {
        Map<Key, ApiProductRuntime> next = new LinkedHashMap<>(snapshot.get().runtimes());
        ApiProductRuntime previous = next.remove(new Key(apiProductId, environmentId));
        if (previous != null) {
            snapshot.set(Snapshot.of(next));
            retire(previous);
        }
    }

    /** Stops every runtime right away, as the gateway shuts down. */
    public synchronized void clear() {
        Map<Key, ApiProductRuntime> stopping = snapshot.get().runtimes();
        snapshot.set(Snapshot.EMPTY);
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

    /** The runtimes pinned for one request on a member API, and their security plans. */
    public record MemberApiRuntimes(List<ApiProductRuntime> runtimes, List<HttpSecurityPlan> securityPlans) {
        static final MemberApiRuntimes NONE = new MemberApiRuntimes(List.of(), List.of());

        /**
         * Product plans are tried before the plans of the API, sorted by order across products. The sort is stable, so
         * plans of equal order keep the order the products were deployed in.
         */
        static MemberApiRuntimes of(List<ApiProductRuntime> runtimes) {
            List<HttpSecurityPlan> securityPlans = runtimes
                .stream()
                .flatMap(runtime -> runtime.securityPlans().stream())
                .sorted(Comparator.comparingInt(HttpSecurityPlan::order))
                .toList();
            return new MemberApiRuntimes(List.copyOf(runtimes), securityPlans);
        }

        private boolean tryAcquireAll() {
            for (int i = 0; i < runtimes.size(); i++) {
                if (!runtimes.get(i).tryAcquire()) {
                    runtimes.subList(0, i).forEach(ApiProductRuntime::release);
                    return false;
                }
            }
            return true;
        }
    }

    /** The runtimes in the order their products were last deployed, and the runtimes serving each member API. */
    private record Snapshot(Map<Key, ApiProductRuntime> runtimes, Map<Key, MemberApiRuntimes> byApi) {
        static final Snapshot EMPTY = new Snapshot(Map.of(), Map.of());

        static Snapshot of(Map<Key, ApiProductRuntime> runtimes) {
            Map<Key, List<ApiProductRuntime>> runtimesByApi = new HashMap<>();
            runtimes.forEach((productKey, runtime) -> {
                if (runtime.product().getApiIds() != null) {
                    runtime
                        .product()
                        .getApiIds()
                        .forEach(apiId ->
                            runtimesByApi.computeIfAbsent(new Key(apiId, productKey.environmentId()), k -> new ArrayList<>()).add(runtime)
                        );
                }
            });
            Map<Key, MemberApiRuntimes> byApi = new HashMap<>();
            runtimesByApi.forEach((apiKey, apiRuntimes) -> byApi.put(apiKey, MemberApiRuntimes.of(apiRuntimes)));
            return new Snapshot(Collections.unmodifiableMap(new LinkedHashMap<>(runtimes)), Map.copyOf(byApi));
        }
    }

    /** An API Product, or a member API, in an environment. */
    private record Key(String id, String environmentId) {}
}

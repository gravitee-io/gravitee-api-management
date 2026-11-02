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
package io.gravitee.gateway.reactive.core.v4.overlay;

import io.gravitee.node.api.opentelemetry.sampling.SamplingConfig;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import java.util.stream.Collectors;
import lombok.CustomLog;

/**
 * The only entry point to runtime overlays. The synchronizer registers and unregisters them, the reactor reads the
 * active ones of its API on each request. Contributions are computed at activation, and a one-shot timer removes
 * each runtime overlay at its end, even if its API never receives a call.
 */
@CustomLog
public class RuntimeOverlays {

    private final Map<String, RuntimeOverlayContributor> contributors = new ConcurrentHashMap<>();
    private final Map<String, ActiveRuntimeOverlay> byId = new ConcurrentHashMap<>();
    private final Map<String, List<ActiveRuntimeOverlay>> byApi = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> timers = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler;
    private final LongSupplier clock;

    public RuntimeOverlays() {
        this(
            Executors.newSingleThreadScheduledExecutor(runnable -> {
                final Thread thread = new Thread(runnable, "gio-runtime-overlays");
                thread.setDaemon(true);
                return thread;
            }),
            System::currentTimeMillis
        );
    }

    public RuntimeOverlays(final ScheduledExecutorService scheduler, final LongSupplier clock) {
        this.scheduler = scheduler;
        this.clock = clock;
    }

    /** Accepted at runtime rather than as a fixed list, so a plugin can bring its own kind later. */
    public void registerContributor(final RuntimeOverlayContributor contributor) {
        contributors.put(contributor.kind(), contributor);
        log.info("🧩 Runtime overlay contributor registered for kind [{}]", contributor.kind());
    }

    public void register(final RuntimeOverlay runtimeOverlay) {
        if (runtimeOverlay.status() == RuntimeOverlayStatus.STOPPED) {
            unregister(runtimeOverlay.id(), "stopped");
            return;
        }
        if (runtimeOverlay.hasEnded(clock.getAsLong())) {
            log.info(
                "⏭️ Runtime overlay [{}] on API [{}] already ended at {}, ignored",
                runtimeOverlay.id(),
                runtimeOverlay.apiId(),
                Instant.ofEpochMilli(runtimeOverlay.endAt())
            );
            return;
        }
        final RuntimeOverlayContributor contributor = contributors.get(runtimeOverlay.kind());
        if (contributor == null) {
            log.warn(
                "❓ No contributor for runtime overlay kind [{}], runtime overlay [{}] ignored",
                runtimeOverlay.kind(),
                runtimeOverlay.id()
            );
            return;
        }
        final ActiveRuntimeOverlay existing = byId.get(runtimeOverlay.id());
        if (existing != null) {
            // The sync window overlaps the previous one, so the same event is read more than once.
            if (existing.runtimeOverlay().equals(runtimeOverlay)) {
                return;
            }
            remove(runtimeOverlay.id());
        }

        final RuntimeOverlayContribution contribution = contributor.contribute(runtimeOverlay);
        final ActiveRuntimeOverlay active = new ActiveRuntimeOverlay(runtimeOverlay, contribution);
        warnOnConflicts(active);
        byId.put(runtimeOverlay.id(), active);
        byApi.compute(runtimeOverlay.apiId(), (apiId, current) -> {
            final List<ActiveRuntimeOverlay> next = current == null ? new ArrayList<>() : new ArrayList<>(current);
            next.add(active);
            return List.copyOf(next);
        });

        final long delay = Math.max(0, runtimeOverlay.endAt() - clock.getAsLong());
        timers.put(runtimeOverlay.id(), scheduler.schedule(() -> unregister(runtimeOverlay.id(), "expired"), delay, TimeUnit.MILLISECONDS));

        log.info(
            "✅ Runtime overlay [{}] of kind [{}] active on API [{}] until {} ({} s left), contribution: {}",
            runtimeOverlay.id(),
            runtimeOverlay.kind(),
            runtimeOverlay.apiId(),
            Instant.ofEpochMilli(runtimeOverlay.endAt()),
            TimeUnit.MILLISECONDS.toSeconds(delay),
            contribution.sampling() != null ? "sampling " + contribution.sampling() : contribution.internalAttributes().keySet()
        );
    }

    public void unregister(final String runtimeOverlayId, final String reason) {
        final ActiveRuntimeOverlay removed = remove(runtimeOverlayId);
        if (removed == null) {
            return;
        }
        final RuntimeOverlay runtimeOverlay = removed.runtimeOverlay();
        final RuntimeOverlayContributor contributor = contributors.get(runtimeOverlay.kind());
        if (contributor != null) {
            contributor.release(runtimeOverlay);
        }
        log.info(
            "🧹 Runtime overlay [{}] {}: removed from API [{}], contribution released",
            runtimeOverlayId,
            reason,
            runtimeOverlay.apiId()
        );
    }

    /** The runtime overlays of this API that have not ended yet. The common case, no runtime overlay at all, costs one read. */
    public List<ActiveRuntimeOverlay> activeFor(final String apiId) {
        if (byApi.isEmpty()) {
            return List.of();
        }
        final List<ActiveRuntimeOverlay> actives = byApi.get(apiId);
        if (actives == null) {
            return List.of();
        }
        // Guards the short window between the end of a runtime overlay and its timer firing.
        final long now = clock.getAsLong();
        return actives
            .stream()
            .filter(active -> !active.runtimeOverlay().hasEnded(now))
            .toList();
    }

    /** The sampling of the first active runtime overlay of this API that sets one: it replaces the gateway's. */
    public Optional<SamplingConfig> samplingFor(final String apiId) {
        return activeFor(apiId)
            .stream()
            .map(active -> active.contribution().sampling())
            .filter(sampling -> sampling != null)
            .findFirst();
    }

    private ActiveRuntimeOverlay remove(final String runtimeOverlayId) {
        final ScheduledFuture<?> timer = timers.remove(runtimeOverlayId);
        if (timer != null) {
            timer.cancel(false);
        }
        final ActiveRuntimeOverlay removed = byId.remove(runtimeOverlayId);
        if (removed != null) {
            byApi.computeIfPresent(removed.runtimeOverlay().apiId(), (apiId, current) -> {
                final List<ActiveRuntimeOverlay> next = current
                    .stream()
                    .filter(active -> active != removed)
                    .toList();
                return next.isEmpty() ? null : next;
            });
        }
        return removed;
    }

    private void warnOnConflicts(final ActiveRuntimeOverlay candidate) {
        final Set<String> keys = candidate.contribution().internalAttributes().keySet();
        byApi
            .getOrDefault(candidate.runtimeOverlay().apiId(), List.of())
            .forEach(existing -> {
                final Set<String> overlap = existing
                    .contribution()
                    .internalAttributes()
                    .keySet()
                    .stream()
                    .filter(keys::contains)
                    .collect(Collectors.toSet());
                if (!overlap.isEmpty()) {
                    log.warn(
                        "⚠️ Runtime overlays [{}] and [{}] on API [{}] both contribute {}, the first one wins",
                        existing.runtimeOverlay().id(),
                        candidate.runtimeOverlay().id(),
                        candidate.runtimeOverlay().apiId(),
                        overlap
                    );
                }
            });
    }
}

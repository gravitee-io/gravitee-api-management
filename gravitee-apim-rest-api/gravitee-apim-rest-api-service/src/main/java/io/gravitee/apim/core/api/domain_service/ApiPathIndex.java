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
package io.gravitee.apim.core.api.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.Path;
import io.gravitee.apim.core.validation.Validator;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Stream;
import lombok.CustomLog;

/**
 * Per-replica in-memory cache of API paths used by {@code VerifyApiPathDomainService} to detect collisions without
 * scanning the full API catalog on every publish.
 *
 * <h2>Invariants</h2>
 * <ul>
 *   <li><b>Lazy per-env seed.</b> A {@code findConflicts} call on an unseeded env triggers the supplied seeder once,
 *       under {@link java.util.concurrent.ConcurrentHashMap#computeIfAbsent}. Concurrent readers on the same env share
 *       a single seed; readers on different envs do not interfere.</li>
 *   <li><b>Writes that arrive before a seed are dropped, NOT auto-recovered.</b> {@link #index} and {@link #remove}
 *       no-op when the env has no snapshot installed (the env is still unseeded). The dropped write is not replayed;
 *       recovery relies on the env being still unseeded at that point, so the very next {@link #findConflicts} call
 *       triggers {@code computeIfAbsent} and the seeder reads the latest Mongo state — which already contains the
 *       observed write because observers fire before the Lucene write returns to the caller. Once seeded, observer
 *       updates keep the snapshot fresh; further writes are no longer dropped.</li>
 *   <li><b>Seed failure retries.</b> If the seeder throws, no mapping is installed; the next read retries.</li>
 *   <li><b>Observer failure recovery.</b> Callers that maintain the snapshot should invoke {@link #invalidate} when
 *       their own update throws so the next read reseeds from the source of truth.</li>
 *   <li><b>No env eviction.</b> Snapshots persist for the lifetime of the JVM; size is bounded by the API count per
 *       env. Restart reclaims memory; runtime env deletion does not (call {@link #invalidate} on env-deleted events
 *       to release).</li>
 *   <li><b>Cross-replica consistency.</b> Each replica seeds independently; observer updates are distributed by the
 *       existing {@code SearchEngineService} Command broadcast, polled by {@code ScheduledSearchIndexerService}
 *       (default cron every 5 seconds — see {@code services.search_indexer.cron}). Remote replicas therefore converge
 *       within one cron interval, not synchronously. {@code VerifyApiPathDomainService} closes the resulting race by
 *       combining the snapshot scan with a "recently updated" Mongo query bounded by {@link FindResult#refreshedAt()}.</li>
 *   <li><b>Watermark semantics.</b> {@code refreshedAt} is {@code max(updatedAt)} over rows applied to the snapshot
 *       (seeded or via {@link #index}/{@link #remove}). Same clock domain as {@code Api.updatedAt}, so callers can
 *       safely filter Mongo with {@code updatedAt > refreshedAt - margin} to catch any row not yet applied via the
 *       broadcast cron. {@link #removeForApi} does not bump the watermark — hard deletes leave no row to compare,
 *       so the validator's per-conflict recheck handles delete races.</li>
 *   <li><b>No env-migration support.</b> An apiId is assumed to live in exactly one env for its lifetime (APIM does
 *       not move APIs between envs; promotion creates a new API). If a moved apiId ever reaches
 *       {@code ApiPathIndexationObserver}, the old (env, apiId) entry would linger until JVM restart — observers
 *       should fail loud rather than silently absorb that case.</li>
 * </ul>
 */
@CustomLog
@DomainService
public class ApiPathIndex {

    public record Conflict(String apiId, Validator.Error error) {}

    public record FindResult(List<Conflict> conflicts, Instant refreshedAt) {
        public List<Validator.Error> errors() {
            return conflicts.stream().map(Conflict::error).toList();
        }
    }

    private static final class EnvSnapshot {

        private final Map<String, List<Path>> paths = new ConcurrentHashMap<>();
        private final AtomicReference<Instant> refreshedAt;

        EnvSnapshot(Instant initial) {
            this.refreshedAt = new AtomicReference<>(initial == null ? Instant.EPOCH : initial);
        }

        void bump(Instant updatedAt) {
            if (updatedAt == null) {
                return;
            }
            refreshedAt.updateAndGet(prev -> updatedAt.isAfter(prev) ? updatedAt : prev);
        }
    }

    private final Map<String, EnvSnapshot> snapshotsByEnv = new ConcurrentHashMap<>();

    public void index(String envId, String apiId, List<Path> paths, Instant updatedAt) {
        var snapshot = snapshotsByEnv.get(envId);
        if (snapshot != null) {
            snapshot.paths.put(apiId, List.copyOf(paths));
            snapshot.bump(updatedAt);
        }
    }

    public void remove(String envId, String apiId, Instant updatedAt) {
        var snapshot = snapshotsByEnv.get(envId);
        if (snapshot != null) {
            snapshot.paths.remove(apiId);
            snapshot.bump(updatedAt);
        }
    }

    /**
     * Drops the cached snapshot for {@code envId}. The next {@link #findConflicts} call on this env will reseed from
     * the supplied seeder. Used to recover from observer failures that may have left the in-memory state diverged from
     * Mongo.
     */
    public void invalidate(String envId) {
        snapshotsByEnv.remove(envId);
    }

    /**
     * Removes {@code apiId} from every env snapshot. Used by observers that learn about a delete from the
     * SearchEngineService broadcast cron, where the {@link io.gravitee.rest.api.model.search.Indexable} payload only
     * carries the id (no env, no proxy, no listeners) — see {@code SearchEngineServiceImpl.process()} ACTION_DELETE
     * branch. Does not bump any env's watermark: a deleted row leaves no {@code updatedAt} to compare against, so the
     * validator relies on a per-conflict recheck to catch delete races within one cron interval.
     */
    public void removeForApi(String apiId) {
        snapshotsByEnv.forEach((envId, snapshot) -> {
            try {
                snapshot.paths.remove(apiId);
            } catch (RuntimeException e) {
                snapshotsByEnv.remove(envId);
                log.warn("removeForApi failed for env=[{}] api=[{}], invalidated env snapshot", envId, apiId, e);
            }
        });
    }

    /**
     * Returns an immutable view of the env's path snapshot plus the watermark up to which observer updates have been
     * applied. Triggers a lazy seed via {@code seeder} on first access for the env.
     */
    public FindResult findConflicts(String envId, String excludeApiId, List<Path> candidatePaths, Supplier<Stream<Api>> seeder) {
        var snapshot = snapshotsByEnv.computeIfAbsent(envId, key -> seedFrom(key, seeder));
        var conflicts = scanPaths(snapshot.paths, excludeApiId, candidatePaths);
        return new FindResult(conflicts, snapshot.refreshedAt.get());
    }

    /**
     * Returns the watermark for the env after lazy-seeding if necessary, along with an immutable copy of the snapshot.
     * Used by the validator to combine the snapshot scan with a "recently updated" Mongo query.
     */
    public Snapshot snapshotOf(String envId, Supplier<Stream<Api>> seeder) {
        var snapshot = snapshotsByEnv.computeIfAbsent(envId, key -> seedFrom(key, seeder));
        return new Snapshot(Map.copyOf(snapshot.paths), snapshot.refreshedAt.get());
    }

    public record Snapshot(Map<String, List<Path>> pathsByApiId, Instant refreshedAt) {}

    /**
     * Scans a {@code apiId -> paths} view for conflicts against the given candidate paths, skipping
     * {@code excludeApiId}. Pure function — used by {@link #findConflicts} and by callers that want to scan a merged
     * snapshot+supplementary view.
     */
    public static List<Conflict> scanPaths(Map<String, List<Path>> pathsByApiId, String excludeApiId, List<Path> candidatePaths) {
        var conflicts = new ArrayList<Conflict>();
        var candidateWithHost = ApiPathExtractor.getPathsWithHost(candidatePaths);
        var candidateWithoutHost = ApiPathExtractor.getPathsWithoutHost(candidatePaths);

        pathsByApiId.forEach((apiId, existingPaths) -> {
            if (apiId.equals(excludeApiId)) {
                return;
            }
            var existingWithHost = ApiPathExtractor.getPathsWithHost(existingPaths);
            var existingWithoutHost = ApiPathExtractor.getPathsWithoutHost(existingPaths);

            candidateWithHost.forEach((host, hostPaths) ->
                hostPaths.forEach(candidatePath ->
                    ApiPathExtractor.findConflictingPathError(candidatePath, existingWithHost.getOrDefault(host, List.of())).ifPresent(
                        err -> conflicts.add(new Conflict(apiId, err))
                    )
                )
            );
            candidateWithoutHost.forEach(candidatePath ->
                ApiPathExtractor.findConflictingPathError(candidatePath, existingWithoutHost).ifPresent(err ->
                    conflicts.add(new Conflict(apiId, err))
                )
            );
        });

        return conflicts;
    }

    private static EnvSnapshot seedFrom(String envId, Supplier<Stream<Api>> seeder) {
        var snapshot = new EnvSnapshot(Instant.EPOCH);
        try (var apis = seeder.get()) {
            apis.forEach(api -> {
                var paths = ApiPathExtractor.extractPaths(api);
                if (!paths.isEmpty()) {
                    snapshot.paths.put(api.getId(), paths);
                }
                if (api.getUpdatedAt() != null) {
                    snapshot.bump(api.getUpdatedAt().toInstant());
                }
            });
        }
        log.info("Seeded path index env=[{}] apiCount=[{}] watermark=[{}]", envId, snapshot.paths.size(), snapshot.refreshedAt.get());
        return snapshot;
    }
}

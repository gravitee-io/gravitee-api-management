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
 *       a single seed; readers on different envs do not interfere. First validate per-env is a full env Mongo scan
 *       (the seeder) — typical cold start cost. Subsequent reads are in-memory.</li>
 *   <li><b>Writes that arrive before a seed are dropped, NOT auto-recovered.</b> {@link #index} and {@link #remove}
 *       no-op when the env has no snapshot installed. The dropped write is not replayed. Recovery relies on the
 *       fact that the API service commits the Mongo write BEFORE invoking {@code SearchEngineService.index(...)}
 *       (see {@code ApiServiceImpl.create}: {@code apiRepository.create(...)} precedes
 *       {@code searchEngineService.index(...)}). So by the time the observer fires and we drop the write, the row is
 *       already in Mongo — the very next {@link #findConflicts} call triggers {@code computeIfAbsent} and the seeder
 *       reads it. Once seeded, observer updates keep the snapshot fresh; further writes are no longer dropped.</li>
 *   <li><b>Seed failure retries.</b> If the seeder itself throws, no mapping is installed; the next read retries.
 *       Per-row extraction failures within the seeder are caught and the offending api is skipped with a warning so
 *       one malformed row cannot leave the env permanently un-installed.</li>
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
public class ApiPathIndex implements ApiPathIndexReader, ApiPathIndexWriter {

    /** A single path collision: the apiId that owns the conflicting path and the user-facing error message. */
    public record Conflict(String apiId, Validator.Error error) {}

    /**
     * Result of a {@link #findConflicts} call: the conflicts found in the snapshot, plus the snapshot's
     * {@code refreshedAt} watermark. Callers can use {@code refreshedAt} to bound a supplementary "recently updated"
     * query when they need cross-replica freshness.
     */
    public record FindResult(List<Conflict> conflicts, Instant refreshedAt) {
        /** Convenience: just the error messages, in the order they were found. */
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

    /**
     * Applies an indexed/updated API to the env snapshot and bumps the watermark.
     *
     * <p>No-op if the env has not been seeded yet (the dropped write is recovered the next time {@link #findConflicts}
     * triggers the seeder — the API service commits the Mongo write before invoking the observer chain, so the seed
     * will see the row). Once seeded, this stores a defensive copy of {@code paths} so caller-side mutation cannot
     * corrupt the snapshot.</p>
     *
     * @param updatedAt the {@code Api.updatedAt} value as of this write — used to advance the env watermark. A
     *                  {@code null} value leaves the watermark unchanged (treat as "unknown freshness").
     */
    @Override
    public void index(String envId, String apiId, List<Path> paths, Instant updatedAt) {
        var snapshot = snapshotsByEnv.get(envId);
        if (snapshot != null) {
            snapshot.paths.put(apiId, List.copyOf(paths));
            snapshot.bump(updatedAt);
        }
    }

    /**
     * Drops the env's snapshot entry for {@code apiId} and bumps the watermark to {@code updatedAt}. Used by the
     * observer when an API has been updated to have no HTTP paths (e.g., transitioned to a non-HTTP listener kind),
     * not for hard deletes — those route through {@link #removeForApi}.
     *
     * <p>No-op if the env has not been seeded yet. See {@link #index} for the recovery story.</p>
     */
    @Override
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
    @Override
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
    @Override
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
     * Scans the env's in-memory path snapshot for conflicts against {@code candidatePaths}, skipping
     * {@code excludeApiId}, and returns the conflicts together with the watermark up to which observer updates have
     * been applied. Triggers a lazy seed via {@code seeder} on first access for the env. The returned watermark is
     * intended to bound a follow-up "recently updated" Mongo query (see {@code VerifyApiPathDomainService}).
     */
    @Override
    public FindResult findConflicts(String envId, String excludeApiId, List<Path> candidatePaths, Supplier<Stream<Api>> seeder) {
        var snapshot = snapshotsByEnv.computeIfAbsent(envId, key -> seedFrom(key, seeder));
        var conflicts = scanPaths(snapshot.paths, excludeApiId, candidatePaths);
        return new FindResult(conflicts, snapshot.refreshedAt.get());
    }

    /**
     * Returns an immutable view of the env's snapshot (apiId -&gt; paths) together with the watermark up to which
     * observer updates have been applied. Triggers a lazy seed via {@code seeder} on first access for the env.
     *
     * <p>The returned map is a defensive copy — caller-side mutation is safe and does not affect the live snapshot,
     * and concurrent observer updates after the copy are not reflected in this view.</p>
     */
    @Override
    public Snapshot snapshotOf(String envId, Supplier<Stream<Api>> seeder) {
        var snapshot = snapshotsByEnv.computeIfAbsent(envId, key -> seedFrom(key, seeder));
        return new Snapshot(Map.copyOf(snapshot.paths), snapshot.refreshedAt.get());
    }

    /** Immutable point-in-time view returned by {@link #snapshotOf}. */
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
        var skipped = new java.util.concurrent.atomic.AtomicInteger();
        try (var apis = seeder.get()) {
            apis.forEach(api -> {
                // One malformed row must not abort the whole seed and leave the env permanently un-installed:
                // computeIfAbsent's lambda throwing leaks past the lock, so every later findConflicts on this env
                // would retry the seed indefinitely. Skip-and-warn keeps the index usable; the supplementary query
                // and per-conflict recheck still pick up rows that didn't make it into the snapshot.
                try {
                    var paths = ApiPathExtractor.extractPaths(api);
                    if (!paths.isEmpty()) {
                        snapshot.paths.put(api.getId(), paths);
                    }
                    if (api.getUpdatedAt() != null) {
                        snapshot.bump(api.getUpdatedAt().toInstant());
                    }
                } catch (RuntimeException e) {
                    skipped.incrementAndGet();
                    log.warn("Skipping api=[{}] during seed of env=[{}] due to extraction failure", api.getId(), envId, e);
                }
            });
        }
        log.info(
            "Seeded path index env=[{}] apiCount=[{}] skipped=[{}] watermark=[{}]",
            envId,
            snapshot.paths.size(),
            skipped.get(),
            snapshot.refreshedAt.get()
        );
        return snapshot;
    }
}

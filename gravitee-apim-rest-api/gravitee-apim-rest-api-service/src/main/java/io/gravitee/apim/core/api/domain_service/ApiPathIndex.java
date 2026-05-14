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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
 *   <li><b>Cross-replica consistency.</b> Not enforced here. Each replica seeds independently; observer updates are
 *       distributed by the existing {@code SearchEngineService} Command broadcast, which is polled by
 *       {@code ScheduledSearchIndexerService} (default cron is every 5 seconds — see
 *       {@code services.search_indexer.cron}). Remote replicas therefore converge within one cron interval,
 *       not synchronously.</li>
 *   <li><b>No env-migration support.</b> An apiId is assumed to live in exactly one env for its lifetime (APIM does
 *       not move APIs between envs; promotion creates a new API). If a moved apiId ever reaches
 *       {@code ApiPathIndexationObserver}, the old (env, apiId) entry would linger until JVM restart — observers
 *       should fail loud rather than silently absorb that case.</li>
 * </ul>
 */
@CustomLog
@DomainService
public class ApiPathIndex {

    private final Map<String, Map<String, List<Path>>> snapshotsByEnv = new ConcurrentHashMap<>();

    public void index(String envId, String apiId, List<Path> paths) {
        var snapshot = snapshotsByEnv.get(envId);
        if (snapshot != null) {
            snapshot.put(apiId, List.copyOf(paths));
        }
    }

    public void remove(String envId, String apiId) {
        var snapshot = snapshotsByEnv.get(envId);
        if (snapshot != null) {
            snapshot.remove(apiId);
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
     * carries the id (no env, no proxy, no listeners) — see
     * {@code SearchEngineServiceImpl.process()} ACTION_DELETE branch.
     */
    public void removeForApi(String apiId) {
        snapshotsByEnv.forEach((envId, snapshot) -> {
            try {
                snapshot.remove(apiId);
            } catch (RuntimeException e) {
                snapshotsByEnv.remove(envId);
                log.warn("removeForApi failed for env=[{}] api=[{}], invalidated env snapshot", envId, apiId, e);
            }
        });
    }

    public List<Validator.Error> findConflicts(String envId, String excludeApiId, List<Path> candidatePaths, Supplier<Stream<Api>> seeder) {
        var snapshot = snapshotsByEnv.computeIfAbsent(envId, key -> seedFrom(key, seeder));

        var errors = new ArrayList<Validator.Error>();
        var candidateWithHost = ApiPathExtractor.getPathsWithHost(candidatePaths);
        var candidateWithoutHost = ApiPathExtractor.getPathsWithoutHost(candidatePaths);

        snapshot.forEach((apiId, existingPaths) -> {
            if (apiId.equals(excludeApiId)) {
                return;
            }
            var existingWithHost = ApiPathExtractor.getPathsWithHost(existingPaths);
            var existingWithoutHost = ApiPathExtractor.getPathsWithoutHost(existingPaths);

            candidateWithHost.forEach((host, hostPaths) ->
                hostPaths.forEach(candidatePath ->
                    ApiPathExtractor.findConflictingPathError(candidatePath, existingWithHost.getOrDefault(host, List.of())).ifPresent(
                        errors::add
                    )
                )
            );
            candidateWithoutHost.forEach(candidatePath ->
                ApiPathExtractor.findConflictingPathError(candidatePath, existingWithoutHost).ifPresent(errors::add)
            );
        });

        return errors;
    }

    private static Map<String, List<Path>> seedFrom(String envId, Supplier<Stream<Api>> seeder) {
        var snapshot = new ConcurrentHashMap<String, List<Path>>();
        try (var apis = seeder.get()) {
            apis.forEach(api -> {
                var paths = ApiPathExtractor.extractPaths(api);
                if (!paths.isEmpty()) {
                    snapshot.put(api.getId(), paths);
                }
            });
        }
        log.info("Seeded path index env=[{}] apiCount=[{}]", envId, snapshot.size());
        return snapshot;
    }
}

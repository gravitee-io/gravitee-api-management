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
package io.gravitee.apim.infra.search;

import io.gravitee.apim.core.api.domain_service.ApiPathExtractor;
import io.gravitee.apim.core.api.domain_service.ApiPathIndexWriter;
import io.gravitee.apim.core.api.model.Path;
import io.gravitee.apim.core.search.IndexationObserver;
import io.gravitee.apim.core.search.model.IndexableApi;
import io.gravitee.rest.api.model.search.Indexable;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.CustomLog;
import org.springframework.stereotype.Service;

/**
 * {@link IndexationObserver} that mirrors {@link io.gravitee.rest.api.model.search.Indexable} events for API entities
 * into the in-memory {@link ApiPathIndex} used by {@code VerifyApiPathDomainService} for path-collision detection.
 *
 * <p>Each call extracts {@code (envId, apiId, paths, updatedAt)} from the {@link Indexable} subtype, then dispatches:
 * empty paths route to {@link ApiPathIndex#remove} (non-HTTP APIs / path-removing updates); non-empty to
 * {@link ApiPathIndex#index}. Hard deletes hit {@link ApiPathIndex#removeForApi} because the cron-rebuilt delete
 * payload carries only the id.</p>
 *
 * <h2>Failure handling</h2>
 * <p>Extraction failures (malformed definition, missing reference) are caught per-event so a single bad row can't
 * silently freeze further dispatches — the event is logged and the index entry for that api is left untouched (the
 * next successful index will repair it; a full reseed via {@link ApiPathIndex#invalidate} also clears any drift).
 * Index/remove failures still invalidate the env snapshot and rethrow to the dispatcher so the next read reseeds
 * from Mongo.</p>
 *
 * <h2>Unknown {@link Indexable} types</h2>
 * <p>API-bearing subclasses not listed in the {@code extract} switch are skipped with a {@code log.warn} the first
 * time each class is seen. A future {@code IndexableApiV5} that compiles and ships without an update here would
 * silently bypass the path index — this warning surfaces that drift in operator logs.</p>
 */
@CustomLog
@Service
public class ApiPathIndexationObserver implements IndexationObserver {

    private final ApiPathIndexWriter apiPathIndex;

    /** Tracks Indexable subclasses we've already warned about, so a high-traffic unknown type doesn't spam logs. */
    private final Set<String> warnedUnknownClasses = ConcurrentHashMap.newKeySet();

    public ApiPathIndexationObserver(ApiPathIndexWriter apiPathIndex) {
        this.apiPathIndex = apiPathIndex;
    }

    @Override
    public void onIndex(Indexable source) {
        ApiPathSnapshot snapshot;
        try {
            snapshot = extract(source).orElse(null);
        } catch (RuntimeException e) {
            // Bad definition / unexpected shape: skip this event rather than blocking the dispatcher. Index state is
            // unchanged; the next successful onIndex for this api will repair it.
            log.warn("Skipping onIndex for [{}] due to extraction failure", source.getClass().getName(), e);
            return;
        }
        if (snapshot == null) {
            return;
        }
        try {
            if (snapshot.paths().isEmpty()) {
                apiPathIndex.remove(snapshot.environmentId(), snapshot.apiId(), snapshot.updatedAt());
            } else {
                apiPathIndex.index(snapshot.environmentId(), snapshot.apiId(), snapshot.paths(), snapshot.updatedAt());
            }
        } catch (RuntimeException e) {
            apiPathIndex.invalidate(snapshot.environmentId());
            throw e;
        }
    }

    @Override
    public void onDelete(Indexable source) {
        // The SearchEngineService broadcast cron rebuilds the Indexable via reflection with only id set
        // (see SearchEngineServiceImpl.process() ACTION_DELETE branch). We have no env/proxy/listeners to look at,
        // so route through the env-agnostic removeForApi which walks every snapshot.
        //
        // delete(locally=false) also fires onDelete once on the originator AND once when the cron picks up its own
        // broadcast — removeForApi is idempotent (a second remove on an already-absent id is a no-op).
        if (!isApiIndexable(source)) {
            return;
        }
        var apiId = source.getId();
        if (apiId == null) {
            log.warn("onDelete received api Indexable with null id (class=[{}]); skipping", source.getClass().getName());
            return;
        }
        apiPathIndex.removeForApi(apiId);
    }

    private static boolean isApiIndexable(Indexable source) {
        return (
            source instanceof IndexableApi || source instanceof io.gravitee.rest.api.model.api.ApiEntity || source instanceof ApiEntity
        );
    }

    private Optional<ApiPathSnapshot> extract(Indexable source) {
        return switch (source) {
            case IndexableApi indexable -> {
                var api = indexable.getApi();
                yield Optional.of(
                    new ApiPathSnapshot(
                        api.getEnvironmentId(),
                        api.getId(),
                        ApiPathExtractor.extractPaths(api),
                        api.getUpdatedAt() == null ? null : api.getUpdatedAt().toInstant()
                    )
                );
            }
            case io.gravitee.rest.api.model.api.ApiEntity v3 -> Optional.of(
                new ApiPathSnapshot(
                    v3.getReferenceId(),
                    v3.getId(),
                    v3.getProxy() == null ? List.of() : ApiPathExtractor.extractPathsFromVirtualHosts(v3.getProxy().getVirtualHosts()),
                    v3.getUpdatedAt() == null ? null : v3.getUpdatedAt().toInstant()
                )
            );
            case ApiEntity v4 -> Optional.of(
                new ApiPathSnapshot(
                    v4.getReferenceId(),
                    v4.getId(),
                    ApiPathExtractor.extractPathsFromV4Listeners(v4.getListeners()),
                    v4.getUpdatedAt() == null ? null : v4.getUpdatedAt().toInstant()
                )
            );
            default -> {
                // Non-API Indexable (PageEntity, UserEntity, ApiProduct, ...) — not our concern.
                // But if a NEW api-bearing Indexable subclass ships without updating this switch, it would silently
                // bypass the path index. We can't statically detect that, so log the unknown class once per type.
                if (looksLikeApiIndexable(source) && warnedUnknownClasses.add(source.getClass().getName())) {
                    log.warn(
                        "Unrecognised Indexable type [{}] in ApiPathIndexationObserver.extract — path index will not be updated for this type until the observer is taught about it",
                        source.getClass().getName()
                    );
                } else {
                    log.debug("onIndex ignoring non-API Indexable class=[{}]", source.getClass().getName());
                }
                yield Optional.empty();
            }
        };
    }

    /**
     * Heuristic check for "this Indexable looks like it might wrap an API but we don't recognise it." Matches by
     * simple class-name suffix so we don't have to keep a static allow-list.
     */
    private static boolean looksLikeApiIndexable(Indexable source) {
        var name = source.getClass().getSimpleName();
        return name.contains("Api") && !name.contains("ApiProduct") && !name.contains("ApiPage");
    }

    private record ApiPathSnapshot(String environmentId, String apiId, List<Path> paths, Instant updatedAt) {}
}

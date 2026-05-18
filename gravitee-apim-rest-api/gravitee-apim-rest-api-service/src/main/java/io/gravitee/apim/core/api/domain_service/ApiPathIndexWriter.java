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

import io.gravitee.apim.core.api.model.Path;
import java.time.Instant;
import java.util.List;

/**
 * Mutator port over the in-memory path index. Only the observer chain (driven by {@code SearchEngineService}) should
 * depend on this side; validators must depend on {@link ApiPathIndexReader} so they cannot inadvertently mutate index
 * state. See {@link ApiPathIndex} for invariants.
 */
public interface ApiPathIndexWriter {
    /**
     * Applies an indexed/updated API to the env snapshot and bumps the watermark. No-op if the env has not been
     * seeded yet — the dropped write is safely recovered by the next read because the API service commits the Mongo
     * row before firing the observer chain (see {@link ApiPathIndex}).
     *
     * @param updatedAt the {@code Api.updatedAt} value as of this write; {@code null} leaves the watermark unchanged.
     */
    void index(String envId, String apiId, List<Path> paths, Instant updatedAt);

    /**
     * Drops the env's snapshot entry for {@code apiId} and bumps the watermark. Used when an API transitioned to
     * having no HTTP paths (path-removing update / non-HTTP listener). For hard deletes route through
     * {@link #removeForApi}.
     */
    void remove(String envId, String apiId, Instant updatedAt);

    /**
     * Removes {@code apiId} from every env snapshot. Used by observers that learn about a hard delete from the
     * broadcast cron — the payload only carries the id. Idempotent: a second call with the same id is a no-op. Does
     * not bump the watermark (no row left to compare); the validator's per-conflict recheck closes the delete-race
     * window.
     */
    void removeForApi(String apiId);

    /**
     * Drops the cached snapshot for {@code envId}. The next read will reseed from the supplier. Used to recover from
     * observer failures that may have left the in-memory state diverged from Mongo.
     */
    void invalidate(String envId);
}

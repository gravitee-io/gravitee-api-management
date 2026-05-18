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

import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.Path;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Read-only port over the in-memory path index. Validators depend on this side to detect collisions; the writer side
 * is separately injected to the observer chain so a validator cannot inadvertently mutate index state.
 */
public interface ApiPathIndexReader {
    /**
     * Scans the env's in-memory path snapshot for conflicts against {@code candidatePaths}, skipping
     * {@code excludeApiId}, and returns the conflicts together with the watermark up to which observer updates have
     * been applied. Triggers a lazy seed via {@code seeder} on first access for the env.
     */
    ApiPathIndex.FindResult findConflicts(String envId, String excludeApiId, List<Path> candidatePaths, Supplier<Stream<Api>> seeder);

    /**
     * Returns an immutable point-in-time view of the env's snapshot together with the watermark up to which observer
     * updates have been applied. Triggers a lazy seed via {@code seeder} on first access for the env.
     */
    ApiPathIndex.Snapshot snapshotOf(String envId, Supplier<Stream<Api>> seeder);
}

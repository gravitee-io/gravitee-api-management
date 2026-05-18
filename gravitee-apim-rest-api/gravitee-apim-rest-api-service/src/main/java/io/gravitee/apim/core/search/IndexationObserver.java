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
package io.gravitee.apim.core.search;

import io.gravitee.rest.api.model.search.Indexable;

/**
 * Side-effect listener invoked by {@code SearchEngineService} when an {@link Indexable} is indexed or deleted.
 *
 * <p>Implementations receive every index operation and every delete operation regardless of caller (modern
 * {@link Indexer} SPI or legacy direct {@code SearchEngineService} callers). For index and for
 * {@code delete(locally=true)}, observers fire <em>before</em> the corresponding Lucene write so downstream state
 * (e.g., the API path index used for collision detection) cannot be left stale by a Lucene failure. For
 * {@code delete(locally=false)}, observers fire immediately on the originator <em>before the broadcast command is
 * sent</em>, but no Lucene write is attempted on the originator — the cron-driven {@code deleteLocally} later
 * invokes {@code onDelete} a second time with an id-only {@link Indexable} rebuilt via reflection. Observer
 * effects on delete must therefore be <strong>idempotent</strong>.</p>
 *
 * <h2>Threading</h2>
 * <p>Origin-side calls run on {@code indexerThreadPoolTaskExecutor} (the {@code @Async} on
 * {@code SearchEngineServiceImpl.index/delete}). Cron-driven calls run on {@code searchIndexerTaskScheduler}, the
 * scheduler that polls the {@code DATA_TO_INDEX} command broadcast. Implementations must be thread-safe.</p>
 *
 * <h2>Failure contract</h2>
 * <p>A thrown {@code RuntimeException} is caught and logged by the dispatcher so that a misbehaving observer cannot
 * block the Lucene write or other observers. Implementations that maintain mutable state (e.g., the path index) are
 * expected to invalidate it themselves on failure so silent drift cannot accumulate. {@code Error}s are not caught
 * by the dispatcher.</p>
 */
public interface IndexationObserver {
    /**
     * Invoked when {@code source} is being indexed. Called before the Lucene index write. The same source may be
     * delivered more than once over time (e.g., re-index after metadata change); implementations must tolerate
     * repeated calls.
     */
    void onIndex(Indexable source);

    /**
     * Invoked when {@code source} is being deleted. Called before the Lucene remove on {@code delete(locally=true)};
     * called once on the originator and once again when the cron picks up the broadcast on
     * {@code delete(locally=false)}. <strong>Must be idempotent</strong> — a second call with the same id must be a
     * no-op so observers like counters and event emitters do not double-fire.
     */
    void onDelete(Indexable source);
}

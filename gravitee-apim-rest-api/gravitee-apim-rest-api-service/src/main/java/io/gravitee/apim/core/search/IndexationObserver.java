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
 * Side-effect listener invoked by {@code SearchEngineService} before every Lucene index/delete attempt.
 *
 * <p>Implementations receive every index/delete operation regardless of caller (modern {@link Indexer} SPI or legacy
 * direct {@code SearchEngineService} callers) and regardless of whether the subsequent Lucene write succeeds. This
 * ordering exists so observers backing critical state (e.g., the API path index used for collision detection) cannot
 * be left stale by a Lucene failure.</p>
 *
 * <p>Observers run on the indexer thread pool. A thrown {@code RuntimeException} is caught and logged by the dispatcher
 * so that a misbehaving observer cannot block the Lucene write; implementations that maintain their own state are
 * expected to invalidate it themselves on failure to avoid silent drift.</p>
 */
public interface IndexationObserver {
    void onIndex(Indexable source);

    void onDelete(Indexable source);
}

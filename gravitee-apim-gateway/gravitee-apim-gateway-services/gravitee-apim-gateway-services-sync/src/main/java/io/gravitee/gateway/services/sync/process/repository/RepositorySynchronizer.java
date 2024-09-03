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
package io.gravitee.gateway.services.sync.process.repository;

import io.reactivex.rxjava3.core.Completable;
import java.util.Set;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface RepositorySynchronizer {
    /**
     * Synchronize the elements retrieving events from the datasource.
     *
     * @param from the beginning timestamp for this synchronization. If <code>-1</code> a <i>full</i> synchronization will be performed, otherwise a <i>incremental</i> synchronization will be made.
     * @param to the end timestamp for this synchronization
     * @param environments the set of environments to filter elements to synchronize
     */
    Completable synchronize(final Long from, final Long to, final Set<String> environments);

    int order();
}

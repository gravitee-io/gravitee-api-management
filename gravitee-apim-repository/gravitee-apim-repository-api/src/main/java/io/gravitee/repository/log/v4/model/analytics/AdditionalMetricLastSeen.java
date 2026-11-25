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
package io.gravitee.repository.log.v4.model.analytics;

import io.gravitee.repository.analytics.engine.api.query.AdditionalMetricField.Root;
import java.time.Instant;

/**
 * An additional metric found on the requests of an environment, under one of the reporter's maps. Its value type is the
 * one its key prefix declares, a keyword under {@link Root#CUSTOM}: a key stored under another type is never discovered.
 *
 * @param root     the map the key is written under
 * @param rawKey   the key under its root, type prefix included for an additional metric ({@code long_failover_count})
 * @param lastSeen the time of the most recent request carrying it within the window
 */
public record AdditionalMetricLastSeen(Root root, String rawKey, Instant lastSeen) {
    /** A key of the {@code additional-metrics} map. */
    public AdditionalMetricLastSeen(String rawKey, Instant lastSeen) {
        this(Root.ADDITIONAL_METRICS, rawKey, lastSeen);
    }

    /** The wire name, which is also the path of the field on a request document. */
    public String name() {
        return root.prefix() + rawKey;
    }
}
